package schrumbo.pv.render

import com.mojang.authlib.GameProfile
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack

/**
 * Builds a non-world [RemotePlayer] carrying the given skin profile and Skyblock armor, purely for
 * GUI rendering of other players. Returns null if no client level exists or construction fails.
 */
object FakePlayer {

    /** [armor] is in Hypixel order: index 0 boots, 1 leggings, 2 chestplate, 3 helmet. */
    fun build(profile: GameProfile, armor: List<ItemStack>): LivingEntity? {
        val level = Minecraft.getInstance().level
        if (level == null) {
            schrumbo.pv.core.PvClient.LOGGER.warn("[pv] FakePlayer: no client level")
            return null
        }
        return runCatching {
            val player = GuiPlayer(level, profile)
            armor.getOrNull(0)?.takeUnless { it.isEmpty }?.let { player.setItemSlot(EquipmentSlot.FEET, it) }
            armor.getOrNull(1)?.takeUnless { it.isEmpty }?.let { player.setItemSlot(EquipmentSlot.LEGS, it) }
            armor.getOrNull(2)?.takeUnless { it.isEmpty }?.let { player.setItemSlot(EquipmentSlot.CHEST, it) }
            armor.getOrNull(3)?.takeUnless { it.isEmpty }?.let { player.setItemSlot(EquipmentSlot.HEAD, it) }
            player
        }.onFailure { schrumbo.pv.core.PvClient.LOGGER.warn("[pv] FakePlayer build failed", it) }.getOrNull()
    }
}
