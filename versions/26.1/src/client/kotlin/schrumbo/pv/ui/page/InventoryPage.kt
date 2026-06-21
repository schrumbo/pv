package schrumbo.pv.ui.page

import net.minecraft.world.item.ItemStack
import schrumbo.pv.data.Backpack
import schrumbo.pv.data.NamedContainer
import schrumbo.pv.data.SackRegistry
import schrumbo.pv.data.SkyblockProfile
import schrumbo.pv.render.SkullItems
import schrumbo.pv.ui.Theme
import schrumbo.pv.ui.component.Clickable
import schrumbo.pv.ui.component.Column
import schrumbo.pv.ui.component.Component
import schrumbo.pv.ui.component.Frame
import schrumbo.pv.ui.component.HAlign
import schrumbo.pv.ui.component.Item
import schrumbo.pv.ui.component.PreviewOnHover
import schrumbo.pv.ui.component.Row
import schrumbo.pv.ui.component.SpaceBetween
import schrumbo.pv.ui.component.Spacer
import schrumbo.pv.ui.component.Text
import schrumbo.pv.ui.component.Tooltip
import schrumbo.pv.ui.component.VAlign
import schrumbo.pv.util.Format

/**
 * Loadout page: a fixed left rail whose first entry is the current loadout (armor, equipment, main
 * inventory), then a combined Storage view (ender chest + backpacks) and the remaining containers,
 * each shown as a chest-style item grid that scrolls.
 */
object InventoryPage {

    private const val SLOT = 20
    private const val GAP = 2

    private val ICONS = mapOf(
        "Armor" to "iron_chestplate", "Equipment" to "leather_boots",
        "Accessories" to "gold_ingot", "Potion Bag" to "splash_potion",
        "Fishing Bag" to "fishing_rod", "Quiver" to "arrow", "Personal Vault" to "iron_door",
        "Wardrobe" to "armor_stand", "Hunting Box" to "prismarine_shard",
        "Candy Bag" to "cookie", "Carnival Mask Bag" to "carved_pumpkin",
    )

    /** Minor bags folded into one "Misc" category (like the reference), Vault kept separate. */
    private val MISC_NAMES = listOf("Potion Bag", "Quiver", "Fishing Bag", "Candy Bag", "Carnival Mask Bag")

    private const val CANDY_TEX = "eyJ0aW1lc3RhbXAiOjE1NzE3MDE5NjUwNDcsInByb2ZpbGVJZCI6IjkxZjA0ZmU5MGYzNjQzYjU4ZjIwZTMzNzVmODZkMzllIiwicHJvZmlsZU5hbWUiOiJTdG9ybVN0b3JteSIsInNpZ25hdHVyZVJlcXVpcmVkIjp0cnVlLCJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTQwY2M5MDBlNTk3OTFjOGE4N2JjYjRmYzMxMmM1MzQzODQyNzhhZTliYjE0ODZhYmU4NzE0MzBhN2IzMzNkZSJ9fX0="
    private const val CARNIVAL_TEX = "ewogICJ0aW1lc3RhbXAiIDogMTcxNTAyMzkwMjQyMSwKICAicHJvZmlsZUlkIiA6ICJkYzA5MjA4MTM2ZDg0Y2Y5OWIwMzFmMGI1NzM4OTdmNSIsCiAgInByb2ZpbGVOYW1lIiA6ICJLRUlUSF8wMzAyIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzQ0YWY5NDMzNzFkNzZlNWRiNWRlYjkzZGYyZWFmMTRkOWIwZTAzYTE2NDhiZWU3ZjkwMTI2YWJmODk4M2MxZDYiCiAgICB9CiAgfQp9"

    /** The Misc-bag nav icon: real skull for the candy/carnival bags, else a vanilla item. */
    private fun bagIcon(name: String): ItemStack = when (name) {
        "Candy Bag" -> SkullItems.fromTexture(CANDY_TEX)
        "Carnival Mask Bag" -> SkullItems.fromTexture(CARNIVAL_TEX)
        else -> PageKit.icon(ICONS[name] ?: "paper")
    }

    fun isEmpty(p: SkyblockProfile): Boolean = p.containers.isEmpty()

    fun disabled(width: Int): Component = Column(
        PageKit.pageHeader("Loadout", "", width),
        Text("This player has their inventory API disabled.", Theme.TEXT_MUTED),
        spacing = 8,
    )

    // Rail entries -----------------------------------------------------------------------------

    private sealed interface Entry { val title: String }
    private data object LoadoutEntry : Entry { override val title = "Loadout" }
    private data object SacksEntry : Entry { override val title = "Sacks" }
    private class StorageEntry(val ec: NamedContainer?, val backpacks: List<Backpack>) : Entry { override val title = "Storage" }
    private class MiscEntry(val bags: List<NamedContainer>) : Entry { override val title = "Misc" }
    private class SingleEntry(val c: NamedContainer) : Entry { override val title get() = c.name }

    /** Fixed order: Loadout, Wardrobe, Storage, Accessories, Sacks, Hunting Box, Misc, Vault. */
    private fun entries(p: SkyblockProfile): List<Entry> {
        val byName = p.containers.associateBy { it.name }
        val list = mutableListOf<Entry>(LoadoutEntry)
        byName["Wardrobe"]?.let { list += SingleEntry(it) }
        val ec = byName["Ender Chest"]
        if (ec != null || p.backpacks.isNotEmpty()) list += StorageEntry(ec, p.backpacks)
        byName["Accessories"]?.let { list += SingleEntry(it) }
        if (ownedSacks(p).isNotEmpty()) list += SacksEntry
        byName["Hunting Box"]?.let { list += SingleEntry(it) }
        val miscBags = MISC_NAMES.mapNotNull { byName[it] }
        if (miscBags.isNotEmpty()) list += MiscEntry(miscBags)
        byName["Personal Vault"]?.let { list += SingleEntry(it) }
        return list
    }

    private fun storageCount(e: StorageEntry): Int =
        (e.ec?.items?.count { !it.isEmpty } ?: 0) + e.backpacks.sumOf { bp -> bp.items.count { !it.isEmpty } }

    /**
     * Each sack the player has any items in, with its icon, name and the FULL (id → amount) list it can
     * hold — every potential item is shown (amount 0 when none), so an opened sack reads completely.
     */
    private fun ownedSacks(p: SkyblockProfile): List<Triple<ItemStack, String, List<Pair<String, Long>>>> =
        SackRegistry.sacks.mapNotNull { def ->
            val items = def.contents.map { id -> id to SackRegistry.count(p.sacks, id) }
            if (items.none { it.second > 0 }) null else Triple(def.icon, def.name, items)
        }

    fun entryCount(p: SkyblockProfile): Int = entries(p).size

    /** Left sub-tab entries (icon + tooltip label with item count) for the category rail. */
    fun subTabs(p: SkyblockProfile): List<Pair<ItemStack, String>> = entries(p).map { e ->
        val icon = when (e) {
            LoadoutEntry -> PageKit.icon("diamond_chestplate")
            is StorageEntry -> PageKit.icon("ender_chest")
            SacksEntry -> SackRegistry.sacks.firstOrNull()?.icon ?: PageKit.icon("paper")
            is MiscEntry -> PageKit.icon("bundle")
            is SingleEntry -> PageKit.icon(ICONS[e.c.name] ?: "paper")
        }
        val count = when (e) {
            LoadoutEntry -> null
            is StorageEntry -> storageCount(e)
            SacksEntry -> ownedSacks(p).size
            is MiscEntry -> e.bags.sumOf { it.items.count { s -> !s.isEmpty } }
            is SingleEntry -> e.c.items.count { !it.isEmpty }
        }
        icon to if (count != null) "${e.title} ($count)" else e.title
    }

    fun gridHeader(p: SkyblockProfile, active: Int, width: Int): Component = when (val e = entries(p)[active]) {
        LoadoutEntry -> PageKit.pageHeader("Loadout", "", width)
        is StorageEntry -> PageKit.pageHeader("Storage", "${storageCount(e)} items", width)
        SacksEntry -> PageKit.pageHeader("Sacks", "${ownedSacks(p).size} sacks", width)
        is MiscEntry -> PageKit.pageHeader("Misc", "${e.bags.size} bags", width)
        is SingleEntry -> {
            val count = e.c.items.count { !it.isEmpty }
            val extra = if (e.c.name == "Accessories") "$count items   ${p.magicalPower} MP" else "$count items"
            PageKit.pageHeader(e.c.name, extra, width)
        }
    }

    fun body(p: SkyblockProfile, active: Int, width: Int, page: Int, onPage: (Int) -> Unit): Component =
        when (val e = entries(p)[active]) {
            LoadoutEntry -> loadout(p, width)
            is StorageEntry -> storage(e.ec, e.backpacks, width, page, onPage)
            SacksEntry -> sacks(ownedSacks(p), width, page, onPage)
            is MiscEntry -> misc(e.bags, width, page, onPage)
            is SingleEntry -> if (e.c.name == "Wardrobe") wardrobe(e.c.items, p, width) else containerBody(e.c, width)
        }

    private val CENTERED = setOf("Potion Bag", "Fishing Bag", "Quiver", "Personal Vault")

    /** Single-page bodies are centred vertically (and horizontally) when they fit the viewport. */
    fun centersBody(p: SkyblockProfile, active: Int): Boolean = when (val e = entries(p)[active]) {
        LoadoutEntry -> true
        is SingleEntry -> e.c.name in CENTERED
        else -> false
    }

    /** Centers a component in the full content [width]. */
    private fun center(width: Int, c: Component): Component =
        Frame(width, c.height, c, hAlign = HAlign.CENTER, vAlign = VAlign.TOP)

    // Loadout ----------------------------------------------------------------------------------

    /**
     * Mirrors a player inventory: armor + equipment as two vertical 4-slot columns on the left, the
     * main inventory (27 + hotbar) as a 9-wide grid on the right. Centered, no labels.
     */
    private fun loadout(p: SkyblockProfile, width: Int): Component {
        val armor = p.armor.reversed()  // stored boots→helmet; show helmet→boots
        val armorCol = Column((0 until 4).map { slot(armor.getOrNull(it) ?: ItemStack.EMPTY) }, spacing = GAP)
        val equipCol = Column((0 until 4).map { slot(p.equipment.getOrNull(it) ?: ItemStack.EMPTY) }, spacing = GAP)
        val inv = p.containers.firstOrNull { it.name == "Inventory" }?.items ?: emptyList()
        val invGrid = if (inv.isEmpty()) Text("Empty", Theme.TEXT_MUTED)
        else Column(rows9(inv.drop(9).take(27)), rows9(inv.take(9)), spacing = GAP)
        return center(width, Row(armorCol, equipCol, Spacer(14, 0), invGrid, spacing = 6, align = VAlign.TOP))
    }

    // Storage (ender chest + backpacks, an icon picker that opens one page at a time) ------------

    /** [slots] = 0 means "trim to used rows" (ender chest); > 0 forces a fixed size (backpacks). */
    private class StoragePage(val icon: ItemStack, val corner: String?, val label: String, val items: List<ItemStack>, val slots: Int)

    private fun storagePages(ec: NamedContainer?, backpacks: List<Backpack>): List<StoragePage> {
        val pages = mutableListOf<StoragePage>()
        val enderChest = PageKit.icon("ender_chest")
        // Show every unlocked ender-chest page, even fully empty ones (trimmed to used rows).
        ec?.items?.chunked(45)?.forEachIndexed { i, pg ->
            pages += StoragePage(enderChest, "${i + 1}", "Ender Chest · Page ${i + 1}", pg, 0)
        }
        backpacks.forEach { bp -> pages += StoragePage(bp.icon, null, bp.icon.hoverName.string, bp.items, bp.slots) }
        return pages
    }

    /** Ender-chest pages trim to used rows; backpacks always show their full slot count. */
    private fun pageGrid(items: List<ItemStack>, slots: Int): Component {
        if (slots <= 0) return chestGrid(items)
        return rows9((0 until slots).map { items.getOrNull(it) ?: ItemStack.EMPTY })
    }

    /**
     * Bax-style storage: a row of the real container icons (ender-chest renders + backpack heads), max
     * 9 per row; clicking one opens that page below, hovering previews its contents. One page at a time.
     */
    private fun storage(ec: NamedContainer?, backpacks: List<Backpack>, width: Int, selected: Int, onSelect: (Int) -> Unit): Component {
        val pages = storagePages(ec, backpacks)
        if (pages.isEmpty()) return Text("Empty", Theme.TEXT_MUTED)
        val sel = selected.coerceIn(0, pages.size - 1)
        val icons = pages.mapIndexed { i, pg -> navIcon(pg, i == sel) { onSelect(i) } }
        // Ender-chest pages share the first row(s); backpacks start on their own row(s) below.
        val ecCount = pages.count { it.corner != null }
        val rows = (icons.take(ecCount).chunked(9) + icons.drop(ecCount).chunked(9)).map { Row(it, spacing = 3) }
        val nav = center(width, Column(rows, spacing = 3))
        return Column(nav, Spacer(0, 8), center(width, pageGrid(pages[sel].items, pages[sel].slots)), spacing = 0)
    }

    /** A chest grid trimmed to the last used row — a page with 9 used slots shows one row, not the full 45. */
    private fun chestGrid(items: List<ItemStack>): Component {
        val last = items.indexOfLast { !it.isEmpty }
        if (last < 0) return Text("Empty", Theme.TEXT_MUTED)
        return rows9(items.take((last / 9 + 1) * 9))
    }

    private fun navIcon(pg: StoragePage, active: Boolean, onClick: () -> Unit): Component {
        val frame = Frame(
            SLOT, SLOT, Item(pg.icon, SLOT - 4, tooltip = false, corner = pg.corner),
            if (active) Theme.SURFACE_ALT else null,
            if (active) Theme.ACCENT else Theme.BORDER,
            HAlign.CENTER, VAlign.CENTER,
        )
        val clickable = Clickable(frame, hoverFill = Theme.HOVER, onClick = onClick)
        return PreviewOnHover(clickable) {
            Column(Text(pg.label, Theme.TEXT), Spacer(0, 4), pageGrid(pg.items, pg.slots), spacing = 0)
        }
    }

    // Sacks (icon picker like storage; opens a sack's contents as items with compact amounts) ----

    private fun sacks(owned: List<Triple<ItemStack, String, List<Pair<String, Long>>>>, width: Int, selected: Int, onSelect: (Int) -> Unit): Component {
        if (owned.isEmpty()) return Text("No sacks", Theme.TEXT_MUTED)
        val sel = selected.coerceIn(0, owned.size - 1)
        val icons = owned.mapIndexed { i, (icon, name, items) -> sackNavIcon(icon, name, items, i == sel) { onSelect(i) } }
        val nav = center(width, Column(icons.chunked(9).map { Row(it, spacing = 3) }, spacing = 3))
        val (_, name, items) = owned[sel]
        return Column(
            nav, Spacer(0, 8),
            center(width, Text(name, Theme.TEXT, scale = Text.SUBTITLE)), Spacer(0, 4),
            center(width, sackGrid(items, width)),
            spacing = 0,
        )
    }

    private fun sackNavIcon(icon: ItemStack, name: String, items: List<Pair<String, Long>>, active: Boolean, onClick: () -> Unit): Component {
        val frame = Frame(
            SLOT, SLOT, Item(icon, SLOT - 4, tooltip = false),
            if (active) Theme.SURFACE_ALT else null, if (active) Theme.ACCENT else Theme.BORDER, HAlign.CENTER, VAlign.CENTER,
        )
        val clickable = Clickable(frame, hoverFill = Theme.HOVER, onClick = onClick)
        return PreviewOnHover(clickable) { Column(Text(name, Theme.TEXT), Spacer(0, 4), sackGrid(items, 9 * (SLOT + GAP)), spacing = 0) }
    }

    private fun sackGrid(items: List<Pair<String, Long>>, width: Int): Component {
        val cols = minOf(9, items.size, ((width + GAP) / (SLOT + GAP)).coerceAtLeast(1))
        return Column(items.chunked(cols).map { row -> Row(row.map { sackSlot(it.first, it.second) }, spacing = GAP) }, spacing = GAP)
    }

    private fun sackSlot(id: String, count: Long): Component {
        val item = Item(SackRegistry.contentIcon(id), SLOT - 4, tooltip = false, corner = Format.short(count).takeIf { count > 0 })
        val frame = Frame(SLOT, SLOT, item, Theme.SURFACE_ALT, Theme.BORDER, HAlign.CENTER, VAlign.CENTER)
        return Tooltip(frame, listOf("§f${prettify(id)}", "§7Amount: §a${"%,d".format(count)}"))
    }

    private fun prettify(id: String): String =
        id.replace('-', '_').split('_').filter { it.isNotEmpty() }
            .joinToString(" ") { it.lowercase().replaceFirstChar { c -> c.uppercase() } }

    // Misc (potion/quiver/fishing/candy/carnival folded into one icon picker) -------------------

    private fun misc(bags: List<NamedContainer>, width: Int, selected: Int, onSelect: (Int) -> Unit): Component {
        if (bags.isEmpty()) return Text("Empty", Theme.TEXT_MUTED)
        val sel = selected.coerceIn(0, bags.size - 1)
        val icons = bags.mapIndexed { i, b -> miscNavIcon(b, i == sel) { onSelect(i) } }
        val nav = center(width, Column(icons.chunked(9).map { Row(it, spacing = 3) }, spacing = 3))
        val bag = bags[sel]
        return Column(
            nav, Spacer(0, 8),
            center(width, Text(bag.name, Theme.TEXT, scale = Text.SUBTITLE)), Spacer(0, 4),
            center(width, chestGrid(bag.items)),
            spacing = 0,
        )
    }

    private fun miscNavIcon(b: NamedContainer, active: Boolean, onClick: () -> Unit): Component {
        val frame = Frame(
            SLOT, SLOT, Item(bagIcon(b.name), SLOT - 4, tooltip = false),
            if (active) Theme.SURFACE_ALT else null, if (active) Theme.ACCENT else Theme.BORDER, HAlign.CENTER, VAlign.CENTER,
        )
        val clickable = Clickable(frame, hoverFill = Theme.HOVER, onClick = onClick)
        return PreviewOnHover(clickable) { Column(Text(b.name, Theme.TEXT), Spacer(0, 4), chestGrid(b.items), spacing = 0) }
    }

    // Other containers -------------------------------------------------------------------------

    private fun containerBody(c: NamedContainer, width: Int): Component = when (c.name) {
        "Accessories" -> widthGrid(c.items, width)
        "Hunting Box" -> widthGrid(c.items, width, ::countSlot)
        else -> center(width, rows9(c.items))
    }

    /**
     * Wardrobe pages of 36 slots (4 rows × 9), each column a set (helmet/chestplate/leggings/boots).
     * Every set is shown (empty included); the equipped set's slots are empty in the data because the
     * armor is worn, so the active armor is overlaid into that column.
     */
    private fun wardrobe(items: List<ItemStack>, p: SkyblockProfile, width: Int): Component {
        val sel = (p.wardrobeSlot - 1).takeIf { p.wardrobeSlot > 0 } ?: -1
        val armor = p.armor.reversed()  // helmet → boots
        val sections = mutableListOf<Component>()
        items.chunked(36).forEachIndexed { pageIdx, page ->
            val equippedHere = sel >= 0 && sel / 9 == pageIdx
            if (page.none { !it.isEmpty } && !equippedHere) return@forEachIndexed
            val slots = page.toMutableList()
            while (slots.size < 36) slots += ItemStack.EMPTY
            if (equippedHere) {
                val col = sel % 9
                for (r in 0 until 4) slots[r * 9 + col] = armor.getOrNull(r) ?: ItemStack.EMPTY
            }
            if (sections.isNotEmpty()) sections += Spacer(0, 8)
            sections += rows9(slots)
        }
        if (sections.isEmpty()) return Text("No wardrobe", Theme.TEXT_MUTED)
        return center(width, Column(sections, spacing = 0))
    }

    /** A grid that fills the full available [width] with as many slots per row as fit. */
    private fun widthGrid(items: List<ItemStack>, width: Int, slotFn: (ItemStack) -> Component = ::slot): Component {
        if (items.none { !it.isEmpty }) return Text("Empty", Theme.TEXT_MUTED)
        val cols = ((width + GAP) / (SLOT + GAP)).coerceAtLeast(1)
        return Column(items.chunked(cols).map { row -> Row(row.map { slotFn(it) }, spacing = GAP) }, spacing = GAP)
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

    /** A slot whose count is shown compact (e.g. 4.1K) instead of the raw number — for big counts. */
    private fun countSlot(stack: ItemStack): Component {
        val inner: Component = if (stack.isEmpty) Spacer(SLOT - 4, SLOT - 4)
        else Item(stack, SLOT - 4, tooltip = true, corner = if (stack.count > 1) Format.short(stack.count.toLong()) else null)
        return Frame(SLOT, SLOT, inner, Theme.SURFACE_ALT, Theme.BORDER, HAlign.CENTER, VAlign.CENTER)
    }
}
