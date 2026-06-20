package schrumbo.pv.render

import com.mojang.authlib.GameProfile
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.ClientMannequin
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.PlayerModelPart
import net.minecraft.world.entity.player.PlayerSkin
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.ResolvableProfile
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

/**
 * GUI-only player render, ported from meowdding/skyblock-pv: a vanilla [ClientMannequin] (the
 * dressing-room avatar) carrying a resolved profile + Skyblock armor. The skin resolves asynchronously
 * through the skin render cache, so it renders the real model/skin/armor instead of a Steve fallback.
 */
class FakePlayer private constructor(
    level: Level,
    profile: GameProfile,
    armor: List<ItemStack>,
) : ClientMannequin(level, Minecraft.getInstance().playerSkinRenderCache()) {

    private val resolved: ResolvableProfile = ResolvableProfile.createResolved(profile).apply {
        resolveProfile(Minecraft.getInstance().services().profileResolver())
    }
    private var resolvedSkin: PlayerSkin = ClientMannequin.DEFAULT_SKIN

    init {
        // Hypixel armor order: 0 boots, 1 leggings, 2 chestplate, 3 helmet.
        armor.getOrNull(3)?.takeUnless { it.isEmpty }?.let { setItemSlot(EquipmentSlot.HEAD, it) }
        armor.getOrNull(2)?.takeUnless { it.isEmpty }?.let { setItemSlot(EquipmentSlot.CHEST, it) }
        armor.getOrNull(1)?.takeUnless { it.isEmpty }?.let { setItemSlot(EquipmentSlot.LEGS, it) }
        armor.getOrNull(0)?.takeUnless { it.isEmpty }?.let { setItemSlot(EquipmentSlot.FEET, it) }
        Minecraft.getInstance().playerSkinRenderCache().lookup(resolved).whenComplete { info, _ ->
            info.ifPresent { resolvedSkin = it.playerSkin() }
        }
    }

    override fun getProfile(): ResolvableProfile = resolved
    override fun getSkin(): PlayerSkin = resolvedSkin
    override fun shouldShowName(): Boolean = false
    override fun isModelPartShown(part: PlayerModelPart): Boolean = part != PlayerModelPart.CAPE
    override fun position(): Vec3 = Minecraft.getInstance().cameraEntity?.position() ?: Vec3.ZERO

    companion object {
        /** Builds the GUI mannequin for [profile] wearing [armor]; null if no client level exists. */
        fun build(profile: GameProfile, armor: List<ItemStack>): LivingEntity? {
            val level = Minecraft.getInstance().level ?: run {
                schrumbo.pv.core.PvClient.LOGGER.warn("[pv] FakePlayer: no client level")
                return null
            }
            return runCatching { FakePlayer(level, profile, armor) }
                .onFailure { schrumbo.pv.core.PvClient.LOGGER.warn("[pv] FakePlayer build failed", it) }
                .getOrNull()
        }
    }
}
