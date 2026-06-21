package schrumbo.pv.api

import net.minecraft.client.Minecraft
import schrumbo.pv.data.GardenData
import schrumbo.pv.data.GardenMapper
import schrumbo.pv.data.ProfileMapper
import schrumbo.pv.data.ProfileState
import schrumbo.pv.util.HypixelRank
import schrumbo.pv.util.HypixelStats

/**
 * Orchestrates a profile lookup: resolves the target UUID, then pulls profiles, status and guild
 * from the [HypixelDataSource] and maps them into a [ProfileState] for the screen.
 */
object ProfileService {

    private val source: HypixelDataSource = DirectKeyClient

    /** Loads [target] (blank = self) off-thread and delivers the result on the MC main thread. */
    fun load(target: String, callback: (ProfileState) -> Unit) {
        Thread.startVirtualThread {
            val state = runCatching { fetch(target) }
                .getOrElse { ProfileState.Error(it.message ?: "Lookup failed") }
            Minecraft.getInstance().execute { callback(state) }
        }
    }

    /** Lazily loads one profile's garden off-thread (a separate endpoint), delivering on the main thread. */
    fun loadGarden(profileId: String, callback: (GardenData?) -> Unit) {
        Thread.startVirtualThread {
            val garden = runCatching { GardenMapper.map(source.garden(profileId)) }.getOrNull()
            Minecraft.getInstance().execute { callback(garden) }
        }
    }

    private fun fetch(target: String): ProfileState {
        val uuid = resolveUuid(target) ?: return ProfileState.Error("Unknown player '$target'")
        val profiles = source.skyblockProfiles(uuid)
        val status = source.status(uuid)
        val guild = source.guild(uuid)

        val (mappedProfiles, selectedIndex) = ProfileMapper.mapAll(profiles, uuid)
        if (mappedProfiles.isEmpty()) return ProfileState.Error("No Skyblock profile found")
        // Hypixel returns these fields as JSON null for players without a session/guild (e.g. never
        // joined, no guild) — getAsJsonObject(name) would raw-cast null → ClassCastException, so guard.
        val session = status.get("session")?.takeIf { it.isJsonObject }?.asJsonObject
        val online = session?.get("online")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false
        val location = if (online) HypixelStats.islandName(session?.get("mode")?.takeIf { it.isJsonPrimitive }?.asString) else null
        val guildName = guild.get("guild")?.takeIf { it.isJsonObject }?.asJsonObject?.get("name")?.asString
        val ownUuid = Minecraft.getInstance().user.profileId?.toString()?.replace("-", "")
        val isSelf = ownUuid != null && uuid.equals(ownUuid, ignoreCase = true)

        val playerData = runCatching { source.player(uuid) }.getOrNull()
            ?.get("player")?.takeIf { it.isJsonObject }?.asJsonObject
        val hypixelLevel = playerData?.get("networkExp")?.asDouble?.let { HypixelStats.networkLevel(it) }

        val gameProfile = MojangApi.fetchProfile(uuid)
        val name = gameProfile?.name ?: if (isSelf) Minecraft.getInstance().user.name else target
        val nametag = HypixelRank.nametag(playerData, name)

        return ProfileState.Loaded(
            mappedProfiles, selectedIndex, online, guildName, isSelf, hypixelLevel, location, gameProfile, nametag,
        )
    }

    /** Blank target = the local player; otherwise resolve the name via Mojang. Undashed UUID. */
    private fun resolveUuid(target: String): String? {
        if (target.isBlank()) {
            return Minecraft.getInstance().user.profileId?.toString()?.replace("-", "")
        }
        return MojangApi.resolveUuid(target)
    }
}
