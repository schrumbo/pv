package schrumbo.pv.ui.page

import schrumbo.pv.data.Perk
import schrumbo.pv.data.PerkRegistry
import schrumbo.pv.ui.component.Column
import schrumbo.pv.ui.component.Component
import schrumbo.pv.ui.component.Item
import schrumbo.pv.ui.component.Row
import schrumbo.pv.ui.component.Spacer
import schrumbo.pv.ui.component.Tooltip

/**
 * Renders a HotM / HotF perk tree in the in-game style: every node is the vanilla item Hypixel uses
 * for that node type and unlock state (copied from skyblock-pv's `SkillTreeItems`), tier 1 at the
 * bottom, with the full per-node tooltip.
 */
object PerkTreeView {

    private const val CELL = 18
    private const val GAP = 2

    /** Per-state node items for one tree, mirroring meowdding's `SkillTreeItems`. */
    data class Icons(
        val tierLocked: String, val tierUnlocked: String, val tierUnlocking: String,
        val coreMax: String, val coreUnlocked: String, val coreLeveling: String, val coreLocked: String,
        val abilityUnlocked: String, val abilityLocked: String,
        val nodeMax: String, val nodeUnlocked: String, val nodeLocked: String,
    )

    val MINING = Icons(
        "red_stained_glass_pane", "green_stained_glass_pane", "yellow_stained_glass_pane",
        "diamond_block", "copper_block", "redstone_block", "bedrock",
        "redstone_block", "coal_block",
        "diamond", "emerald", "coal",
    )
    val FORAGING = Icons(
        "red_stained_glass_pane", "green_stained_glass_pane", "yellow_stained_glass_pane",
        "oak_wood", "stripped_birch_wood", "stripped_oak_wood", "stripped_pale_oak_wood",
        "cherry_sapling", "pale_oak_sapling",
        "oak_log", "stripped_oak_log", "pale_oak_button",
    )

    fun render(perks: List<Perk>, levels: Map<String, Int>, treeLevel: Int, icons: Icons): Component {
        val minX = perks.minOf { it.x }
        val maxX = perks.maxOf { it.x }
        val maxY = perks.maxOf { it.y }
        val maxTier = perks.filter { it.type == "TIER" }.maxOfOrNull { PerkRegistry.tierNumber(it.name) } ?: 10
        val byPos = HashMap<Long, Perk>()
        perks.forEach { byPos[key(it.x - minX, it.y)] = it }

        val rows = (maxY downTo 0).map { r ->
            Row((0..(maxX - minX)).map { c ->
                byPos[key(c, r)]?.let { node(it, levels, treeLevel, maxTier, icons) } ?: Spacer(CELL, CELL)
            }, spacing = GAP)
        }
        return Column(rows, spacing = GAP)
    }

    private fun key(x: Int, y: Int) = x.toLong() shl 32 or (y.toLong() and 0xFFFFFFFFL)

    private fun node(perk: Perk, levels: Map<String, Int>, treeLevel: Int, maxTier: Int, icons: Icons): Component {
        if (perk.type == "SPACER") return Spacer(CELL, CELL)
        val level = perk.id?.let { levels[it] } ?: -1
        val corner = (level.takeIf { it > 1 && perk.type != "TIER" })?.toString()
        val item = Item(PageKit.icon(iconId(perk, level, treeLevel, maxTier, icons)), CELL, tooltip = false, corner = corner)
        val tip = PerkRegistry.tooltip(perk, level, treeLevel)
        return if (tip.isEmpty()) item else Tooltip(item, tip)
    }

    private fun iconId(perk: Perk, level: Int, treeLevel: Int, maxTier: Int, ic: Icons): String = when (perk.type) {
        "TIER" -> {
            val n = PerkRegistry.tierNumber(perk.name)
            when {
                treeLevel >= n -> ic.tierUnlocked
                treeLevel + 1 >= n -> ic.tierUnlocking
                else -> ic.tierLocked
            }
        }
        "CORE" -> when {
            treeLevel >= maxTier -> ic.coreMax
            treeLevel <= 0 -> ic.coreLocked
            treeLevel == 1 -> ic.coreUnlocked
            else -> ic.coreLeveling
        }
        "ABILITY" -> if (level > 0) ic.abilityUnlocked else ic.abilityLocked
        else -> when {
            level < 0 -> ic.nodeLocked
            perk.max != null && level >= perk.max -> ic.nodeMax
            level > 0 -> ic.nodeUnlocked
            else -> ic.nodeLocked
        }
    }
}
