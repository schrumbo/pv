package schrumbo.pv.ui.page

import schrumbo.pv.data.SkyblockProfile
import schrumbo.pv.ui.Theme
import schrumbo.pv.ui.component.Column
import schrumbo.pv.ui.component.Component
import schrumbo.pv.ui.component.Row
import schrumbo.pv.ui.component.SpaceBetween
import schrumbo.pv.ui.component.Spacer
import schrumbo.pv.ui.component.Text
import schrumbo.pv.ui.component.VAlign
import schrumbo.pv.util.Format

/**
 * Combat tab content. The sub-page rail itself is drawn by the screen (it mirrors the top tab bar,
 * rotated to the left edge); this object only declares the sub-pages and builds their bodies.
 */
object CombatPage {

    private const val COL_GAP = 14

    /** Sub-pages in rail order, each with a representative chip icon. */
    enum class Sub(val label: String, val icon: String) {
        BESTIARY("Bestiary", "writable_book"),
        MOBS("Mobs", "zombie_head"),
        CRIMSON("Crimson Isle", "blaze_powder"),
    }

    /** Mobs: two columns — kills and deaths — each sorted by count, with a totals subtitle. */
    fun mobs(p: SkyblockProfile, width: Int): Component {
        val m = p.combat.mobs
        if (m.kills.isEmpty() && m.deaths.isEmpty()) return Text("No combat stats", Theme.TEXT_MUTED)
        val colW = (width - COL_GAP) / 2
        fun list(title: String, total: Long, items: List<schrumbo.pv.data.MobCount>): Component {
            val rows = items.map { mc ->
                SpaceBetween(colW, Text(PageKit.clip(mc.name, colW - 60), Theme.TEXT), Text(Format.compact(mc.count), Theme.ACCENT))
            }
            return Column(
                listOf(
                    SpaceBetween(colW, Text(title, Theme.TEXT, scale = Text.SUBTITLE), Text(Format.compact(total), Theme.TEXT_MUTED)),
                    Spacer(0, 4),
                ) + rows,
                spacing = 2,
            )
        }
        return Row(
            list("Kills", m.totalKills, m.kills),
            list("Deaths", m.totalDeaths, m.deaths),
            spacing = COL_GAP,
            align = VAlign.TOP,
        )
    }

    /** Crimson Isle: faction + reputation tiles, Kuudra completions, Dojo grades. */
    fun crimson(p: SkyblockProfile, width: Int): Component {
        val c = p.combat.crimson
        val mageOn = c.selectedFaction == "Mage"
        val barbOn = c.selectedFaction == "Barbarian"
        val factions = PageKit.tileRow(
            width,
            listOf(
                "Faction" to ((c.selectedFaction ?: "None") to (if (c.selectedFaction != null) Theme.ACCENT else Theme.TEXT_MUTED)),
                "Mage Rep" to (Format.compact(c.mageReputation.toLong()) to (if (mageOn) Theme.ACCENT else Theme.TEXT)),
                "Barbarian Rep" to (Format.compact(c.barbarianReputation.toLong()) to (if (barbOn) Theme.ACCENT else Theme.TEXT)),
            ),
        )

        val kuudra = PageKit.tileRow(
            width,
            c.kuudra.map { t -> t.name to (Format.compact(t.completions.toLong()) to if (t.completions > 0) Theme.GOLD else Theme.TEXT_MUTED) },
        )

        val dojoTileW = PageKit.cellW(width, 4)
        val dojoTiles = c.dojo.map { d ->
            val color = if (d.points >= 1000) Theme.GOLD else if (d.points > 0) Theme.ACCENT else Theme.TEXT_MUTED
            PageKit.tile(dojoTileW, d.name, "${d.grade} · ${d.points}", color)
        }
        val dojoRows = dojoTiles.chunked(4).map { Row(it, spacing = 8) }

        return Column(
            Text("Crimson Isle", Theme.TEXT, scale = Text.SUBTITLE),
            factions,
            Spacer(0, 4),
            Text("Kuudra", Theme.TEXT, scale = Text.SUBTITLE),
            kuudra,
            Spacer(0, 4),
            Text("Dojo", Theme.TEXT, scale = Text.SUBTITLE),
            Column(dojoRows, spacing = 6),
            spacing = 6,
        )
    }
}
