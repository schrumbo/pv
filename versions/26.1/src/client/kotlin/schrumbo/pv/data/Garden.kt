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

/** One garden crop: collected total and its crop-upgrade tier (0..9). */
data class GardenCrop(val display: String, val icon: String, val collected: Long, val upgrade: Int)

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

    /** All 10 greenhouse mutation crops, in display order (API uses lower-case names). */
    val GREENHOUSE = listOf(
        "veilshroom", "thornshade", "ashwreath", "gloomgourd", "scourroot",
        "shadevine", "choconut", "coalroot", "dustgrain", "chocoberry",
    )

    /** Greenhouse crop name (lower-case) → base64 head texture (NEU). */
    val greenhouseSkulls: Map<String, String> =
        javaClass.getResourceAsStream("/assets/pv/greenhouse_skulls.json")?.reader()?.use {
            JsonParser.parseReader(it).asJsonObject.entrySet().associate { (k, v) -> k to v.asString }
        } ?: emptyMap()

    fun greenhouseName(key: String): String = key.replaceFirstChar { it.uppercase() }

    /** Garden level (1..15), its max, and progress toward the next level from total [experience]. */
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
        return Triple(max, max, 1.0)
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
            GardenCrop(c.display, c.icon, collected?.num(c.key)?.toLong() ?: 0L, upgrades?.num(c.key)?.toInt() ?: 0)
        }
        val gu = g.obj("garden_upgrades")
        val comp = g.obj("composter_data")
        val cd = g.obj("commission_data")
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
            visits = cd?.num("visits")?.toLong() ?: 0L,
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
