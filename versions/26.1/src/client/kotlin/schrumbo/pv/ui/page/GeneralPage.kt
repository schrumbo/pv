package schrumbo.pv.ui.page

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import schrumbo.pv.data.ProfileState
import schrumbo.pv.data.SkillEntry
import schrumbo.pv.data.SkillType
import schrumbo.pv.data.SkyblockProfile
import schrumbo.pv.data.SlayerEntry
import schrumbo.pv.ui.SkillTab
import schrumbo.pv.ui.Theme
import schrumbo.pv.ui.component.Box
import schrumbo.pv.ui.component.Clickable
import schrumbo.pv.ui.component.Column
import schrumbo.pv.ui.component.Component
import schrumbo.pv.ui.component.Frame
import schrumbo.pv.ui.component.Item
import schrumbo.pv.ui.component.PlayerRender
import schrumbo.pv.ui.component.ProgressBar
import schrumbo.pv.ui.component.Row
import schrumbo.pv.ui.component.SpaceBetween
import schrumbo.pv.ui.component.Spacer
import schrumbo.pv.ui.component.Text
import schrumbo.pv.ui.component.Tooltip
import schrumbo.pv.ui.component.VAlign
import schrumbo.pv.util.Format
import kotlin.math.roundToInt

/** Builds the General page as two independent columns so the main one can be scaled to fit. */
object GeneralPage {

    const val SIDE_WIDTH = 150
    const val GAP = 12
    private const val COL_GAP = 16
    private const val XP_WIDTH = 36
    private const val CARD_PAD = 4

    /**
     * Left column: compact stats. [onSkill] deep-links a skill into its Skills sub-page;
     * [onCatacombs] opens the Dungeons page. Caller positions/scales it.
     */
    fun main(
        state: ProfileState.Loaded,
        index: Int,
        width: Int,
        onSkill: (SkillType) -> Unit,
        onCatacombs: () -> Unit,
    ): Component {
        val p = state.profiles[index]
        return Column(
            SpaceBetween(
                width,
                Text("Hypixel Lvl ${state.hypixelLevel ?: "?"}", Theme.TEXT_MUTED),
                Text(p.gameMode?.replaceFirstChar { it.uppercase() } ?: "Classic", Theme.TEXT_MUTED),
            ),
            skyblockLevel(p, width),
            catacombs(p, width, onCatacombs),
            section("SKILLS", "SA %.2f".format(p.skillAverage), width, skills(p, width, onSkill)),
            section("SLAYERS", null, width, slayers(p, width)),
            guild(state, width),
            spacing = 8,
        )
    }

    /** Right column: player render with floating nametag + status line. */
    fun side(state: ProfileState.Loaded, index: Int, height: Int, entity: LivingEntity?): Component {
        val playerHeight = height.coerceIn(120, 260)
        val status = if (state.online) "§a online §8· §b${state.location ?: "Skyblock"}" else "§7offline"
        return if (entity != null) {
            PlayerRender(SIDE_WIDTH, playerHeight, entity, state.nametag, status, Theme.SURFACE_ALT, Theme.BORDER)
        } else {
            Frame(SIDE_WIDTH, playerHeight, Text("no render", Theme.TEXT_MUTED), Theme.SURFACE_ALT, Theme.BORDER)
        }
    }

    private fun skyblockLevel(p: SkyblockProfile, width: Int): Component {
        val into = (p.skyblockLevel.totalXp % 100).toInt()
        return Column(
            SpaceBetween(
                width,
                Text("Skyblock Level", Theme.TEXT_MUTED),
                Text(p.skyblockLevel.level.toString(), Theme.ACCENT),
            ),
            Row(
                ProgressBar(width - XP_WIDTH, 4, into / 100.0, Theme.ACCENT, Theme.SURFACE_ALT),
                Text("$into/100", Theme.TEXT_MUTED),
                spacing = 6,
                align = VAlign.CENTER,
            ),
            spacing = 2,
        )
    }

    /** Catacombs slot — a bordered, clickable card that opens the Dungeons page. */
    private fun catacombs(p: SkyblockProfile, width: Int, onClick: () -> Unit): Component {
        val c = p.catacombs
        val innerW = width - CARD_PAD * 2
        val inner = Column(
            SpaceBetween(
                innerW,
                Row(
                    Text("Catacombs", Theme.TEXT_MUTED),
                    Text(c.level.toString(), Theme.GREEN),
                    spacing = 5,
                    align = VAlign.CENTER,
                ),
                Text("Dungeons →", Theme.TEXT_MUTED),
            ),
            bar(c.progress, c.totalXp, innerW, fg = Theme.GREEN),
            spacing = 2,
        )
        return Clickable(card(inner, width), hoverBorder = Theme.ACCENT, onClick = onClick)
    }

    private fun skills(p: SkyblockProfile, width: Int, onSkill: (SkillType) -> Unit): Component {
        val cellW = (width - COL_GAP) / 2
        return twoColumns(p.skills.map { skillCell(it, cellW, onSkill) })
    }

    private fun skillCell(e: SkillEntry, cellW: Int, onSkill: (SkillType) -> Unit): Component {
        val xpLevel = e.level.level
        val maxed = xpLevel >= e.type.cap
        val levelText = "$xpLevel"
        val cell = Column(
            SpaceBetween(
                cellW,
                Row(Item(icon(e.type.icon), 11, tooltip = false), Text(e.type.display, Theme.TEXT), spacing = 3, align = VAlign.CENTER),
                Text(levelText, if (maxed) Theme.ACCENT else Theme.TEXT_MUTED),
            ),
            bar(e.level.progress, e.level.totalXp, cellW),
            spacing = 1,
        )
        val withTooltip = Tooltip(cell, skillTooltip(e, levelText, maxed))
        return if (SkillTab.forSkill(e.type) != null) {
            Clickable(withTooltip, hoverBorder = Theme.ACCENT) { onSkill(e.type) }
        } else {
            withTooltip
        }
    }

    private fun skillTooltip(e: SkillEntry, levelText: String, maxed: Boolean): List<String> = listOfNotNull(
        "§b${e.type.display} §7$levelText",
        "§7Total XP: §f${"%,d".format(e.level.totalXp)}",
        if (maxed) "§6Maxed" else "§7To next: §f${Format.compact(e.level.xpToNext)} §8(${(e.level.progress * 100).roundToInt()}%)",
        SkillTab.forSkill(e.type)?.let { "§8Click to open ${e.type.display}" },
    )

    private fun slayers(p: SkyblockProfile, width: Int): Component {
        val cellW = (width - COL_GAP) / 2
        return twoColumns(p.slayers.map { slayerCell(it, cellW) })
    }

    private fun slayerCell(e: SlayerEntry, cellW: Int): Component {
        val cell = Column(
            SpaceBetween(
                cellW,
                Row(Item(icon(e.type.icon), 11, tooltip = false), Text(e.type.display, Theme.TEXT), spacing = 3, align = VAlign.CENTER),
                Text(e.level.level.toString(), if (e.level.maxed) Theme.ACCENT else Theme.TEXT_MUTED),
            ),
            bar(e.level.progress, e.level.totalXp, cellW),
            spacing = 1,
        )
        return Tooltip(cell, slayerTooltip(e))
    }

    private fun slayerTooltip(e: SlayerEntry): List<String> {
        val lines = mutableListOf(
            "§b${e.type.display} §7${e.level.level}",
            "§7Total XP: §f${"%,d".format(e.level.totalXp)}",
            "",
        )
        e.tierKills.forEachIndexed { i, kills ->
            // T5 only exists for some bosses; hide it when there are no kills there.
            if (i < 4 || kills > 0) lines += "§7Tier ${i + 1}: §f${"%,d".format(kills)} §8kills"
        }
        return lines
    }

    private fun bar(progress: Double, totalXp: Long, cellW: Int, fg: Int = Theme.ACCENT): Component = Row(
        ProgressBar(cellW - XP_WIDTH, 3, progress, fg, Theme.SURFACE_ALT),
        Text(Format.compact(totalXp), Theme.TEXT_MUTED),
        spacing = 5,
        align = VAlign.CENTER,
    )

    private fun guild(state: ProfileState.Loaded, width: Int): Component = SpaceBetween(
        width,
        Text("GUILD", Theme.TEXT_MUTED),
        Text(state.guild ?: "—", Theme.TEXT),
    )

    private fun twoColumns(rows: List<Component>): Component {
        val half = (rows.size + 1) / 2
        return Row(
            Column(rows.take(half), spacing = 4),
            Column(rows.drop(half), spacing = 4),
            spacing = COL_GAP,
        )
    }

    private fun section(title: String, badge: String?, width: Int, content: Component): Component = Column(
        SpaceBetween(width, Text(title, Theme.TEXT_MUTED), badge?.let { Text(it, Theme.ACCENT) } ?: Spacer(0)),
        Box(width, 1, Theme.BORDER),
        Spacer(0, 1),
        content,
        spacing = 3,
    )

    private fun card(inner: Component, width: Int): Component = Frame(
        width, inner.height + CARD_PAD * 2, inner,
        background = Theme.SURFACE_ALT, borderColor = Theme.BORDER,
    )

    private fun icon(name: String): ItemStack {
        val id = Identifier.tryParse(name) ?: return ItemStack.EMPTY
        return ItemStack(BuiltInRegistries.ITEM.getValue(id))
    }
}
