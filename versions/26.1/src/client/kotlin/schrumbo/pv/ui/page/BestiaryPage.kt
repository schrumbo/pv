package schrumbo.pv.ui.page

import net.minecraft.client.Minecraft
import schrumbo.pv.data.BestiaryRegistry
import schrumbo.pv.data.IslandProgress
import schrumbo.pv.data.MobTier
import schrumbo.pv.data.SkyblockProfile
import schrumbo.pv.ui.Theme
import schrumbo.pv.ui.component.Clickable
import schrumbo.pv.ui.component.Column
import schrumbo.pv.ui.component.Component
import schrumbo.pv.ui.component.Frame
import schrumbo.pv.ui.component.HAlign
import schrumbo.pv.ui.component.Item
import schrumbo.pv.ui.component.ProgressBar
import schrumbo.pv.ui.component.Row
import schrumbo.pv.ui.component.SpaceBetween
import schrumbo.pv.ui.component.Text
import schrumbo.pv.ui.component.Tooltip
import schrumbo.pv.ui.component.VAlign
import schrumbo.pv.util.Format

/** Bestiary page: a horizontal island selector and the selected island's mob tiers as a grid. */
object BestiaryPage {

    private const val COL_GAP = 14

    fun islands(p: SkyblockProfile): List<IslandProgress> = BestiaryRegistry.resolve(p.bestiaryKills)

    fun header(islands: List<IslandProgress>, width: Int, active: Int = -1): Component {
        val maxed = islands.sumOf { it.maxedCount }
        val total = islands.sumOf { it.total }
        val kills = islands.sumOf { i -> i.mobs.sumOf { it.kills } }
        val stats = Row(
            Text("${Format.compact(kills)} kills", Theme.TEXT_MUTED),
            Text("$maxed/$total maxed", Theme.TEXT_MUTED),
            spacing = 14,
            align = VAlign.CENTER,
        )
        val name = islands.getOrNull(active)?.def?.name ?: return stats
        return SpaceBetween(width, Text(name, Theme.TEXT, scale = Text.SUBTITLE), stats)
    }

    /** Horizontal island selector: every island as an icon, wrapping to fit [width]. */
    fun selector(islands: List<IslandProgress>, active: Int, width: Int, onIsland: (Int) -> Unit): Component {
        val cell = 20
        val gap = 3
        val perRow = ((width + gap) / (cell + gap)).coerceAtLeast(1)
        val chips = islands.mapIndexed { i, island -> islandChip(island, i == active) { onIsland(i) } }
        return Column(chips.chunked(perRow).map { Row(it, spacing = gap) }, spacing = gap)
    }

    private fun islandChip(island: IslandProgress, active: Boolean, onClick: () -> Unit): Component {
        val frame = Frame(
            20, 20, Item(BestiaryRegistry.islandIcon(island.def), 16, tooltip = false),
            if (active) Theme.SURFACE_ALT else null,
            if (active) Theme.ACCENT else null,
            HAlign.CENTER, VAlign.CENTER,
        )
        val tip = listOf("§f${island.def.name}", "§7${island.maxedCount}§7/§f${island.total}§7 maxed")
        return Clickable(Tooltip(frame, tip), hoverFill = Theme.HOVER, onClick = onClick)
    }

    fun grid(island: IslandProgress, width: Int): Component {
        val cols = (width / 200).coerceIn(2, 3)
        val cellW = (width - COL_GAP * (cols - 1)) / cols
        val cells = island.mobs.map { mobCell(it, cellW) }
        val per = (cells.size + cols - 1) / cols
        return Row(
            (0 until cols).map { c -> Column(cells.drop(c * per).take(per), spacing = 5) },
            spacing = COL_GAP,
            align = VAlign.TOP,
        )
    }

    private fun mobCell(m: MobTier, cellW: Int): Component {
        val innerW = cellW - 16 - 5
        val nameColor = if (m.maxed) Theme.GOLD else Theme.TEXT
        val tierLabel = "${m.tier}/${m.maxTier}"
        val tierColor = when {
            m.maxed -> Theme.GOLD
            m.tier == 0 -> Theme.TEXT_MUTED
            else -> Theme.ACCENT
        }
        val fg = if (m.maxed) Theme.GOLD else Theme.ACCENT
        val cell = Row(
            Item(BestiaryRegistry.icon(m.def), 16, tooltip = false),
            Column(
                SpaceBetween(innerW, Text(clip(m.def.name, innerW - 30), nameColor), Text(tierLabel, tierColor)),
                ProgressBar(innerW, 3, m.progress, fg, Theme.SURFACE_ALT),
                Text("${Format.compact(m.kills)} kills", Theme.TEXT_MUTED),
                spacing = 2,
            ),
            spacing = 5,
            align = VAlign.CENTER,
        )
        val nextLine = if (m.maxed) "§7Maxed" else "§7Next: §f${Format.compact(m.kills)}§7/§f${Format.compact(m.next)}"
        return Tooltip(cell, listOf("§f${m.def.name}", "§7Tier §f${m.tier}§7/§f${m.maxTier}", nextLine))
    }

    private fun clip(s: String, maxW: Int): String {
        val font = Minecraft.getInstance().font
        if (font.width(s) <= maxW) return s
        var t = s
        while (t.isNotEmpty() && font.width("$t…") > maxW) t = t.dropLast(1)
        return "$t…"
    }
}
