package schrumbo.pv.ui.page

import net.minecraft.client.Minecraft
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import schrumbo.pv.data.SkillType
import schrumbo.pv.data.SkyblockProfile
import schrumbo.pv.ui.Theme
import schrumbo.pv.ui.component.Box
import schrumbo.pv.ui.component.Column
import schrumbo.pv.ui.component.Component
import schrumbo.pv.ui.component.Frame
import schrumbo.pv.ui.component.HAlign
import schrumbo.pv.ui.component.ProgressBar
import schrumbo.pv.ui.component.Row
import schrumbo.pv.ui.component.SpaceBetween
import schrumbo.pv.ui.component.Spacer
import schrumbo.pv.ui.component.Text
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

    /** A page title + value summary line, with a divider underneath. */
    fun pageHeader(title: String, summary: String, width: Int): Component = Column(
        Row(Text(title, Theme.TEXT), Text(summary, Theme.TEXT_MUTED), spacing = 6, align = VAlign.CENTER),
        Box(width, 1, Theme.BORDER),
        spacing = 4,
    )

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
                Row(Text(type.display, Theme.TEXT), Text(levelText, fg), spacing = 6, align = VAlign.CENTER),
                Text(right, Theme.TEXT_MUTED),
            ),
            ProgressBar(width, 4, lvl.progress, fg, Theme.SURFACE_ALT),
            if (extra.isEmpty()) Spacer(0, 0) else Text(extra, Theme.TEXT_MUTED),
            Box(width, 1, Theme.BORDER),
            spacing = 4,
        )
    }

    /** A bordered key/value tile (label on top, value below). */
    fun tile(w: Int, key: String, value: String, valueColor: Int = Theme.TEXT): Component {
        val content = Column(Text(key, Theme.TEXT_MUTED), Spacer(0, 1), Text(value, valueColor), spacing = 0)
        return Frame(w, content.height + 12, Row(Spacer(6), content), Theme.SURFACE_ALT, Theme.BORDER, HAlign.START, VAlign.CENTER)
    }

    /** A row of equal-width tiles spanning [width]. */
    fun tileRow(width: Int, tiles: List<Pair<String, Pair<String, Int>>>): Component {
        val gap = 8
        val w = (width - gap * (tiles.size - 1)) / tiles.size
        return Row(tiles.map { (k, v) -> tile(w, k, v.first, v.second) }, spacing = gap)
    }

    /** A titled section: label + optional right-aligned badge, divider, then [content]. */
    fun section(title: String, width: Int, content: Component, badge: Component? = null): Component = Column(
        SpaceBetween(width, Text(title, Theme.TEXT_MUTED), badge ?: Spacer(0)),
        Box(width, 1, Theme.BORDER),
        Spacer(0, 1),
        content,
        spacing = 3,
    )

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
