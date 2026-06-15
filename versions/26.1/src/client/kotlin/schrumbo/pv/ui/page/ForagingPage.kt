package schrumbo.pv.ui.page

import schrumbo.pv.data.ForagingData
import schrumbo.pv.data.SkyblockProfile
import schrumbo.pv.ui.Theme
import schrumbo.pv.ui.component.Column
import schrumbo.pv.ui.component.Component
import schrumbo.pv.ui.component.SpaceBetween
import schrumbo.pv.ui.component.Text
import schrumbo.pv.util.Format

/** Foraging (Galatea) page: forest whispers and per-tree gift milestones. */
object ForagingPage {

    fun build(p: SkyblockProfile, width: Int): Component {
        val f = p.foraging
        return Column(
            PageKit.pageHeader("Foraging", "· ${Format.compact(f.whispers)} forest whispers", width),
            PageKit.tileRow(
                width,
                listOf(
                    "Forest Whispers" to (Format.compact(f.whispers) to Theme.GREEN),
                    "Whispers Spent" to (Format.compact(f.whispersSpent) to Theme.TEXT),
                    "Daily Trees Cut" to (Format.compact(f.dailyTrees) to Theme.TEXT),
                ),
            ),
            PageKit.section("TREE GIFTS", width, trees(f, width)),
            spacing = 10,
        )
    }

    private fun trees(f: ForagingData, width: Int): Component {
        if (f.trees.isEmpty()) return Text("No tree gifts yet", Theme.TEXT_MUTED)
        val cellW = PageKit.cellW(width, 2)
        val rows = f.trees.map { tree ->
            SpaceBetween(
                cellW,
                Text(tree.name, Theme.TEXT),
                Text("${Format.compact(tree.gifts)} gifts §8· §7Tier ${tree.tier}", Theme.TEXT_MUTED),
            )
        }
        return PageKit.grid(rows, width, cols = 2)
    }
}
