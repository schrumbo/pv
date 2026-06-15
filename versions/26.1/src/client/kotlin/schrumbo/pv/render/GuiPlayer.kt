package schrumbo.pv.render

import com.mojang.authlib.GameProfile
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.RemotePlayer
import net.minecraft.world.entity.player.PlayerSkin

/**
 * A [RemotePlayer] for GUI rendering whose skin resolves from the profile's texture property via the
 * skin manager (a plain remote player has no [getPlayerInfo] entry, so it would fall back to Steve).
 */
class GuiPlayer(level: ClientLevel, profile: GameProfile) : RemotePlayer(level, profile) {

    private val skinLookup = Minecraft.getInstance().skinManager.createLookup(profile, false)

    override fun getSkin(): PlayerSkin = runCatching { skinLookup.get() }.getOrElse { super.getSkin() }
}
