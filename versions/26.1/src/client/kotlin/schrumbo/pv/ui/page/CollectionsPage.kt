package schrumbo.pv.ui.page

import net.minecraft.client.Minecraft
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import schrumbo.pv.data.CategoryProgress
import schrumbo.pv.data.CollectionTier
import schrumbo.pv.data.CollectionsRegistry
import schrumbo.pv.data.SkyblockProfile
import schrumbo.pv.ui.Theme
import schrumbo.pv.ui.component.Box
import schrumbo.pv.ui.component.Clickable
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

/** Collections page: a fixed left category rail and the selected category's collections, scrolling. */
object CollectionsPage {

    const val RAIL_W = 110
    private const val COL_GAP = 14
    private const val ROW_H = 14

    fun categories(p: SkyblockProfile): List<CategoryProgress> = CollectionsRegistry.resolve(p.collections)

    fun header(cats: List<CategoryProgress>, width: Int): Component {
        val maxed = cats.sumOf { it.maxedCount }
        val total = cats.sumOf { it.total }
        val collected = cats.sumOf { c -> c.items.sumOf { it.amount } }
        return Row(
            Text("${Format.compact(collected)} collected", Theme.TEXT_MUTED),
            Text("$maxed/$total maxed", Theme.TEXT_MUTED),
            spacing = 14,
            align = VAlign.CENTER,
        )
    }

    fun rail(cats: List<CategoryProgress>, active: Int, onCategory: (Int) -> Unit): Component =
        Column(cats.mapIndexed { i, cat -> categoryRow(cat, i == active) { onCategory(i) } }, spacing = 1)

    private fun categoryRow(cat: CategoryProgress, active: Boolean, onClick: () -> Unit): Component {
        val color = if (active) Theme.ACCENT else Theme.TEXT_MUTED
        val row = Row(
            Box(2, 11, if (active) Theme.ACCENT else null),
            Item(icon(cat.def.icon), 11, tooltip = false),
            Text(cat.def.name, color),
            spacing = 4,
            align = VAlign.CENTER,
        )
        val frame = Frame(RAIL_W, ROW_H, row, if (active) Theme.SURFACE_ALT else null, null, HAlign.START, VAlign.CENTER)
        return Clickable(frame, hoverFill = Theme.HOVER, onClick = onClick)
    }

    fun grid(cat: CategoryProgress, width: Int): Component {
        val cellW = (width - COL_GAP) / 2
        val cells = cat.items.map { cell(it, cellW) }
        val half = (cells.size + 1) / 2
        return Row(
            Column(cells.take(half), spacing = 6),
            Column(cells.drop(half), spacing = 6),
            spacing = COL_GAP,
            align = VAlign.TOP,
        )
    }

    private fun cell(c: CollectionTier, cellW: Int): Component {
        val nameColor = if (c.maxed) Theme.GOLD else Theme.TEXT
        val tierLabel = "${c.tier}/${c.maxTier}"
        val tierColor = if (c.maxed) Theme.GOLD else if (c.tier == 0) Theme.TEXT_MUTED else Theme.ACCENT
        val fg = if (c.maxed) Theme.GOLD else Theme.ACCENT
        val innerW = cellW - 16 - 5
        val body = Column(
            SpaceBetween(innerW, Text(clip(c.def.name, innerW - 28), nameColor), Text(tierLabel, tierColor)),
            ProgressBar(innerW, 3, c.progress, fg, Theme.SURFACE_ALT),
            SpaceBetween(innerW, Text(Format.compact(c.amount), Theme.TEXT_MUTED), Spacer(0)),
            spacing = 2,
        )
        val cell = Row(Item(itemIcon(c.def.key), 16, tooltip = false), body, spacing = 5, align = VAlign.CENTER)
        val nextLine = if (c.maxed) "§7Maxed" else "§7Next: §f${Format.compact(c.amount)}§7/§f${Format.compact(c.next)}"
        val toMax = c.def.reqs.lastOrNull()?.let { (it - c.amount).coerceAtLeast(0) } ?: 0L
        val maxLine = if (c.maxed) "§7Fully maxed" else "§7To max: §f${Format.compact(toMax)}"
        return Tooltip(cell, listOf("§f${c.def.name}", "§7Tier §f${c.tier}§7/§f${c.maxTier}", "§7Total: §f${Format.compact(c.amount)}", nextLine, maxLine))
    }

    /**
     * Maps each Skyblock collection key (legacy 1.8 material name, often with a `:data` suffix, plus
     * a handful of Skyblock-only items) onto a crisp modern vanilla item. Custom items use the closest
     * vanilla stand-in. Anything unmapped falls back to the stripped, lower-cased key.
     */
    private val ICON_MAP = mapOf(
        // Farming
        "WHEAT" to "wheat", "SEEDS" to "wheat_seeds", "CARROT_ITEM" to "carrot", "POTATO_ITEM" to "potato",
        "PUMPKIN" to "pumpkin", "MELON" to "melon_slice", "SUGAR_CANE" to "sugar_cane", "CACTUS" to "cactus",
        "NETHER_STALK" to "nether_wart", "MUSHROOM_COLLECTION" to "red_mushroom", "INK_SACK:3" to "cocoa_beans",
        "RAW_CHICKEN" to "chicken", "MUTTON" to "mutton", "PORK" to "porkchop", "RAW_FISH" to "cod",
        "RABBIT" to "rabbit", "LEATHER" to "leather", "FEATHER" to "feather",
        // Mining
        "COBBLESTONE" to "cobblestone", "COAL" to "coal", "IRON_INGOT" to "iron_ingot", "GOLD_INGOT" to "gold_ingot",
        "DIAMOND" to "diamond", "LAPIS" to "lapis_lazuli", "INK_SACK:4" to "lapis_lazuli", "EMERALD" to "emerald",
        "REDSTONE" to "redstone", "QUARTZ" to "quartz", "OBSIDIAN" to "obsidian", "GLOWSTONE_DUST" to "glowstone_dust",
        "GRAVEL" to "gravel", "ICE" to "ice", "NETHERRACK" to "netherrack", "SAND" to "sand", "SAND:1" to "red_sand",
        "ENDER_STONE" to "end_stone", "MYCEL" to "mycelium", "SULPHUR" to "glowstone_dust", "SULPHUR_ORE" to "glowstone",
        "HARD_STONE" to "stone", "MITHRIL_ORE" to "prismarine_crystals", "GEMSTONE_COLLECTION" to "amethyst_shard",
        "TUNGSTEN" to "netherite_scrap", "UMBER" to "brown_terracotta", "GLACITE" to "blue_ice",
        // Combat
        "ROTTEN_FLESH" to "rotten_flesh", "BONE" to "bone", "STRING" to "string", "SPIDER_EYE" to "spider_eye",
        "SLIME_BALL" to "slime_ball", "ENDER_PEARL" to "ender_pearl", "BLAZE_ROD" to "blaze_rod",
        "GHAST_TEAR" to "ghast_tear", "MAGMA_CREAM" to "magma_cream", "CHILI_PEPPER" to "sweet_berries",
        // Foraging
        "LOG" to "oak_log", "LOG:1" to "spruce_log", "LOG:2" to "birch_log", "LOG:3" to "jungle_log",
        "LOG_2" to "acacia_log", "LOG_2:1" to "dark_oak_log", "MANGROVE_LOG" to "mangrove_log",
        "FIG_LOG" to "stripped_birch_log", "TENDER_WOOD" to "pale_oak_log",
        // Fishing
        "RAW_FISH:1" to "salmon", "RAW_FISH:2" to "tropical_fish", "RAW_FISH:3" to "pufferfish",
        "PRISMARINE_SHARD" to "prismarine_shard", "PRISMARINE_CRYSTALS" to "prismarine_crystals",
        "CLAY_BALL" to "clay_ball", "WATER_LILY" to "lily_pad", "INK_SACK" to "ink_sac", "SPONGE" to "sponge",
        "MAGMA_FISH" to "pufferfish",
        // Rift
        "AGARICUS_CAP" to "brown_mushroom", "CADUCOUS_STEM" to "warped_roots", "HALF_EATEN_CARROT" to "golden_carrot",
        "HEMOVIBE" to "redstone_ore", "METAL_HEART" to "heart_of_the_sea", "TIMITE" to "nether_star",
        "WILTED_BERBERIS" to "dead_bush",
        // Galatea / misc flora
        "MOONFLOWER" to "lily_of_the_valley", "WILD_ROSE" to "poppy", "DOUBLE_PLANT" to "sunflower",
        "LUSHLILAC" to "lilac", "VINESAP" to "vine", "SEA_LUMIES" to "glow_berries", "LOTUS" to "pink_petals",
    )

    /** Real head texture for Skyblock-custom collections, else a crisp vanilla icon for the key. */
    private fun itemIcon(key: String): net.minecraft.world.item.ItemStack {
        CollectionsRegistry.skulls[key]?.let { return schrumbo.pv.render.SkullItems.fromTexture(it) }
        return PageKit.icon(ICON_MAP[key] ?: key.substringBefore(':').lowercase())
    }

    private fun clip(s: String, maxW: Int): String {
        val font = Minecraft.getInstance().font
        if (font.width(s) <= maxW) return s
        var t = s
        while (t.isNotEmpty() && font.width("$t…") > maxW) t = t.dropLast(1)
        return "$t…"
    }

    private fun icon(name: String): ItemStack {
        val id = Identifier.tryParse(name) ?: return ItemStack.EMPTY
        return ItemStack(BuiltInRegistries.ITEM.getValue(id))
    }
}
