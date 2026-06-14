package schrumbo.pv.core

import net.fabricmc.api.ClientModInitializer
import org.slf4j.LoggerFactory

/** Client entrypoint. Wires the command and any client-side registrations. */
object PvClient : ClientModInitializer {

    const val MOD_ID = "pv"
    val LOGGER = LoggerFactory.getLogger("ProfileViewer")

    override fun onInitializeClient() {
        PvCommand.register()
        LOGGER.info("[Profile Viewer] initialized")
    }
}
