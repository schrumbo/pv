package schrumbo.pv.data

import com.google.gson.JsonParser
import net.minecraft.ChatFormatting
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.ItemLore
import schrumbo.pv.render.SkullItems
import net.minecraft.network.chat.Component as McText

/** One hunting attribute and the shard that feeds it (from `attributes.min.json`). */
data class AttributeDef(
    val id: String,
    val name: String,
    val shardKey: String,
    val shardName: String,
    val rarity: String,
    val category: String,
    val max: Int,
    val tex: String,
    val item: String,
)

/** A resolved attribute: syphoned shard total, current level (0..10), and progress to the next level. */
data class AttributeEntry(val def: AttributeDef, val syphoned: Int, val level: Int, val into: Int, val needed: Int) {
    val maxed: Boolean get() = level >= 10
    val progress: Double get() = if (needed <= 0) 1.0 else (into.toDouble() / needed).coerceIn(0.0, 1.0)
}

/**
 * Loads the embedded `attributes.json` (all 189 hunting attributes with their shard, rarity, head
 * texture and per-rarity level costs). Attribute level maxes at 10; how many shards each level costs
 * depends on the rarity.
 */
object AttributeRegistry {

    val attributes: List<AttributeDef>
    private val levelling: Map<String, List<Int>>
    private val byId: Map<String, AttributeDef>
    private val byShard: Map<String, AttributeDef>

    private val RARITY_FMT = mapOf(
        "COMMON" to ChatFormatting.WHITE, "UNCOMMON" to ChatFormatting.GREEN, "RARE" to ChatFormatting.BLUE,
        "EPIC" to ChatFormatting.DARK_PURPLE, "LEGENDARY" to ChatFormatting.GOLD,
    )

    init {
        val root = javaClass.getResourceAsStream("/assets/pv/attributes.json")?.reader()?.use {
            JsonParser.parseReader(it).asJsonObject
        } ?: error("missing attributes.json")
        levelling = root.getAsJsonObject("levelling").entrySet()
            .associate { (k, v) -> k.uppercase() to v.asJsonArray.map { e -> e.asInt } }
        attributes = root.getAsJsonArray("attributes").map { it.asJsonObject }.map { o ->
            AttributeDef(
                id = o.get("id").asString, name = o.get("name").asString,
                shardKey = o.get("shardKey").asString, shardName = o.get("shardName").asString,
                rarity = o.get("rarity").asString, category = o.get("category").asString,
                max = o.get("max").asInt, tex = o.get("tex").asString, item = o.get("item").asString,
            )
        }
        byId = attributes.associateBy { it.id.uppercase() }
        byShard = attributes.associateBy { it.shardKey.uppercase() }
    }

    fun byShard(key: String): AttributeDef? = byShard[key.uppercase()]

    fun icon(def: AttributeDef): ItemStack = when {
        def.tex.isNotEmpty() -> SkullItems.fromTexture(def.tex)
        def.item.isNotEmpty() -> SkullItems.vanilla(def.item)
        else -> SkullItems.vanilla("prismarine_shard")
    }

    /** Resolves the attribute's level (0..10) and progress from its [syphoned] shard total. */
    fun resolve(def: AttributeDef, syphoned: Int): AttributeEntry {
        val costs = levelling[def.rarity.uppercase()] ?: return AttributeEntry(def, syphoned, 0, syphoned, 0)
        var acc = 0
        var level = 0
        for (i in costs.indices) {
            if (syphoned >= acc + costs[i]) {
                acc += costs[i]; level = i + 1
            } else {
                return AttributeEntry(def, syphoned, level, syphoned - acc, costs[i])
            }
        }
        return AttributeEntry(def, syphoned, level, 0, 0)
    }

    /** A hunting-box item for a shard [key]: its head (fresh copy) with owned [amount] as the stack size. */
    fun boxStack(key: String, amount: Int): ItemStack {
        val def = byShard(key)
        val base = if (def != null) icon(def) else SkullItems.vanilla("prismarine_shard")
        val stack = base.copy().apply { count = amount }
        val fmt = RARITY_FMT[def?.rarity?.uppercase()] ?: ChatFormatting.WHITE
        val name = def?.shardName ?: key.lowercase().split('_').joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        stack.set(DataComponents.CUSTOM_NAME, McText.literal(name).withStyle(fmt))
        val sub = def?.let { "${it.rarity.lowercase().replaceFirstChar { c -> c.uppercase() }} ${it.category} Shard" } ?: "Shard"
        stack.set(
            DataComponents.LORE,
            ItemLore(
                listOf(
                    McText.literal(sub).withStyle(ChatFormatting.GRAY),
                    McText.literal("Owned: ").withStyle(ChatFormatting.GRAY)
                        .append(McText.literal("$amount").withStyle(ChatFormatting.WHITE)),
                ),
            ),
        )
        return stack
    }
}
