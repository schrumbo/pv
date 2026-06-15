package schrumbo.pv.ui.page

import schrumbo.pv.data.SkyblockProfile
import schrumbo.pv.data.TrophyData
import schrumbo.pv.data.TrophyFish
import schrumbo.pv.ui.Theme
import schrumbo.pv.ui.component.Column
import schrumbo.pv.ui.component.Component
import schrumbo.pv.ui.component.Row
import schrumbo.pv.ui.component.SpaceBetween
import schrumbo.pv.ui.component.Text
import schrumbo.pv.ui.component.Tooltip
import schrumbo.pv.ui.component.VAlign
import schrumbo.pv.util.Format

/** Fishing page: trophy-fish catches per tier (no skill level — that's General). */
object FishingPage {

    private const val BRONZE = 0xFFCD7F32.toInt()
    private const val SILVER = 0xFFD8D8D8.toInt()
    private const val GOLD = 0xFFFFB534.toInt()
    private const val DIAMOND = 0xFF4DD0E1.toInt()

    fun build(p: SkyblockProfile, width: Int): Component {
        val t = p.trophy
        return Column(
            PageKit.pageHeader("Fishing", "· ${Format.compact(t.totalCaught)} trophy fish caught", width),
            PageKit.tileRow(
                width,
                listOf(
                    "Bronze" to (Format.compact(t.fish.sumOf { it.bronze }) to BRONZE),
                    "Silver" to (Format.compact(t.fish.sumOf { it.silver }) to SILVER),
                    "Gold" to (Format.compact(t.fish.sumOf { it.gold }) to GOLD),
                    "Diamond" to (Format.compact(t.fish.sumOf { it.diamond }) to DIAMOND),
                ),
            ),
            PageKit.section("TROPHY FISH", width, fish(t, width)),
            spacing = 10,
        )
    }

    private fun fish(t: TrophyData, width: Int): Component {
        val cellW = PageKit.cellW(width, 2)
        return PageKit.grid(t.fish.map { fishRow(it, cellW) }, width, cols = 2)
    }

    private fun fishRow(f: TrophyFish, cellW: Int): Component {
        val nameColor = when (f.highestTier) {
            4 -> DIAMOND
            3 -> GOLD
            2 -> SILVER
            1 -> BRONZE
            else -> Theme.TEXT_MUTED
        }
        val counts = Row(
            Text(Format.compact(f.bronze), BRONZE),
            Text(Format.compact(f.silver), SILVER),
            Text(Format.compact(f.gold), GOLD),
            Text(Format.compact(f.diamond), DIAMOND),
            spacing = 7,
            align = VAlign.CENTER,
        )
        val row = SpaceBetween(cellW, Text(PageKit.clip(f.name, cellW - 90), nameColor), counts)
        return Tooltip(
            row,
            listOf(
                "§f${f.name}",
                "§7Total: §f${Format.compact(f.total)}",
                "§7Bronze §f${f.bronze}  §7Silver §f${f.silver}",
                "§7Gold §f${f.gold}  §7Diamond §f${f.diamond}",
            ),
        )
    }
}
