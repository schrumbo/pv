package schrumbo.pv.ui.page

import schrumbo.pv.data.ResolvedTree
import schrumbo.pv.data.SkillTreeRegistry
import schrumbo.pv.data.TreeNode
import schrumbo.pv.data.TreeNodeDef
import schrumbo.pv.ui.Theme
import schrumbo.pv.ui.component.Column
import schrumbo.pv.ui.component.Component
import schrumbo.pv.ui.component.Item
import schrumbo.pv.ui.component.Row
import schrumbo.pv.ui.component.Spacer
import schrumbo.pv.ui.component.Text
import schrumbo.pv.ui.component.Tooltip
import schrumbo.pv.util.Format

/** Renders a perk tree (HOTM / Foraging) as a fixed grid of item icons; the item encodes node state. */
object SkillTreeView {

    private const val CELL = 18
    private const val GAP = 2

    /** A one-line "X maxed · Y/Z unlocked" badge for a resolved [tree] (placed nodes + extras). */
    fun badge(tree: ResolvedTree): Component {
        val all = tree.nodes + tree.extras
        val maxed = tree.nodes.count { it.maxed }
        val unlocked = all.count { it.unlocked }
        return Text("$maxed maxed · $unlocked/${all.size} unlocked", Theme.TEXT_MUTED)
    }

    fun resolve(defs: List<TreeNodeDef>, levels: Map<String, Int>): ResolvedTree =
        SkillTreeRegistry.resolve(defs, levels)

    fun grid(tree: ResolvedTree): Component {
        val byPos = tree.nodes.associateBy { it.def.col to it.def.row }
        // Repo rows are bottom-up (tier 1 = row 0), the in-game tree shows tier 1 at the bottom → flip.
        val rows = (tree.rows - 1 downTo 0).map { r ->
            Row(
                (0 until tree.cols).map { c -> byPos[c to r]?.let { cell(it) } ?: Spacer(CELL, CELL) },
                spacing = GAP,
            )
        }
        val gridCol = Column(rows, spacing = GAP)
        if (tree.extras.isEmpty()) return gridCol
        val cols = tree.cols.coerceAtLeast(1)
        val extraRows = tree.extras.map { cell(it) }.chunked(cols).map { Row(it, spacing = GAP) }
        return Column(
            gridCol,
            Spacer(0, 3),
            Text("OTHER", Theme.TEXT_MUTED, shadow = false),
            Column(extraRows, spacing = GAP),
            spacing = GAP,
        )
    }

    /** The block/item a node is drawn as: core=redstone block, abilities=emerald/coal block, maxed &
     *  unlevelable=diamond, levelled=emerald, locked=coal. */
    private fun itemId(n: TreeNode): String = when {
        n.def.type == "C" -> "redstone_block"
        n.def.type == "A" -> if (n.unlocked) "emerald_block" else "coal_block"
        !n.unlocked -> "coal"
        n.maxed || n.def.type == "U" -> "diamond"
        else -> "emerald"
    }

    private fun cell(n: TreeNode): Component {
        val corner = if (n.def.type == "P" && n.unlocked) n.level.toString() else null
        val icon = Item(PageKit.icon(itemId(n)), CELL, tooltip = false, corner = corner)
        val levelLine = when {
            n.def.type == "P" -> "§7Level §f${n.level}§7/§f${n.def.max}"
            n.level > 1 -> "§7Level §f${n.level}"
            else -> if (n.unlocked) "§aUnlocked" else "§8Locked"
        }
        return Tooltip(icon, listOf("§e${n.def.name}", levelLine))
    }
}
