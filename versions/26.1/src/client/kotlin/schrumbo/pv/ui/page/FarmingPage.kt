package schrumbo.pv.ui.page

import net.minecraft.world.item.ItemStack
import schrumbo.pv.data.GardenData
import schrumbo.pv.data.GardenRegistry
import schrumbo.pv.data.GreenhouseData
import schrumbo.pv.data.JacobsData
import schrumbo.pv.data.SkillType
import schrumbo.pv.data.SkyblockProfile
import schrumbo.pv.ui.Theme
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

/** Farming page (3 sub-tabs): General (garden + Jacob's), Greenhouse mutations, and the Composter. */
object FarmingPage {

    private const val DIAMOND = 0xFF4DD0E1.toInt()
    private const val PLATINUM = 0xFFB9D6E0.toInt()
    private const val GOLD = 0xFFFFB534.toInt()
    private const val SILVER = 0xFFD8D8D8.toInt()
    private const val BRONZE = 0xFFCD7F32.toInt()

    // ---- General -----------------------------------------------------------------------------

    fun general(p: SkyblockProfile, garden: GardenData?, width: Int): Component {
        val children = mutableListOf<Component>(
            PageKit.skillHeader(p, SkillType.FARMING, width, "${p.jacobs.contests} Jacob's contests"),
            schrumbo.pv.ui.component.Spacer(0, 4),
        )
        if (garden == null) {
            children += Text("Loading garden…", Theme.TEXT_MUTED)
        } else {
            children += gardenSummary(p, garden, width)
            children += section("Crop Milestones", crops(garden, width))
        }
        children += section("Jacob's Medals", medals(p.jacobs, width))
        return Column(children, spacing = 12)
    }

    /** A muted section caption above its content. */
    private fun section(title: String, content: Component): Component =
        Column(Text(title.uppercase(), Theme.TEXT_MUTED, scale = Text.SMALL), schrumbo.pv.ui.component.Spacer(0, 4), content, spacing = 0)

    private fun gardenSummary(p: SkyblockProfile, g: GardenData, width: Int): Component {
        val overflow = g.level >= g.maxLevel
        val fg = if (overflow) Theme.GOLD else Theme.ACCENT
        return Column(
            SpaceBetween(
                width,
                Row(Item(PageKit.icon("oak_sapling"), 12, tooltip = false), Text("Garden Level ${g.level}", fg), spacing = 5, align = VAlign.CENTER),
                Text("${g.plotsUnlocked}/${g.plotsTotal} plots", Theme.TEXT_MUTED),
            ),
            ProgressBar(width, 4, g.levelProgress, fg, Theme.SURFACE_ALT),
            PageKit.tileRow(
                width,
                listOf(
                    "Growth Speed" to ("${g.growthSpeed}" to Theme.GREEN),
                    "Yield" to ("${g.yieldLevel}" to Theme.GREEN),
                    "Visitors Served" to ("${g.uniqueVisitors}" to Theme.ACCENT),
                    "Visits" to (Format.compact(g.visits) to Theme.TEXT_MUTED),
                ),
            ),
            PageKit.tileRow(
                width,
                listOf(
                    "Copper" to (Format.compact(p.greenhouse.copper.toLong()) to BRONZE),
                    "Larva Consumed" to ("${p.greenhouse.larvaConsumed}" to Theme.GREEN),
                    "Farming Cap" to ("${50 + p.jacobs.farmingCap}" to Theme.ACCENT),
                    "Double Drops" to ("${p.jacobs.doubleDrops}/15" to Theme.GREEN),
                ),
            ),
            spacing = 6,
        )
    }

    private fun crops(g: GardenData, width: Int): Component {
        val cols = 2
        val cellW = PageKit.cellW(width, cols)
        return PageKit.grid(g.crops.map { cropCell(it, cellW) }, width, cols = cols, rowGap = 8)
    }

    /** A crop's (overflow) milestone tier with a progress bar and its collected total. */
    private fun cropCell(c: schrumbo.pv.data.GardenCrop, cellW: Int): Component {
        val color = if (c.milestoneMax) Theme.GOLD else Theme.ACCENT
        val bodyW = cellW - 16 - 6
        val body = Column(
            SpaceBetween(
                bodyW,
                Text(c.display, Theme.TEXT),
                Row(Text("Tier ${c.milestoneTier}", color), Text(Format.compact(c.collected), Theme.TEXT_MUTED), spacing = 6, align = VAlign.CENTER),
            ),
            ProgressBar(bodyW, 3, c.milestoneProgress, color, Theme.SURFACE_ALT),
            spacing = 3,
        )
        return Row(Item(PageKit.icon(c.icon), 16, tooltip = false), body, spacing = 6, align = VAlign.CENTER)
    }

    private fun medals(j: JacobsData, width: Int): Component = Column(
        PageKit.tileRow(
            width,
            listOf(
                "Diamond" to ("${j.diamond}" to DIAMOND),
                "Platinum" to ("${j.platinum}" to PLATINUM),
                "Gold" to ("${j.gold}" to GOLD),
                "Silver" to ("${j.silver}" to SILVER),
                "Bronze" to ("${j.bronze}" to BRONZE),
            ),
        ),
        PageKit.tileRow(
            width,
            listOf(
                "Unique Gold" to ("${j.uniqueGold}" to GOLD),
                "Unique Silver" to ("${j.uniqueSilver}" to SILVER),
                "Unique Bronze" to ("${j.uniqueBronze}" to BRONZE),
                "Contests" to (Format.compact(j.contests.toLong()) to Theme.TEXT),
            ),
        ),
        spacing = 8,
    )

    // ---- Greenhouse (mutations) --------------------------------------------------------------

    private const val SLOT = 20
    private const val SLOT_GAP = 2
    private val GRAY_DYE: ItemStack by lazy { PageKit.icon("gray_dye") }

    /** Greenhouse mutations as head-only rows, one rarity per row (Legendary on top → Common). */
    fun greenhouse(p: SkyblockProfile, width: Int): Component {
        val gh = p.greenhouse
        val analyzed = gh.analyzed.map { it.uppercase() }.toSet()
        val discovered = gh.discovered.map { it.uppercase() }.toSet()
        // Non-analyzable mutations (e.g. Deadplant) don't count toward the analysed goal.
        val analyzableTotal = GardenRegistry.MUTATIONS.count { it.analyzable }
        val analyzedCount = GardenRegistry.MUTATIONS.count { it.analyzable && it.id in analyzed }

        val rows = GardenRegistry.RARITY_ORDER.mapNotNull { rarity ->
            val muts = GardenRegistry.MUTATIONS.filter { it.rarity == rarity }
            if (muts.isEmpty()) null
            else Row(muts.map { mutationSlot(it, analyzed, discovered) }, spacing = SLOT_GAP)
        }
        return Column(
            PageKit.tileRow(
                width,
                listOf(
                    "Analyzed" to ("$analyzedCount/$analyzableTotal" to Theme.GOLD),
                    "Discovered" to ("${discovered.size}/${GardenRegistry.MUTATIONS.size}" to Theme.GREEN),
                    "Copper" to (Format.compact(gh.copper.toLong()) to BRONZE),
                ),
            ),
            schrumbo.pv.ui.component.Spacer(0, 10),
            Column(rows, spacing = SLOT_GAP),
            spacing = 0,
        )
    }

    private fun mutationSlot(m: GardenRegistry.Mutation, analyzed: Set<String>, discovered: Set<String>): Component {
        // Non-analyzable mutations always show their render; analysed ones too — everything else is grey.
        val shown = !m.analyzable || m.id in analyzed
        val isDiscovered = shown || m.id in discovered
        val icon = if (shown) GardenRegistry.mutationRender(m.id).takeIf { !it.isEmpty } ?: GRAY_DYE else GRAY_DYE
        val status = when {
            !m.analyzable -> "§7Not analysable"
            m.id in analyzed -> "§6§lANALYZED"
            m.id in discovered -> "§a§lDISCOVERED"
            else -> "§c§lUndiscovered"
        }
        val tip = listOf(GardenRegistry.rarityColorCode(m.rarity) + m.name, "", status)
        return Tooltip(
            Frame(SLOT, SLOT, Item(icon, SLOT - 4, tooltip = false), Theme.SURFACE_ALT, Theme.BORDER, HAlign.CENTER, VAlign.CENTER),
            tip,
        )
    }

    // ---- Composter ---------------------------------------------------------------------------

    private val COMPOSTER_UPGRADES = listOf(
        "SPEED" to "Speed", "MULTI_DROP" to "Multi Drop", "FUEL_CAP" to "Fuel Cap",
        "ORGANIC_MATTER_CAP" to "Organic Matter Cap", "COST_REDUCTION" to "Cost Reduction",
    )
    private const val COMPOSTER_UPGRADE_MAX = 25

    fun composter(garden: GardenData?, width: Int): Component {
        if (garden == null) return Text("Loading garden…", Theme.TEXT_MUTED)
        return Column(
            section(
                "Stored",
                PageKit.tileRow(
                    width,
                    listOf(
                        "Organic Matter" to (Format.compact(garden.composterOrganic) to Theme.GREEN),
                        "Fuel" to (Format.compact(garden.composterFuel) to Theme.GOLD),
                        "Compost" to (Format.compact(garden.composterCompost) to Theme.ACCENT),
                        "Items" to (Format.compact(garden.composterItems) to Theme.TEXT_MUTED),
                    ),
                ),
            ),
            section("Upgrades", upgrades(garden, width)),
            spacing = 12,
        )
    }

    private fun upgrades(g: GardenData, width: Int): Component {
        val cellW = PageKit.cellW(width, 2)
        val rows = COMPOSTER_UPGRADES.map { (key, name) ->
            val level = g.composterUpgrades[key] ?: 0
            val maxed = level >= COMPOSTER_UPGRADE_MAX
            val color = if (maxed) Theme.GOLD else Theme.ACCENT
            SpaceBetween(cellW, Text(name, Theme.TEXT), Text("$level/$COMPOSTER_UPGRADE_MAX", color))
        }
        return PageKit.grid(rows, width, cols = 2)
    }
}
