package schrumbo.pv.api

import com.google.gson.JsonObject

/**
 * Abstraction over Hypixel data access. [DirectKeyClient] uses an embedded developer key now;
 * a server-side `ProxyClient` can replace it later without touching feature code.
 * All calls are blocking and must run off the main thread.
 */
interface HypixelDataSource {

    /** `GET /v2/skyblock/profiles?uuid=` — all Skyblock profiles for the player. */
    fun skyblockProfiles(uuid: String): JsonObject

    /** `GET /v2/status?uuid=` — online status and current session location. */
    fun status(uuid: String): JsonObject

    /** `GET /v2/guild?player=` — the player's guild, or an empty guild payload. */
    fun guild(uuid: String): JsonObject

    /** `GET /v2/player?uuid=` — global player data (network level, etc.). */
    fun player(uuid: String): JsonObject
}

/** Thrown when the rate limiter rejects a call or Hypixel returns HTTP 429. */
class RateLimitedException(message: String) : RuntimeException(message)

/** Thrown for any other non-2xx Hypixel response or transport failure. */
class HypixelException(message: String) : RuntimeException(message)
