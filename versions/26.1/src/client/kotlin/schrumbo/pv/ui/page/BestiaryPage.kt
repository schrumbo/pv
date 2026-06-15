package schrumbo.pv.ui.page

import net.minecraft.client.Minecraft
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import schrumbo.pv.data.BestiaryRegistry
import schrumbo.pv.data.IslandProgress
import schrumbo.pv.data.MobTier
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

/** Bestiary page: a left island rail and the selected island's mob tiers as a 2-column grid. */
object BestiaryPage {

    private const val RAIL_W = 120
    private const val GAP = 14
    private const val COL_GAP = 14
    private const val ROW_H = 14

    /** Representative vanilla icon per island key, shown in the left rail. */
    private val ISLAND_ICONS = mapOf(
        "dynamic" to "grass_block", "hub" to "oak_sapling", "farming_1" to "wheat",
        "combat_1" to "string", "combat_3" to "ender_pearl", "crimson_isle" to "blaze_powder",
        "mining_2" to "iron_pickaxe", "mining_3" to "diamond_pickaxe", "crystal_hollows" to "amethyst_shard",
        "foraging_1" to "oak_log", "foraging_2" to "jungle_log", "spooky_festival" to "carved_pumpkin",
        "mythological_creatures" to "bone", "jerry" to "snowball", "kuudra" to "magma_cream",
        "fishing" to "fishing_rod", "catacombs" to "skeleton_skull", "garden" to "jungle_sapling",
        "lotus_atoll" to "lily_pad",
    )

    fun build(p: SkyblockProfile, width: Int, selected: Int, onIsland: (Int) -> Unit): Component {
        val islands = BestiaryRegistry.resolve(p.bestiaryKills)
        val active = selected.coerceIn(0, islands.size - 1)
        return Column(
            header(islands, width),
            Row(
                rail(islands, active, onIsland),
                grid(islands[active], width - RAIL_W - GAP),
                spacing = GAP,
                align = VAlign.TOP,
            ),
            spacing = 8,
        )
    }

    private fun header(islands: List<IslandProgress>, width: Int): Component {
        val maxed = islands.sumOf { it.maxedCount }
        val total = islands.sumOf { it.total }
        val tiers = islands.sumOf { i -> i.mobs.sumOf { it.tier } }
        return Column(
            Row(
                Text("Bestiary", Theme.TEXT),
                Text("· $maxed/$total maxed · $tiers tiers", Theme.TEXT_MUTED),
                spacing = 6,
                align = VAlign.CENTER,
            ),
            Box(width, 1, Theme.BORDER),
            spacing = 4,
        )
    }

    private fun rail(islands: List<IslandProgress>, active: Int, onIsland: (Int) -> Unit): Component =
        Column(islands.mapIndexed { i, island -> islandRow(island, i == active) { onIsland(i) } }, spacing = 1)

    private fun islandRow(island: IslandProgress, active: Boolean, onClick: () -> Unit): Component {
        val color = if (active) Theme.ACCENT else Theme.TEXT_MUTED
        val row = Row(
            Box(2, 11, if (active) Theme.ACCENT else null),
            Item(icon(ISLAND_ICONS[island.def.key] ?: "paper"), 11, tooltip = false),
            Text(clip(island.def.name, RAIL_W - 26), color),
            spacing = 4,
            align = VAlign.CENTER,
        )
        val frame = Frame(RAIL_W, ROW_H, row, if (active) Theme.SURFACE_ALT else null, null, HAlign.START, VAlign.CENTER)
        return Clickable(frame, hoverFill = Theme.HOVER, onClick = onClick)
    }

    private fun grid(island: IslandProgress, width: Int): Component {
        val cellW = (width - COL_GAP) / 2
        val cells = island.mobs.map { mobCell(it, cellW) }
        val half = (cells.size + 1) / 2
        return Row(
            Column(cells.take(half), spacing = 5),
            Column(cells.drop(half), spacing = 5),
            spacing = COL_GAP,
            align = VAlign.TOP,
        )
    }

    private fun mobCell(m: MobTier, cellW: Int): Component {
        val innerW = cellW - 16 - 5
        val nameColor = if (m.maxed) Theme.GOLD else Theme.TEXT
        val tierLabel = when {
            m.maxed -> "MAX"
            m.tier == 0 -> "—"
            else -> Format.roman(m.tier)
        }
        val tierColor = when {
            m.maxed -> Theme.GOLD
            m.tier == 0 -> Theme.TEXT_MUTED
            else -> Theme.ACCENT
        }
        val fg = if (m.maxed) Theme.GOLD else Theme.ACCENT
        val cell = Row(
            Item(BestiaryRegistry.icon(m.def), 16, tooltip = false),
            Column(
                SpaceBetween(innerW, Text(clip(m.def.name, innerW - 30), nameColor), Text(tierLabel, tierColor)),
                ProgressBar(innerW, 3, m.progress, fg, Theme.SURFACE_ALT),
                spacing = 2,
            ),
            spacing = 5,
            align = VAlign.CENTER,
        )
        val nextLine = if (m.maxed) "§7Maxed" else "§7Next: §f${Format.compact(m.kills)}§7/§f${Format.compact(m.next)}"
        return Tooltip(cell, listOf("§f${m.def.name}", "§7Tier §f${m.tier}§7/§f${m.maxTier}", nextLine))
    }

    private fun clip(s: String, maxW: Int): String {
        val font = Minecraft.getInstance().font
        if (font.width(s) <= maxW) return s
        var t = s
        while (t.isNotEmpty() && font.width("$t…") > maxW) t = t.dropLast(1)
        return "$t…"
    }

    private fun icon(name: String): ItemStack {
        val id = Identifier.tryParse(name) ?: return ItemStack.EMPTY
        return ItemStack(BuiltInRegistries.ITEM.getValue(id))
    }
}
