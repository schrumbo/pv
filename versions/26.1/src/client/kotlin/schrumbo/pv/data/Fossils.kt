package schrumbo.pv.data

import com.google.gson.JsonParser

/** The eight Glacite fossils in display order, each with its NEU head texture. */
object Fossils {

    data class Fossil(val id: String, val name: String, val texture: String)

    private val ORDER = listOf(
        "HELIX" to "Helix Fossil",
        "FOOTPRINT_FOSSIL" to "Footprint Fossil",
        "CLAW_FOSSIL" to "Claw Fossil",
        "TUSK_FOSSIL" to "Tusk Fossil",
        "SPINE_FOSSIL" to "Spine Fossil",
        "WEBBED_FOSSIL" to "Webbed Fossil",
        "CLUBBED_FOSSIL" to "Clubbed Fossil",
        "UGLY_FOSSIL" to "Ugly Fossil",
    )

    val all: List<Fossil> by lazy {
        val tex = javaClass.getResourceAsStream("/assets/pv/fossil_skulls.json")?.reader()?.use {
            JsonParser.parseReader(it).asJsonObject.entrySet().associate { (k, v) -> k to v.asString }
        } ?: emptyMap()
        ORDER.map { (id, name) -> Fossil(id, name, tex[id] ?: "") }
    }
}
