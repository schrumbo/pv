package schrumbo.pv.data

import schrumbo.pv.util.Leveling

/** One dungeon class (Healer/Mage/Berserk/Archer/Tank) with its computed level. */
data class DungeonClass(val name: String, val level: Leveling.Level)

/** Completion counts for one floor index (0 = Entrance) in Normal and Master mode. */
data class FloorStat(val floor: Int, val completions: Long, val masterCompletions: Long)

/** Resolved Catacombs data: level, class levels + average, selected class, and per-floor completions. */
data class DungeonData(
    val catacombs: Leveling.Level,
    val classes: List<DungeonClass>,
    val classAverage: Double,
    val selectedClass: String?,
    val floors: List<FloorStat>,
)
