package schrumbo.pv.data

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.minecraft.world.item.ItemStack
import schrumbo.pv.render.SkullItems

/** One sack: its display name, custom icon item and the Skyblock item ids it can hold. */
data class SackDef(val name: String, val icon: ItemStack, val contents: List<String>)

/**
 * Loads the embedded `sacks.json` — the sack definitions plus an icon spec (vanilla material or skull
 * texture) for every sack and every content item, generated from the NEU repo. Content icons resolve
 * to the real item (vanilla or custom skull) on demand and are cached.
 */
object SackRegistry {

    /** A pre-parsed icon: exactly one of [tex] / [item] is set. */
    private class IconSpec(val tex: String?, val item: String?)

    val sacks: List<SackDef>
    private val iconSpecs: Map<String, IconSpec>
    private val cache = HashMap<String, ItemStack>()

    init {
        val stream = javaClass.getResourceAsStream("/assets/pv/sacks.json") ?: error("missing sacks.json")
        val root = stream.reader().use { JsonParser.parseReader(it).asJsonObject }
        iconSpecs = root.getAsJsonObject("icons").entrySet().associate { (k, v) -> k to spec(v.asJsonObject) }
        sacks = root.getAsJsonArray("sacks").map { it.asJsonObject }.map { sack ->
            SackDef(
                name = sack.get("name").asString,
                icon = stack(spec(sack.getAsJsonObject("icon"))),
                contents = sack.getAsJsonArray("contents").map { c -> c.asString },
            )
        }
    }

    private fun spec(o: JsonObject) = IconSpec(o.get("tex")?.asString, o.get("item")?.asString)

    private fun stack(spec: IconSpec): ItemStack = when {
        spec.tex != null -> SkullItems.fromTexture(spec.tex)
        spec.item != null -> SkullItems.vanilla(spec.item).takeIf { !it.isEmpty } ?: FALLBACK
        else -> FALLBACK
    }

    /** Looks up a sack-content count, tolerating the `-`/`:` data-variant separator difference. */
    fun count(counts: Map<String, Long>, id: String): Long =
        counts[id] ?: counts[id.replace("-", ":")] ?: 0L

    /** The real item (vanilla or custom skull) for a content id; falls back to paper if unknown. */
    fun contentIcon(id: String): ItemStack = cache.getOrPut(id) {
        val spec = iconSpecs[id] ?: iconSpecs[id.replace("-", ";")]
        if (spec != null) stack(spec) else FALLBACK
    }

    private val FALLBACK = SkullItems.vanilla("paper")
}
