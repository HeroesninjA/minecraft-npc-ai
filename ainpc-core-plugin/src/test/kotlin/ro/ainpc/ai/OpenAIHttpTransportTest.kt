package ro.ainpc.ai

import com.google.gson.Gson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.Authenticator
import java.net.ConnectException
import java.net.CookieHandler
import java.net.ProxySelector
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpTimeoutException
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.Optional
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.ExecutionException
import java.util.concurrent.Flow
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLParameters

class OpenAIHttpTransportTest {
    private val responseLimitBytes = 2 * 1024 * 1024

    @Test
    fun `expired deadline cancels the exchange with interruption requested`() {
        val exchange = TrackingFuture()
        val client = StubHttpClient(exchange)

        val failure = assertThrows(HttpTimeoutException::class.java) {
            OpenAIHttpTransport.send(client, request(Duration.ofMillis(25)))
        }

        assertTrue(exchange.isCancelled)
        assertEquals(true, exchange.interruptionRequested)
        assertTrue(OpenAITextSupport.shouldEnterOfflineBackoff(failure))
    }

    @Test
    fun `caller interruption cancels the exchange without becoming a network outage`() {
        val exchange = TrackingFuture()
        val client = StubHttpClient(exchange)
        val failure = AtomicReference<Throwable>()
        val caller = Thread {
            try {
                OpenAIHttpTransport.send(client, request())
            } catch (exception: Throwable) {
                failure.set(exception)
            }
        }.apply { isDaemon = true }
        caller.start()
        try {
            assertTrue(client.started.await(2L, TimeUnit.SECONDS))
            caller.interrupt()
            caller.join(2_000L)

            assertFalse(caller.isAlive)
            assertTrue(failure.get() is InterruptedException)
            assertTrue(exchange.isCancelled)
            assertEquals(true, exchange.interruptionRequested)
            assertFalse(OpenAITextSupport.shouldEnterOfflineBackoff(IOException("Interrupted", failure.get())))
        } finally {
            caller.interrupt()
        }
    }

    @Test
    fun `asynchronous IO failures retain their identity and backoff classification`() {
        for (failure in listOf(HttpTimeoutException("Timed out"), ConnectException("Connection refused"))) {
            val client = StubHttpClient(CompletableFuture.failedFuture(failure))

            assertSame(failure, assertThrows(IOException::class.java) { OpenAIHttpTransport.send(client, request()) })
            assertTrue(OpenAITextSupport.shouldEnterOfflineBackoff(failure))
        }
    }

    @Test
    fun `cancelled exchanges remain IO failures without entering network backoff`() {
        val exchange = TrackingFuture().apply { cancel(true) }

        val failure = assertThrows(IOException::class.java) {
            OpenAIHttpTransport.send(StubHttpClient(exchange), request())
        }

        assertTrue(failure.cause is CancellationException)
        assertFalse(OpenAITextSupport.shouldEnterOfflineBackoff(failure))
    }

    @Test
    fun `wrapped cancellation remains distinct from timeout`() {
        val cancellation = CancellationException("Canceled")
        val client = StubHttpClient(CompletableFuture.failedFuture(CompletionException(cancellation)))

        val failure = assertThrows(IOException::class.java) { OpenAIHttpTransport.send(client, request()) }

        assertSame(cancellation, failure.cause)
        assertFalse(OpenAITextSupport.shouldEnterOfflineBackoff(failure))
    }

    @Test
    fun `unexpected runtime failures are not hidden as IO failures`() {
        val failure = IllegalStateException("Invalid transport state")
        val client = StubHttpClient(CompletableFuture.failedFuture(failure))

        assertSame(failure, assertThrows(IllegalStateException::class.java) { OpenAIHttpTransport.send(client, request()) })
    }

    @Test
    fun `requests without deadlines are rejected before starting an exchange`() {
        val client = StubHttpClient(TrackingFuture())
        val request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:1/v1/responses")).build()

        assertThrows(IllegalArgumentException::class.java) { OpenAIHttpTransport.send(client, request) }
        assertEquals(1L, client.started.count)
    }

    @Test
    fun `legacy five argument model probe keeps a finite default deadline`() {
        val previousProbeResult = OpenAIConnectionProbe.getLastProbeResult()
        val client = StubHttpClient(CompletableFuture.failedFuture(IOException("Local simulated failure")))
        try {
            val status = OpenAIConnectionProbe.probeConnection("test-model", "http://127.0.0.1:1/v1", "test-key", client, Gson())

            assertFalse(status.isReachable())
            assertEquals(Duration.ofSeconds(120), client.lastRequest?.timeout()?.orElse(null))
        } finally {
            OpenAIConnectionProbe::class.java.getDeclaredField("lastProbeResult").apply {
                isAccessible = true
                set(null, previousProbeResult)
            }
        }
    }

    @Test
    fun `body exactly at the byte limit remains complete`() {
        val (subscriber, subscription) = subscriber()
        val body = "a".repeat(responseLimitBytes)

        subscriber.onNext(listOf(ByteBuffer.wrap(body.toByteArray())))
        subscriber.onComplete()

        assertEquals(body, subscriber.body.toCompletableFuture().get(1L, TimeUnit.SECONDS))
        assertFalse(subscription.cancelled)
    }

    @Test
    fun `oversized single chunk is cancelled without returning partial text`() {
        val (subscriber, subscription) = subscriber()

        subscriber.onNext(listOf(ByteBuffer.wrap(ByteArray(responseLimitBytes + 1) { 'a'.code.toByte() })))
        subscriber.onComplete()

        val failure = bodyFailure(subscriber)
        assertTrue(failure.message.orEmpty().contains("limita"))
        assertNull(failure.cause)
        assertTrue(subscription.cancelled)
        assertFalse(OpenAITextSupport.shouldEnterOfflineBackoff(failure))
    }

    @Test
    fun `byte limit is cumulative across callbacks and buffers`() {
        val (subscriber, subscription) = subscriber()
        subscriber.onNext(listOf(ByteBuffer.wrap(ByteArray(responseLimitBytes - 2))))

        subscriber.onNext(listOf(ByteBuffer.wrap(byteArrayOf(1)), ByteBuffer.wrap(byteArrayOf(2, 3))))
        subscriber.onComplete()

        bodyFailure(subscriber)
        assertTrue(subscription.cancelled)
    }

    @Test
    fun `only buffer remaining bytes count toward the limit`() {
        val (subscriber, subscription) = subscriber()
        val sliced = ByteBuffer.wrap("ignoredabcignored".toByteArray()).apply { position(7); limit(10) }.asReadOnlyBuffer()
        subscriber.onNext(listOf(sliced, ByteBuffer.wrap(ByteArray(responseLimitBytes - 3) { 'x'.code.toByte() })))
        subscriber.onComplete()

        val text = subscriber.body.toCompletableFuture().get(1L, TimeUnit.SECONDS)
        assertEquals(responseLimitBytes, text.length)
        assertTrue(text.startsWith("abc"))
        assertFalse(subscription.cancelled)
    }

    @Test
    fun `byte cap cannot be bypassed by multibyte text below the character limit`() {
        val (subscriber, subscription) = subscriber()
        val bytes = "ă".repeat(responseLimitBytes / 2 + 1).toByteArray(StandardCharsets.UTF_8)

        subscriber.onNext(listOf(ByteBuffer.wrap(bytes)))
        subscriber.onComplete()

        bodyFailure(subscriber)
        assertTrue(subscription.cancelled)
    }

    @Test
    fun `normal charset decoding remains delegated to the JDK handler`() {
        val (subscriber, _) = subscriber(headers = mapOf("Content-Type" to listOf("text/plain; charset=ISO-8859-1")))
        subscriber.onNext(listOf(ByteBuffer.wrap("Café".toByteArray(StandardCharsets.ISO_8859_1))))
        subscriber.onComplete()

        assertEquals("Café", subscriber.body.toCompletableFuture().get(1L, TimeUnit.SECONDS))
    }

    @Test
    fun `oversized error bodies retain HTTP status and Retry After but not body error codes`() {
        val (subscriber, subscription) = subscriber(429, mapOf("Retry-After" to listOf("600")))
        subscriber.onNext(listOf(ByteBuffer.wrap(ByteArray(responseLimitBytes + 1))))
        subscriber.onComplete()

        val failure = bodyFailure(subscriber)
        assertTrue(failure is OpenAIHttpException)
        failure as OpenAIHttpException
        assertEquals(429, failure.statusCode)
        assertEquals("600", failure.retryAfter)
        assertEquals("", failure.errorCode)
        assertTrue(subscription.cancelled)
        assertTrue(OpenAIHttpBackoff.delayMillis(429, failure.errorCode, failure.retryAfter, 0L, 15_000L)!! >= 600_000L)
    }

    @Test
    fun `body limit error stays terminal after late signals and never exposes the bytes`() {
        val (subscriber, subscription) = subscriber()
        subscriber.onNext(listOf(ByteBuffer.wrap("PRIVATE_BODY_DETAIL".toByteArray())))
        subscriber.onNext(listOf(ByteBuffer.wrap(ByteArray(responseLimitBytes))))
        subscriber.onNext(listOf(ByteBuffer.wrap("PRIVATE_LATE_DETAIL".toByteArray())))
        subscriber.onError(IOException("PRIVATE_LATE_ERROR"))
        subscriber.onComplete()

        val failure = bodyFailure(subscriber)
        assertFalse(failure.toString().contains("PRIVATE_"))
        assertNull(failure.cause)
        assertEquals(1, subscription.cancellations)
    }

    @Test
    fun `ordinary body IO failures retain their identity`() {
        val (subscriber, _) = subscriber()
        val failure = IOException("Local simulated read failure")

        subscriber.onError(failure)

        assertSame(failure, bodyFailure(subscriber))
    }

    private fun bodyFailure(subscriber: HttpResponse.BodySubscriber<String>): IOException {
        val failure = assertThrows(ExecutionException::class.java) {
            subscriber.body.toCompletableFuture().get(1L, TimeUnit.SECONDS)
        }.cause
        assertTrue(failure is IOException)
        return failure as IOException
    }

    private fun subscriber(
        status: Int = 200, headers: Map<String, List<String>> = emptyMap(),
    ): Pair<HttpResponse.BodySubscriber<String>, TrackingSubscription> {
        val captureFailure = IOException("Capture handler without a network exchange")
        val client = StubHttpClient(CompletableFuture.failedFuture(captureFailure))
        assertSame(captureFailure, assertThrows(IOException::class.java) { OpenAIHttpTransport.send(client, request()) })
        @Suppress("UNCHECKED_CAST")
        val handler = client.lastBodyHandler as HttpResponse.BodyHandler<String>
        val subscriber = handler.apply(object : HttpResponse.ResponseInfo {
            override fun statusCode(): Int = status
            override fun headers(): HttpHeaders = HttpHeaders.of(headers) { _, _ -> true }
            override fun version(): HttpClient.Version = HttpClient.Version.HTTP_1_1
        })
        val subscription = TrackingSubscription()
        subscriber.onSubscribe(subscription)
        return subscriber to subscription
    }

    private class TrackingSubscription : Flow.Subscription {
        var cancellations = 0
        val cancelled: Boolean get() = cancellations > 0
        override fun request(count: Long) = Unit
        override fun cancel() { cancellations++ }
    }

    private fun request(timeout: Duration = Duration.ofSeconds(30)): HttpRequest =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:1/v1/responses")).timeout(timeout).build()

    private class TrackingFuture : CompletableFuture<HttpResponse<String>>() {
        var interruptionRequested: Boolean? = null

        override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
            interruptionRequested = mayInterruptIfRunning
            return super.cancel(mayInterruptIfRunning)
        }
    }

    private class StubHttpClient(private val exchange: CompletableFuture<HttpResponse<String>>) : HttpClient() {
        val started = CountDownLatch(1)
        var lastRequest: HttpRequest? = null
        var lastBodyHandler: HttpResponse.BodyHandler<*>? = null

        override fun cookieHandler(): Optional<CookieHandler> = Optional.empty()
        override fun connectTimeout(): Optional<Duration> = Optional.empty()
        override fun followRedirects(): Redirect = Redirect.NEVER
        override fun proxy(): Optional<ProxySelector> = Optional.empty()
        override fun sslContext(): SSLContext = SSLContext.getDefault()
        override fun sslParameters(): SSLParameters = SSLParameters()
        override fun authenticator(): Optional<Authenticator> = Optional.empty()
        override fun version(): Version = Version.HTTP_1_1
        override fun executor(): Optional<Executor> = Optional.empty()

        override fun <Body> send(request: HttpRequest, handler: HttpResponse.BodyHandler<Body>): HttpResponse<Body> =
            throw AssertionError("Synchronous HTTP transport must not be used")

        @Suppress("UNCHECKED_CAST")
        override fun <Body> sendAsync(request: HttpRequest, handler: HttpResponse.BodyHandler<Body>): CompletableFuture<HttpResponse<Body>> {
            lastRequest = request
            lastBodyHandler = handler
            started.countDown()
            return exchange as CompletableFuture<HttpResponse<Body>>
        }

        override fun <Body> sendAsync(
            request: HttpRequest, handler: HttpResponse.BodyHandler<Body>, pushPromiseHandler: HttpResponse.PushPromiseHandler<Body>?,
        ): CompletableFuture<HttpResponse<Body>> = sendAsync(request, handler)
    }
}
