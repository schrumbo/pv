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
    private val customLevels: Map<String, List<Double>>
    private val names: Map<String, String>
    private val textures: Map<String, String>
    private val skins: Map<String, String>
    private val held: Map<String, String>
    private val heldVanilla: Map<String, String>

    init {
        val stream = javaClass.getResourceAsStream("/assets/pv/pets.json")
            ?: error("missing pets.json resource")
        val root = stream.reader().use { JsonParser.parseReader(it).asJsonObject }
        levels = root.getAsJsonArray("levels").map { it.asDouble }
        offsets = root.getAsJsonObject("offsets").entrySet().associate { (k, v) -> k to v.asInt }
        custom = root.getAsJsonObject("custom").entrySet().associate { (k, v) -> k to v.asInt }
        customLevels = root.getAsJsonObject("customLevels")?.entrySet()
            ?.associate { (k, v) -> k to v.asJsonArray.map { e -> e.asDouble } } ?: emptyMap()
        names = root.getAsJsonObject("names").entrySet().associate { (k, v) -> k to v.asString }
        textures = root.getAsJsonObject("textures").entrySet().associate { (k, v) -> k to v.asString }
        skins = root.getAsJsonObject("skins")?.entrySet()?.associate { (k, v) -> k to v.asString } ?: emptyMap()
        held = root.getAsJsonObject("held")?.entrySet()?.associate { (k, v) -> k to v.asString } ?: emptyMap()
        heldVanilla = root.getAsJsonObject("heldVanilla")?.entrySet()?.associate { (k, v) -> k to v.asString } ?: emptyMap()
    }

    /** Icon for a pet's held item (PET_ITEM_… skull or vanilla); a name-tag placeholder when unmapped. */
    fun heldIcon(id: String?): ItemStack {
        if (id == null) return ItemStack.EMPTY
        held[id]?.let { val s = SkullItems.fromTexture(it); if (!s.isEmpty) return s }
        heldVanilla[id]?.let { val s = SkullItems.vanilla(it); if (!s.isEmpty) return s }
        return SkullItems.vanilla("name_tag")
    }

    fun level(entry: PetEntry): PetLevel {
        val maxLevel = custom[entry.type] ?: 100
        val offset = offsets[entry.tier] ?: 0
        // Levels 1..100 come from the rarity-sliced base table; pets with extended leveling (dragons,
        // 100..200) append their own per-level costs on top, matching NEU/SkyCrypt.
        val base = levels.drop(offset).take(99)
        val costs = (base + (customLevels[entry.type] ?: emptyList())).take(maxLevel - 1)
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

    /** The pet's textured skull, or a bone fallback so newer pets without a bundled texture still show. */
    fun icon(type: String): ItemStack {
        val skull = textures[type]?.let { SkullItems.fromTexture(it) }
        return if (skull != null && !skull.isEmpty) skull else SkullItems.vanilla("bone")
    }

    /** The pet's icon, preferring its applied skin texture over the default species skull. */
    fun iconFor(entry: PetEntry): ItemStack {
        entry.skin?.let { skins[it] }?.let { val s = SkullItems.fromTexture(it); if (!s.isEmpty) return s }
        return icon(entry.type)
    }

    fun displayName(type: String): String =
        names[type] ?: type.split('_').joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
}
