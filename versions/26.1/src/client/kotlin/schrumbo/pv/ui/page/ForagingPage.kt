package schrumbo.pv.ui.page

import net.minecraft.world.item.ItemStack
import schrumbo.pv.data.ForagingData
import schrumbo.pv.data.SkillTreeRegistry
import schrumbo.pv.data.SkillType
import schrumbo.pv.data.SkyblockProfile
import schrumbo.pv.render.SkullItems
import schrumbo.pv.ui.Theme
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
import schrumbo.pv.ui.component.VAlign
import schrumbo.pv.util.Format

/** Foraging (Galatea) page: the Heart-of-the-Forest tree left, forest whispers + tree gifts right. */
object ForagingPage {

    private const val MILESTONE_MAX = 7

    /** Real Galatea tree-gift head textures (NEU `SKYBLOCK_*_TREE_GIFTS`). */
    private val GIFT_TEX = mapOf(
        "Fig" to "ewogICJ0aW1lc3RhbXAiIDogMTcxNjQzNTYzNjIzMSwKICAicHJvZmlsZUlkIiA6ICJmMjc0YzRkNjI1MDQ0ZTQxOGVmYmYwNmM3NWIyMDIxMyIsCiAgInByb2ZpbGVOYW1lIiA6ICJIeXBpZ3NlbCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS83NDA3OGRhOWI5MzgyZTI3MmMzOTFiYTAxZTIwMGU2YzE3NWNmZWNkNGNjNWQwN2VlYmQ1ODNkYzA2MmYwMGQwIiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=",
        "Mangrove" to "ewogICJ0aW1lc3RhbXAiIDogMTcxNjQzNTg1ODY5MCwKICAicHJvZmlsZUlkIiA6ICIyMWUzNjdkNzI1Y2Y0ZTNiYjI2OTJjNGEzMDBhNGRlYiIsCiAgInByb2ZpbGVOYW1lIiA6ICJHZXlzZXJNQyIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9iMTgwZmI4NzA5OGY4YzMxOTM0ODY0OWQxOGEwMmEzYTc3M2EwNTlhNjBhMmNiZTYxMzgwYmFiMTkxZWMwZDdkIiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=",
    )

    private fun giftIcon(name: String): ItemStack =
        GIFT_TEX[name]?.let { SkullItems.fromTexture(it) } ?: PageKit.icon("oak_log")

    fun build(p: SkyblockProfile, width: Int): Component {
        val f = p.foraging
        val tree = SkillTreeView.resolve(SkillTreeRegistry.foraging, f.nodes)
        val grid = SkillTreeView.grid(tree)
        val total = f.whispers + f.whispersSpent
        val rw = (width - grid.width - 16).coerceAtLeast(150)
        val right = Column(
            whispersChip(rw, total),
            trees(f, rw),
            spacing = 8,
        )
        return Column(
            PageKit.skillHeader(p, SkillType.FORAGING, width),
            Spacer(0, 4),
            Row(grid, right, spacing = 16, align = VAlign.TOP),
            spacing = 8,
        )
    }

    /** Compact whispers chip mirroring the mining powder chips; value is earned total. */
    private fun whispersChip(w: Int, total: Long): Component {
        val body = Column(Text("Forest Whispers", Theme.TEXT_MUTED), Text(Format.compact(total), Theme.GREEN), spacing = 1)
        val row = Row(Item(PageKit.icon("cyan_dye"), 14, tooltip = false), body, spacing = 4, align = VAlign.CENTER)
        return Frame(w, row.height + 8, Row(Spacer(4), row), Theme.SURFACE_ALT, Theme.BORDER, HAlign.START, VAlign.CENTER)
    }

    /** One stat card per tree: gift head, name, milestone tier and total gifts with a tier progress bar. */
    private fun trees(f: ForagingData, width: Int): Component {
        if (f.trees.isEmpty()) return Text("No tree gifts yet", Theme.TEXT_MUTED)
        return Column(f.trees.map { treeCard(it.name, it.tier, it.gifts, width) }, spacing = 6)
    }

    private fun treeCard(name: String, tier: Int, gifts: Long, width: Int): Component {
        val maxed = tier >= MILESTONE_MAX
        val tierColor = if (maxed) Theme.GOLD else Theme.ACCENT
        val innerW = width - 24 - 5 - 8
        val body = Column(
            SpaceBetween(innerW, Text(name, Theme.TEXT), Text("$tier/$MILESTONE_MAX", tierColor)),
            ProgressBar(innerW, 3, tier.toDouble() / MILESTONE_MAX, tierColor, Theme.SURFACE_ALT),
            Text("${Format.compact(gifts)} gifts", Theme.TEXT_MUTED),
            spacing = 2,
        )
        val row = Row(Item(giftIcon(name), 24, tooltip = false), body, spacing = 5, align = VAlign.CENTER)
        return Frame(width, row.height + 8, Row(Spacer(4), row), Theme.SURFACE_ALT, Theme.BORDER, HAlign.START, VAlign.CENTER)
    }
}
