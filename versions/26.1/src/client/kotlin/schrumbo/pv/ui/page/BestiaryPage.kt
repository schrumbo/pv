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
import schrumbo.pv.ui.component.Column
import schrumbo.pv.ui.component.Component
import schrumbo.pv.ui.component.Item
import schrumbo.pv.ui.component.ProgressBar
import schrumbo.pv.ui.component.Row
import schrumbo.pv.ui.component.SpaceBetween
import schrumbo.pv.ui.component.Text
import schrumbo.pv.ui.component.Tooltip
import schrumbo.pv.ui.component.VAlign
import schrumbo.pv.util.Format

/** Bestiary page: a fixed left island rail and the selected island's mob tiers as a scrolling grid. */
object BestiaryPage {

    const val RAIL_W = 22
    private const val COL_GAP = 14

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

    fun islands(p: SkyblockProfile): List<IslandProgress> = BestiaryRegistry.resolve(p.bestiaryKills)

    fun header(islands: List<IslandProgress>, width: Int): Component {
        val maxed = islands.sumOf { it.maxedCount }
        val total = islands.sumOf { it.total }
        val kills = islands.sumOf { i -> i.mobs.sumOf { it.kills } }
        return Row(
            Text("${Format.compact(kills)} kills", Theme.TEXT_MUTED),
            Text("$maxed/$total maxed", Theme.TEXT_MUTED),
            spacing = 14,
            align = VAlign.CENTER,
        )
    }

    fun rail(islands: List<IslandProgress>, active: Int, onIsland: (Int) -> Unit): Component =
        Column(islands.mapIndexed { i, island -> islandRow(island, i == active) { onIsland(i) } }, spacing = 1)

    private fun islandRow(island: IslandProgress, active: Boolean, onClick: () -> Unit): Component =
        PageKit.bookmark(RAIL_W, icon(ISLAND_ICONS[island.def.key] ?: "paper"), island.def.name, active, onClick)

    fun grid(island: IslandProgress, width: Int): Component {
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
        val tierLabel = "${m.tier}/${m.maxTier}"
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
                Text("${Format.compact(m.kills)} kills", Theme.TEXT_MUTED),
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
