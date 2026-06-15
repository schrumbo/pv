package schrumbo.pv.util

/**
 * XP-to-level math for Skyblock skills and slayers. Skill tables are per-level increments;
 * overflow continues past the cap at a constant cost equal to the cap's last increment
 * (`level = cap + remainingXp / table[cap]`), matching the community overflow standard.
 */
object Leveling {

    /** Per-level skill XP increments, levels 1..60. Verified: cumulative L50 = 55_172_425, L60 = 111_672_425. */
    val SKILL_XP = longArrayOf(
        50, 125, 200, 300, 500, 750, 1000, 1500, 2000, 3500,
        5000, 7500, 10000, 15000, 20000, 30000, 50000, 75000, 100000, 200000,
        300000, 400000, 500000, 600000, 700000, 800000, 900000, 1000000, 1100000, 1200000,
        1300000, 1400000, 1500000, 1600000, 1700000, 1800000, 1900000, 2000000, 2100000, 2200000,
        2300000, 2400000, 2500000, 2600000, 2750000, 2900000, 3100000, 3400000, 3700000, 4000000,
        4300000, 4600000, 4900000, 5200000, 5500000, 5800000, 6100000, 6400000, 6700000, 7000000,
    )

    /** Per-level runecrafting XP increments, levels 1..25. */
    val RUNECRAFTING_XP = longArrayOf(
        50, 100, 125, 160, 200, 250, 315, 400, 500, 625,
        785, 1000, 1250, 1600, 2000, 2465, 3125, 4000, 5000, 6200,
        7800, 9800, 12200, 15300, 19050,
    )

    /** Per-level Catacombs (and dungeon class) XP increments, levels 1..50. Cumulative L50 = 569_809_640. */
    val CATACOMBS_XP = longArrayOf(
        50, 75, 110, 160, 230, 330, 470, 670, 950, 1340,
        1890, 2665, 3760, 5260, 7380, 10300, 14400, 20000, 27600, 38000,
        52500, 71500, 97000, 132000, 180000, 243000, 328000, 445000, 600000, 800000,
        1065000, 1410000, 1900000, 2500000, 3300000, 4300000, 5600000, 7200000, 9200000, 12000000,
        15000000, 19000000, 24000000, 30000000, 38000000, 48000000, 60000000, 75000000, 93000000, 116250000,
    )

    /** Cumulative slayer XP thresholds per boss; slayer level = count of thresholds reached. */
    private val SLAYER_THRESHOLDS = mapOf(
        "zombie" to longArrayOf(5, 15, 200, 1000, 5000, 20000, 100000, 400000, 1000000),
        "spider" to longArrayOf(5, 25, 200, 1000, 5000, 20000, 100000, 400000, 1000000),
        "wolf" to longArrayOf(10, 30, 250, 1500, 5000, 20000, 100000, 400000, 1000000),
        "enderman" to longArrayOf(10, 30, 250, 1500, 5000, 20000, 100000, 400000, 1000000),
        "blaze" to longArrayOf(10, 30, 250, 1500, 5000, 20000, 100000, 400000, 1000000),
        "vampire" to longArrayOf(20, 75, 240, 840, 2400),
    )

    /** A resolved level: integer [level], [fractional] (incl. overflow), and [progress] 0..1 to the next. */
    data class Level(
        val level: Int,
        val fractional: Double,
        val totalXp: Long,
        val cap: Int,
        val maxed: Boolean,
        val progress: Double,
        val xpToNext: Long,
    )

    /** Flat XP cost per Catacombs/class level past 50 (SoopyV2 dungeon overflow). */
    const val DUNGEON_OVERFLOW_STEP = 200_000_000L

    /**
     * Resolves a skill level from [totalXp] using the full [table]. With [overflow], levels continue
     * past the table end: when [overflowStep] is given each further level costs that flat amount
     * (Catacombs/dungeon classes, 200M); otherwise the SoopyV2 skill formula is used — each level
     * beyond starts at `table.last + 600_000`, the step grows by `600_000` and doubles every 10.
     */
    fun skill(totalXp: Long, table: LongArray, overflow: Boolean = true, overflowStep: Long? = null): Level {
        val cap = table.size
        var remaining = totalXp
        var level = 0
        for (i in 0 until cap) {
            val need = table[i]
            if (remaining >= need) {
                remaining -= need
                level = i + 1
            } else {
                val frac = remaining.toDouble() / need
                return Level(level, level + frac, totalXp, cap, false, frac, need - remaining)
            }
        }
        if (overflow) {
            if (overflowStep != null) {
                while (remaining > overflowStep) {
                    level++
                    remaining -= overflowStep
                }
                val frac = remaining.toDouble() / overflowStep
                return Level(level, level + frac, totalXp, cap, true, frac, overflowStep - remaining)
            }
            var slope = 600_000L
            var xpForNext = table.last() + slope
            while (remaining > xpForNext) {
                level++
                remaining -= xpForNext
                xpForNext += slope
                if (level % 10 == 0) slope *= 2
            }
            val frac = remaining.toDouble() / xpForNext
            return Level(level, level + frac, totalXp, cap, true, frac, xpForNext - remaining)
        }
        return Level(cap, cap.toDouble(), totalXp, cap, true, 1.0, 0)
    }

    /** Resolves a slayer level (and progress) for [boss] from its cumulative thresholds. */
    fun slayer(totalXp: Long, boss: String): Level {
        val t = SLAYER_THRESHOLDS[boss.lowercase()] ?: return Level(0, 0.0, totalXp, 0, false, 0.0, 0)
        val max = t.size
        var level = 0
        while (level < max && totalXp >= t[level]) level++
        if (level >= max) return Level(max, max.toDouble(), totalXp, max, true, 1.0, 0)
        val floor = if (level == 0) 0L else t[level - 1]
        val next = t[level]
        val span = next - floor
        val into = totalXp - floor
        val frac = if (span > 0) into.toDouble() / span else 0.0
        return Level(level, level + frac, totalXp, max, false, frac, next - totalXp)
    }
}
