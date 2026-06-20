package schrumbo.pv.data

/** A single mob's kills or deaths count, with a prettified display name. */
data class MobCount(val name: String, val count: Long)

/** Per-mob kills & deaths from `player_stats`, for the Combat → Mobs sub-page. */
data class MobStats(
    val kills: List<MobCount>,
    val deaths: List<MobCount>,
    val totalKills: Long,
    val totalDeaths: Long,
)

/** One Kuudra tier's completions and highest reached wave. */
data class KuudraTier(val id: String, val name: String, val completions: Int, val highestWave: Int)

/** One Dojo discipline's points and best time (ms; -1 when untried). */
data class DojoTest(val id: String, val name: String, val points: Int, val timeMs: Int) {
    /** Hypixel letter grade by points. */
    val grade: String get() = when {
        points >= 1000 -> "S"
        points >= 800 -> "A"
        points >= 600 -> "B"
        points >= 400 -> "C"
        points >= 200 -> "D"
        points > 0 -> "F"
        else -> "—"
    }
}

/** Crimson Isle (`nether_island_player_data`) summary for the Combat → Crimson Isle sub-page. */
data class CrimsonIsleData(
    val selectedFaction: String?,
    val mageReputation: Int,
    val barbarianReputation: Int,
    val kuudra: List<KuudraTier>,
    val dojo: List<DojoTest>,
)

/** Everything behind the Combat tab beyond the bestiary: mob kills/deaths and Crimson Isle. */
data class CombatData(
    val mobs: MobStats,
    val crimson: CrimsonIsleData,
)
