package schrumbo.pv.core

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.tree.CommandNode
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.fabric.api.event.Event
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import schrumbo.pv.ui.screen.PvScreen

/**
 * Registers `/pv [name]` for the profile viewer. Runs in a late event phase, then drops any existing
 * `pv` node (incl. another mod's argument branch) and registers ours fresh, so we fully own `/pv`.
 */
object PvCommand {

    private val LATE_PHASE = Identifier.fromNamespaceAndPath("pv", "late")

    fun register() {
        ClientCommandRegistrationCallback.EVENT.addPhaseOrdering(Event.DEFAULT_PHASE, LATE_PHASE)
        ClientCommandRegistrationCallback.EVENT.register(
            LATE_PHASE,
            ClientCommandRegistrationCallback { dispatcher, _ -> build(dispatcher) },
        )
    }

    private fun build(dispatcher: CommandDispatcher<FabricClientCommandSource>) {
        removeExisting(dispatcher, "pv")
        dispatcher.register(
            LiteralArgumentBuilder.literal<FabricClientCommandSource>("pv")
                .executes { open(Minecraft.getInstance().user.name); 1 }
                .then(
                    RequiredArgumentBuilder.argument<FabricClientCommandSource, String>(
                        "name", StringArgumentType.word()
                    ).executes { ctx -> open(StringArgumentType.getString(ctx, "name")); 1 }
                )
        )
    }

    private fun open(target: String) {
        val mc = Minecraft.getInstance()
        mc.execute { mc.setScreen(PvScreen(target)) }
    }

    /** Removes a literal (and its whole subtree) from the dispatcher root; Brigadier has no public API. */
    private fun removeExisting(dispatcher: CommandDispatcher<FabricClientCommandSource>, name: String) {
        val root = dispatcher.root
        for (field in listOf("children", "literals", "arguments")) {
            runCatching {
                CommandNode::class.java.getDeclaredField(field).apply { isAccessible = true }
                    .let { (it.get(root) as MutableMap<*, *>).remove(name) }
            }
        }
    }
}
