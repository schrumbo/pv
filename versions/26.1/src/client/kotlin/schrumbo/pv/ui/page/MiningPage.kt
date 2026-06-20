package schrumbo.pv.ui.page

import schrumbo.pv.data.CrystalSkulls
import schrumbo.pv.data.MiningData
import schrumbo.pv.data.SkillType
import schrumbo.pv.data.SkillTreeRegistry
import schrumbo.pv.data.SkyblockProfile
import schrumbo.pv.render.SkullItems
import schrumbo.pv.ui.Theme
import schrumbo.pv.ui.component.Column
import schrumbo.pv.ui.component.Component
import schrumbo.pv.ui.component.Frame
import schrumbo.pv.ui.component.HAlign
import schrumbo.pv.ui.component.Item
import schrumbo.pv.ui.component.Row
import schrumbo.pv.ui.component.SpaceBetween
import schrumbo.pv.ui.component.Spacer
import schrumbo.pv.ui.component.Text
import schrumbo.pv.ui.component.VAlign
import schrumbo.pv.util.Format

/** Mining page: the Heart-of-the-Mountain tree on the left, powder + Crystal Nucleus + Glacite right. */
object MiningPage {

    private const val MITHRIL = 0xFF55FFFF.toInt()
    private const val GEMSTONE = 0xFFFF66CC.toInt()
    private const val GLACITE = 0xFFA8D8F0.toInt()

    /** Coloured-dye fallbacks for any crystal without a bundled gemstone skull. */
    private val CRYSTAL_FALLBACK = mapOf(
        "Jade" to "lime_dye", "Amber" to "orange_dye", "Topaz" to "yellow_dye",
        "Sapphire" to "blue_dye", "Amethyst" to "amethyst_shard", "Jasper" to "magenta_dye",
        "Ruby" to "red_dye", "Onyx" to "black_dye", "Aquamarine" to "cyan_dye",
        "Citrine" to "brown_dye", "Peridot" to "green_dye", "Opal" to "white_dye",
    )

    /** The real gemstone head texture for a crystal, or a coloured-dye fallback. */
    private fun crystalIcon(name: String): net.minecraft.world.item.ItemStack {
        CrystalSkulls.skulls[name]?.let { val s = SkullItems.fromTexture(it); if (!s.isEmpty) return s }
        return PageKit.icon(CRYSTAL_FALLBACK[name] ?: "quartz")
    }

    fun build(p: SkyblockProfile, width: Int): Component {
        val m = p.mining
        val hotm = SkillTreeView.resolve(SkillTreeRegistry.hotm, m.nodes)
        val grid = SkillTreeView.grid(hotm)
        val rw = (width - grid.width - 16).coerceAtLeast(150)
        val right = Column(
            powderRow(m, rw),
            crystals(m, rw),
            glacite(m, rw),
            spacing = 12,
        )
        return Column(
            PageKit.skillHeader(p, SkillType.MINING, width),
            Spacer(0, 4),
            Row(grid, right, spacing = 16, align = VAlign.TOP),
            spacing = 8,
        )
    }

    /** Three compact powder chips; the value is the earned total (available + already spent). */
    private fun powderRow(m: MiningData, width: Int): Component {
        val gap = 6
        val w = (width - gap * 2) / 3
        return Row(
            powderChip(w, "Mithril", Format.compact(m.mithrilCount), MITHRIL, "prismarine_crystals"),
            powderChip(w, "Gemstone", Format.compact(m.gemstoneCount), GEMSTONE, "purple_dye"),
            powderChip(w, "Glacite", Format.compact(m.glaciteCount), GLACITE, "blue_ice"),
            spacing = gap,
        )
    }

    private fun powderChip(w: Int, label: String, value: String, color: Int, iconId: String): Component {
        val body = Column(Text(label, Theme.TEXT_MUTED), Text(value, color), spacing = 1)
        val row = Row(Item(PageKit.icon(iconId), 14, tooltip = false), body, spacing = 4, align = VAlign.CENTER)
        return Frame(w, row.height + 8, Row(Spacer(4), row), Theme.SURFACE_ALT, Theme.BORDER, HAlign.START, VAlign.CENTER)
    }

    private fun crystals(m: MiningData, width: Int): Component {
        if (m.crystals.isEmpty()) return Text("No crystal data", Theme.TEXT_MUTED)
        val cellW = PageKit.cellW(width, 2)
        val rows = m.crystals.map { c ->
            val color = when (c.state) {
                "PLACED" -> Theme.ACCENT
                "FOUND" -> Theme.GREEN
                else -> Theme.TEXT_MUTED
            }
            val label = when (c.state) {
                "PLACED" -> "Placed"
                "FOUND" -> "Found"
                else -> "—"
            }
            Row(
                Item(crystalIcon(c.name), 12, tooltip = false),
                SpaceBetween(cellW - 12 - 5, Text(c.name, Theme.TEXT), Text(label, color)),
                spacing = 5,
                align = VAlign.CENTER,
            )
        }
        return PageKit.grid(rows, width, cols = 2)
    }

    private fun glacite(m: MiningData, width: Int): Component {
        val cellW = PageKit.cellW(width, 2)
        val rows = mutableListOf<Component>()
        m.corpses.forEach { (name, count) ->
            rows += SpaceBetween(cellW, Text("$name Corpses", Theme.TEXT), Text(Format.compact(count), Theme.TEXT_MUTED))
        }
        rows += SpaceBetween(cellW, Text("Fossils Donated", Theme.TEXT), Text("${m.fossilsDonated}", Theme.TEXT_MUTED))
        rows += SpaceBetween(cellW, Text("Mineshafts", Theme.TEXT), Text(Format.compact(m.mineshafts), Theme.TEXT_MUTED))
        return PageKit.grid(rows, width, cols = 2)
    }
}
