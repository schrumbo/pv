package schrumbo.pv.data

import schrumbo.pv.util.Leveling

/** One dungeon class (Healer/Mage/Berserk/Archer/Tank) with its icon item and computed level. */
data class DungeonClass(val name: String, val icon: String, val level: Leveling.Level)

/** A floor's best run: combined [score], its [timestamp], and the fastest S+ clear time in millis. */
data class FloorRun(val score: Int, val timestamp: Long, val sPlusTimeMs: Long?)

/** Completion counts for one floor index (0 = Entrance) plus the best run, in Normal and Master mode. */
data class FloorStat(
    val floor: Int,
    val completions: Long,
    val masterCompletions: Long,
    val normalBest: FloorRun?,
    val masterBest: FloorRun?,
)

/** Resolved Catacombs data: level, class levels + average, floors, and global secrets/run stats. */
data class DungeonData(
    val catacombs: Leveling.Level,
    val classes: List<DungeonClass>,
    val classAverage: Double,
    val selectedClass: String?,
    val floors: List<FloorStat>,
    val secrets: Long,
    val totalRuns: Long,
    val highestFloorNormal: Int,
    val highestFloorMaster: Int,
) {
    /** Lifetime secrets divided by total runs (Normal + Master), 0 when no runs. */
    val secretsPerRun: Double get() = if (totalRuns > 0) secrets.toDouble() / totalRuns else 0.0
}
