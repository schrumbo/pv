package schrumbo.pv.ui.page

import schrumbo.pv.data.DojoTest
import schrumbo.pv.data.KuudraTier
import schrumbo.pv.data.MobCount
import schrumbo.pv.data.SkyblockProfile
import schrumbo.pv.render.SkullItems
import schrumbo.pv.ui.Theme
import schrumbo.pv.ui.component.Column
import schrumbo.pv.ui.component.Component
import schrumbo.pv.ui.component.Frame
import schrumbo.pv.ui.component.HAlign
import schrumbo.pv.ui.component.Item
import schrumbo.pv.ui.component.Row
import schrumbo.pv.ui.component.SpaceBetween
import schrumbo.pv.ui.component.Spacer
import schrumbo.pv.ui.component.Text
import schrumbo.pv.ui.component.Tooltip
import schrumbo.pv.ui.component.VAlign
import schrumbo.pv.util.Format

/**
 * Combat tab content. The sub-page rail itself is drawn by the screen (it mirrors the top tab bar,
 * rotated to the left edge); this object only declares the sub-pages and builds their bodies.
 */
object CombatPage {

    /** Faction accent colours (Mages = purple, Barbarians = red). */
    private val MAGE = 0xFFB06CF0.toInt()
    private val BARB = 0xFFE0654F.toInt()

    /** The in-game Kuudra tier-key skulls, keyed by API tier id — one render per tier. */
    private val KUUDRA_KEY_TEX = mapOf(
        "none" to
            "ewogICJ0aW1lc3RhbXAiIDogMTY0MzY1MjgzNTU0NCwKICAicHJvZmlsZUlkIiA6ICJkYmQ4MDQ2M2EwMzY0Y2FjYjI3OGNhODBhMDBkZGIxMyIsCiAgInByb2ZpbGVOYW1lIiA6ICJ4bG9nMjEiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmZkM2U3MTgzOGMwZTc2Zjg5MDIxMzEyMGI0Y2U3NDQ5NTc3NzM2NjA0MzM4YThkMjhiNGM4NmRiMjU0N2U3MSIKICAgIH0KICB9Cn0=",
        "hot" to
            "ewogICJ0aW1lc3RhbXAiIDogMTY0MzY1Mjg2NTc1MiwKICAicHJvZmlsZUlkIiA6ICJkMGI4MjE1OThmMTE0NzI1ODBmNmNiZTliOGUxYmU3MCIsCiAgInByb2ZpbGVOYW1lIiA6ICJqYmFydHl5IiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2MwMjU5ZTg5NjRjM2RlYjk1YjEyMzNiYjJkYzgyYzk4NjE3N2U2M2FlMzZjMTEyNjVjYjM4NTE4MGJiOTFjYzAiCiAgICB9CiAgfQp9",
        "burning" to
            "ewogICJ0aW1lc3RhbXAiIDogMTY0MzY1Mjg4MjI5NSwKICAicHJvZmlsZUlkIiA6ICI1YjY2YzNkZWZhYTI0NWMzYTcwNjM3OTA3NTQ0Yjg3MCIsCiAgInByb2ZpbGVOYW1lIiA6ICJSZWFuX1JhaWNvMDgxNiIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8zMzBmNmY2ZTYzYjI0NWY4MzllM2NjZGNlNWE1ZjIyMDU2MjAxZDAyNzQ0MTFkZmU1ZDk0YmJlNDQ5YzRlY2UiCiAgICB9CiAgfQp9",
        "fiery" to
            "ewogICJ0aW1lc3RhbXAiIDogMTY0MzY1Mjg5ODM0MSwKICAicHJvZmlsZUlkIiA6ICI5ZDQyNWFiOGFmZjg0MGU1OWM3NzUzZjc5Mjg5YjMyZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJUb21wa2luNDIiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmQ4NTQzOTNiYmY5NDQ0NTQyNTAyNTgyZDRiNWEyM2NjNzM4OTY1MDZlMmZjNzM5ZDU0NWJjMzViYzdiMWMwNiIKICAgIH0KICB9Cn0=",
        "infernal" to
            "ewogICJ0aW1lc3RhbXAiIDogMTY0MzY1MjkxMzA5NiwKICAicHJvZmlsZUlkIiA6ICJjNTlkMDFlMDI4MWI0MGNhOTczNjc5ODc4NmRmN2FmNiIsCiAgInByb2ZpbGVOYW1lIiA6ICJvWm9va3hQYXJjY2VyIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzgyZWUyNTQxNGFhN2VmYjRhMmI0OTAxYzZlMzNlNWVhYTcwNWE2YWIyMTJlYmViZmQ2YTRkZTk4NDEyNWM3YTAiCiAgICB9CiAgfQp9",
    )

    private fun kuudraKey(tierId: String) = SkullItems.fromTexture(KUUDRA_KEY_TEX[tierId] ?: KUUDRA_KEY_TEX.getValue("none"))

    private fun gradeColor(grade: String): Int = when (grade) {
        "S" -> Theme.GOLD
        "A" -> Theme.GREEN
        "B" -> Theme.ACCENT
        "C" -> 0xFFF2C94C.toInt()
        "D" -> 0xFFE0904F.toInt()
        "F" -> Theme.WARN
        else -> Theme.TEXT_MUTED
    }

    /** Sub-pages in rail order, each with a representative chip icon. */
    enum class Sub(val label: String, val icon: String) {
        BESTIARY("Bestiary", "writable_book"),
        MOBS("Mobs", "zombie_head"),
        CRIMSON("Crimson Isle", "blaze_powder"),
    }

    /** One Mobs column (kills or deaths), sorted by count, with a totals subtitle. Each scrolls alone. */
    fun mobsColumn(title: String, total: Long, items: List<MobCount>, width: Int): Component {
        val rows = items.map { mc ->
            SpaceBetween(width, Text(PageKit.clip(mc.name, width - 60), Theme.TEXT), Text(Format.compact(mc.count), Theme.ACCENT))
        }
        return Column(
            listOf(
                SpaceBetween(width, Text(title, Theme.TEXT, scale = Text.SUBTITLE), Text(Format.compact(total), Theme.TEXT_MUTED)),
                Spacer(0, 4),
            ) + rows,
            spacing = 2,
        )
    }

    /** Crimson Isle: faction + reputation tiles, Kuudra completions, Dojo grades. */
    fun crimson(p: SkyblockProfile, width: Int): Component {
        val c = p.combat.crimson
        val gap = 8

        // Faction + reputation: two cards, each in its faction colour; the selected one is filled.
        val factionW = (width - gap) / 2
        val factions = Row(
            factionCard(factionW, "Mages", c.mageReputation, c.selectedFaction == "Mage", MAGE),
            factionCard(factionW, "Barbarians", c.barbarianReputation, c.selectedFaction == "Barbarian", BARB),
            spacing = gap,
        )

        // Kuudra: a card per tier with the boss head, completions, and highest wave in the tooltip.
        val totalRuns = c.kuudra.sumOf { it.completions }
        val kuudraW = PageKit.cellW(width, 5, 6)
        val kuudraCards = c.kuudra.map { kuudraCard(kuudraW, it) }

        // Dojo: a card per discipline, graded and coloured by grade.
        val dojoW = PageKit.cellW(width, 4)
        val dojoCards = c.dojo.map { dojoCard(dojoW, it) }
        val dojoRows = dojoCards.chunked(4).map { Row(it, spacing = 14) }

        return Column(
            factions,
            Spacer(0, 6),
            SpaceBetween(width, sectionTitle(kuudraKey("infernal"), "Kuudra"), Text("$totalRuns runs", Theme.TEXT_MUTED)),
            Row(kuudraCards, spacing = 6),
            Spacer(0, 6),
            Text("Dojo", Theme.TEXT, scale = Text.SUBTITLE),
            Column(dojoRows, spacing = 6),
            spacing = 5,
        )
    }

    /** A subtitle preceded by an item/skull icon. */
    private fun sectionTitle(icon: net.minecraft.world.item.ItemStack, title: String): Component =
        Row(Item(icon, 16, tooltip = false), Text(title, Theme.TEXT, scale = Text.SUBTITLE), spacing = 5, align = VAlign.CENTER)

    private fun factionCard(w: Int, name: String, rep: Int, selected: Boolean, color: Int): Component {
        val body = Column(
            Text(name, color),
            Text("${Format.compact(rep.toLong())} rep", if (selected) Theme.TEXT else Theme.TEXT_MUTED),
            spacing = 2,
        )
        return Frame(
            w, body.height + 12, Row(Spacer(6), body),
            if (selected) Theme.SURFACE_ALT else null,
            if (selected) color else Theme.BORDER,
            HAlign.START, VAlign.CENTER,
        )
    }

    private fun kuudraCard(w: Int, t: KuudraTier): Component {
        val done = t.completions > 0
        val card = Frame(
            w, 38,
            Column(
                Item(kuudraKey(t.id), 16, tooltip = false),
                Text(Format.compact(t.completions.toLong()), if (done) Theme.GOLD else Theme.TEXT_MUTED),
                spacing = 2,
                align = HAlign.CENTER,
            ),
            Theme.SURFACE_ALT, if (done) Theme.GOLD else Theme.BORDER,
        )
        return Tooltip(card, listOf("§f${t.name}", "§7Completions: §f${t.completions}", "§7Highest wave: §f${t.highestWave}"))
    }

    /** Vanilla item shown next to each dojo discipline. */
    private fun dojoIcon(name: String): String = when (name) {
        "Stamina" -> "rabbit_foot"
        "Mastery" -> "bow"
        "Discipline" -> "diamond_sword"
        "Swiftness" -> "lead"
        "Control" -> "ender_eye"
        "Tenacity" -> "fire_charge"
        "Force" -> "stick"
        else -> "stick"
    }

    private fun dojoCard(w: Int, d: DojoTest): Component {
        val color = gradeColor(d.grade)
        val leftPad = 4
        val rightPad = 10
        val left = Row(
            Item(PageKit.icon(dojoIcon(d.name)), 16, tooltip = false),
            Column(Text(d.name, Theme.TEXT), Text("${d.points} pts", Theme.TEXT_MUTED, scale = Text.SMALL), spacing = 1),
            spacing = 5,
            align = VAlign.CENTER,
        )
        val grade = Text(d.grade, color, scale = Text.SUBTITLE)
        val body = SpaceBetween(w - leftPad - rightPad, left, grade)
        return Frame(w, maxOf(left.height, grade.height) + 10, Row(Spacer(leftPad), body), Theme.SURFACE_ALT, Theme.BORDER, HAlign.START, VAlign.CENTER)
    }
}
