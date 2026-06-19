package schrumbo.pv.ui.page

import net.minecraft.world.item.ItemStack
import schrumbo.pv.data.NamedContainer
import schrumbo.pv.data.SkyblockProfile
import schrumbo.pv.render.SkullItems
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
 * Loadout page: a fixed left rail whose first entry is the current loadout (armor, equipment, pet,
 * power) and the rest are the player's containers, each shown as a chest-style item grid that scrolls.
 */
object InventoryPage {

    const val RAIL_W = 128
    private const val SLOT = 20
    private const val GAP = 2
    private const val ROW_H = 16

    /** Greater Backpack head, used as the Backpacks rail icon. */
    private const val BACKPACK_TEX =
        "eyJ0aW1lc3RhbXAiOjE1NjgyMTMwNTE0MjYsInByb2ZpbGVJZCI6IjgyYzYwNmM1YzY1MjRiNzk4YjkxYTEyZDNhNjE2OTc3IiwicHJvZmlsZU5hbWUiOiJOb3ROb3RvcmlvdXNOZW1vIiwic2lnbmF0dXJlUmVxdWlyZWQiOnRydWUsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS82MmYzYjNhMDU0ODFjZGU3NzI0MDAwNWMwZGRjZWUxYzA2OWU1NTA0YTYyY2UwOTc3ODc5ZjU1YTM5Mzk2MTQ2In19fQ=="

    private val ICONS = mapOf(
        "Inventory" to "chest", "Armor" to "iron_chestplate", "Equipment" to "leather_boots",
        "Ender Chest" to "ender_chest", "Accessories" to "gold_ingot", "Potion Bag" to "splash_potion",
        "Fishing Bag" to "fishing_rod", "Quiver" to "arrow", "Personal Vault" to "iron_door",
        "Wardrobe" to "armor_stand", "Hunting Box" to "prismarine_shard",
    )

    fun isEmpty(p: SkyblockProfile): Boolean = p.containers.isEmpty()

    fun disabled(width: Int): Component = Column(
        PageKit.pageHeader("Loadout", "", width),
        Text("This player has their inventory API disabled.", Theme.TEXT_MUTED),
        spacing = 8,
    )

    /** Rail tabs: every container except Inventory, which is folded into the Loadout tab. */
    private fun tabs(p: SkyblockProfile): List<NamedContainer> = p.containers.filter { it.name != "Inventory" }

    /** Rail entries: a leading "Loadout" summary, then one row per tab container. */
    fun entryCount(p: SkyblockProfile): Int = 1 + tabs(p).size

    fun rail(p: SkyblockProfile, active: Int, onTab: (Int) -> Unit): Component {
        val rows = mutableListOf(loadoutRow(active == 0) { onTab(0) })
        tabs(p).forEachIndexed { i, c -> rows += railRow(c, active == i + 1) { onTab(i + 1) } }
        return Column(rows, spacing = 1)
    }

    private fun loadoutRow(active: Boolean, onClick: () -> Unit): Component {
        val color = if (active) Theme.ACCENT else Theme.TEXT_MUTED
        val row = Row(
            Box(2, 11, if (active) Theme.ACCENT else null),
            Item(PageKit.icon("diamond_chestplate"), 11, tooltip = false),
            Text("Loadout", color),
            spacing = 4, align = VAlign.CENTER,
        )
        return Clickable(Frame(RAIL_W, ROW_H, row, if (active) Theme.SURFACE_ALT else null, null, HAlign.START, VAlign.CENTER), hoverFill = Theme.HOVER, onClick = onClick)
    }

    private fun railRow(c: NamedContainer, active: Boolean, onClick: () -> Unit): Component {
        val color = if (active) Theme.ACCENT else Theme.TEXT_MUTED
        val count = c.items.count { !it.isEmpty }
        val innerW = RAIL_W - 2 - 11 - 4 * 2 - 6
        val icon = if (c.name == "Backpacks") SkullItems.fromTexture(BACKPACK_TEX) else PageKit.icon(ICONS[c.name] ?: "paper")
        val row = Row(
            Box(2, 11, if (active) Theme.ACCENT else null),
            Item(icon, 11, tooltip = false),
            SpaceBetween(innerW, Text(PageKit.clip(c.name, innerW - 18), color), Text("$count", color)),
            spacing = 4, align = VAlign.CENTER,
        )
        return Clickable(Frame(RAIL_W, ROW_H, row, if (active) Theme.SURFACE_ALT else null, null, HAlign.START, VAlign.CENTER), hoverFill = Theme.HOVER, onClick = onClick)
    }

    fun gridHeader(p: SkyblockProfile, active: Int, width: Int): Component {
        if (active == 0) return PageKit.pageHeader("Loadout", "", width)
        val c = tabs(p)[active - 1]
        val count = c.items.count { !it.isEmpty }
        val extra = if (c.name == "Accessories") "· $count items · ${p.magicalPower} MP" else "· $count items"
        return PageKit.pageHeader(c.name, extra, width)
    }

    fun body(p: SkyblockProfile, active: Int, width: Int, page: Int, onPage: (Int) -> Unit): Component {
        if (active == 0) return loadout(p, width)
        return grid(tabs(p)[active - 1], width, page, onPage)
    }

    /** The current loadout: worn armor, worn equipment and the player's main inventory. */
    private fun loadout(p: SkyblockProfile, width: Int): Component {
        val inv = p.containers.firstOrNull { it.name == "Inventory" }
        return Column(
            label("ARMOR"), slotRow(p.armor, 4),
            Spacer(0, 4), label("EQUIPMENT"), slotRow(p.equipment, 4),
            Spacer(0, 4), label("INVENTORY"),
            inv?.let { inventoryLayout(it.items) } ?: Text("Empty", Theme.TEXT_MUTED),
            spacing = 3,
        )
    }

    private fun label(s: String): Component = Text(s, Theme.TEXT_MUTED, shadow = false)

    private fun slotRow(items: List<ItemStack>, n: Int): Component =
        Row((0 until n).map { slot(items.getOrNull(it) ?: ItemStack.EMPTY) }, spacing = GAP)

    private fun grid(c: NamedContainer, width: Int, page: Int, onPage: (Int) -> Unit): Component = when (c.name) {
        "Ender Chest" -> pagedView(c.items, 45, "Page", width, page, onPage)
        "Backpacks" -> pagedView(c.items, 54, "Backpack", width, page, onPage)
        "Accessories" -> widthGrid(c.items, width)
        else -> rows9(c.items)
    }

    /** A grid that fills the full available [width] with as many slots per row as fit (accessories). */
    private fun widthGrid(items: List<ItemStack>, width: Int): Component {
        if (items.none { !it.isEmpty }) return Text("Empty", Theme.TEXT_MUTED)
        val cols = ((width + GAP) / (SLOT + GAP)).coerceAtLeast(1)
        return Column(items.chunked(cols).map { row -> Row(row.map { slot(it) }, spacing = GAP) }, spacing = GAP)
    }

    private fun inventoryLayout(items: List<ItemStack>): Component {
        if (items.isEmpty()) return Text("Empty", Theme.TEXT_MUTED)
        return Column(rows9(items.drop(9).take(27)), Spacer(0, 6), rows9(items.take(9)), spacing = GAP)
    }

    private const val GRID_W = 9 * SLOT + 8 * GAP

    /**
     * Paged container (Ender Chest, Backpacks): a clickable page picker over one chest-sized grid at a
     * time, so a 9-page ender chest or a wall of backpacks is a tidy switch instead of an endless scroll.
     */
    private fun pagedView(items: List<ItemStack>, pageSize: Int, label: String, width: Int, page: Int, onPage: (Int) -> Unit): Component {
        val pages = items.chunked(pageSize)
        if (pages.none { pg -> pg.any { !it.isEmpty } }) return Text("Empty", Theme.TEXT_MUTED)
        val sel = page.coerceIn(0, pages.size - 1)
        val pg = pages[sel]
        val count = pg.count { !it.isEmpty }
        return Column(
            SpaceBetween(
                GRID_W,
                Text("$label ${sel + 1} / ${pages.size}", Theme.TEXT, shadow = false),
                Text("$count items", Theme.TEXT_MUTED, shadow = false),
            ),
            Box(GRID_W, 1, Theme.BORDER),
            Spacer(0, 2),
            if (pages.size > 1) pageChips(pages.size, sel, width, onPage) else Spacer(0, 0),
            Spacer(0, 4),
            if (count == 0) Text("Empty", Theme.TEXT_MUTED) else rows9(pg),
            spacing = 3,
        )
    }

    /** Wrapping row of numbered page chips; the active one is accent-filled. */
    private fun pageChips(n: Int, sel: Int, width: Int, onPage: (Int) -> Unit): Component {
        val gap = 4
        val chips = (0 until n).map { i -> pageChip(i + 1, i == sel) { onPage(i) } }
        val rows = mutableListOf<MutableList<Component>>(mutableListOf())
        var w = 0
        for (c in chips) {
            val cw = c.width + gap
            if (w + cw > width && rows.last().isNotEmpty()) { rows += mutableListOf<Component>(); w = 0 }
            rows.last() += c; w += cw
        }
        return Column(rows.map { Row(it, spacing = gap) }, spacing = gap)
    }

    private fun pageChip(label: Int, active: Boolean, onClick: () -> Unit): Component {
        val text = "$label"
        val w = PageKit.font().width(text) + 12
        val frame = Frame(
            w, 14, Text(text, if (active) Theme.SURFACE else Theme.TEXT_MUTED, shadow = false),
            if (active) Theme.ACCENT else Theme.SURFACE_ALT, if (active) Theme.ACCENT else Theme.BORDER, HAlign.CENTER, VAlign.CENTER,
        )
        return Clickable(frame, hoverFill = Theme.HOVER, onClick = onClick)
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
