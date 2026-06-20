package schrumbo.pv.ui.page

import net.minecraft.client.Minecraft
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import schrumbo.pv.data.ProfileState
import schrumbo.pv.data.SkillEntry
import schrumbo.pv.data.SkillType
import schrumbo.pv.data.SkyblockProfile
import schrumbo.pv.data.SlayerEntry
import schrumbo.pv.ui.Page
import schrumbo.pv.ui.Theme
import schrumbo.pv.ui.component.Card
import schrumbo.pv.ui.component.Clickable
import schrumbo.pv.ui.component.Column
import schrumbo.pv.ui.component.Component
import schrumbo.pv.ui.component.Frame
import schrumbo.pv.ui.component.HAlign
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
    private const val FAIRY_SOUL_MAX = 248
    private const val MAX_GROUP_GAP = 8
    private const val COL_GAP = 16
    private const val XP_WIDTH = 40
    private const val XP_GAP = 5

    /**
     * Left column: compact stats. [onSkill] deep-links a skill into its Skills sub-page;
     * [onCatacombs] opens the Dungeons page. Caller positions/scales it.
     */
    fun main(
        state: ProfileState.Loaded,
        index: Int,
        width: Int,
        height: Int,
        onSkill: (SkillType) -> Unit,
        onCatacombs: () -> Unit,
    ): Component {
        val p = state.profiles[index]
        val blocks = listOf(
            skyblockLevel(p, width),
            catacombs(p, width, onCatacombs),
            section("SKILLS", "SA %.2f".format(p.skillAverage), width, skills(p, width, onSkill)),
            section("SLAYERS", null, width, slayers(p, width)),
        )
        return fillHeight(blocks, base = 8, target = height)
    }

    /** Stacks [blocks] with a modest gap; when shorter than [target] the gap grows only up to a cap
     *  (the rest of the slack stays at the bottom) so groupings don't drift far apart. */
    private fun fillHeight(blocks: List<Component>, base: Int, target: Int): Component {
        val natural = blocks.sumOf { it.height } + base * (blocks.size - 1).coerceAtLeast(0)
        if (blocks.size < 2 || target <= natural) return Column(blocks, spacing = base)
        val gaps = blocks.size - 1
        val extra = ((target - natural) / gaps).coerceAtMost(MAX_GROUP_GAP)
        return Column(blocks, spacing = base + extra)
    }

    /** Right column: player render on top, profile info board below — both share surface + border. */
    fun side(state: ProfileState.Loaded, index: Int, height: Int, entity: LivingEntity?): Component {
        val p = state.profiles[index]
        val info = infoPanel(state, p)
        val playerH = (height - info.height - 6).coerceIn(90, 240)
        val status = if (state.online) "§a online §8· §b${state.location ?: "Skyblock"}" else "§7offline"
        val player = if (entity != null) {
            PlayerRender(SIDE_WIDTH, playerH, entity, state.nametag, status, Theme.SURFACE_ALT, Theme.BORDER)
        } else {
            Frame(SIDE_WIDTH, playerH, Text("no render", Theme.TEXT_MUTED), Theme.SURFACE_ALT, Theme.BORDER)
        }
        return Column(player, info, spacing = 6)
    }

    /** Identity + economy board pinned under the player render. */
    private fun infoPanel(state: ProfileState.Loaded, p: SkyblockProfile): Component {
        val w = SIDE_WIDTH - 12
        val content = Column(
            infoRow(w, "Mode", p.gameMode?.replaceFirstChar { it.uppercase() } ?: "Classic", Theme.TEXT),
            infoRow(w, "Hypixel", state.hypixelLevel?.toString() ?: "?", Theme.TEXT),
            infoRow(w, "Guild", state.guild ?: "—", Theme.TEXT),
            infoRow(w, "Bank", Format.compact(p.bank), Theme.GOLD),
            infoRow(w, "Purse", Format.compact(p.purse), Theme.GOLD),
            infoRow(w, "Created", Format.date(p.firstJoin), Theme.TEXT),
            infoRow(w, "Fairy Souls", "${p.fairySouls}/$FAIRY_SOUL_MAX", Theme.TEXT),
            spacing = 2,
        )
        return Card(content, padding = 6, background = Theme.SURFACE_ALT, borderColor = Theme.BORDER)
    }

    private fun infoRow(w: Int, key: String, value: String, valueColor: Int): Component {
        val font = Minecraft.getInstance().font
        val valMax = w - font.width(key) - 6
        return SpaceBetween(w, Text(key, Theme.TEXT_MUTED), Text(clip(value, valMax, font), valueColor))
    }

    private fun clip(s: String, maxW: Int, font: net.minecraft.client.gui.Font): String {
        if (font.width(s) <= maxW) return s
        var t = s
        while (t.isNotEmpty() && font.width("$t…") > maxW) t = t.dropLast(1)
        return "$t…"
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

    /** Catacombs slot — flat like the Skyblock level block, clickable to open the Dungeons page. */
    private fun catacombs(p: SkyblockProfile, width: Int, onClick: () -> Unit): Component {
        val c = p.catacombs
        val inner = Column(
            Row(
                Item(icon("dead_bush"), 11, tooltip = false),
                Text("Catacombs", Theme.TEXT_MUTED),
                Text(c.level.toString(), Theme.CATA),
                spacing = 4,
                align = VAlign.CENTER,
            ),
            bar(c.progress, c.totalXp, width, fg = Theme.CATA, bg = Theme.CATA_TRACK),
            spacing = 2,
        )
        return Clickable(inner, hoverFill = Theme.HOVER, pad = 2, onClick = onClick)
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
            Row(
                Item(icon(e.type.icon), 11, tooltip = false),
                Text(e.type.display, Theme.TEXT),
                Text(levelText, if (maxed) Theme.ACCENT else Theme.TEXT_MUTED),
                spacing = 4,
                align = VAlign.CENTER,
            ),
            bar(e.level.progress, e.level.totalXp, cellW),
            spacing = 1,
        )
        val withTooltip = Tooltip(cell, skillTooltip(e, levelText, maxed))
        return if (Page.forSkill(e.type) != null) {
            Clickable(withTooltip, hoverFill = Theme.HOVER, pad = 2) { onSkill(e.type) }
        } else {
            withTooltip
        }
    }

    private fun skillTooltip(e: SkillEntry, levelText: String, maxed: Boolean): List<String> = listOfNotNull(
        "§b${e.type.display} §7$levelText",
        "§7Total XP: §f${"%,d".format(e.level.totalXp)}",
        if (maxed) "§6Maxed" else "§7To next: §f${Format.compact(e.level.xpToNext)} §8(${(e.level.progress * 100).roundToInt()}%)",
        Page.forSkill(e.type)?.let { "§8Click to open ${e.type.display}" },
    )

    private fun slayers(p: SkyblockProfile, width: Int): Component {
        val cellW = (width - COL_GAP) / 2
        return twoColumns(p.slayers.map { slayerCell(it, cellW) })
    }

    private fun slayerCell(e: SlayerEntry, cellW: Int): Component {
        val cell = Column(
            Row(
                Item(icon(e.type.icon), 11, tooltip = false),
                Text(e.type.display, Theme.TEXT),
                Text(e.level.level.toString(), if (e.level.maxed) Theme.ACCENT else Theme.TEXT_MUTED),
                spacing = 4,
                align = VAlign.CENTER,
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
            if (i < 4 || kills > 0) lines += "§7Tier ${i + 1}: §f${"%,d".format(kills)}"
        }
        return lines
    }

    /** Bar + a fixed-width, right-aligned XP slot so every cell is exactly [cellW] wide regardless of
     *  the XP string length (keeps skill/slayer cells — and their hover areas — uniform). */
    private fun bar(progress: Double, totalXp: Long, cellW: Int, fg: Int = Theme.ACCENT, bg: Int = Theme.SURFACE_ALT): Component = Row(
        ProgressBar(cellW - XP_WIDTH - XP_GAP, 3, progress, fg, bg),
        Frame(XP_WIDTH, Minecraft.getInstance().font.lineHeight, Text(Format.compact(totalXp), Theme.TEXT_MUTED), hAlign = HAlign.END),
        spacing = XP_GAP,
        align = VAlign.CENTER,
    )

    private fun twoColumns(rows: List<Component>): Component {
        val half = (rows.size + 1) / 2
        return Row(
            Column(rows.take(half), spacing = 4),
            Column(rows.drop(half), spacing = 4),
            spacing = COL_GAP,
        )
    }

    /** A section group, no label/divider — spacing separates blocks; an optional [badge] floats right. */
    @Suppress("UNUSED_PARAMETER")
    private fun section(title: String, badge: String?, width: Int, content: Component): Component =
        if (badge != null) Column(SpaceBetween(width, Spacer(0), Text(badge, Theme.ACCENT)), content, spacing = 3)
        else content

    private fun icon(name: String): ItemStack {
        val id = Identifier.tryParse(name) ?: return ItemStack.EMPTY
        return ItemStack(BuiltInRegistries.ITEM.getValue(id))
    }
}
