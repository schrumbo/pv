package schrumbo.pv.ui.page

import schrumbo.pv.data.JacobsData
import schrumbo.pv.data.SkyblockProfile
import schrumbo.pv.ui.Theme
import schrumbo.pv.ui.component.Column
import schrumbo.pv.ui.component.Component
import schrumbo.pv.ui.component.SpaceBetween
import schrumbo.pv.ui.component.Text
import schrumbo.pv.util.Format

/** Farming page: Jacob's contest medals, unique brackets and per-crop personal bests. */
object FarmingPage {

    private const val GOLD = 0xFFFFB534.toInt()
    private const val SILVER = 0xFFD8D8D8.toInt()
    private const val BRONZE = 0xFFCD7F32.toInt()

    fun build(p: SkyblockProfile, width: Int): Component {
        val j = p.jacobs
        return Column(
            PageKit.pageHeader("Farming", "· ${j.contests} Jacob's contests", width),
            PageKit.tileRow(
                width,
                listOf(
                    "Gold Medals" to ("${j.gold}" to GOLD),
                    "Silver Medals" to ("${j.silver}" to SILVER),
                    "Bronze Medals" to ("${j.bronze}" to BRONZE),
                    "Contests" to (Format.compact(j.contests.toLong()) to Theme.TEXT),
                ),
            ),
            PageKit.section("UNIQUE GOLD CROPS", width, unique(j, width)),
            PageKit.section("PERSONAL BESTS", width, pbs(j, width)),
            PageKit.section("PERKS", width, perks(j, width)),
            spacing = 10,
        )
    }

    private fun unique(j: JacobsData, width: Int): Component = SpaceBetween(
        width,
        Text("Crops bracketed", Theme.TEXT),
        Text("§6${j.uniqueGold} gold  §f${j.uniqueSilver} silver  §c${j.uniqueBronze} bronze", Theme.TEXT),
    )

    private fun pbs(j: JacobsData, width: Int): Component {
        if (j.pbs.isEmpty()) return Text("No data", Theme.TEXT_MUTED)
        val cellW = PageKit.cellW(width, 2)
        val rows = j.pbs.map { pb ->
            SpaceBetween(cellW, Text(pb.crop, Theme.TEXT), Text(Format.compact(pb.best), Theme.GOLD))
        }
        return PageKit.grid(rows, width, cols = 2)
    }

    private fun perks(j: JacobsData, width: Int): Component {
        val cellW = PageKit.cellW(width, 2)
        return PageKit.grid(
            listOf(
                SpaceBetween(cellW, Text("Double Drops", Theme.TEXT), Text("${j.doubleDrops}", Theme.ACCENT)),
                SpaceBetween(cellW, Text("Farming Level Cap", Theme.TEXT), Text("+${j.farmingCap}", Theme.ACCENT)),
            ),
            width, cols = 2,
        )
    }
}
