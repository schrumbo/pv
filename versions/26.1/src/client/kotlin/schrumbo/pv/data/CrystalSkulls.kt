package schrumbo.pv.data

import com.google.gson.JsonParser

/** Crystal-Nucleus gemstone name (Title-case, e.g. "Amber") → base64 head texture, from NEU. */
object CrystalSkulls {

    val skulls: Map<String, String> =
        javaClass.getResourceAsStream("/assets/pv/crystal_skulls.json")?.reader()?.use {
            JsonParser.parseReader(it).asJsonObject.entrySet().associate { (k, v) -> k to v.asString }
        } ?: emptyMap()
}
