package schrumbo.pv.util

import com.google.common.collect.LinkedHashMultimap
import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property
import com.mojang.authlib.properties.PropertyMap
import java.util.UUID

/** Builds a [GameProfile] carrying a skin `textures` property. */
object SkinProfile {

    /**
     * The bundled authlib exposes an immutable property map on a bare [GameProfile], so we populate a
     * mutable multimap first and pass it through the three-arg constructor.
     */
    fun withTextures(id: UUID, name: String, value: String, signature: String? = null): GameProfile {
        val map = LinkedHashMultimap.create<String, Property>()
        map.put("textures", if (signature != null) Property("textures", value, signature) else Property("textures", value))
        return GameProfile(id, name, PropertyMap(map))
    }
}
