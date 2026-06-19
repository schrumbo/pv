package schrumbo.pv.ui.page

import schrumbo.pv.data.SkyblockProfile
import schrumbo.pv.ui.Theme
import schrumbo.pv.ui.component.Column
import schrumbo.pv.ui.component.Component
import schrumbo.pv.util.Format

/** Farming page: Jacob's contest medals (diamond is best). */
object FarmingPage {

    private const val DIAMOND = 0xFF4DD0E1.toInt()
    private const val PLATINUM = 0xFFB9D6E0.toInt()
    private const val GOLD = 0xFFFFB534.toInt()
    private const val SILVER = 0xFFD8D8D8.toInt()
    private const val BRONZE = 0xFFCD7F32.toInt()

    fun build(p: SkyblockProfile, width: Int): Component {
        val j = p.jacobs
        return Column(
            PageKit.skillHeader(p, schrumbo.pv.data.SkillType.FARMING, width, "${j.contests} Jacob's contests played"),
            PageKit.section(
                "MEDALS",
                width,
                Column(
                    PageKit.tileRow(
                        width,
                        listOf(
                            "Diamond" to ("${j.diamond}" to DIAMOND),
                            "Platinum" to ("${j.platinum}" to PLATINUM),
                            "Gold" to ("${j.gold}" to GOLD),
                        ),
                    ),
                    PageKit.tileRow(
                        width,
                        listOf(
                            "Silver" to ("${j.silver}" to SILVER),
                            "Bronze" to ("${j.bronze}" to BRONZE),
                            "Contests" to (Format.compact(j.contests.toLong()) to Theme.TEXT),
                        ),
                    ),
                    spacing = 8,
                ),
            ),
            spacing = 10,
        )
    }
}
