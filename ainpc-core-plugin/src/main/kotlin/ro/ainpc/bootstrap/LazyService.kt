package ro.ainpc.bootstrap

import java.util.function.Supplier
import java.util.logging.Logger

class LazyService<T> private constructor(
    private val name: String,
    private val factory: Supplier<T>
) {
    @Volatile
    private var instance: T? = null

    private val logger = Logger.getLogger(LazyService::class.java.name)

    fun get(): T {
        val cached = instance
        if (cached != null) return cached
        synchronized(this) {
            val doubleCheck = instance
            if (doubleCheck != null) return doubleCheck
            logger.fine("[LazyService] Initializing $name...")
            val created = factory.get()
            instance = created
            logger.info("[LazyService] $name initialized.")
            return created
        }
    }

    fun isInitialized(): Boolean = instance != null

    fun reset() {
        synchronized(this) {
            instance = null
        }
    }

    companion object {
        @JvmStatic
        fun <T> of(name: String, factory: () -> T): LazyService<T> {
            return LazyService(name, Supplier { factory() })
        }
    }
}
