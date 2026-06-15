package schrumbo.pv.api

import net.fabricmc.loader.api.FabricLoader
import java.util.Properties

/** Loads the Hypixel developer key from a gitignored local file (never in VCS or jar). */
object Secrets {

    private const val FILE = "pv-secrets.properties"
    private const val KEY = "hypixel_api_key"

    @Volatile
    private var cached: String? = null

    /** Re-reads the file until a key is found, so dropping it in later doesn't require a restart. */
    val hypixelKey: String?
        get() = cached ?: load()?.also { cached = it }

    private fun load(): String? {
        val path = FabricLoader.getInstance().configDir.resolve(FILE)
        if (!path.toFile().exists()) return null
        return runCatching {
            Properties().apply { path.toFile().inputStream().use { load(it) } }
                .getProperty(KEY)?.trim()?.ifEmpty { null }
        }.getOrNull()
    }
}
