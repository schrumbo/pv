package schrumbo.pv.data

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import schrumbo.pv.render.SkullItems
import net.minecraft.world.item.ItemStack

/** Static definition of one bestiary mob family: display name, icon, kill keys and tier brackets. */
data class MobDef(
    val name: String,
    val cap: Long,
    val bracket: String,
    val keys: List<String>,
    val texture: String?,
    val item: String?,
)

/** Static definition of one bestiary island: its key, display name, in-game menu icon and mobs. */
data class IslandDef(
    val key: String,
    val name: String,
    val mobs: List<MobDef>,
    val texture: String?,
    val item: String?,
)

/**
 * A mob resolved against a player's kills: its definition, summed kills, current and max tier,
 * and progress toward the next tier. [maxed] when [tier] has reached [maxTier].
 */
data class MobTier(
    val def: MobDef,
    val kills: Long,
    val tier: Int,
    val maxTier: Int,
    val progress: Double,
    val next: Long,
) {
    val maxed: Boolean get() = tier >= maxTier
}

/** An island with its mobs resolved against a player's kills, plus per-island maxed/total counts. */
data class IslandProgress(val def: IslandDef, val mobs: List<MobTier>) {
    val maxedCount: Int get() = mobs.count { it.maxed }
    val total: Int get() = mobs.size
}

/**
 * Loads the embedded `bestiary.json` once and resolves player kills into per-island tiers.
 * The bracket table maps a bracket key to cumulative kill thresholds; a mob's max tier is the
 * number of thresholds at or below its `cap`.
 */
object BestiaryRegistry {

    private val brackets: Map<String, List<Long>>
    val islands: List<IslandDef>

    init {
        val stream = javaClass.getResourceAsStream("/assets/pv/bestiary.json")
            ?: error("missing bestiary.json resource")
        val root = stream.reader().use { JsonParser.parseReader(it).asJsonObject }
        brackets = root.getAsJsonObject("brackets").entrySet().associate { (k, v) ->
            k to v.asJsonArray.map { it.asLong }
        }
        islands = root.getAsJsonArray("islands").map { it.asJsonObject }.map { island ->
            val ic = island.get("icon")?.takeIf { it.isJsonObject }?.asJsonObject
            IslandDef(
                key = island.get("key").asString,
                name = island.get("name").asString,
                mobs = island.getAsJsonArray("mobs").map { it.asJsonObject }.map { mob -> mobDef(mob) },
                texture = ic?.get("tex")?.asString,
                item = ic?.get("item")?.asString,
            )
        }
    }

    /** The island's in-game bestiary-menu icon — a textured skull or vanilla item. */
    fun islandIcon(def: IslandDef): ItemStack = when {
        def.texture != null -> SkullItems.fromTexture(def.texture)
        def.item != null -> SkullItems.vanilla(def.item)
        else -> ItemStack.EMPTY
    }

    private fun mobDef(mob: JsonObject): MobDef = MobDef(
        name = mob.get("name").asString,
        cap = mob.get("cap").asLong,
        bracket = mob.get("bracket").asString,
        keys = mob.getAsJsonArray("keys").map { it.asString },
        texture = mob.get("tex")?.asString,
        item = mob.get("item")?.asString,
    )

    /** Resolves every island against [kills] (the `bestiary.kills` map: mob key → count). */
    fun resolve(kills: Map<String, Long>): List<IslandProgress> =
        islands.map { island -> IslandProgress(island, island.mobs.map { tier(it, kills) }) }

    private fun tier(def: MobDef, kills: Map<String, Long>): MobTier {
        val total = def.keys.sumOf { kills[it] ?: 0L }
        val thresholds = brackets[def.bracket] ?: emptyList()
        val maxTier = thresholds.count { it <= def.cap }.coerceAtLeast(1)
        val tier = thresholds.count { total >= it }.coerceAtMost(maxTier)
        return if (tier >= maxTier) {
            MobTier(def, total, tier, maxTier, 1.0, def.cap)
        } else {
            val prev = if (tier == 0) 0L else thresholds[tier - 1]
            val next = thresholds[tier]
            val progress = (total - prev).toDouble() / (next - prev).coerceAtLeast(1)
            MobTier(def, total, tier, maxTier, progress.coerceIn(0.0, 1.0), next)
        }
    }

    /** Cached, fully-built icon [ItemStack] for a mob — a textured skull, a vanilla item, or empty. */
    fun icon(def: MobDef): ItemStack = when {
        def.texture != null -> SkullItems.fromTexture(def.texture)
        def.item != null -> SkullItems.vanilla(def.item)
        else -> ItemStack.EMPTY
    }
}
