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

    /** Legacy 1.8 numeric item ids → modern item names, limited to what appears in armor slots. */
    private val LEGACY = mapOf(
        86 to "carved_pumpkin", 91 to "jack_o_lantern",
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
