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
import schrumbo.pv.ui.component.Tooltip
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

    private const val HOTF_TEX = "ewogICJ0aW1lc3RhbXAiIDogMTcxNzAyMTQ2Njk1NSwKICAicHJvZmlsZUlkIiA6ICIzZGE2ZDgxOTI5MTY0MTNlODhlNzg2MjQ3NzA4YjkzZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJGZXJTdGlsZSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS81ZWY1MzliMTY1MTI1Y2ZhNDZiMDZmZmI5NjU5ZTdjZjg5MDg0YmJkM2VkZTFiMzE0ZWRjOGY0NDMzNDNkNjFjIiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0="

    /** Heart-of-the-Forest head, for the tree sub-tab icon. */
    val hotfIcon: ItemStack by lazy { SkullItems.fromTexture(HOTF_TEX) }

    private const val SLOT = 20
    private const val GAP = 2
    private const val GEAR_ROWS = 4
    private const val GEAR_W = SLOT * 3 + GAP * 2
    private const val GEAR_H = SLOT * GEAR_ROWS + GAP * (GEAR_ROWS - 1)
    private const val WHISPER_W = 110
    private const val TOP_GAP = 18

    /** General sub-page: gear · whispers · tree gifts side by side, all the same height — mirrors Mining. */
    fun general(p: SkyblockProfile, width: Int): Component {
        val f = p.foraging
        val treesW = (width - GEAR_W - WHISPER_W - TOP_GAP * 2).coerceIn(220, 320)
        return Column(
            PageKit.skillHeader(p, SkillType.FORAGING, width, extra = "Heart of the Forest ${f.hotfLevel}"),
            Spacer(0, 12),
            Row(gear(p), whispersColumn(f), trees(f, treesW), spacing = TOP_GAP, align = VAlign.TOP),
            spacing = 0,
        )
    }

    /** HotF tree sub-page: the perk tree with in-game-style tooltips, centred. */
    fun tree(p: SkyblockProfile, width: Int): Component {
        val grid = PerkTreeView.render(schrumbo.pv.data.PerkRegistry.hotf, p.foraging.nodes, p.foraging.hotfLevel, PerkTreeView.FORAGING)
        return Frame(width, grid.height, grid, hAlign = HAlign.CENTER, vAlign = VAlign.TOP)
    }

    /** Gear as a clean 3×4 grid: armor · equipment · axes, every column padded to [GEAR_ROWS]. */
    private fun gear(p: SkyblockProfile): Component = Row(
        slotColumn(p.foragingArmor),
        slotColumn(p.foragingEquipment),
        slotColumn(p.foragingTools),
        spacing = GAP,
        align = VAlign.TOP,
    )

    private fun slotColumn(items: List<ItemStack>): Component =
        Column((0 until GEAR_ROWS).map { slot(items.getOrNull(it) ?: ItemStack.EMPTY) }, spacing = GAP)

    private fun slot(stack: ItemStack): Component {
        val inner: Component = if (stack.isEmpty) Spacer(SLOT - 4, SLOT - 4) else Item(stack, SLOT - 4, tooltip = true, decorations = true)
        return Frame(SLOT, SLOT, inner, Theme.SURFACE_ALT, Theme.BORDER, HAlign.CENTER, VAlign.CENTER)
    }

    /**
     * Currency chips stacked beside the gear, sized so the column ends flush with the gear grid.
     * Two boxes (= 2 gear slots each) for now; a future second whisper type slots in here too.
     */
    private fun whispersColumn(f: ForagingData): Component = Column(
        whisperChip(GEAR_H, "Forest Whispers", f.whispers + f.whispersSpent, Theme.GREEN, PageKit.icon("cyan_dye")),
        spacing = GAP,
    )

    private fun whisperChip(h: Int, label: String, total: Long, color: Int, icon: ItemStack): Component {
        val body = Row(Item(icon, 12, tooltip = false), Text(Format.compact(total), color), spacing = 5, align = VAlign.CENTER)
        return Tooltip(
            Frame(WHISPER_W, h, body, Theme.SURFACE_ALT, Theme.BORDER, HAlign.CENTER, VAlign.CENTER),
            listOf("§f$label", "§7${Format.compact(total)} §8total"),
        )
    }

    /** Tree-gift cards filling the gear height, so the column ends flush with gear and whispers. */
    private fun trees(f: ForagingData, width: Int): Component {
        if (f.trees.isEmpty()) return Frame(width, GEAR_H, Text("No tree gifts yet", Theme.TEXT_MUTED), hAlign = HAlign.START, vAlign = VAlign.TOP)
        val h = PageKit.fillHeights(f.trees.size, GEAR_H, GAP)
        return Column(f.trees.mapIndexed { i, t -> treeCard(t.name, t.tier, t.gifts, width, h[i]) }, spacing = GAP)
    }

    private fun treeCard(name: String, tier: Int, gifts: Long, width: Int, h: Int): Component {
        val maxed = tier >= MILESTONE_MAX
        val tierColor = if (maxed) Theme.GOLD else Theme.ACCENT
        val pad = 6
        val iconSize = 20
        val body = Column(
            Row(Text(name, Theme.TEXT), Text("$tier/$MILESTONE_MAX", tierColor), spacing = 8, align = VAlign.CENTER),
            Row(
                ProgressBar(120, 4, tier.toDouble() / MILESTONE_MAX, tierColor, Theme.SURFACE),
                Text("${Format.compact(gifts)} gifts", Theme.TEXT_MUTED, scale = Text.SMALL),
                spacing = 8,
                align = VAlign.CENTER,
            ),
            spacing = 5,
        )
        val row = Row(Item(giftIcon(name), iconSize, tooltip = false), body, spacing = 8, align = VAlign.CENTER)
        return Frame(width, h, Row(Spacer(pad), row), Theme.SURFACE_ALT, Theme.BORDER, HAlign.START, VAlign.CENTER)
    }
}
