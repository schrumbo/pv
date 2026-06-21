package schrumbo.pv.data

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import schrumbo.pv.util.Eval
import kotlin.math.floor

/** One Heart-of-the-Mountain / Heart-of-the-Forest tree node (from the meowdding perk repo). */
data class Perk(
    val type: String,
    val id: String?,
    val x: Int,
    val y: Int,
    val max: Int?,
    val costKind: String?,
    val cost: String?,
    /** Placeholder name → formula (single-reward perks use the key `reward`; multi-reward use named keys). */
    val reward: Map<String, String>,
    val name: String?,
    val tooltip: List<String>,
)

/** Loads `perks.json` and builds in-game-style node tooltips (effect at level, level, powder cost). */
object PerkRegistry {

    val hotm: List<Perk>
    val hotf: List<Perk>

    init {
        val stream = javaClass.getResourceAsStream("/assets/pv/perks.json") ?: error("missing perks.json")
        val root = stream.reader().use { JsonParser.parseReader(it).asJsonObject }
        hotm = root.getAsJsonArray("hotm").map { perk(it.asJsonObject) }
        hotf = root.getAsJsonArray("hotf").map { perk(it.asJsonObject) }
    }

    private fun perk(o: JsonObject): Perk = Perk(
        type = o.get("type").asString,
        id = o.get("id")?.asString,
        x = o.get("x").asInt,
        y = o.get("y").asInt,
        max = o.get("max_level")?.asInt,
        costKind = o.get("costKind")?.asString,
        cost = o.get("cost")?.asString,
        reward = o.get("reward")?.asJsonObject?.entrySet()?.associate { it.key to it.value.asString } ?: emptyMap(),
        name = o.get("name")?.asString,
        tooltip = o.get("tooltip")?.asJsonArray?.map { it.asString } ?: emptyList(),
    )

    private val POWDER_COLOR = mapOf("MITHRIL" to "§2", "GEMSTONE" to "§d", "GLACITE" to "§b")

    /** Node tooltip in the in-game style: name, level, effect, powder spent, enabled state. */
    fun tooltip(perk: Perk, level: Int, treeLevel: Int): List<String> {
        if (perk.type == "SPACER") return emptyList()
        if (perk.type == "TIER") return tierTooltip(perk, treeLevel)

        val out = mutableListOf<String>()
        val locked = level < 0
        val maxed = perk.max != null && level >= perk.max
        out += (if (locked) "§c" else "§a") + (perk.name ?: "")

        if (perk.max != null) {
            out += if (maxed) "§7Level §f$level" else "§7Level §f${maxOf(level, 0)}§7/§8${perk.max}"
            out += ""
        }

        val effectLevel = if (level > 0) level else 1
        val vars = mapOf(
            "level" to effectLevel.toDouble(),
            "effectiveLevel" to effectLevel.toDouble(),
            "hotmLevel" to treeLevel.toDouble(),
        )
        perk.tooltip.forEach { line ->
            var l = line
            perk.reward.forEach { (k, formula) -> l = l.replace("%$k%", fmt(Eval.eval(formula, vars))) }
            out += tags(l)
        }

        when {
            perk.type == "CORE" -> {}
            locked -> { out += ""; out += "§c§lLOCKED" }
            perk.type == "ABILITY" -> { out += ""; out += "§a§lUNLOCKED" }
            else -> {
                out += ""
                if (perk.max != null && perk.cost != null && perk.costKind != null) out += powderSpent(perk, level)
                out += "§a§lENABLED"
            }
        }
        return out
    }

    private fun tierTooltip(perk: Perk, treeLevel: Int): List<String> {
        val n = tierNumber(perk.name)
        val unlocked = treeLevel >= n
        val unlocking = !unlocked && treeLevel + 1 >= n
        val color = if (unlocked) "§a" else if (unlocking) "§e" else "§c"
        val state = if (unlocked) "§a§lUNLOCKED" else if (unlocking) "§e§lUNLOCKING" else "§c§lRequires Tier $n"
        return listOf("$color${perk.name ?: "Tier $n"}", "", state)
    }

    private fun powderSpent(perk: Perk, level: Int): List<String> {
        val kind = perk.costKind ?: return emptyList()
        val formula = perk.cost ?: return emptyList()
        fun sum(to: Int) = (1..to).sumOf { floor(Eval.eval(formula, mapOf("level" to it.toDouble()))).toLong() }
        val spent = sum(level)
        val required = sum(perk.max ?: level)
        val name = kind.lowercase().replaceFirstChar { it.uppercase() }
        val pct = if (required > 0 && spent < required) " §e(${spent * 100 / required}%)" else ""
        return listOf(
            "§7Powder Spent",
            "${POWDER_COLOR[kind] ?: "§7"}$name§7: §f${"%,d".format(spent)}§7/§8${"%,d".format(required)}$pct",
        )
    }

    fun tierNumber(name: String?): Int = name?.filter { it.isDigit() }?.toIntOrNull() ?: 0

    private fun fmt(v: Double): String =
        if (v == floor(v)) "%,d".format(v.toLong()) else "%.1f".format(v)

    private val TAGS = mapOf(
        "black" to "§0", "dark_blue" to "§1", "dark_green" to "§2", "dark_aqua" to "§3", "dark_red" to "§4",
        "dark_purple" to "§5", "gold" to "§6", "gray" to "§7", "dark_gray" to "§8", "blue" to "§9",
        "green" to "§a", "aqua" to "§b", "red" to "§c", "light_purple" to "§d", "purple" to "§d",
        "yellow" to "§e", "white" to "§f", "bold" to "§l", "b" to "§l", "italic" to "§o", "i" to "§o",
        "underline" to "§n", "strikethrough" to "§m", "obfuscated" to "§k", "reset" to "§r",
    )
    private val TAG_RE = Regex("</?([a-z_]+)>")

    /** Converts MiniMessage-style colour tags (`<gray>`, `<aqua>`) into legacy § codes. */
    private fun tags(s: String): String = TAG_RE.replace(s) { m -> TAGS[m.groupValues[1]] ?: "" }
}
