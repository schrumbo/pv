package schrumbo.pv.data

import com.google.gson.JsonParser

/** Static definition of one collection: its key, display name and the per-tier amount thresholds. */
data class CollectionDef(val key: String, val name: String, val reqs: List<Long>)

/** Static definition of one collection category: key, display name, rail icon and its collections. */
data class CategoryDef(val key: String, val name: String, val icon: String, val items: List<CollectionDef>)

/**
 * A collection resolved against a player's amount: definition, collected amount, current/max tier
 * and progress toward the next tier. [maxed] when [tier] has reached [maxTier].
 */
data class CollectionTier(
    val def: CollectionDef,
    val amount: Long,
    val tier: Int,
    val maxTier: Int,
    val progress: Double,
    val next: Long,
) {
    val maxed: Boolean get() = tier >= maxTier
}

/** A category with its collections resolved against a player's amounts, plus maxed/total counts. */
data class CategoryProgress(val def: CategoryDef, val items: List<CollectionTier>) {
    val maxedCount: Int get() = items.count { it.maxed }
    val total: Int get() = items.size
}

/** Loads the embedded `collections.json` once and resolves player collection amounts into tiers. */
object CollectionsRegistry {

    val categories: List<CategoryDef>

    init {
        val stream = javaClass.getResourceAsStream("/assets/pv/collections.json")
            ?: error("missing collections.json resource")
        val root = stream.reader().use { JsonParser.parseReader(it).asJsonObject }
        categories = root.getAsJsonArray("categories").map { it.asJsonObject }.map { cat ->
            CategoryDef(
                key = cat.get("key").asString,
                name = cat.get("name").asString,
                icon = cat.get("icon").asString,
                items = cat.getAsJsonArray("items").map { it.asJsonObject }.map { item ->
                    CollectionDef(
                        key = item.get("key").asString,
                        name = item.get("name").asString,
                        reqs = item.getAsJsonArray("reqs").map { r -> r.asLong },
                    )
                },
            )
        }
    }

    /** Resolves every category against [amounts] (the `collection` map: key → collected amount). */
    fun resolve(amounts: Map<String, Long>): List<CategoryProgress> =
        categories.map { cat -> CategoryProgress(cat, cat.items.map { tier(it, amounts) }) }

    private fun tier(def: CollectionDef, amounts: Map<String, Long>): CollectionTier {
        val amount = amounts[def.key] ?: 0L
        val maxTier = def.reqs.size.coerceAtLeast(1)
        val tier = def.reqs.count { amount >= it }.coerceAtMost(maxTier)
        return if (tier >= maxTier) {
            CollectionTier(def, amount, tier, maxTier, 1.0, def.reqs.lastOrNull() ?: amount)
        } else {
            val prev = if (tier == 0) 0L else def.reqs[tier - 1]
            val next = def.reqs[tier]
            val progress = (amount - prev).toDouble() / (next - prev).coerceAtLeast(1)
            CollectionTier(def, amount, tier, maxTier, progress.coerceIn(0.0, 1.0), next)
        }
    }
}
