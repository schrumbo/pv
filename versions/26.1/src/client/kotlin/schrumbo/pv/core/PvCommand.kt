package schrumbo.pv.core

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.Minecraft
import schrumbo.pv.ui.screen.PvScreen

/** Registers `/pv [name]` to open the profile viewer for a player (or self). */
object PvCommand {

    fun register() {
        ClientCommandRegistrationCallback.EVENT.register(ClientCommandRegistrationCallback { dispatcher, _ ->
            dispatcher.register(
                LiteralArgumentBuilder.literal<FabricClientCommandSource>("pv")
                    .executes { open(""); 1 }
                    .then(
                        RequiredArgumentBuilder.argument<FabricClientCommandSource, String>(
                            "name", StringArgumentType.word()
                        ).executes { ctx -> open(StringArgumentType.getString(ctx, "name")); 1 }
                    )
            )
        })
    }

    private fun open(target: String) {
        val mc = Minecraft.getInstance()
        mc.execute { mc.setScreen(PvScreen(target)) }
    }
}
