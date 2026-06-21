package schrumbo.pv.data

import com.google.gson.JsonObject
import com.google.gson.JsonParser

/** Greenhouse "mutation" crops + currencies, from `member.garden_player_data` (in the profile payload). */
data class GreenhouseData(
    val copper: Int,
    val larvaConsumed: Int,
    val discovered: Set<String>,
    val analyzed: Set<String>,
)

/**
 * One garden crop: collected total, crop-upgrade tier (0..9), and the (overflow) crop milestone
 * derived from [collected] — [milestoneTier] can exceed the 46-tier cap, [milestoneProgress] is the
 * fraction toward the next tier.
 */
data class GardenCrop(
    val display: String,
    val icon: String,
    val collected: Long,
    val upgrade: Int,
    val milestoneTier: Int,
    val milestoneProgress: Double,
    val milestoneMax: Boolean,
)

/** Full Garden summary from the separate `/v2/skyblock/garden` endpoint. */
data class GardenData(
    val level: Int,
    val maxLevel: Int,
    val levelProgress: Double,
    val plotsUnlocked: Int,
    val plotsTotal: Int,
    val crops: List<GardenCrop>,
    val growthSpeed: Int,
    val yieldLevel: Int,
    val plotLimit: Int,
    val composterOrganic: Long,
    val composterFuel: Long,
    val composterCompost: Long,
    val composterItems: Long,
    /** Composter upgrade level by type (SPEED, MULTI_DROP, FUEL_CAP, ORGANIC_MATTER_CAP, COST_REDUCTION). */
    val composterUpgrades: Map<String, Int>,
    val visits: Long,
    val uniqueVisitors: Int,
)

/** Garden constants (from NEU `constants/garden.json`) + crop/greenhouse definitions and icons. */
object GardenRegistry {

    /** Incremental XP per garden level; index i = cost to advance from level i to i+1 (15 levels). */
    private val GARDEN_EXP = longArrayOf(0, 70, 70, 140, 240, 600, 1500, 2000, 2500, 3000, 10000, 10000, 10000, 10000, 10000)
    const val CROP_UPGRADE_MAX = 9
    const val PLOTS_TOTAL = 24

    data class CropDef(val key: String, val display: String, val icon: String)

    /** The 13 garden crops (classic 10 + Galatea flora), in display order, with vanilla icons. */
    val CROPS = listOf(
        CropDef("WHEAT", "Wheat", "wheat"),
        CropDef("CARROT_ITEM", "Carrot", "carrot"),
        CropDef("POTATO_ITEM", "Potato", "potato"),
        CropDef("PUMPKIN", "Pumpkin", "pumpkin"),
        CropDef("MELON", "Melon", "melon_slice"),
        CropDef("SUGAR_CANE", "Sugar Cane", "sugar_cane"),
        CropDef("CACTUS", "Cactus", "cactus"),
        CropDef("INK_SACK:3", "Cocoa Beans", "cocoa_beans"),
        CropDef("NETHER_STALK", "Nether Wart", "nether_wart"),
        CropDef("MUSHROOM_COLLECTION", "Mushroom", "red_mushroom"),
        CropDef("DOUBLE_PLANT", "Sunflower", "sunflower"),
        CropDef("MOONFLOWER", "Moonflower", "lily_of_the_valley"),
        CropDef("WILD_ROSE", "Wild Rose", "poppy"),
    )

    /** One greenhouse mutation: id, display name, rarity, and whether it must be analyzed at all. */
    data class Mutation(val id: String, val name: String, val rarity: String, val analyzable: Boolean)

    /** All greenhouse mutations from the meowdding repo, ordered best (Legendary) → worst. */
    val MUTATIONS: List<Mutation> by lazy {
        val arr = javaClass.getResourceAsStream("/assets/pv/mutations.json")?.reader()?.use {
            JsonParser.parseReader(it).asJsonArray
        } ?: return@lazy emptyList()
        arr.map { it.asJsonObject }
            .map { Mutation(it.get("id").asString, it.get("name").asString, it.get("rarity").asString, it.get("analyzable")?.asBoolean ?: true) }
            .sortedBy { RARITY_ORDER.indexOf(it.rarity) }
    }

    /** Mutation id → render spec: either `{tex:…}` (skull) or `{item:…}` (vanilla), from NEU. */
    private val mutationRenders: Map<String, Pair<String?, String?>> by lazy {
        javaClass.getResourceAsStream("/assets/pv/mutation_renders.json")?.reader()?.use {
            JsonParser.parseReader(it).asJsonObject.entrySet().associate { (k, v) ->
                val o = v.asJsonObject
                k to (o.get("tex")?.asString to o.get("item")?.asString)
            }
        } ?: emptyMap()
    }

    /** The real item icon for a mutation (textured head or vanilla item); empty if unknown. */
    fun mutationRender(id: String): net.minecraft.world.item.ItemStack {
        val (tex, item) = mutationRenders[id] ?: return net.minecraft.world.item.ItemStack.EMPTY
        return when {
            tex != null -> schrumbo.pv.render.SkullItems.fromTexture(tex)
            item != null -> {
                val loc = net.minecraft.resources.Identifier.tryParse(item) ?: return net.minecraft.world.item.ItemStack.EMPTY
                net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(loc))
            }
            else -> net.minecraft.world.item.ItemStack.EMPTY
        }
    }

    /** Rarity sort order (best first) and its display colour. */
    val RARITY_ORDER = listOf("LEGENDARY", "EPIC", "RARE", "UNCOMMON", "COMMON")
    fun rarityColor(rarity: String): Int = when (rarity) {
        "LEGENDARY" -> 0xFFFFAA00.toInt()
        "EPIC" -> 0xFFAA00AA.toInt()
        "RARE" -> 0xFF5555FF.toInt()
        "UNCOMMON" -> 0xFF55FF55.toInt()
        else -> 0xFFFFFFFF.toInt()
    }

    /** Legacy § colour code for a rarity, for item tooltips. */
    fun rarityColorCode(rarity: String): String = when (rarity) {
        "LEGENDARY" -> "§6"
        "EPIC" -> "§5"
        "RARE" -> "§9"
        "UNCOMMON" -> "§a"
        else -> "§f"
    }

    /**
     * Garden level + its base cap + progress toward the next level. Past the cap the level overflows,
     * each further level costing the last bracket (SkyHanni-style overflow leveling).
     */
    fun level(experience: Long): Triple<Int, Int, Double> {
        val max = GARDEN_EXP.size - 1
        var acc = 0L
        for (lvl in 1 until GARDEN_EXP.size) {
            val need = GARDEN_EXP[lvl]
            if (experience < acc + need) {
                return Triple(lvl, max, ((experience - acc).toDouble() / need).coerceIn(0.0, 1.0))
            }
            acc += need
        }
        val step = GARDEN_EXP.last()
        val over = experience - acc
        return Triple(max + (over / step).toInt(), max, (over % step).toDouble() / step)
    }

    /** Per-crop milestone brackets (incremental amounts per tier), from the meowdding repo. */
    private val CROP_MILESTONES: Map<String, LongArray> by lazy {
        javaClass.getResourceAsStream("/assets/pv/crop_milestones.json")?.reader()?.use {
            JsonParser.parseReader(it).asJsonObject.entrySet().associate { (k, v) ->
                k to v.asJsonArray.map { e -> e.asLong }.toLongArray()
            }
        } ?: emptyMap()
    }

    /** Crop milestone for [collected] of crop [key]: (tier, progress, atOrPastCap). Tier overflows past 46. */
    fun cropMilestone(key: String, collected: Long): Triple<Int, Double, Boolean> {
        val brackets = CROP_MILESTONES[key] ?: return Triple(0, 0.0, false)
        var acc = 0L
        for (i in brackets.indices) {
            val need = brackets[i]
            if (collected < acc + need) return Triple(i, (collected - acc).toDouble() / need, false)
            acc += need
        }
        val step = brackets.last()
        val over = collected - acc
        return Triple(brackets.size + (over / step).toInt(), (over % step).toDouble() / step, true)
    }
}

/** Maps a `/v2/skyblock/garden` payload's `garden` object into [GardenData]. */
object GardenMapper {

    fun map(root: JsonObject): GardenData? {
        val g = root.obj("garden") ?: return null
        val exp = g.num("garden_experience")?.toLong() ?: 0L
        val (lvl, max, prog) = GardenRegistry.level(exp)
        val collected = g.obj("resources_collected")
        val upgrades = g.obj("crop_upgrade_levels")
        val crops = GardenRegistry.CROPS.map { c ->
            val amount = collected?.num(c.key)?.toLong() ?: 0L
            val (mTier, mProg, mMax) = GardenRegistry.cropMilestone(c.key, amount)
            GardenCrop(c.display, c.icon, amount, upgrades?.num(c.key)?.toInt() ?: 0, mTier, mProg, mMax)
        }
        val gu = g.obj("garden_upgrades")
        val comp = g.obj("composter_data")
        val cd = g.obj("commission_data")
        val compUpgrades = comp?.obj("upgrades")?.entrySet()
            ?.mapNotNull { (k, v) -> if (v.isJsonPrimitive) k.uppercase() to v.asInt else null }?.toMap() ?: emptyMap()
        return GardenData(
            level = lvl, maxLevel = max, levelProgress = prog,
            plotsUnlocked = g.arr("unlocked_plots_ids")?.size() ?: 0,
            plotsTotal = GardenRegistry.PLOTS_TOTAL,
            crops = crops,
            growthSpeed = gu?.num("GROWTH_SPEED")?.toInt() ?: 0,
            yieldLevel = gu?.num("YIELD")?.toInt() ?: 0,
            plotLimit = gu?.num("PLOT_LIMIT")?.toInt() ?: 0,
            composterOrganic = comp?.num("organic_matter")?.toLong() ?: 0L,
            composterFuel = comp?.num("fuel_units")?.toLong() ?: 0L,
            composterCompost = comp?.num("compost_units")?.toLong() ?: 0L,
            composterItems = comp?.num("compost_items")?.toLong() ?: 0L,
            composterUpgrades = compUpgrades,
            visits = cd?.num("total_completed")?.toLong() ?: 0L,
            uniqueVisitors = cd?.num("unique_npcs_served")?.toInt() ?: 0,
        )
    }

    private fun JsonObject.obj(name: String): JsonObject? =
        get(name)?.takeIf { it.isJsonObject }?.asJsonObject

    private fun JsonObject.arr(name: String) =
        get(name)?.takeIf { it.isJsonArray }?.asJsonArray

    private fun JsonObject.num(name: String): Double? =
        get(name)?.takeIf { it.isJsonPrimitive }?.asDouble
}
