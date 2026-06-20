package schrumbo.pv.ui.page

import net.minecraft.client.Minecraft
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import schrumbo.pv.data.SkillType
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
import schrumbo.pv.ui.component.Spacer
import schrumbo.pv.ui.component.Text
import schrumbo.pv.ui.component.Tooltip
import schrumbo.pv.ui.component.VAlign
import schrumbo.pv.util.Format

/** Shared layout helpers for the simpler stat pages (Mining, Fishing, Farming, Hunting, Foraging). */
object PageKit {

    fun font() = Minecraft.getInstance().font
    fun lh(): Int = font().lineHeight

    fun icon(name: String): ItemStack {
        val id = Identifier.tryParse(name) ?: return ItemStack.EMPTY
        return ItemStack(BuiltInRegistries.ITEM.getValue(id))
    }

    fun clip(s: String, maxW: Int): String {
        if (font().width(s) <= maxW) return s
        var t = s
        while (t.isNotEmpty() && font().width("$t…") > maxW) t = t.dropLast(1)
        return "$t…"
    }

    /** A floating page title + value summary line (no divider; sections separate via spacing). */
    fun pageHeader(title: String, summary: String, width: Int): Component {
        val parts = buildList {
            if (title.isNotEmpty()) add(Text(title, Theme.TEXT, scale = Text.SUBTITLE))
            if (summary.isNotEmpty()) add(Text(summary, Theme.TEXT_MUTED))
        }
        return if (parts.isEmpty()) Spacer(0, 0) else Row(parts, spacing = 8, align = VAlign.CENTER)
    }

    /**
     * A skill-page header: the skill name + its (overflow) level, a full-width progress bar, the XP /
     * to-next line, plus an optional page-specific [extra] summary. Falls back to [pageHeader] if the
     * skill is missing. Maxed skills render in gold.
     */
    fun skillHeader(p: SkyblockProfile, type: SkillType, width: Int, extra: String = ""): Component {
        val lvl = p.skills.firstOrNull { it.type == type }?.level ?: return pageHeader(type.display, extra, width)
        val fg = if (lvl.maxed) Theme.GOLD else Theme.ACCENT
        val levelText = if (lvl.maxed) "Level ${lvl.level} · MAX" else "Level ${lvl.level}"
        val right = if (lvl.maxed) "${Format.compact(lvl.totalXp)} XP"
        else "${(lvl.progress * 100).toInt()}% · ${Format.compact(lvl.xpToNext)} to next"
        return Column(
            SpaceBetween(
                width,
                Row(Text(type.display, Theme.TEXT, scale = Text.SUBTITLE), Text(levelText, fg), spacing = 6, align = VAlign.CENTER),
                Text(right, Theme.TEXT_MUTED),
            ),
            ProgressBar(width, 4, lvl.progress, fg, Theme.SURFACE_ALT),
            if (extra.isEmpty()) Spacer(0, 0) else Text(extra, Theme.TEXT_MUTED),
            spacing = 4,
        )
    }

    const val RAIL_ROW_H = 18

    /**
     * An icon-only side-rail bookmark: the active entry blends into the content surface and is marked
     * by an accent left bar over a [SURFACE_ALT] highlight; inactive entries are bare icons. No border
     * (so no boxed outline). The [label] shows as a tooltip on hover.
     */
    fun bookmark(width: Int, icon: ItemStack, label: String, active: Boolean, onClick: () -> Unit): Component {
        val inner = Row(
            Box(2, RAIL_ROW_H - 6, if (active) Theme.ACCENT else null),
            Item(icon, 14, tooltip = false),
            spacing = 3,
            align = VAlign.CENTER,
        )
        val frame = Frame(
            width, RAIL_ROW_H, Row(Spacer(2), inner),
            if (active) Theme.SURFACE_ALT else null, null,
            HAlign.START, VAlign.CENTER,
        )
        return Clickable(Tooltip(frame, listOf(label)), hoverFill = Theme.HOVER, onClick = onClick)
    }

    /** A bordered key/value tile (small caption on top, value below). */
    fun tile(w: Int, key: String, value: String, valueColor: Int = Theme.TEXT): Component {
        val content = Column(Text(key, Theme.TEXT_MUTED, scale = Text.SMALL), Spacer(0, 2), Text(value, valueColor), spacing = 0)
        return Frame(w, content.height + 12, Row(Spacer(6), content), Theme.SURFACE_ALT, Theme.BORDER, HAlign.START, VAlign.CENTER)
    }

    /** A row of equal-width tiles spanning [width]. */
    fun tileRow(width: Int, tiles: List<Pair<String, Pair<String, Int>>>): Component {
        val gap = 8
        val w = (width - gap * (tiles.size - 1)) / tiles.size
        return Row(tiles.map { (k, v) -> tile(w, k, v.first, v.second) }, spacing = gap)
    }

    /**
     * A section group, no label and no divider — separation comes from the caller's spacing.
     * [title] is kept for call-site readability but no longer rendered; an optional [badge] floats
     * right above the content when present.
     */
    @Suppress("UNUSED_PARAMETER")
    fun section(title: String, width: Int, content: Component, badge: Component? = null): Component =
        if (badge != null) Column(SpaceBetween(width, Spacer(0), badge), content, spacing = 3) else content

    /** Lays [rows] into [cols] balanced columns. */
    fun grid(rows: List<Component>, width: Int, cols: Int = 2, colGap: Int = 14, rowGap: Int = 5): Component {
        if (rows.isEmpty()) return Text("No data", Theme.TEXT_MUTED)
        val per = (rows.size + cols - 1) / cols
        val columns = (0 until cols).map { c ->
            Column(rows.drop(c * per).take(per), spacing = rowGap)
        }
        return Row(columns, spacing = colGap, align = VAlign.TOP)
    }

    fun cellW(width: Int, cols: Int = 2, colGap: Int = 14): Int = (width - colGap * (cols - 1)) / cols
}
