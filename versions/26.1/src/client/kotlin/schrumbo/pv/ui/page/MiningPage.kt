package schrumbo.pv.ui.page

import schrumbo.pv.data.MiningData
import schrumbo.pv.data.SkillTreeRegistry
import schrumbo.pv.data.SkyblockProfile
import schrumbo.pv.ui.Theme
import schrumbo.pv.ui.component.Column
import schrumbo.pv.ui.component.Component
import schrumbo.pv.ui.component.Row
import schrumbo.pv.ui.component.SpaceBetween
import schrumbo.pv.ui.component.Text
import schrumbo.pv.ui.component.VAlign
import schrumbo.pv.util.Format

/** Mining page: the Heart-of-the-Mountain perk tree, total powder, Crystal Nucleus and Glacite. */
object MiningPage {

    private const val MITHRIL = 0xFF55FFFF.toInt()
    private const val GEMSTONE = 0xFFFF66CC.toInt()
    private const val GLACITE = 0xFFA8D8F0.toInt()

    fun build(p: SkyblockProfile, width: Int): Component {
        val m = p.mining
        val hotm = SkillTreeView.resolve(SkillTreeRegistry.hotm, m.nodes)
        val grid = SkillTreeView.grid(hotm)
        // The HOTM tree is narrow; fill the empty space to its right with the (total) powder tiles.
        val powderW = (width - grid.width - 14).coerceAtLeast(96)
        val powder = Column(
            PageKit.tile(powderW, "Total Mithril Powder", Format.compact(m.mithrilTotal), MITHRIL),
            PageKit.tile(powderW, "Total Gemstone Powder", Format.compact(m.gemstoneTotal), GEMSTONE),
            PageKit.tile(powderW, "Total Glacite Powder", Format.compact(m.glaciteTotal), GLACITE),
            PageKit.tile(powderW, "HOTM Tokens", Format.compact(m.tokens)),
            spacing = 6,
        )
        return Column(
            PageKit.skillHeader(p, schrumbo.pv.data.SkillType.MINING, width),
            PageKit.section(
                "HEART OF THE MOUNTAIN", width,
                Row(grid, powder, spacing = 14, align = VAlign.TOP),
                SkillTreeView.badge(hotm),
            ),
            PageKit.section("CRYSTAL NUCLEUS", width, crystals(m, width)),
            PageKit.section("GLACITE", width, glacite(m, width)),
            spacing = 10,
        )
    }

    private fun crystals(m: MiningData, width: Int): Component {
        val cellW = PageKit.cellW(width, 3)
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
            SpaceBetween(cellW, Text(c.name, Theme.TEXT), Text(label, color))
        }
        return PageKit.grid(rows, width, cols = 3)
    }

    private fun glacite(m: MiningData, width: Int): Component {
        val cellW = PageKit.cellW(width, 2)
        val rows = mutableListOf<Component>()
        m.corpses.forEach { (name, count) ->
            rows += SpaceBetween(cellW, Text("$name Corpses", Theme.TEXT), Text(Format.compact(count), Theme.TEXT_MUTED))
        }
        rows += SpaceBetween(cellW, Text("Fossils Donated", Theme.TEXT), Text("${m.fossilsDonated}", Theme.TEXT_MUTED))
        rows += SpaceBetween(cellW, Text("Mineshafts Entered", Theme.TEXT), Text(Format.compact(m.mineshafts), Theme.TEXT_MUTED))
        return PageKit.grid(rows, width, cols = 2)
    }
}
