package schrumbo.pv.ui.page

import net.minecraft.world.item.ItemStack
import schrumbo.pv.data.RiftData
import schrumbo.pv.data.SkyblockProfile
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

/** Rift page: motes, enigma souls, unlocked porthals and the player's rift gear + inventory. */
object RiftPage {

    private const val MOTE = 0xFFB070FF.toInt()
    private const val ENIGMA_MAX = 52
    private const val SLOT = 20
    private const val GAP = 2

    private const val BURGER_TEX =
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDMzZGRiOTJjYjZiM2E3OTI4MGI4YmRjZWQ4OTc2YWVhYjEzYTRiZmZlYWVmMmQ0NmQ4MjhiZDkxZGVlMGYzZSJ9fX0="
    private const val MONTEZUMA_TEX =
        "ewogICJ0aW1lc3RhbXAiIDogMTY0ODExMzgxMjE5OCwKICAicHJvZmlsZUlkIiA6ICIyYzEwNjRmY2Q5MTc0MjgyODRlM2JmN2ZhYTdlM2UxYSIsCiAgInByb2ZpbGVOYW1lIiA6ICJOYWVtZSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9kZjY1NmMwNmU4YTVjYjQ2OTI1NjRlZTIxNzQ4YmRkZWM5ZDc4NWQxODM0Mjg0YWFhMTQzOTYwMWJiYTQ3ZDZiIiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0="

    fun build(p: SkyblockProfile, width: Int): Component {
        val r = p.rift
        return Column(
            PageKit.pageHeader("The Rift", "${Format.compact(r.motes)} motes", width),
            PageKit.tileRow(
                width,
                listOf(
                    "Motes Purse" to (Format.compact(r.motes) to MOTE),
                    "Lifetime Motes" to (Format.compact(r.lifetimeMotes) to Theme.TEXT),
                    "Enigma Souls" to ("${r.enigmaSouls}/$ENIGMA_MAX" to Theme.ACCENT),
                    "Rift Visits" to (Format.compact(r.visits) to Theme.GREEN),
                ),
            ),
            PageKit.section("PROGRESS", width, progress(r, width)),
            PageKit.section("TIMECHARMS", width, timecharms(r)),
            PageKit.section("PORTHALS", width, porthals(r)),
            PageKit.section("RIFT GEAR & INVENTORY", width, gearAndInventory(r)),
            spacing = 10,
        )
    }

    private fun progress(r: RiftData, width: Int): Component {
        val cellW = PageKit.cellW(width, 2)
        val rows = listOf(
            iconRow(cellW, schrumbo.pv.render.SkullItems.fromTexture(BURGER_TEX), "McGrubber's Burgers", "${r.burgers}/5"),
            iconRow(cellW, schrumbo.pv.render.SkullItems.fromTexture(MONTEZUMA_TEX), "Montezuma (Death pet)", if (r.hasMontezuma) "Owned" else "—"),
        )
        return PageKit.grid(rows, width, cols = 2)
    }

    private fun iconRow(cellW: Int, icon: net.minecraft.world.item.ItemStack, name: String, value: String): Component = Row(
        Item(icon, 12, tooltip = false),
        SpaceBetween(cellW - 12 - 5, Text(name, Theme.TEXT), Text(value, Theme.TEXT_MUTED)),
        spacing = 5,
        align = VAlign.CENTER,
    )

    /** The eight Rift Gallery timecharms with their vanilla icon (from NEU `RIFT_TROPHY_*` models). */
    private data class Timecharm(val id: String, val name: String, val icon: String)
    private val TIMECHARMS = listOf(
        Timecharm("wyldly_supreme", "Supreme Timecharm", "spruce_leaves"),
        Timecharm("chicken_n_egg", "Chicken N Egg Timecharm", "soul_sand"),
        Timecharm("mirrored", "Mirrorverse Timecharm", "glass"),
        Timecharm("citizen", "SkyBlock Citizen Timecharm", "emerald"),
        Timecharm("lazy_living", "Living Timecharm", "lapis_ore"),
        Timecharm("slime", "Globulate Timecharm", "slime_block"),
        Timecharm("vampiric", "Vampiric Timecharm", "redstone_block"),
        Timecharm("mountain", "Celestial Timecharm", "snow_block"),
    )

    /** Each timecharm rendered as its item when secured (with the secure date), greyed out otherwise. */
    private fun timecharms(r: RiftData): Component {
        val cells = TIMECHARMS.map { tc ->
            val ts = r.securedTrophies[tc.id]
            val icon = Item(PageKit.icon(if (ts != null) tc.icon else "gray_dye"), 18, tooltip = false)
            val lines = if (ts != null) listOf("§a${tc.name}", "§7Secured: §f${Format.date(ts)}")
            else listOf("§7${tc.name}", "§cNot secured")
            Tooltip(icon, lines)
        }
        return Row(cells, spacing = 4)
    }

    private fun porthals(r: RiftData): Component {
        if (r.porthals.isEmpty()) return Text("No porthals unlocked", Theme.TEXT_MUTED)
        val names = r.porthals.map { it.split('_').joinToString(" ") { w -> w.replaceFirstChar { c -> c.uppercase() } } }
        return Text(names.joinToString("  ·  "), Theme.GREEN)
    }

    /** Armor (4 slots) and Equipment (4 slots) stacked vertically, sitting beside the rift inventory. */
    private fun gearAndInventory(r: RiftData): Component = Row(
        labeledCol("Armor", vSlots(r.armor)),
        labeledCol("Equipment", vSlots(r.equipment)),
        labeledCol("Inventory", grid(r.inventory)),
        spacing = 12, align = VAlign.TOP,
    )

    private fun labeledCol(title: String, content: Component): Component =
        Column(Text(title, Theme.TEXT_MUTED, shadow = false), content, spacing = 2)

    private fun vSlots(items: List<ItemStack>): Component =
        Column((0 until 4).map { slot(items.getOrNull(it) ?: ItemStack.EMPTY) }, spacing = GAP)

    private fun grid(items: List<ItemStack>): Component {
        if (items.none { !it.isEmpty }) return Text("Empty", Theme.TEXT_MUTED)
        return Column(items.chunked(9).map { Row(it.map { s -> slot(s) }, spacing = GAP) }, spacing = GAP)
    }

    private fun slot(stack: ItemStack): Component {
        val inner: Component = if (stack.isEmpty) Spacer(SLOT - 4, SLOT - 4) else Item(stack, SLOT - 4, tooltip = true, decorations = true)
        return Frame(SLOT, SLOT, inner, Theme.SURFACE_ALT, Theme.BORDER, HAlign.CENTER, VAlign.CENTER)
    }
}
