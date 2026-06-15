package schrumbo.pv.core

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry
import org.slf4j.LoggerFactory
import schrumbo.pv.render.ItemPipRenderer

/** Client entrypoint. Wires the command and any client-side registrations. */
object PvClient : ClientModInitializer {

    const val MOD_ID = "pv"
    val LOGGER = LoggerFactory.getLogger("ProfileViewer")

    override fun onInitializeClient() {
        PvCommand.register()
        PictureInPictureRendererRegistry.register { ctx -> ItemPipRenderer(ctx.bufferSource()) }
        LOGGER.info("[Profile Viewer] initialized")
    }
}
