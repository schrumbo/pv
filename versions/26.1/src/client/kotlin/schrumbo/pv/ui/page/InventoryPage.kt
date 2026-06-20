package schrumbo.pv.ui.page

import net.minecraft.world.item.ItemStack
import schrumbo.pv.data.NamedContainer
import schrumbo.pv.data.SkyblockProfile
import schrumbo.pv.ui.Theme
import schrumbo.pv.ui.component.Box
import schrumbo.pv.ui.component.Clickable
import schrumbo.pv.ui.component.Column
import schrumbo.pv.ui.component.Component
import schrumbo.pv.ui.component.Frame
import schrumbo.pv.ui.component.HAlign
import schrumbo.pv.ui.component.Item
import schrumbo.pv.ui.component.Row
import schrumbo.pv.ui.component.SpaceBetween
import schrumbo.pv.ui.component.Spacer
import schrumbo.pv.ui.component.Text
import schrumbo.pv.ui.component.VAlign

/**
 * Loadout page: a fixed left rail whose first entry is the current loadout (armor, equipment, main
 * inventory), then a combined Storage view (ender chest + backpacks) and the remaining containers,
 * each shown as a chest-style item grid that scrolls.
 */
object InventoryPage {

    const val RAIL_W = 128
    private const val SLOT = 20
    private const val GAP = 2
    private const val ROW_H = 16
    private const val GRID_W = 9 * SLOT + 8 * GAP

    private val ICONS = mapOf(
        "Armor" to "iron_chestplate", "Equipment" to "leather_boots",
        "Accessories" to "gold_ingot", "Potion Bag" to "splash_potion",
        "Fishing Bag" to "fishing_rod", "Quiver" to "arrow", "Personal Vault" to "iron_door",
        "Wardrobe" to "armor_stand", "Hunting Box" to "prismarine_shard",
    )

    fun isEmpty(p: SkyblockProfile): Boolean = p.containers.isEmpty()

    fun disabled(width: Int): Component = Column(
        PageKit.pageHeader("Loadout", "", width),
        Text("This player has their inventory API disabled.", Theme.TEXT_MUTED),
        spacing = 8,
    )

    // Rail entries -----------------------------------------------------------------------------

    private sealed interface Entry { val title: String }
    private data object LoadoutEntry : Entry { override val title = "Loadout" }
    private class StorageEntry(val ec: NamedContainer?, val bp: NamedContainer?) : Entry { override val title = "Storage" }
    private class SingleEntry(val c: NamedContainer) : Entry { override val title get() = c.name }

    /** Loadout, then a combined Storage entry, then every other container (Inventory is folded in). */
    private fun entries(p: SkyblockProfile): List<Entry> {
        val byName = p.containers.associateBy { it.name }
        val list = mutableListOf<Entry>(LoadoutEntry)
        val ec = byName["Ender Chest"]
        val bp = byName["Backpacks"]
        if (ec != null || bp != null) list += StorageEntry(ec, bp)
        val handled = setOf("Inventory", "Ender Chest", "Backpacks")
        p.containers.filter { it.name !in handled }.forEach { list += SingleEntry(it) }
        return list
    }

    fun entryCount(p: SkyblockProfile): Int = entries(p).size

    fun rail(p: SkyblockProfile, active: Int, onTab: (Int) -> Unit): Component =
        Column(entries(p).mapIndexed { i, e -> railRow(e, i == active) { onTab(i) } }, spacing = 1)

    private fun railRow(e: Entry, active: Boolean, onClick: () -> Unit): Component {
        val color = if (active) Theme.ACCENT else Theme.TEXT_MUTED
        val iconId = when (e) {
            LoadoutEntry -> "diamond_chestplate"
            is StorageEntry -> "ender_chest"
            is SingleEntry -> ICONS[e.c.name] ?: "paper"
        }
        val count = when (e) {
            LoadoutEntry -> null
            is StorageEntry -> (e.ec?.items?.count { !it.isEmpty } ?: 0) + (e.bp?.items?.count { !it.isEmpty } ?: 0)
            is SingleEntry -> e.c.items.count { !it.isEmpty }
        }
        val innerW = RAIL_W - 2 - 11 - 4 * 2 - 6
        val label: Component = if (count == null) Text(e.title, color)
        else SpaceBetween(innerW, Text(PageKit.clip(e.title, innerW - 18), color), Text("$count", color))
        val row = Row(
            Box(2, 11, if (active) Theme.ACCENT else null),
            Item(PageKit.icon(iconId), 11, tooltip = false),
            label,
            spacing = 4, align = VAlign.CENTER,
        )
        return Clickable(Frame(RAIL_W, ROW_H, row, if (active) Theme.SURFACE_ALT else null, null, HAlign.START, VAlign.CENTER), hoverFill = Theme.HOVER, onClick = onClick)
    }

    fun gridHeader(p: SkyblockProfile, active: Int, width: Int): Component = when (val e = entries(p)[active]) {
        LoadoutEntry -> PageKit.pageHeader("Loadout", "", width)
        is StorageEntry -> {
            val count = (e.ec?.items?.count { !it.isEmpty } ?: 0) + (e.bp?.items?.count { !it.isEmpty } ?: 0)
            PageKit.pageHeader("Storage", "$count items", width)
        }
        is SingleEntry -> {
            val count = e.c.items.count { !it.isEmpty }
            val extra = if (e.c.name == "Accessories") "$count items   ${p.magicalPower} MP" else "$count items"
            PageKit.pageHeader(e.c.name, extra, width)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun body(p: SkyblockProfile, active: Int, width: Int, page: Int, onPage: (Int) -> Unit): Component =
        when (val e = entries(p)[active]) {
            LoadoutEntry -> loadout(p, width)
            is StorageEntry -> storage(e.ec, e.bp, width)
            is SingleEntry -> containerBody(e.c, width)
        }

    // Loadout ----------------------------------------------------------------------------------

    /** Armor and Equipment side by side on top, the main inventory below — no labels. */
    private fun loadout(p: SkyblockProfile, width: Int): Component {
        val inv = p.containers.firstOrNull { it.name == "Inventory" }
        return Column(
            Row(slotRow(p.armor, 4), slotRow(p.equipment, 4), spacing = 12),
            Spacer(0, 6),
            inv?.let { inventoryLayout(it.items) } ?: Text("Empty", Theme.TEXT_MUTED),
            spacing = 4,
        )
    }

    private fun slotRow(items: List<ItemStack>, n: Int): Component =
        Row((0 until n).map { slot(items.getOrNull(it) ?: ItemStack.EMPTY) }, spacing = GAP)

    private fun inventoryLayout(items: List<ItemStack>): Component {
        if (items.isEmpty()) return Text("Empty", Theme.TEXT_MUTED)
        return Column(rows9(items.drop(9).take(27)), Spacer(0, 6), rows9(items.take(9)), spacing = GAP)
    }

    // Storage (combined ender chest + backpacks, one scrolling view with page dividers) ---------

    private fun storage(ec: NamedContainer?, bp: NamedContainer?, width: Int): Component {
        val children = mutableListOf<Component>()
        fun part(items: List<ItemStack>, pageSize: Int, label: String) {
            items.chunked(pageSize).forEachIndexed { i, pg ->
                if (pg.none { !it.isEmpty }) return@forEachIndexed
                if (children.isNotEmpty()) children += Box(GRID_W, 1, Theme.BORDER)
                children += Text("$label ${i + 1}", Theme.TEXT_MUTED, shadow = false)
                children += rows9(pg)
            }
        }
        ec?.let { part(it.items, 45, "Page") }
        bp?.let { part(it.items, 54, "Backpack") }
        if (children.isEmpty()) return Text("Empty", Theme.TEXT_MUTED)
        return Column(children, spacing = 4)
    }

    // Other containers -------------------------------------------------------------------------

    private fun containerBody(c: NamedContainer, width: Int): Component = when (c.name) {
        "Accessories", "Hunting Box" -> widthGrid(c.items, width)
        else -> rows9(c.items)
    }

    /** A grid that fills the full available [width] with as many slots per row as fit. */
    private fun widthGrid(items: List<ItemStack>, width: Int): Component {
        if (items.none { !it.isEmpty }) return Text("Empty", Theme.TEXT_MUTED)
        val cols = ((width + GAP) / (SLOT + GAP)).coerceAtLeast(1)
        return Column(items.chunked(cols).map { row -> Row(row.map { slot(it) }, spacing = GAP) }, spacing = GAP)
    }

    private fun rows9(items: List<ItemStack>): Component {
        val rows = items.chunked(9).map { rowItems -> Row(rowItems.map { slot(it) }, spacing = GAP) }
        if (rows.isEmpty()) return Text("Empty", Theme.TEXT_MUTED)
        return Column(rows, spacing = GAP)
    }

    private fun slot(stack: ItemStack): Component {
        val inner: Component = if (stack.isEmpty) Spacer(SLOT - 4, SLOT - 4) else Item(stack, SLOT - 4, tooltip = true, decorations = true)
        return Frame(SLOT, SLOT, inner, Theme.SURFACE_ALT, Theme.BORDER, HAlign.CENTER, VAlign.CENTER)
    }
}
