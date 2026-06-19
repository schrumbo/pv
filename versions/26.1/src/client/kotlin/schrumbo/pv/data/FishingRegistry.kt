package schrumbo.pv.data

import com.google.gson.JsonParser
import net.minecraft.world.item.ItemStack
import schrumbo.pv.render.SkullItems

/** Loads the embedded `fishing.json` (trophy-fish head textures per tier) once. */
object FishingRegistry {

    private val trophy: Map<String, String>

    init {
        val stream = javaClass.getResourceAsStream("/assets/pv/fishing.json")
            ?: error("missing fishing.json resource")
        val root = stream.reader().use { JsonParser.parseReader(it).asJsonObject }
        trophy = root.getAsJsonObject("trophy").entrySet().associate { (k, v) -> k to v.asString }
    }

    /** Head for a trophy fish at [tier] (bronze/silver/gold/diamond); empty when missing. */
    fun head(fishKey: String, tier: String): ItemStack =
        trophy["${fishKey}_$tier"]?.let { SkullItems.fromTexture(it) } ?: ItemStack.EMPTY
}
