package schrumbo.pv.api

/** Token bucket: [capacity] tokens refilled smoothly over [windowMillis]. Thread-safe, non-blocking. */
class RateLimiter(
    private val capacity: Int = 300,
    private val windowMillis: Long = 5 * 60 * 1000,
) {
    private var tokens = capacity.toDouble()
    private var last = System.currentTimeMillis()
    private val lock = Any()

    /** Takes one token if available. Returns false when the limit is exhausted. */
    fun tryAcquire(): Boolean = synchronized(lock) {
        refill()
        if (tokens >= 1.0) {
            tokens -= 1.0
            true
        } else {
            false
        }
    }

    private fun refill() {
        val now = System.currentTimeMillis()
        val elapsed = now - last
        if (elapsed <= 0) return
        tokens = minOf(capacity.toDouble(), tokens + elapsed * capacity.toDouble() / windowMillis)
        last = now
    }
}
