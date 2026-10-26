package ro.ainpc.ai

import java.io.IOException
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpTimeoutException
import java.nio.ByteBuffer
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletionStage
import java.util.concurrent.ExecutionException
import java.util.concurrent.Flow
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

internal object OpenAIHttpTransport {
    private const val MAX_RESPONSE_BYTES = 2L * 1024 * 1024

    @Throws(IOException::class, InterruptedException::class)
    fun send(httpClient: HttpClient, request: HttpRequest): HttpResponse<String> {
        val timeoutNanos = request.timeout().orElseThrow {
            IllegalArgumentException("Cererea OpenAI necesita un timeout.")
        }.toNanos()
        val startedAt = System.nanoTime()
        val exchange = httpClient.sendAsync(request, HttpResponse.BodyHandler { responseInfo ->
            LimitedBodySubscriber(HttpResponse.BodyHandlers.ofString().apply(responseInfo), responseInfo)
        })
        try {
            val remainingNanos = (timeoutNanos - (System.nanoTime() - startedAt)).coerceAtLeast(0L)
            return exchange.get(remainingNanos, TimeUnit.NANOSECONDS)
        } catch (exception: TimeoutException) {
            exchange.cancel(true)
            throw HttpTimeoutException("Cererea OpenAI a depasit timpul limita.").apply { initCause(exception) }
        } catch (exception: InterruptedException) {
            exchange.cancel(true)
            throw exception
        } catch (exception: CancellationException) {
            throw IOException("Cererea HTTP OpenAI a fost anulata.", exception)
        } catch (exception: ExecutionException) {
            when (val cause = exception.cause) {
                is IOException -> throw cause
                is InterruptedException -> throw cause
                is CancellationException -> throw IOException("Cererea HTTP OpenAI a fost anulata.", cause)
                is RuntimeException -> throw cause
                is Error -> throw cause
                else -> throw IOException("Cererea HTTP OpenAI a esuat.", cause ?: exception)
            }
        }
    }

    private class LimitedBodySubscriber(
        private val delegate: HttpResponse.BodySubscriber<String>,
        private val responseInfo: HttpResponse.ResponseInfo,
    ) : HttpResponse.BodySubscriber<String> {
        private var subscription: Flow.Subscription? = null
        private var receivedBytes = 0L
        private var terminal = false

        override fun getBody(): CompletionStage<String> = delegate.body

        override fun onSubscribe(subscription: Flow.Subscription) {
            if (this.subscription != null || terminal) {
                subscription.cancel()
                return
            }
            this.subscription = subscription
            delegate.onSubscribe(subscription)
        }

        override fun onNext(buffers: List<ByteBuffer>) {
            if (terminal) return
            for (buffer in buffers) {
                val remaining = buffer.remaining().toLong()
                if (remaining > MAX_RESPONSE_BYTES - receivedBytes) {
                    terminal = true
                    subscription?.cancel()
                    delegate.onError(limitFailure())
                    return
                }
                receivedBytes += remaining
            }
            delegate.onNext(buffers)
        }

        override fun onError(throwable: Throwable) {
            if (terminal) return
            terminal = true
            delegate.onError(throwable)
        }

        override fun onComplete() {
            if (terminal) return
            terminal = true
            delegate.onComplete()
        }

        private fun limitFailure(): IOException {
            val message = "Raspunsul HTTP OpenAI a depasit limita de $MAX_RESPONSE_BYTES octeti."
            val status = responseInfo.statusCode()
            return if (status in 200..299) IOException(message) else OpenAIHttpException(
                status, "", responseInfo.headers().firstValue("Retry-After").orElse(null), "HTTP $status - $message",
            )
        }
    }
}
