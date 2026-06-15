package schrumbo.pv.data

import com.google.gson.JsonParser
import net.minecraft.world.item.ItemStack
import schrumbo.pv.render.SkullItems

/** One owned pet: its type, rarity tier, experience, and whether it is the active pet. */
data class PetEntry(
    val type: String,
    val tier: String,
    val exp: Double,
    val active: Boolean,
    val heldItem: String?,
    val skin: String?,
)

/** A resolved pet level: current [level], the pet's [maxLevel], and [progress] toward the next. */
data class PetLevel(val level: Int, val maxLevel: Int, val progress: Double) {
    val maxed: Boolean get() = level >= maxLevel
}

/**
 * Loads the embedded `pets.json` (NEU leveling + textures) once and resolves pet experience into
 * levels. The per-level cost table is sliced by the rarity offset; custom pets cap at their own max.
 */
object PetRegistry {

    private val levels: List<Double>
    private val offsets: Map<String, Int>
    private val custom: Map<String, Int>
    private val names: Map<String, String>
    private val textures: Map<String, String>

    init {
        val stream = javaClass.getResourceAsStream("/assets/pv/pets.json")
            ?: error("missing pets.json resource")
        val root = stream.reader().use { JsonParser.parseReader(it).asJsonObject }
        levels = root.getAsJsonArray("levels").map { it.asDouble }
        offsets = root.getAsJsonObject("offsets").entrySet().associate { (k, v) -> k to v.asInt }
        custom = root.getAsJsonObject("custom").entrySet().associate { (k, v) -> k to v.asInt }
        names = root.getAsJsonObject("names").entrySet().associate { (k, v) -> k to v.asString }
        textures = root.getAsJsonObject("textures").entrySet().associate { (k, v) -> k to v.asString }
    }

    fun level(entry: PetEntry): PetLevel {
        val maxLevel = custom[entry.type] ?: 100
        val offset = offsets[entry.tier] ?: 0
        val costs = levels.drop(offset).take(maxLevel - 1)
        var acc = 0.0
        var level = 1
        var i = 0
        while (i < costs.size && entry.exp >= acc + costs[i]) {
            acc += costs[i]; level++; i++
        }
        val progress = if (level >= maxLevel || i >= costs.size) 1.0
        else ((entry.exp - acc) / costs[i]).coerceIn(0.0, 1.0)
        return PetLevel(level, maxLevel, progress)
    }

    fun icon(type: String): ItemStack = textures[type]?.let { SkullItems.fromTexture(it) } ?: ItemStack.EMPTY

    fun displayName(type: String): String =
        names[type] ?: type.split('_').joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
}
