package schrumbo.pv.api

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.mojang.authlib.GameProfile
import schrumbo.pv.util.SkinProfile
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Resolves Minecraft usernames to undashed UUIDs and fetches skin profiles, cached for the session. */
object MojangApi {

    private val gson = Gson()
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
    private val cache = ConcurrentHashMap<String, String>()
    private val profiles = ConcurrentHashMap<String, GameProfile>()

    /** Blocking — call off the main thread. Returns the undashed UUID, or null if unknown. */
    fun resolveUuid(name: String): String? {
        val key = name.lowercase()
        cache[key]?.let { return it }
        return runCatching {
            val req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.mojang.com/users/profiles/minecraft/$name"))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build()
            val resp = http.send(req, HttpResponse.BodyHandlers.ofString())
            if (resp.statusCode() != 200) return null
            val id = gson.fromJson(resp.body(), JsonObject::class.java).get("id")?.asString ?: return null
            cache[key] = id
            id
        }.getOrNull()
    }

    /** Blocking — fetches a [GameProfile] (incl. skin texture property) by undashed UUID, cached. */
    fun fetchProfile(undashedUuid: String): GameProfile? {
        profiles[undashedUuid]?.let { return it }
        return runCatching {
            val req = HttpRequest.newBuilder()
                .uri(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/$undashedUuid?unsigned=false"))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build()
            val resp = http.send(req, HttpResponse.BodyHandlers.ofString())
            if (resp.statusCode() != 200) return null
            val json = gson.fromJson(resp.body(), JsonObject::class.java)
            val name = json.get("name")?.asString ?: return null
            val textures = json.getAsJsonArray("properties")
                ?.map { it.asJsonObject }
                ?.firstOrNull { it.get("name")?.asString == "textures" }
            val value = textures?.get("value")?.asString ?: return null
            val signature = textures.get("signature")?.asString
            val profile = SkinProfile.withTextures(dash(undashedUuid), name, value, signature)
            profiles[undashedUuid] = profile
            profile
        }.getOrNull()
    }

    private fun dash(u: String): UUID =
        UUID.fromString(u.replaceFirst(Regex("(.{8})(.{4})(.{4})(.{4})(.{12})"), "$1-$2-$3-$4-$5"))
}
