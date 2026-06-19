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

    fun build(p: SkyblockProfile, width: Int): Component {
        val r = p.rift
        return Column(
            PageKit.pageHeader("The Rift", "· ${Format.compact(r.motes)} motes", width),
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
            row(cellW, "Wither Eyes Killed", "${r.witherEyes}"),
            row(cellW, "Cats Found", "${r.catsFound}"),
            row(cellW, "Montezuma (Death pet)", if (r.hasMontezuma) "Owned" else "—"),
            row(cellW, "McGrubber's Burgers", "${r.burgers}/5"),
        )
        return PageKit.grid(rows, width, cols = 2)
    }

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

    private fun row(cellW: Int, name: String, value: String): Component =
        SpaceBetween(cellW, Text(name, Theme.TEXT), Text(value, Theme.TEXT_MUTED))
}
