package schrumbo.pv.ui.page

import net.minecraft.client.Minecraft
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import schrumbo.pv.data.DungeonClass
import schrumbo.pv.data.DungeonData
import schrumbo.pv.data.FloorRun
import schrumbo.pv.data.FloorStat
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
import schrumbo.pv.ui.component.VAlign
import schrumbo.pv.util.Format

/** Catacombs page: level, secrets/run tiles, class levels (with icons), and per-floor S+ PBs. */
object DungeonsPage {

    private const val COL_GAP = 16
    private const val XP_WIDTH = 40
    private const val XP_GAP = 5
    private const val TILE_GAP = 8

    // Floor table columns. The completion bar takes whatever width is left.
    private const val NAME_W = 64
    private const val COUNT_W = 40
    private const val PB_W = 60
    private const val DATE_W = 60
    private const val ROW_GAP = 10

    fun build(p: SkyblockProfile, width: Int, master: Boolean, onTier: (Boolean) -> Unit): Component {
        val d = p.dungeons
        return Column(
            catacombs(d, width),
            tiles(d, width),
            section("CLASSES", Text("CA %.2f".format(d.classAverage), Theme.ACCENT), width, classes(d, width)),
            section("FLOORS", tierToggle(master, onTier), width, floors(d, width, master)),
            spacing = 10,
        )
    }

    private fun catacombs(d: DungeonData, width: Int): Component = Column(
        Row(
            Item(icon("dead_bush"), 11, tooltip = false),
            Text("Catacombs", Theme.TEXT_MUTED),
            Text(d.catacombs.level.toString(), Theme.CATA),
            spacing = 4,
            align = VAlign.CENTER,
        ),
        bar(d.catacombs.progress, d.catacombs.totalXp, width, Theme.CATA, Theme.CATA_TRACK),
        spacing = 2,
    )

    private fun tiles(d: DungeonData, width: Int): Component {
        val w = (width - TILE_GAP * 3) / 4
        val highest = if (d.highestFloorMaster > 0) "M${d.highestFloorMaster}" else "F${d.highestFloorNormal}"
        return Row(
            tile(w, "Secrets Found", "%,d".format(d.secrets), Theme.GOLD),
            tile(w, "Secrets / Run", "%.2f".format(d.secretsPerRun), Theme.TEXT),
            tile(w, "Completions", "%,d".format(d.totalRuns), Theme.TEXT),
            tile(w, "Highest Floor", highest, Theme.TEXT),
            spacing = TILE_GAP,
        )
    }

    private fun tile(w: Int, key: String, value: String, valueColor: Int): Component {
        val content = Column(Text(key, Theme.TEXT_MUTED), Spacer(0, 1), Text(value, valueColor), spacing = 0)
        return Frame(w, content.height + 12, Row(Spacer(6), content), Theme.SURFACE_ALT, Theme.BORDER, HAlign.START, VAlign.CENTER)
    }

    private fun classes(d: DungeonData, width: Int): Component {
        val cellW = (width - COL_GAP) / 2
        return twoColumns(d.classes.map { classCell(it, cellW, d.selectedClass) })
    }

    private fun classCell(c: DungeonClass, cellW: Int, selected: String?): Component {
        val isSelected = selected != null && c.name.equals(selected, ignoreCase = true)
        return Column(
            Row(
                Item(icon(c.icon), 11, tooltip = false),
                Text(c.name, if (isSelected) Theme.GREEN else Theme.TEXT),
                Text(c.level.level.toString(), if (c.level.maxed) Theme.ACCENT else Theme.TEXT_MUTED),
                spacing = 4,
                align = VAlign.CENTER,
            ),
            bar(c.level.progress, c.level.totalXp, cellW, Theme.ACCENT, Theme.SURFACE_ALT),
            spacing = 1,
        )
    }

    /** Normal / Master toggle styled like the bottom profile chips — bordered, active = accent. */
    private fun tierToggle(master: Boolean, onTier: (Boolean) -> Unit): Component = Row(
        segButton("Normal", !master) { onTier(false) },
        segButton("Master", master) { onTier(true) },
        spacing = 6,
    )

    private fun segButton(label: String, active: Boolean, onClick: () -> Unit): Component {
        val w = font().width(label) + 16
        val btn = Frame(
            w, 14,
            Text(label, if (active) Theme.ACCENT else Theme.TEXT_MUTED),
            Theme.SURFACE_ALT,
            if (active) Theme.ACCENT else Theme.BORDER,
            HAlign.CENTER, VAlign.CENTER,
        )
        return Clickable(btn, hoverFill = Theme.HOVER, onClick = onClick)
    }

    private fun floors(d: DungeonData, width: Int, master: Boolean): Component {
        val complW = (width - NAME_W - COUNT_W - PB_W - DATE_W - ROW_GAP * 4).coerceAtLeast(20)
        val maxCompl = d.floors.maxOf { (if (master) it.masterCompletions else it.completions) }.coerceAtLeast(1)
        val rows = mutableListOf<Component>(floorHeader(complW))
        // Always render all 8 rows (Entrance + F1–F7) so toggling Normal/Master never resizes the page.
        d.floors.sortedByDescending { it.floor }.forEach { rows += floorRow(it, complW, maxCompl, master) }
        return Column(rows, spacing = 3)
    }

    private fun floorHeader(complW: Int): Component = Row(
        Frame(NAME_W, lh(), Text("Floor", Theme.TEXT_MUTED), hAlign = HAlign.START),
        Frame(complW, lh(), Text("Completions", Theme.TEXT_MUTED), hAlign = HAlign.START),
        Frame(COUNT_W, lh(), Text("", Theme.TEXT_MUTED), hAlign = HAlign.END),
        Frame(PB_W, lh(), Text("S+ PB", Theme.TEXT_MUTED), hAlign = HAlign.START),
        Frame(DATE_W, lh(), Text("", Theme.TEXT_MUTED), hAlign = HAlign.END),
        spacing = ROW_GAP,
        align = VAlign.CENTER,
    )

    private fun floorRow(f: FloorStat, complW: Int, maxCompl: Long, master: Boolean): Component {
        val name = if (f.floor == 0) "Entrance" else "Floor ${f.floor}"
        val count = if (master) f.masterCompletions else f.completions
        val run = if (master) f.masterBest else f.normalBest
        val color = if (count > 0) Theme.TEXT else Theme.TEXT_MUTED
        return Row(
            Frame(NAME_W, lh(), Text(name, color), hAlign = HAlign.START),
            ProgressBar(complW, 5, count.toDouble() / maxCompl, Theme.CATA, Theme.SURFACE_ALT),
            Frame(COUNT_W, lh(), Text("%,d".format(count), color), hAlign = HAlign.END),
            Frame(PB_W, lh(), pbCell(run), hAlign = HAlign.START),
            Frame(DATE_W, lh(), Text(run?.timestamp?.let { Format.shortDate(it) } ?: "—", Theme.TEXT_MUTED), hAlign = HAlign.END),
            spacing = ROW_GAP,
            align = VAlign.CENTER,
        )
    }

    /** S+ personal best when one exists; otherwise the floor's best grade, or a dash. */
    private fun pbCell(run: FloorRun?): Component = when {
        run?.sPlusTimeMs != null -> Row(Text("S+", Theme.GOLD), Text(Format.duration(run.sPlusTimeMs), Theme.TEXT), spacing = 4)
        run != null && run.score > 0 -> grade(run.score)
        else -> Text("—", Theme.TEXT_MUTED)
    }

    /** Score → grade chip (S+/S/A/B/C/D) coloured by tier. */
    private fun grade(score: Int): Component = when {
        score >= 300 -> Text("S+", Theme.GOLD)
        score >= 270 -> Text("S", Theme.ACCENT)
        score >= 230 -> Text("A", Theme.GREEN)
        score >= 160 -> Text("B", Theme.TEXT)
        score >= 100 -> Text("C", Theme.TEXT_MUTED)
        else -> Text("D", Theme.TEXT_MUTED)
    }

    private fun lh(): Int = font().lineHeight
    private fun font() = Minecraft.getInstance().font

    private fun bar(progress: Double, totalXp: Long, cellW: Int, fg: Int, bg: Int): Component = Row(
        ProgressBar(cellW - XP_WIDTH - XP_GAP, 3, progress, fg, bg),
        Frame(XP_WIDTH, lh(), Text(Format.compact(totalXp), Theme.TEXT_MUTED), hAlign = HAlign.END),
        spacing = XP_GAP,
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
