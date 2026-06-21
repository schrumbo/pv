package schrumbo.pv.ui.page

import net.minecraft.world.item.ItemStack
import schrumbo.pv.data.CrystalSkulls
import schrumbo.pv.data.Fossils
import schrumbo.pv.data.MiningData
import schrumbo.pv.data.SkillType
import schrumbo.pv.data.SkillTreeRegistry
import schrumbo.pv.data.SkyblockProfile
import schrumbo.pv.render.SkullItems
import schrumbo.pv.ui.Theme
import schrumbo.pv.ui.component.Box
import schrumbo.pv.ui.component.Column
import schrumbo.pv.ui.component.Component
import schrumbo.pv.ui.component.Frame
import schrumbo.pv.ui.component.HAlign
import schrumbo.pv.ui.component.Item
import schrumbo.pv.ui.component.Overlay
import schrumbo.pv.ui.component.Row
import schrumbo.pv.ui.component.SpaceBetween
import schrumbo.pv.ui.component.Spacer
import schrumbo.pv.ui.component.Text
import schrumbo.pv.ui.component.Tooltip
import schrumbo.pv.ui.component.VAlign
import schrumbo.pv.util.Format

/** Mining page: the Heart-of-the-Mountain tree on the left, powder + Crystal Nucleus + Glacite right. */
object MiningPage {

    // Powder colours mirror skyblock-pv's PowderType: Mithril §2, Gemstone §d, Glacite §b.
    private const val MITHRIL = 0xFF00AA00.toInt()
    private const val GEMSTONE = 0xFFFF55FF.toInt()
    private const val GLACITE = 0xFF55FFFF.toInt()

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

    private const val HOTM_TEX = "ewogICJ0aW1lc3RhbXAiIDogMTYxOTAxNDUyMjgzOCwKICAicHJvZmlsZUlkIiA6ICIyMzYxYmNlZjZkMWM0ZWI1OGNhMDUzNDFjNGU4MGM0YyIsCiAgInByb2ZpbGVOYW1lIiA6ICJIaXJvQ2FwdWNjaW5vODciLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODZmMDZlYWEzMDA0YWVlZDA5YjNkNWI0NWQ5NzZkZTU4NGU2OTFjMGU5Y2FkZTEzMzYzNWRlOTNkMjNiOWVkYiIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9"

    /** Heart-of-the-Mountain head, for the tree sub-tab icon. */
    val hotmIcon: net.minecraft.world.item.ItemStack by lazy { SkullItems.fromTexture(HOTM_TEX) }

    private const val SLOT = 20
    private const val GAP = 2
    private const val GEAR_W = SLOT * 3 + GAP * 2
    private const val POWDER_W = 92
    private const val TOP_GAP = 18

    /** General sub-page: gear · powder · crystals side by side up top, stats and Glacite below. */
    fun general(p: SkyblockProfile, width: Int): Component {
        val m = p.mining
        val crystalsW = (width - GEAR_W - POWDER_W - TOP_GAP * 2).coerceAtLeast(120)
        return Column(
            PageKit.skillHeader(p, SkillType.MINING, width),
            Spacer(0, 12),
            Row(gear(p), powderColumn(m), crystals(m, crystalsW), spacing = TOP_GAP, align = VAlign.TOP),
            Spacer(0, 16),
            statsRow(m, width),
            Spacer(0, 12),
            fossilsRow(m, width),
            Spacer(0, 12),
            glacite(m, width),
            spacing = 0,
        )
    }

    /** Key mining counters as a tile row: HotM level, nucleus runs, fossil dust. */
    private fun statsRow(m: MiningData, width: Int): Component = PageKit.tileRow(
        width,
        listOf(
            "HotM Level" to ("${m.hotmLevel}" to Theme.ACCENT),
            "Nucleus Runs" to (Format.compact(m.nucleusRuns) to Theme.TEXT),
            "Commissions" to (Format.compact(m.commissions) to Theme.TEXT),
        ),
    )

    /** Translucent grey wash drawn over a fossil head that hasn't been donated yet. */
    private val FOSSIL_DIM = 0xB0141414.toInt()

    /** All fossils as their real heads, centred; un-donated ones are dimmed grey. */
    private fun fossilsRow(m: MiningData, width: Int): Component {
        val row = Row(
            Fossils.all.map { f ->
                // The API stores the short key (CLAW, FOOTPRINT, …), not the full item id (CLAW_FOSSIL).
                val donated = f.id.substringBefore("_") in m.donatedFossils
                val head = Item(SkullItems.fromTexture(f.texture), SLOT - 4, tooltip = false)
                val inner: Component = if (donated) head else Overlay(head, Box(SLOT - 4, SLOT - 4, FOSSIL_DIM))
                val tip = listOf(
                    (if (donated) "§a" else "§7") + f.name,
                    "",
                    if (donated) "§a§lDONATED" else "§7§lNot donated",
                )
                Tooltip(Frame(SLOT, SLOT, inner, Theme.SURFACE_ALT, Theme.BORDER, HAlign.CENTER, VAlign.CENTER), tip)
            },
            spacing = GAP,
        )
        return Frame(width, row.height, row, hAlign = HAlign.CENTER, vAlign = VAlign.TOP)
    }

    private const val GEAR_ROWS = 4

    /** Gear as a clean 3×4 grid: armor · equipment · drills, every column padded to [GEAR_ROWS]. */
    private fun gear(p: SkyblockProfile): Component = Row(
        slotColumn(p.miningArmor),
        slotColumn(p.miningEquipment),
        slotColumn(p.miningTools),
        spacing = GAP,
        align = VAlign.TOP,
    )

    private fun slotColumn(items: List<ItemStack>): Component =
        Column((0 until GEAR_ROWS).map { slot(items.getOrNull(it) ?: ItemStack.EMPTY) }, spacing = GAP)

    private fun slot(stack: ItemStack): Component {
        val inner: Component = if (stack.isEmpty) Spacer(SLOT - 4, SLOT - 4) else Item(stack, SLOT - 4, tooltip = true, decorations = true)
        return Frame(SLOT, SLOT, inner, Theme.SURFACE_ALT, Theme.BORDER, HAlign.CENTER, VAlign.CENTER)
    }

    /** HotM tree sub-page: the perk tree with in-game-style tooltips, centred. */
    fun tree(p: SkyblockProfile, width: Int): Component {
        val grid = PerkTreeView.render(schrumbo.pv.data.PerkRegistry.hotm, p.mining.nodes, p.mining.hotmLevel, PerkTreeView.MINING)
        return Frame(width, grid.height, grid, hAlign = HAlign.CENTER, vAlign = VAlign.TOP)
    }

    /** Powder chips stacked beside the gear: icon + centred total (available + spent), no label. */
    private fun powderColumn(m: MiningData): Component = Column(
        powderChip("Mithril", Format.compact(m.mithrilCount), MITHRIL, "prismarine_crystals"),
        powderChip("Gemstone", Format.compact(m.gemstoneCount), GEMSTONE, "amethyst_shard"),
        powderChip("Glacite", Format.compact(m.glaciteCount), GLACITE, "blue_ice"),
        spacing = GAP,
    )

    private fun powderChip(label: String, value: String, color: Int, iconId: String): Component {
        val body = Column(
            Item(PageKit.icon(iconId), 14, tooltip = false),
            Text(value, color),
            spacing = 3,
            align = HAlign.CENTER,
        )
        return Tooltip(
            Frame(POWDER_W, body.height + 10, body, Theme.SURFACE_ALT, Theme.BORDER, HAlign.CENTER, VAlign.CENTER),
            listOf("§f$label Powder", "§7$value §8earned"),
        )
    }

    /** All crystals in a 3-column grid; each cell fills its column (icon + name left, state dot right). */
    private fun crystals(m: MiningData, width: Int): Component {
        if (m.crystals.isEmpty()) return Text("No crystal data", Theme.TEXT_MUTED)
        val cols = 3
        val cellW = PageKit.cellW(width, cols)
        val rows = m.crystals.map { c ->
            val color = when (c.state) {
                "PLACED" -> Theme.ACCENT
                "FOUND" -> Theme.GREEN
                else -> Theme.TEXT_MUTED
            }
            SpaceBetween(
                cellW,
                Row(
                    Item(crystalIcon(c.name), 12, tooltip = false),
                    Text(PageKit.clip(c.name, cellW - 12 - 5 - 8), color),
                    spacing = 5,
                    align = VAlign.CENTER,
                ),
                Text("●", color),
            )
        }
        return PageKit.grid(rows, width, cols = cols)
    }

    private fun glacite(m: MiningData, width: Int): Component {
        val cols = 3
        val cellW = PageKit.cellW(width, cols)
        val rows = mutableListOf<Component>()
        m.corpses.forEach { (name, count) ->
            rows += SpaceBetween(cellW, Text("$name Corpses", Theme.TEXT), Text(Format.compact(count), Theme.TEXT_MUTED))
        }
        rows += SpaceBetween(cellW, Text("Mineshafts", Theme.TEXT), Text(Format.compact(m.mineshafts), Theme.TEXT_MUTED))
        return PageKit.grid(rows, width, cols = cols)
    }
}
