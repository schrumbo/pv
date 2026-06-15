package schrumbo.pv.ui.page

import schrumbo.pv.data.AttributesData
import schrumbo.pv.data.SkyblockProfile
import schrumbo.pv.ui.Theme
import schrumbo.pv.ui.component.Column
import schrumbo.pv.ui.component.Component
import schrumbo.pv.ui.component.SpaceBetween
import schrumbo.pv.ui.component.Text
import schrumbo.pv.util.Format

/** Hunting page: every attribute the player owns and its stack level, plus a shard summary. */
object HuntingPage {

    fun build(p: SkyblockProfile, width: Int): Component {
        val a = p.attributes
        return Column(
            PageKit.pageHeader(
                "Hunting",
                "· ${a.attributes.size} attributes · ${Format.compact(a.shardsOwned)} shards (${a.shardTypes} types)",
                width,
            ),
            PageKit.section("ATTRIBUTES", width, attributes(a, width)),
            spacing = 10,
        )
    }

    private fun attributes(a: AttributesData, width: Int): Component {
        if (a.attributes.isEmpty()) return Text("No attributes unlocked", Theme.TEXT_MUTED)
        val cellW = PageKit.cellW(width, 3)
        val rows = a.attributes.map { attr ->
            val color = when {
                attr.level >= 10 -> Theme.GOLD
                attr.level >= 5 -> Theme.ACCENT
                else -> Theme.TEXT_MUTED
            }
            SpaceBetween(cellW, Text(PageKit.clip(attr.name, cellW - 24), Theme.TEXT), Text("${attr.level}", color))
        }
        return PageKit.grid(rows, width, cols = 3)
    }
}
