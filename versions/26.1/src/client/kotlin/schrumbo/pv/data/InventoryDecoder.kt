package schrumbo.pv.data

import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.NbtIo
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.alchemy.PotionContents
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.component.DyedItemColor
import net.minecraft.world.item.component.ItemLore
import net.minecraft.world.item.component.ResolvableProfile
import schrumbo.pv.util.SkinProfile
import java.io.ByteArrayInputStream
import java.util.Base64
import java.util.UUID

/**
 * Decodes Hypixel's gzipped-base64 legacy inventory NBT into [ItemStack]s. The vanilla item is a
 * 1.8 numeric id (`id:301s`); the legacy `tag` carries the Skyblock name, lore, leather dye and
 * skull skin, which are mapped onto modern data components so icons and tooltips look right.
 */
object InventoryDecoder {

    /** Legacy 1.8 numeric item ids -> modern item names (data-value variants map to a base). */
    private val LEGACY = mapOf(
        1 to "stone", 2 to "grass_block", 3 to "dirt", 4 to "cobblestone", 5 to "oak_planks", 6 to "oak_sapling",
        7 to "bedrock", 12 to "sand", 13 to "gravel", 14 to "gold_ore", 15 to "iron_ore", 16 to "coal_ore",
        17 to "oak_log", 18 to "oak_leaves", 19 to "sponge", 20 to "glass", 21 to "lapis_ore",
        22 to "lapis_block", 23 to "dispenser", 24 to "sandstone", 25 to "note_block", 27 to "powered_rail",
        28 to "detector_rail", 29 to "sticky_piston", 30 to "cobweb", 31 to "short_grass", 32 to "dead_bush",
        33 to "piston", 35 to "white_wool", 37 to "dandelion", 38 to "poppy", 39 to "brown_mushroom",
        40 to "red_mushroom", 41 to "gold_block", 42 to "iron_block", 44 to "smooth_stone_slab", 45 to "bricks",
        46 to "tnt", 47 to "bookshelf", 48 to "mossy_cobblestone", 49 to "obsidian", 50 to "torch",
        52 to "spawner", 53 to "oak_stairs", 54 to "chest", 56 to "diamond_ore", 57 to "diamond_block",
        58 to "crafting_table", 60 to "farmland", 61 to "furnace", 65 to "ladder", 66 to "rail",
        67 to "stone_stairs", 69 to "lever", 70 to "stone_pressure_plate", 72 to "wooden_pressure_plate",
        73 to "redstone_ore", 76 to "redstone_torch", 77 to "stone_button", 78 to "snow", 79 to "ice",
        80 to "snow", 81 to "cactus", 82 to "clay", 84 to "jukebox", 85 to "fence", 86 to "pumpkin",
        87 to "netherrack", 88 to "soul_sand", 89 to "glowstone", 91 to "jack_o_lantern",
        95 to "white_stained_glass", 96 to "oak_trapdoor", 97 to "infested_stone", 98 to "stone_bricks",
        99 to "brown_mushroom_block", 100 to "red_mushroom_block", 101 to "iron_bars", 102 to "glass_pane",
        103 to "melon", 106 to "vine", 107 to "oak_fence_gate", 108 to "brick_stairs",
        109 to "stone_brick_stairs", 110 to "mycelium", 111 to "lily_pad", 112 to "nether_bricks",
        113 to "nether_brick_fence", 114 to "nether_brick_stairs", 116 to "enchanting_table",
        120 to "end_portal_frame", 121 to "end_stone", 122 to "dragon_egg", 123 to "redstone_lamp",
        126 to "oak_slab", 128 to "sandstone_stairs", 129 to "emerald_ore", 130 to "ender_chest",
        131 to "tripwire_hook", 133 to "emerald_block", 134 to "spruce_stairs", 135 to "birch_stairs",
        136 to "jungle_stairs", 137 to "command_block", 138 to "beacon", 139 to "cobblestone_wall",
        143 to "wooden_button", 145 to "anvil", 146 to "trapped_chest", 147 to "light_weighted_pressure_plate",
        148 to "heavy_weighted_pressure_plate", 151 to "daylight_detector", 152 to "redstone_block",
        153 to "quartz_ore", 154 to "hopper", 155 to "quartz_block", 156 to "quartz_stairs",
        157 to "activator_rail", 158 to "dropper", 159 to "white_terracotta", 160 to "white_stained_glass_pane",
        161 to "acacia_leaves", 162 to "acacia_log", 163 to "acacia_stairs", 164 to "dark_oak_stairs",
        165 to "slime", 166 to "barrier", 167 to "iron_trapdoor", 168 to "prismarine", 169 to "sea_lantern",
        170 to "hay_block", 171 to "white_carpet", 172 to "terracotta", 173 to "coal_block", 174 to "packed_ice",
        175 to "sunflower", 179 to "red_sandstone", 180 to "red_sandstone_stairs", 182 to "red_sandstone_slab",
        183 to "spruce_fence_gate", 184 to "birch_fence_gate", 185 to "jungle_fence_gate",
        186 to "dark_oak_fence_gate", 187 to "acacia_fence_gate", 188 to "spruce_fence", 189 to "birch_fence",
        190 to "jungle_fence", 191 to "dark_oak_fence", 192 to "acacia_fence", 256 to "iron_shovel",
        257 to "iron_pickaxe", 258 to "iron_axe", 259 to "flint_and_steel", 260 to "apple", 261 to "bow",
        262 to "arrow", 263 to "coal", 264 to "diamond", 265 to "iron_ingot", 266 to "gold_ingot",
        267 to "iron_sword", 268 to "wooden_sword", 269 to "wooden_shovel", 270 to "wooden_pickaxe",
        271 to "wooden_axe", 272 to "stone_sword", 273 to "stone_shovel", 274 to "stone_pickaxe",
        275 to "stone_axe", 276 to "diamond_sword", 277 to "diamond_shovel", 278 to "diamond_pickaxe",
        279 to "diamond_axe", 280 to "stick", 281 to "bowl", 282 to "mushroom_stew", 283 to "golden_sword",
        284 to "golden_shovel", 285 to "golden_pickaxe", 286 to "golden_axe", 287 to "string", 288 to "feather",
        289 to "gunpowder", 290 to "wooden_hoe", 291 to "stone_hoe", 292 to "iron_hoe", 293 to "diamond_hoe",
        294 to "golden_hoe", 295 to "wheat_seeds", 296 to "wheat", 297 to "bread", 298 to "leather_helmet",
        299 to "leather_chestplate", 300 to "leather_leggings", 301 to "leather_boots",
        302 to "chainmail_helmet", 303 to "chainmail_chestplate", 304 to "chainmail_leggings",
        305 to "chainmail_boots", 306 to "iron_helmet", 307 to "iron_chestplate", 308 to "iron_leggings",
        309 to "iron_boots", 310 to "diamond_helmet", 311 to "diamond_chestplate", 312 to "diamond_leggings",
        313 to "diamond_boots", 314 to "golden_helmet", 315 to "golden_chestplate", 316 to "golden_leggings",
        317 to "golden_boots", 318 to "flint", 319 to "porkchop", 320 to "cooked_porkchop", 321 to "painting",
        322 to "golden_apple", 323 to "oak_sign", 324 to "oak_door", 325 to "bucket", 326 to "water_bucket",
        327 to "lava_bucket", 328 to "minecart", 329 to "saddle", 330 to "iron_door", 331 to "redstone",
        332 to "snowball", 333 to "oak_boat", 334 to "leather", 335 to "milk_bucket", 336 to "brick",
        337 to "clay_ball", 338 to "sugar_cane", 339 to "paper", 340 to "book", 341 to "slime_ball",
        342 to "chest_minecart", 343 to "furnace_minecart", 344 to "egg", 345 to "compass", 346 to "fishing_rod",
        347 to "clock", 348 to "glowstone_dust", 349 to "cod", 350 to "cooked_cod", 351 to "ink_sac",
        352 to "bone", 353 to "sugar", 354 to "cake", 355 to "red_bed", 356 to "repeater", 357 to "cookie",
        358 to "filled_map", 359 to "shears", 360 to "melon", 361 to "pumpkin_seeds", 362 to "melon_seeds",
        363 to "beef", 364 to "cooked_beef", 365 to "chicken", 366 to "cooked_chicken", 367 to "rotten_flesh",
        368 to "ender_pearl", 369 to "blaze_rod", 370 to "ghast_tear", 371 to "gold_nugget",
        372 to "nether_wart", 373 to "potion", 374 to "glass_bottle", 375 to "spider_eye",
        376 to "fermented_spider_eye", 377 to "blaze_powder", 378 to "magma_cream", 379 to "brewing_stand",
        380 to "cauldron", 381 to "ender_eye", 382 to "glistering_melon_slice", 384 to "experience_bottle",
        385 to "fire_charge", 386 to "writable_book", 387 to "written_book", 388 to "emerald",
        389 to "item_frame", 390 to "flower_pot", 391 to "carrot", 392 to "potato", 393 to "baked_potato",
        394 to "poisonous_potato", 395 to "map", 396 to "golden_carrot", 397 to "player_head",
        398 to "carrot_on_a_stick", 399 to "nether_star", 400 to "pumpkin_pie", 401 to "firework_rocket",
        402 to "firework_star", 403 to "enchanted_book", 404 to "comparator", 405 to "nether_brick",
        406 to "quartz", 407 to "tnt_minecart", 408 to "hopper_minecart", 409 to "prismarine_shard",
        410 to "prismarine_crystals", 411 to "rabbit", 412 to "cooked_rabbit", 413 to "rabbit_stew",
        414 to "rabbit_foot", 415 to "rabbit_hide", 416 to "armor_stand", 417 to "iron_horse_armor",
        418 to "golden_horse_armor", 419 to "diamond_horse_armor", 420 to "lead", 421 to "name_tag",
        422 to "command_block_minecart", 423 to "mutton", 424 to "cooked_mutton", 425 to "white_banner",
        427 to "spruce_door", 428 to "birch_door", 429 to "jungle_door", 430 to "acacia_door",
        431 to "dark_oak_door", 2256 to "music_disc_13", 2257 to "music_disc_cat", 2258 to "music_disc_blocks",
        2259 to "music_disc_chirp", 2260 to "music_disc_far", 2261 to "music_disc_mall",
        2262 to "music_disc_mellohi", 2263 to "music_disc_stal", 2264 to "music_disc_strad",
        2265 to "music_disc_ward", 2266 to "music_disc_11", 2267 to "music_disc_wait",
    )

    /** Returns one stack per slot (empty/unknown become [ItemStack.EMPTY]); empty list on failure. */
    fun decode(base64Gzip: String): List<ItemStack> {
        val list = runCatching {
            val bytes = Base64.getDecoder().decode(base64Gzip.trim())
            val root = NbtIo.readCompressed(ByteArrayInputStream(bytes), NbtAccounter.unlimitedHeap())
            root.getListOrEmpty("i")
        }.getOrNull() ?: return emptyList()

        return (0 until list.size).map { i ->
            runCatching { toStack(list.getCompoundOrEmpty(i)) }
                .onFailure { schrumbo.pv.core.PvClient.LOGGER.warn("[pv] armor slot $i decode failed: {}", it.toString()) }
                .getOrDefault(ItemStack.EMPTY)
        }
    }

    /**
     * Decodes a single icon item (e.g. a `backpack_icons` entry) — the stored item already carries the
     * backpack's current look in its skull texture, so an applied skin renders as-is. Handles both the
     * `{i:[item]}` wrapper and a bare item-compound root.
     */
    fun decodeIcon(base64Gzip: String): ItemStack {
        val root = runCatching {
            val bytes = Base64.getDecoder().decode(base64Gzip.trim())
            NbtIo.readCompressed(ByteArrayInputStream(bytes), NbtAccounter.unlimitedHeap())
        }.getOrNull() ?: return ItemStack.EMPTY
        val list = root.getListOrEmpty("i")
        val compound = if (list.size > 0) list.getCompoundOrEmpty(0) else root
        return runCatching { toStack(compound) }.getOrDefault(ItemStack.EMPTY)
    }

    /** The Skyblock item id (`tag.ExtraAttributes.id`) of a single icon item, or null. */
    fun iconId(base64Gzip: String): String? {
        val root = runCatching {
            val bytes = Base64.getDecoder().decode(base64Gzip.trim())
            NbtIo.readCompressed(ByteArrayInputStream(bytes), NbtAccounter.unlimitedHeap())
        }.getOrNull() ?: return null
        val list = root.getListOrEmpty("i")
        val compound = if (list.size > 0) list.getCompoundOrEmpty(0) else root
        val tag = compound.getCompound("tag").orElse(null) ?: return null
        val extra = tag.getCompound("ExtraAttributes").orElse(null) ?: return null
        return extra.getString("id").orElse(null)
    }

    private fun toStack(c: CompoundTag): ItemStack {
        val tag = c.getCompound("tag").orElse(null)
        val name = itemName(c, tag) ?: return ItemStack.EMPTY
        val location = Identifier.tryParse(name) ?: return ItemStack.EMPTY
        val item = BuiltInRegistries.ITEM.getValue(location)
        val count = c.getByteOr("Count", 1).toInt().coerceAtLeast(1)
        val stack = ItemStack(item, count)

        if (tag != null) {
            runCatching { applyDisplay(stack, tag) }
                .onFailure { schrumbo.pv.core.PvClient.LOGGER.warn("[pv] applyDisplay failed", it) }
            runCatching { applyPotion(stack, tag) }
                .onFailure { schrumbo.pv.core.PvClient.LOGGER.warn("[pv] applyPotion failed", it) }
            runCatching { applySkin(stack, tag) }
                .onFailure { schrumbo.pv.core.PvClient.LOGGER.warn("[pv] applySkin failed", it) }
            runCatching { applyId(stack, tag) }.getOrNull()
        }
        return stack
    }

    /** Stashes the Skyblock item id (`ExtraAttributes.id`) so features like gear scanning can read it. */
    private fun applyId(stack: ItemStack, tag: CompoundTag) {
        val id = tag.getCompound("ExtraAttributes").orElse(null)?.getString("id")?.orElse(null) ?: return
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(CompoundTag().apply { putString("pv_id", id) }))
    }

    /** The Skyblock item id stashed on a decoded stack, or null. */
    fun skyblockId(stack: ItemStack): String? =
        stack.get(DataComponents.CUSTOM_DATA)?.copyTag()?.getString("pv_id")?.orElse(null)

    /** Decodes items, reading each one's Skyblock id (`tag.ExtraAttributes.id`) straight from the NBT. */
    fun decodeWithIds(base64Gzip: String): List<Pair<ItemStack, String?>> {
        val list = runCatching {
            val bytes = Base64.getDecoder().decode(base64Gzip.trim())
            NbtIo.readCompressed(ByteArrayInputStream(bytes), NbtAccounter.unlimitedHeap()).getListOrEmpty("i")
        }.getOrNull() ?: return emptyList()
        return (0 until list.size).map { i ->
            val c = list.getCompoundOrEmpty(i)
            val id = c.getCompound("tag").orElse(null)
                ?.getCompound("ExtraAttributes")?.orElse(null)?.getString("id")?.orElse(null)
            runCatching { toStack(c) }.getOrDefault(ItemStack.EMPTY) to id
        }
    }

    private fun applyDisplay(stack: ItemStack, tag: CompoundTag) {
        val display = tag.getCompound("display").orElse(null) ?: return
        display.getString("Name").orElse(null)?.let {
            stack.set(DataComponents.CUSTOM_NAME, Component.literal(it))
        }
        display.getInt("color").orElse(null)?.let {
            stack.set(DataComponents.DYED_COLOR, DyedItemColor(it))
        }
        val lore = display.getListOrEmpty("Lore")
        if (lore.size > 0) {
            val lines = (0 until lore.size)
                .mapNotNull { lore.getString(it).orElse(null) }
                .map { Component.literal(it) }
            stack.set(DataComponents.LORE, ItemLore(lines))
        }
    }

    /**
     * SkyBlock potions are all the legacy `potion` item, so they'd all render as the same awkward
     * bottle. Colour them by the 1.8 `CustomPotionColor` when present, else by a stable colour derived
     * from the Skyblock potion type / name so each kind is at least visually distinct.
     */
    private fun applyPotion(stack: ItemStack, tag: CompoundTag) {
        if (stack.item != Items.POTION && stack.item != Items.SPLASH_POTION && stack.item != Items.LINGERING_POTION) return
        val color = potionColor(tag) ?: return
        stack.set(
            DataComponents.POTION_CONTENTS,
            PotionContents(java.util.Optional.empty(), java.util.Optional.of(color), emptyList(), java.util.Optional.empty()),
        )
    }

    private fun potionColor(tag: CompoundTag): Int? {
        tag.getInt("CustomPotionColor").orElse(null)?.let { return it }
        val extra = tag.getCompound("ExtraAttributes").orElse(null)
        val key = extra?.getString("potion")?.orElse(null)
            ?: tag.getCompound("display").orElse(null)?.getString("Name")?.orElse(null)
            ?: return null
        val h = key.lowercase().hashCode()
        val r = 90 + (h and 0x7F)
        val g = 90 + ((h ushr 7) and 0x7F)
        val b = 90 + ((h ushr 14) and 0x7F)
        return (r shl 16) or (g shl 8) or b
    }

    private fun applySkin(stack: ItemStack, tag: CompoundTag) {
        val skull = tag.getCompound("SkullOwner").orElse(null) ?: return
        val properties = skull.getCompound("Properties").orElse(null) ?: return
        val textures = properties.getListOrEmpty("textures")
        if (textures.size == 0) return
        val value = textures.getCompoundOrEmpty(0).getString("Value").orElse(null) ?: return
        val uuid = skull.getString("Id").orElse(null)?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?: UUID.randomUUID()
        val profile = SkinProfile.withTextures(uuid, "skin", value)
        stack.set(DataComponents.PROFILE, ResolvableProfile.createResolved(profile))
    }

    /**
     * Resolves a modern item name: a textured skull always becomes a player head (covers custom
     * Skyblock skulls of any numeric id), else a string id, else the legacy numeric id.
     */
    private fun itemName(c: CompoundTag, tag: CompoundTag?): String? {
        if (tag != null && hasSkullTexture(tag)) return "player_head"
        c.getString("id").orElse(null)?.let { return it }
        val numeric = c.getShortOr("id", -1).toInt()
        return if (numeric < 0) null else LEGACY[numeric]
    }

    private fun hasSkullTexture(tag: CompoundTag): Boolean {
        val skull = tag.getCompound("SkullOwner").orElse(null) ?: return false
        val properties = skull.getCompound("Properties").orElse(null) ?: return false
        return properties.getListOrEmpty("textures").size > 0
    }
}
