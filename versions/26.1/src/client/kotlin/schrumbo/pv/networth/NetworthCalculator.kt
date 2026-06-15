package schrumbo.pv.networth

import com.google.gson.JsonObject
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.NbtIo
import java.io.ByteArrayInputStream
import java.util.Base64

/**
 * Base-value networth: every item priced at its SkyHelper base price, plus sacks, purse and bank.
 * This is the first layer of a faithful SkyHelper-Networth port; item modifiers (reforge, stars,
 * enchants, gemstones, recombobulator, pets …) are added on top in later layers.
 */
object NetworthCalculator {

    /** Computes the networth for one profile member with the given [bankBalance] and price map. */
    fun compute(member: JsonObject, bankBalance: Double, prices: Map<String, Double>): Long {
        var total = bankBalance
        total += member.obj("currencies")?.get("coin_purse")?.asDouble ?: 0.0

        val inv = member.obj("inventory") ?: member
        for (data in containers(inv)) total += valueContainer(data, prices)
        total += sacks(inv, prices)
        return total.toLong()
    }

    private fun containers(inv: JsonObject): List<String> {
        val out = mutableListOf<String>()
        for (key in TOP_LEVEL) inv.obj(key)?.get("data")?.asString?.let(out::add)
        for (group in NESTED) inv.obj(group)?.entrySet()?.forEach { (_, v) ->
            (v as? JsonObject)?.get("data")?.asString?.let(out::add)
        }
        return out
    }

    private fun valueContainer(base64Gzip: String, prices: Map<String, Double>): Double = runCatching {
        val bytes = Base64.getDecoder().decode(base64Gzip.trim())
        val root = NbtIo.readCompressed(ByteArrayInputStream(bytes), NbtAccounter.unlimitedHeap())
        val list = root.getListOrEmpty("i")
        var sum = 0.0
        for (i in 0 until list.size) sum += valueItem(list.getCompoundOrEmpty(i), prices)
        sum
    }.getOrDefault(0.0)

    private fun valueItem(c: CompoundTag, prices: Map<String, Double>): Double {
        val ea = c.getCompound("tag").orElse(null)?.getCompound("ExtraAttributes")?.orElse(null) ?: return 0.0
        val id = ea.getString("id").orElse(null)?.uppercase() ?: return 0.0
        val count = c.getByteOr("Count", 1).toInt().coerceAtLeast(1)
        return (prices[id] ?: 0.0) * count
    }

    private fun sacks(inv: JsonObject, prices: Map<String, Double>): Double {
        val sacks = inv.obj("sacks_counts") ?: return 0.0
        var sum = 0.0
        for ((id, v) in sacks.entrySet()) {
            sum += (prices[id.uppercase()] ?: 0.0) * runCatching { v.asLong }.getOrDefault(0L)
        }
        return sum
    }

    private fun JsonObject.obj(name: String): JsonObject? =
        get(name)?.takeIf { it.isJsonObject }?.asJsonObject

    private val TOP_LEVEL = listOf(
        "inv_contents", "inv_armor", "ender_chest_contents", "equipment_contents",
        "personal_vault_contents", "wardrobe_contents",
    )
    private val NESTED = listOf("backpack_contents", "bag_contents")
}
