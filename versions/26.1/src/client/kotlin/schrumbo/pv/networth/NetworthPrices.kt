package schrumbo.pv.networth

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Loads the SkyHelper price list (`pricesV2.json`, id → coins). Cached with a 5-minute TTL.
 * This is the same source the reference SkyHelper-Networth library uses.
 */
object NetworthPrices {

    private const val URL = "https://raw.githubusercontent.com/SkyHelperBot/Prices/main/pricesV2.json"
    private const val TTL_MILLIS = 5 * 60 * 1000L

    private val gson = Gson()
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
    private val type = object : TypeToken<Map<String, Double>>() {}.type

    @Volatile private var cache: Map<String, Double> = emptyMap()
    @Volatile private var fetchedAt = 0L

    /** Blocking — returns the price map, refreshing it when the cache is stale. */
    fun get(): Map<String, Double> {
        if (cache.isNotEmpty() && System.currentTimeMillis() - fetchedAt < TTL_MILLIS) return cache
        return runCatching {
            val req = HttpRequest.newBuilder().uri(URI.create(URL)).timeout(Duration.ofSeconds(20)).GET().build()
            val resp = http.send(req, HttpResponse.BodyHandlers.ofString())
            if (resp.statusCode() != 200) return cache
            val parsed: Map<String, Double> = gson.fromJson(resp.body(), type)
            cache = parsed
            fetchedAt = System.currentTimeMillis()
            parsed
        }.getOrDefault(cache)
    }
}
