package schrumbo.pv.render

import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.ResolvableProfile
import schrumbo.pv.util.SkinProfile
import java.util.UUID

/** Builds and caches the [ItemStack]s used as bestiary icons: textured player heads and vanilla items. */
object SkullItems {

    private val skulls = HashMap<String, ItemStack>()
    private val items = HashMap<String, ItemStack>()

    /**
     * A `player_head` carrying the given base64 skin texture; [ItemStack.EMPTY] if construction fails.
     * The texture must go through the three-arg GameProfile constructor (authlib's property map on a
     * bare profile is immutable, so a post-construction put is dropped and the skull renders blank);
     * [SkinProfile.withTextures] does this, mirroring the decoded-inventory path.
     */
    fun fromTexture(base64: String): ItemStack = skulls.getOrPut(base64) {
        runCatching {
            val profile = SkinProfile.withTextures(UUID.randomUUID(), "pv", base64)
            ItemStack(Items.PLAYER_HEAD).apply {
                set(DataComponents.PROFILE, ResolvableProfile.createResolved(profile))
            }
        }.getOrDefault(ItemStack.EMPTY)
    }

    /** A plain vanilla item by id (e.g. `rotten_flesh`); [ItemStack.EMPTY] when unknown. */
    fun vanilla(id: String): ItemStack = items.getOrPut(id) {
        val key = Identifier.tryParse(id) ?: return@getOrPut ItemStack.EMPTY
        ItemStack(BuiltInRegistries.ITEM.getValue(key))
    }
}
