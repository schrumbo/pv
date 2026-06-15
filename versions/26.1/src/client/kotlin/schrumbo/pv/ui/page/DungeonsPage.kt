package schrumbo.pv.ui.page

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import schrumbo.pv.data.DungeonClass
import schrumbo.pv.data.DungeonData
import schrumbo.pv.data.FloorStat
import schrumbo.pv.data.SkyblockProfile
import schrumbo.pv.ui.Theme
import schrumbo.pv.ui.component.Box
import schrumbo.pv.ui.component.Clickable
import schrumbo.pv.ui.component.Column
import schrumbo.pv.ui.component.Component
import schrumbo.pv.ui.component.Item
import schrumbo.pv.ui.component.ProgressBar
import schrumbo.pv.ui.component.Row
import schrumbo.pv.ui.component.SpaceBetween
import schrumbo.pv.ui.component.Spacer
import schrumbo.pv.ui.component.Text
import schrumbo.pv.ui.component.VAlign
import schrumbo.pv.util.Format

/** Catacombs page: level, class levels + average, and per-floor completions (Normal / Master). */
object DungeonsPage {

    private const val COL_GAP = 16
    private const val XP_WIDTH = 36

    fun build(p: SkyblockProfile, width: Int, master: Boolean, onTier: (Boolean) -> Unit): Component {
        val d = p.dungeons
        return Column(
            catacombs(d, width),
            section("CLASSES", Text("CA %.2f".format(d.classAverage), Theme.ACCENT), width, classes(d, width)),
            section("FLOORS", tierToggle(master, onTier), width, floors(d, width, master)),
            spacing = 8,
        )
    }

    private fun catacombs(d: DungeonData, width: Int): Component = Column(
        SpaceBetween(
            width,
            Row(Item(icon("dead_bush"), 11, tooltip = false), Text("Catacombs", Theme.TEXT_MUTED), spacing = 4, align = VAlign.CENTER),
            Text(d.catacombs.level.toString(), Theme.GREEN),
        ),
        bar(d.catacombs.progress, d.catacombs.totalXp, width, Theme.GREEN),
        spacing = 2,
    )

    private fun classes(d: DungeonData, width: Int): Component {
        val cellW = (width - COL_GAP) / 2
        return twoColumns(d.classes.map { classCell(it, cellW) })
    }

    private fun classCell(c: DungeonClass, cellW: Int): Component = Column(
        SpaceBetween(
            cellW,
            Text(c.name, Theme.TEXT),
            Text(c.level.level.toString(), if (c.level.maxed) Theme.ACCENT else Theme.TEXT_MUTED),
        ),
        bar(c.level.progress, c.level.totalXp, cellW, Theme.ACCENT),
        spacing = 1,
    )

    private fun tierToggle(master: Boolean, onTier: (Boolean) -> Unit): Component = Row(
        Clickable(Text("Normal", if (!master) Theme.ACCENT else Theme.TEXT_MUTED)) { onTier(false) },
        Text("/", Theme.BORDER),
        Clickable(Text("Master", if (master) Theme.ACCENT else Theme.TEXT_MUTED)) { onTier(true) },
        spacing = 5,
    )

    private fun floors(d: DungeonData, width: Int, master: Boolean): Component {
        val rows = d.floors
            .filter { !master || it.floor > 0 } // Master has no Entrance floor
            .map { floorRow(it, width, master) }
        return Column(rows, spacing = 3)
    }

    private fun floorRow(f: FloorStat, width: Int, master: Boolean): Component {
        val name = if (f.floor == 0) "Entrance" else "Floor ${f.floor}"
        val count = if (master) f.masterCompletions else f.completions
        return SpaceBetween(
            width,
            Text(name, if (count > 0) Theme.TEXT else Theme.TEXT_MUTED),
            Text("%,d".format(count), if (count > 0) Theme.TEXT else Theme.TEXT_MUTED),
        )
    }

    private fun bar(progress: Double, totalXp: Long, cellW: Int, fg: Int): Component = Row(
        ProgressBar(cellW - XP_WIDTH, 3, progress, fg, Theme.SURFACE_ALT),
        Text(Format.compact(totalXp), Theme.TEXT_MUTED),
        spacing = 5,
        align = VAlign.CENTER,
    )

    private fun twoColumns(rows: List<Component>): Component {
        val half = (rows.size + 1) / 2
        return Row(
            Column(rows.take(half), spacing = 4),
            Column(rows.drop(half), spacing = 4),
            spacing = COL_GAP,
        )
    }

    private fun section(title: String, badge: Component, width: Int, content: Component): Component = Column(
        SpaceBetween(width, Text(title, Theme.TEXT_MUTED), badge),
        Box(width, 1, Theme.BORDER),
        Spacer(0, 1),
        content,
        spacing = 3,
    )

    private fun icon(name: String): ItemStack {
        val id = Identifier.tryParse(name) ?: return ItemStack.EMPTY
        return ItemStack(BuiltInRegistries.ITEM.getValue(id))
    }
}
