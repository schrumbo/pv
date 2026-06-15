package schrumbo.pv.render

import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.ResolvableProfile
import java.util.UUID

/** Builds and caches the [ItemStack]s used as bestiary icons: textured player heads and vanilla items. */
object SkullItems {

    private val skulls = HashMap<String, ItemStack>()
    private val items = HashMap<String, ItemStack>()

    /** A `player_head` carrying the given base64 skin texture; [ItemStack.EMPTY] if construction fails. */
    fun fromTexture(base64: String): ItemStack = skulls.getOrPut(base64) {
        runCatching {
            val profile = GameProfile(UUID.randomUUID(), "pv")
            profile.properties.put("textures", Property("textures", base64))
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
