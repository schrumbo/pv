package schrumbo.pv.util

import kotlin.math.sqrt

/** Hypixel-wide stat helpers (network level, island names). */
object HypixelStats {

    /** Exact Hypixel network level from total network XP (BASE 10000, GROWTH 2500). */
    fun networkLevel(networkExp: Double): Int = (-3.5 + sqrt(12.25 + 0.0008 * networkExp)).toInt()

    /** Friendly island name from a `/status` `session.mode`, prettified when unmapped. */
    fun islandName(mode: String?): String? {
        if (mode.isNullOrBlank()) return null
        return ISLANDS[mode] ?: mode.split("_").joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
    }

    private val ISLANDS = mapOf(
        "dynamic" to "Private Island",
        "hub" to "Hub",
        "mining_1" to "Gold Mine",
        "mining_2" to "Deep Caverns",
        "mining_3" to "Dwarven Mines",
        "crystal_hollows" to "Crystal Hollows",
        "combat_1" to "Spider's Den",
        "combat_3" to "The End",
        "farming_1" to "The Barn",
        "farming_2" to "Mushroom Desert",
        "foraging_1" to "The Park",
        "winter" to "Jerry's Workshop",
        "dungeon_hub" to "Dungeon Hub",
        "dungeon" to "Dungeon",
        "garden" to "The Garden",
        "rift" to "The Rift",
        "crimson_isle" to "Crimson Isle",
        "instanced" to "Kuudra",
    )
}
