package schrumbo.pv.data

import com.mojang.authlib.GameProfile

/** The lifecycle of a profile lookup, consumed by the screen. */
sealed interface ProfileState {
    data object Loading : ProfileState
    data class Error(val message: String) : ProfileState
    data class Loaded(
        val profiles: List<SkyblockProfile>,
        val selectedIndex: Int,
        val online: Boolean,
        val guild: String?,
        val isSelf: Boolean,
        val hypixelLevel: Int?,
        val location: String?,
        val gameProfile: GameProfile?,
        val nametag: String,
    ) : ProfileState
}
