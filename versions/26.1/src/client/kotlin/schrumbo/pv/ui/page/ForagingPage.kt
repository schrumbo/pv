package schrumbo.pv.ui.page

import schrumbo.pv.data.ForagingData
import schrumbo.pv.data.SkillTreeRegistry
import schrumbo.pv.data.SkillType
import schrumbo.pv.data.SkyblockProfile
import schrumbo.pv.ui.Theme
import schrumbo.pv.ui.component.Column
import schrumbo.pv.ui.component.Component
import schrumbo.pv.ui.component.Item
import schrumbo.pv.ui.component.Row
import schrumbo.pv.ui.component.Text
import schrumbo.pv.ui.component.Tooltip
import schrumbo.pv.util.Format

/** Foraging (Galatea) page: the Heart-of-the-Forest perk tree and per-tree gift milestones. */
object ForagingPage {

    /** Galatea has two tree-gift types (Fig, Mangrove), each with 7 milestone tiers. */
    private val WOOD = mapOf("Fig" to "stripped_birch_log", "Mangrove" to "mangrove_log")
    private val SKULL_KEY = mapOf("Fig" to "FIG_LOG", "Mangrove" to "MANGROVE_LOG")
    private const val MILESTONE_MAX = 7

    /** The tree's real head texture (from the collection-skull data) or a vanilla wood fallback. */
    private fun treeIcon(name: String): net.minecraft.world.item.ItemStack {
        SKULL_KEY[name]?.let { schrumbo.pv.data.CollectionsRegistry.skulls[it] }
            ?.let { return schrumbo.pv.render.SkullItems.fromTexture(it) }
        return PageKit.icon(WOOD[name] ?: "oak_log")
    }

    fun build(p: SkyblockProfile, width: Int): Component {
        val f = p.foraging
        val tree = SkillTreeView.resolve(SkillTreeRegistry.foraging, f.nodes)
        val total = f.whispers + f.whispersSpent
        return Column(
            PageKit.skillHeader(p, SkillType.FORAGING, width, "${Format.compact(total)} total forest whispers"),
            PageKit.section("HEART OF THE FOREST", width, SkillTreeView.grid(tree)),
            PageKit.section("TREE GIFTS", width, trees(f)),
            spacing = 10,
        )
    }

    private fun trees(f: ForagingData): Component {
        if (f.trees.isEmpty()) return Text("No tree gifts yet", Theme.TEXT_MUTED)
        val cells = f.trees.map { tree ->
            val icon = Item(treeIcon(tree.name), 20, tooltip = false, corner = "${tree.tier}/$MILESTONE_MAX")
            Tooltip(
                icon,
                listOf(
                    "§a${tree.name}",
                    "§7Milestone: §f${tree.tier}§7/§f$MILESTONE_MAX",
                    "§7Total gifts: §f${Format.compact(tree.gifts)}",
                ),
            )
        }
        return Row(cells, spacing = 4)
    }
}
