package schrumbo.pv.data

import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.NbtIo
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
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

    /** Legacy 1.8 numeric item ids → modern item names (base item, ignoring data-value variants). */
    private val LEGACY = mapOf(
        // common blocks
        1 to "stone", 2 to "grass_block", 3 to "dirt", 4 to "cobblestone", 5 to "oak_planks",
        7 to "bedrock", 12 to "sand", 13 to "gravel", 14 to "gold_ore", 15 to "iron_ore", 16 to "coal_ore",
        17 to "oak_log", 18 to "oak_leaves", 19 to "sponge", 20 to "glass", 35 to "white_wool",
        41 to "gold_block", 42 to "iron_block", 45 to "bricks", 46 to "tnt", 47 to "bookshelf",
        49 to "obsidian", 54 to "chest", 56 to "diamond_ore", 57 to "diamond_block", 58 to "crafting_table",
        73 to "redstone_ore", 79 to "ice", 80 to "snow_block", 81 to "cactus", 82 to "clay",
        86 to "carved_pumpkin", 87 to "netherrack", 89 to "glowstone", 91 to "jack_o_lantern",
        103 to "melon", 112 to "nether_bricks", 121 to "end_stone", 129 to "emerald_ore", 133 to "emerald_block",
        152 to "redstone_block", 153 to "nether_quartz_ore", 155 to "quartz_block",
        // tools & weapons
        256 to "iron_shovel", 257 to "iron_pickaxe", 258 to "iron_axe", 259 to "flint_and_steel",
        261 to "bow", 262 to "arrow", 267 to "iron_sword", 268 to "wooden_sword", 269 to "wooden_shovel",
        270 to "wooden_pickaxe", 271 to "wooden_axe", 272 to "stone_sword", 273 to "stone_shovel",
        274 to "stone_pickaxe", 275 to "stone_axe", 276 to "diamond_sword", 277 to "diamond_shovel",
        278 to "diamond_pickaxe", 279 to "diamond_axe", 283 to "golden_sword", 284 to "golden_shovel",
        285 to "golden_pickaxe", 286 to "golden_axe", 290 to "wooden_hoe", 291 to "stone_hoe",
        292 to "iron_hoe", 293 to "diamond_hoe", 294 to "golden_hoe", 346 to "fishing_rod",
        // materials & misc
        260 to "apple", 263 to "coal", 264 to "diamond", 265 to "iron_ingot", 266 to "gold_ingot",
        280 to "stick", 287 to "string", 288 to "feather", 289 to "gunpowder", 297 to "bread",
        318 to "flint", 320 to "cooked_porkchop", 322 to "golden_apple", 331 to "redstone",
        339 to "paper", 340 to "book", 341 to "slime_ball", 345 to "compass", 347 to "clock",
        348 to "glowstone_dust", 351 to "ink_sac", 352 to "bone", 357 to "cookie", 360 to "melon_slice",
        364 to "cooked_beef", 368 to "ender_pearl", 369 to "blaze_rod", 370 to "ghast_tear",
        371 to "gold_nugget", 373 to "potion", 374 to "glass_bottle", 377 to "blaze_powder",
        378 to "magma_cream", 381 to "ender_eye", 384 to "experience_bottle", 385 to "fire_charge",
        386 to "writable_book", 387 to "written_book", 388 to "emerald", 399 to "nether_star",
        403 to "enchanted_book", 406 to "quartz", 408 to "prismarine_shard", 409 to "prismarine_crystals",
        // armor
        298 to "leather_helmet", 299 to "leather_chestplate", 300 to "leather_leggings", 301 to "leather_boots",
        302 to "chainmail_helmet", 303 to "chainmail_chestplate", 304 to "chainmail_leggings", 305 to "chainmail_boots",
        306 to "iron_helmet", 307 to "iron_chestplate", 308 to "iron_leggings", 309 to "iron_boots",
        310 to "diamond_helmet", 311 to "diamond_chestplate", 312 to "diamond_leggings", 313 to "diamond_boots",
        314 to "golden_helmet", 315 to "golden_chestplate", 316 to "golden_leggings", 317 to "golden_boots",
        397 to "player_head",
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
            runCatching { applySkin(stack, tag) }
                .onFailure { schrumbo.pv.core.PvClient.LOGGER.warn("[pv] applySkin failed", it) }
        }
        return stack
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
