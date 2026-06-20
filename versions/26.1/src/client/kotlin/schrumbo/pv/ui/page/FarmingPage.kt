package schrumbo.pv.ui.page

import net.minecraft.world.item.ItemStack
import schrumbo.pv.data.GardenData
import schrumbo.pv.data.GardenRegistry
import schrumbo.pv.data.GreenhouseData
import schrumbo.pv.data.SkillType
import schrumbo.pv.data.SkyblockProfile
import schrumbo.pv.render.SkullItems
import schrumbo.pv.ui.Theme
import schrumbo.pv.ui.component.Column
import schrumbo.pv.ui.component.Component
import schrumbo.pv.ui.component.Item
import schrumbo.pv.ui.component.ProgressBar
import schrumbo.pv.ui.component.Row
import schrumbo.pv.ui.component.SpaceBetween
import schrumbo.pv.ui.component.Text
import schrumbo.pv.ui.component.VAlign
import schrumbo.pv.util.Format

/** Farming page: the Garden (level, plots, upgrades, crops), greenhouse mutations and Jacob's medals. */
object FarmingPage {

    private const val DIAMOND = 0xFF4DD0E1.toInt()
    private const val PLATINUM = 0xFFB9D6E0.toInt()
    private const val GOLD = 0xFFFFB534.toInt()
    private const val SILVER = 0xFFD8D8D8.toInt()
    private const val BRONZE = 0xFFCD7F32.toInt()

    fun build(p: SkyblockProfile, garden: GardenData?, width: Int): Component {
        val children = mutableListOf<Component>(
            PageKit.skillHeader(p, SkillType.FARMING, width, "${p.jacobs.contests} Jacob's contests"),
        )
        if (garden == null) {
            children += Text("Loading garden…", Theme.TEXT_MUTED)
        } else {
            children += gardenSummary(garden, width)
            children += crops(garden, width)
        }
        children += mutations(p.greenhouse, width)
        children += medals(p.jacobs, width)
        return Column(children, spacing = 12)
    }

    private fun gardenSummary(g: GardenData, width: Int): Component {
        val maxed = g.level >= g.maxLevel
        val fg = if (maxed) Theme.GOLD else Theme.ACCENT
        val levelText = if (maxed) "Garden Level ${g.level} · MAX" else "Garden Level ${g.level}"
        return Column(
            SpaceBetween(
                width,
                Row(Item(PageKit.icon("oak_sapling"), 12, tooltip = false), Text(levelText, fg), spacing = 5, align = VAlign.CENTER),
                Text("${g.plotsUnlocked}/${g.plotsTotal} plots", Theme.TEXT_MUTED),
            ),
            ProgressBar(width, 4, g.levelProgress, fg, Theme.SURFACE_ALT),
            PageKit.tileRow(
                width,
                listOf(
                    "Growth Speed" to ("${g.growthSpeed}" to Theme.GREEN),
                    "Yield" to ("${g.yieldLevel}" to Theme.GREEN),
                    "Visitors Served" to ("${g.uniqueVisitors}" to Theme.ACCENT),
                    "Compost" to (Format.compact(g.composterCompost) to Theme.GOLD),
                ),
            ),
            spacing = 6,
        )
    }

    private fun crops(g: GardenData, width: Int): Component {
        val cellW = PageKit.cellW(width, 2)
        return PageKit.grid(g.crops.map { c -> cropCell(c.display, c.icon, c.collected, c.upgrade, cellW) }, width, cols = 2)
    }

    private fun cropCell(name: String, icon: String, collected: Long, upgrade: Int, cellW: Int): Component {
        val tierColor = if (upgrade >= GardenRegistry.CROP_UPGRADE_MAX) Theme.GOLD else Theme.ACCENT
        return Row(
            Item(PageKit.icon(icon), 14, tooltip = false),
            SpaceBetween(
                cellW - 14 - 5,
                Text(name, Theme.TEXT),
                Row(
                    Text("T$upgrade", tierColor),
                    Text(Format.compact(collected), Theme.TEXT_MUTED),
                    spacing = 6,
                    align = VAlign.CENTER,
                ),
            ),
            spacing = 5,
            align = VAlign.CENTER,
        )
    }

    private fun mutations(gh: GreenhouseData, width: Int): Component {
        val cellW = PageKit.cellW(width, 2)
        val cells = GardenRegistry.GREENHOUSE.map { mutationCell(it, gh, cellW) }
        return Column(
            Row(
                Text("Mutations", Theme.TEXT),
                Text("${gh.analyzed.size}/${GardenRegistry.GREENHOUSE.size} analyzed", Theme.TEXT_MUTED),
                Text("${gh.copper} copper", Theme.GOLD),
                spacing = 12,
                align = VAlign.CENTER,
            ),
            PageKit.grid(cells, width, cols = 2),
            spacing = 6,
        )
    }

    private fun mutationCell(key: String, gh: GreenhouseData, cellW: Int): Component {
        val analyzed = key in gh.analyzed
        val discovered = key in gh.discovered
        val (label, color) = when {
            analyzed -> "Analyzed" to Theme.GOLD
            discovered -> "Discovered" to Theme.GREEN
            else -> "—" to Theme.TEXT_MUTED
        }
        val nameColor = if (discovered) Theme.TEXT else Theme.TEXT_MUTED
        return Row(
            Item(mutationIcon(key), 14, tooltip = false),
            SpaceBetween(cellW - 14 - 5, Text(GardenRegistry.greenhouseName(key), nameColor), Text(label, color)),
            spacing = 5,
            align = VAlign.CENTER,
        )
    }

    private fun mutationIcon(key: String): ItemStack =
        GardenRegistry.greenhouseSkulls[key]?.let { SkullItems.fromTexture(it) } ?: PageKit.icon("fern")

    private fun medals(j: schrumbo.pv.data.JacobsData, width: Int): Component = Column(
        PageKit.tileRow(
            width,
            listOf(
                "Diamond" to ("${j.diamond}" to DIAMOND),
                "Platinum" to ("${j.platinum}" to PLATINUM),
                "Gold" to ("${j.gold}" to GOLD),
            ),
        ),
        PageKit.tileRow(
            width,
            listOf(
                "Silver" to ("${j.silver}" to SILVER),
                "Bronze" to ("${j.bronze}" to BRONZE),
                "Contests" to (Format.compact(j.contests.toLong()) to Theme.TEXT),
            ),
        ),
        spacing = 8,
    )
}
