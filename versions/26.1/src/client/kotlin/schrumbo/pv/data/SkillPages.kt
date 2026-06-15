package schrumbo.pv.data

/** One Crystal Nucleus crystal and its state (`NOT_FOUND` / `FOUND` / `PLACED`). */
data class CrystalState(val name: String, val state: String)

/** Heart-of-the-Mountain / Crystal Hollows / Glacite summary for the Mining page. */
data class MiningData(
    val mithril: Long,
    val mithrilTotal: Long,
    val gemstone: Long,
    val gemstoneTotal: Long,
    val glacite: Long,
    val glaciteTotal: Long,
    val tokens: Long,
    val crystals: List<CrystalState>,
    val corpses: List<Pair<String, Long>>,
    val fossilsDonated: Int,
    val mineshafts: Long,
)

/** One trophy fish with its per-tier catch counts; [total] and [highestTier] are derived. */
data class TrophyFish(
    val name: String,
    val bronze: Long,
    val silver: Long,
    val gold: Long,
    val diamond: Long,
) {
    val total: Long get() = bronze + silver + gold + diamond
    /** 0 = none, 1 = bronze, 2 = silver, 3 = gold, 4 = diamond. */
    val highestTier: Int get() = when {
        diamond > 0 -> 4
        gold > 0 -> 3
        silver > 0 -> 2
        bronze > 0 -> 1
        else -> 0
    }
}

/** Trophy fishing summary for the Fishing page. */
data class TrophyData(val totalCaught: Long, val fish: List<TrophyFish>)

/** Best collected amount for a single crop, for Jacob's personal bests. */
data class CropPB(val crop: String, val best: Long)

/** Jacob's contest summary for the Farming page. */
data class JacobsData(
    val gold: Int,
    val silver: Int,
    val bronze: Int,
    val contests: Int,
    val uniqueGold: Int,
    val uniqueSilver: Int,
    val uniqueBronze: Int,
    val doubleDrops: Int,
    val farmingCap: Int,
    val pbs: List<CropPB>,
)

/** A named, leveled entry (an attribute stack). */
data class NamedLevel(val name: String, val level: Int)

/** Attribute / shard summary for the Hunting page. */
data class AttributesData(val attributes: List<NamedLevel>, val shardsOwned: Long, val shardTypes: Int)

/** One tree's gift count and claimed milestone tier, for the Foraging page. */
data class TreeGift(val name: String, val gifts: Long, val tier: Int)

/** Foraging (Galatea) summary for the Foraging page. */
data class ForagingData(
    val whispers: Long,
    val whispersSpent: Long,
    val dailyTrees: Long,
    val trees: List<TreeGift>,
)

/** The Rift dimension summary for the Rift page. */
data class RiftData(
    val motes: Long,
    val lifetimeMotes: Long,
    val enigmaSouls: Int,
    val galleryTrophies: Int,
    val witherEyes: Int,
    val catsFound: Int,
    val areas: Int,
    val hasMontezuma: Boolean,
)
