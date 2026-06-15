package schrumbo.pv.ui.page

import net.minecraft.client.Minecraft
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import schrumbo.pv.data.CategoryProgress
import schrumbo.pv.data.CollectionTier
import schrumbo.pv.data.CollectionsRegistry
import schrumbo.pv.data.SkyblockProfile
import schrumbo.pv.ui.Theme
import schrumbo.pv.ui.component.Box
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

/** Collections page: a left category rail and the selected category's collections as a 2-column grid. */
object CollectionsPage {

    private const val RAIL_W = 110
    private const val GAP = 14
    private const val COL_GAP = 14
    private const val ROW_H = 14

    fun build(p: SkyblockProfile, width: Int, selected: Int, onCategory: (Int) -> Unit): Component {
        val cats = CollectionsRegistry.resolve(p.collections)
        val active = selected.coerceIn(0, cats.size - 1)
        return Column(
            header(cats, width),
            Row(
                rail(cats, active, onCategory),
                grid(cats[active], width - RAIL_W - GAP),
                spacing = GAP,
                align = VAlign.TOP,
            ),
            spacing = 8,
        )
    }

    private fun header(cats: List<CategoryProgress>, width: Int): Component {
        val maxed = cats.sumOf { it.maxedCount }
        val total = cats.sumOf { it.total }
        val tiers = cats.sumOf { c -> c.items.sumOf { it.tier } }
        return Column(
            Row(
                Text("Collections", Theme.TEXT),
                Text("· $maxed/$total maxed · $tiers tiers", Theme.TEXT_MUTED),
                spacing = 6,
                align = VAlign.CENTER,
            ),
            Box(width, 1, Theme.BORDER),
            spacing = 4,
        )
    }

    private fun rail(cats: List<CategoryProgress>, active: Int, onCategory: (Int) -> Unit): Component =
        Column(cats.mapIndexed { i, cat -> categoryRow(cat, i == active) { onCategory(i) } }, spacing = 1)

    private fun categoryRow(cat: CategoryProgress, active: Boolean, onClick: () -> Unit): Component {
        val color = if (active) Theme.ACCENT else Theme.TEXT_MUTED
        val row = Row(
            Box(2, 11, if (active) Theme.ACCENT else null),
            Item(icon(cat.def.icon), 11, tooltip = false),
            Text(cat.def.name, color),
            spacing = 4,
            align = VAlign.CENTER,
        )
        val frame = Frame(RAIL_W, ROW_H, row, if (active) Theme.SURFACE_ALT else null, null, HAlign.START, VAlign.CENTER)
        return Clickable(frame, hoverFill = Theme.HOVER, onClick = onClick)
    }

    private fun grid(cat: CategoryProgress, width: Int): Component {
        val cellW = (width - COL_GAP) / 2
        val cells = cat.items.map { cell(it, cellW) }
        val half = (cells.size + 1) / 2
        return Row(
            Column(cells.take(half), spacing = 6),
            Column(cells.drop(half), spacing = 6),
            spacing = COL_GAP,
            align = VAlign.TOP,
        )
    }

    private fun cell(c: CollectionTier, cellW: Int): Component {
        val nameColor = if (c.maxed) Theme.GOLD else Theme.TEXT
        val tierLabel = when {
            c.maxed -> "MAX"
            c.tier == 0 -> "—"
            else -> Format.roman(c.tier)
        }
        val tierColor = when {
            c.maxed -> Theme.GOLD
            c.tier == 0 -> Theme.TEXT_MUTED
            else -> Theme.ACCENT
        }
        val fg = if (c.maxed) Theme.GOLD else Theme.ACCENT
        val body = Column(
            SpaceBetween(cellW, Text(clip(c.def.name, cellW - 30), nameColor), Text(tierLabel, tierColor)),
            ProgressBar(cellW, 3, c.progress, fg, Theme.SURFACE_ALT),
            spacing = 2,
        )
        val nextLine = if (c.maxed) "§7Maxed" else "§7Next: §f${Format.compact(c.amount)}§7/§f${Format.compact(c.next)}"
        return Tooltip(body, listOf("§f${c.def.name}", "§7Tier §f${c.tier}§7/§f${c.maxTier}", "§7Total: §f${Format.compact(c.amount)}", nextLine))
    }

    private fun clip(s: String, maxW: Int): String {
        val font = Minecraft.getInstance().font
        if (font.width(s) <= maxW) return s
        var t = s
        while (t.isNotEmpty() && font.width("$t…") > maxW) t = t.dropLast(1)
        return "$t…"
    }

    private fun icon(name: String): ItemStack {
        val id = Identifier.tryParse(name) ?: return ItemStack.EMPTY
        return ItemStack(BuiltInRegistries.ITEM.getValue(id))
    }
}
