package schrumbo.pv.ui.page

import schrumbo.pv.data.MiningData
import schrumbo.pv.data.SkyblockProfile
import schrumbo.pv.ui.Theme
import schrumbo.pv.ui.component.Column
import schrumbo.pv.ui.component.Component
import schrumbo.pv.ui.component.SpaceBetween
import schrumbo.pv.ui.component.Text
import schrumbo.pv.util.Format

/** Mining page: powders, Crystal Nucleus state and Glacite progress (no skill level — that's General). */
object MiningPage {

    fun build(p: SkyblockProfile, width: Int): Component {
        val m = p.mining
        return Column(
            PageKit.pageHeader("Mining", "· ${Format.compact(m.tokens)} HotM tokens", width),
            PageKit.tileRow(
                width,
                listOf(
                    "Mithril Powder" to (Format.compact(m.mithril) to Theme.ACCENT),
                    "Gemstone Powder" to (Format.compact(m.gemstone) to Theme.GREEN),
                    "Glacite Powder" to (Format.compact(m.glacite) to Theme.TEXT),
                    "HotM Tokens" to (Format.compact(m.tokens) to Theme.GOLD),
                ),
            ),
            PageKit.section("POWDER (TOTAL EARNED)", width, powder(m, width)),
            PageKit.section("CRYSTAL NUCLEUS", width, crystals(m, width)),
            PageKit.section("GLACITE", width, glacite(m, width)),
            spacing = 10,
        )
    }

    private fun powder(m: MiningData, width: Int): Component = Column(
        powderRow("Mithril", m.mithril, m.mithrilTotal, Theme.ACCENT, width),
        powderRow("Gemstone", m.gemstone, m.gemstoneTotal, Theme.GREEN, width),
        powderRow("Glacite", m.glacite, m.glaciteTotal, Theme.TEXT, width),
        spacing = 3,
    )

    private fun powderRow(name: String, cur: Long, total: Long, color: Int, width: Int): Component =
        SpaceBetween(
            width,
            Text(name, color),
            Text("${Format.compact(cur)} §8/ §7${Format.compact(total)} total", Theme.TEXT),
        )

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
