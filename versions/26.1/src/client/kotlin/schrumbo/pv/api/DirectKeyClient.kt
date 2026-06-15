package schrumbo.pv.api

import com.google.gson.Gson
import com.google.gson.JsonObject
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Hypixel client backed by the embedded developer key. Shares the dev rate limit, so a
 * token-bucket limiter guards every call. The key is loaded from a gitignored local file.
 */
object DirectKeyClient : HypixelDataSource {

    private const val BASE = "https://api.hypixel.net"

    private val gson = Gson()
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
    private val limiter = RateLimiter()

    override fun skyblockProfiles(uuid: String): JsonObject = get("/v2/skyblock/profiles", "uuid", uuid)

    override fun status(uuid: String): JsonObject = get("/v2/status", "uuid", uuid)

    override fun guild(uuid: String): JsonObject = get("/v2/guild", "player", uuid)

    override fun player(uuid: String): JsonObject = get("/v2/player", "uuid", uuid)

    private fun get(path: String, paramName: String, paramValue: String): JsonObject {
        val key = Secrets.hypixelKey
            ?: throw HypixelException("No Hypixel API key configured (config/pv-secrets.properties).")
        if (!limiter.tryAcquire()) throw RateLimitedException("Local rate limit reached, try again shortly.")

        val req = HttpRequest.newBuilder()
            .uri(URI.create("$BASE$path?$paramName=$paramValue"))
            .header("API-Key", key)
            .timeout(Duration.ofSeconds(15))
            .GET()
            .build()

        val resp = runCatching { http.send(req, HttpResponse.BodyHandlers.ofString()) }
            .getOrElse { throw HypixelException("Request to $path failed: ${it.message}") }

        when (resp.statusCode()) {
            in 200..299 -> return gson.fromJson(resp.body(), JsonObject::class.java)
            429 -> throw RateLimitedException("Hypixel rate limit (HTTP 429) on $path.")
            else -> throw HypixelException("Hypixel returned HTTP ${resp.statusCode()} on $path.")
        }
    }
}
