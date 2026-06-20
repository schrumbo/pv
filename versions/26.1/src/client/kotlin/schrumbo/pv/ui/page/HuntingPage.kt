package schrumbo.pv.ui.page

import net.minecraft.client.Minecraft
import schrumbo.pv.data.AttributeEntry
import schrumbo.pv.data.AttributeRegistry
import schrumbo.pv.data.SkyblockProfile
import schrumbo.pv.ui.Theme
import schrumbo.pv.ui.component.Column
import schrumbo.pv.ui.component.Component
import schrumbo.pv.ui.component.Item
import schrumbo.pv.ui.component.ProgressBar
import schrumbo.pv.ui.component.Row
import schrumbo.pv.ui.component.SpaceBetween
import schrumbo.pv.ui.component.Text
import schrumbo.pv.ui.component.Tooltip
import schrumbo.pv.ui.component.VAlign

/**
 * Hunting page: the player's hunting attributes (level 0..10, syphoned shards → level via the rarity
 * cost table). Empty search shows unlocked attributes; searching matches the attribute name, the
 * shard/mob name, the alignment and the rarity. The raw caught shards live in the Storage Hunting Box.
 */
object HuntingPage {

    private const val COL_GAP = 14

    private val RARITY = mapOf(
        "COMMON" to 0xFFFFFFFF.toInt(), "UNCOMMON" to 0xFF55FF55.toInt(), "RARE" to 0xFF5599FF.toInt(),
        "EPIC" to 0xFFAA44FF.toInt(), "LEGENDARY" to 0xFFFFB534.toInt(),
    )
    private val RANK = listOf("COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY")

    fun header(p: SkyblockProfile, width: Int): Component {
        val unlocked = p.attributes.attributeStacks.count { it.value > 0 }
        return PageKit.skillHeader(p, schrumbo.pv.data.SkillType.HUNTING, width, "$unlocked / ${AttributeRegistry.attributes.size} attributes unlocked")
    }

    /** Empty [query] → unlocked attributes; otherwise search every attribute by name/shard/mob/rarity. */
    fun grid(p: SkyblockProfile, query: String, width: Int): Component {
        val stacks = p.attributes.attributeStacks
        val q = query.trim().lowercase()
        val base = if (q.isEmpty()) {
            AttributeRegistry.attributes.filter { (stacks[it.id.uppercase()] ?: 0) > 0 }
        } else {
            AttributeRegistry.attributes.filter {
                it.name.lowercase().contains(q) || it.shardName.lowercase().contains(q) ||
                    it.category.lowercase().contains(q) || it.rarity.lowercase().contains(q)
            }
        }
        val entries = base
            .map { AttributeRegistry.resolve(it, stacks[it.id.uppercase()] ?: 0) }
            .sortedWith(
                compareByDescending<AttributeEntry> { it.syphoned > 0 }
                    .thenByDescending { it.level }
                    .thenByDescending { RANK.indexOf(it.def.rarity.uppercase()) },
            )
        if (entries.isEmpty()) return Text(if (q.isEmpty()) "No attributes unlocked — type to search all" else "No matching attributes", Theme.TEXT_MUTED)
        val cols = (width / 185).coerceIn(2, 4)
        val cellW = (width - COL_GAP * (cols - 1)) / cols
        val cells = entries.map { cell(it, cellW) }
        val per = (cells.size + cols - 1) / cols
        return Row((0 until cols).map { c -> Column(cells.drop(c * per).take(per), spacing = 5) }, spacing = COL_GAP, align = VAlign.TOP)
    }

    private fun cell(e: AttributeEntry, cellW: Int): Component {
        val rarityColor = RARITY[e.def.rarity.uppercase()] ?: Theme.TEXT
        val levelColor = if (e.maxed) Theme.GOLD else Theme.ACCENT
        val innerW = cellW - 16 - 5
        val body = Column(
            SpaceBetween(innerW, Text(clip(e.def.name, innerW - 30), rarityColor), Text("${e.level}/10", levelColor)),
            ProgressBar(innerW, 3, e.progress, if (e.maxed) Theme.GOLD else Theme.ACCENT, Theme.SURFACE_ALT),
            spacing = 2,
        )
        val cell = Row(Item(AttributeRegistry.icon(e.def), 16, tooltip = false), body, spacing = 5, align = VAlign.CENTER)
        val needed = if (e.maxed) "§6Maxed" else "§7Need §f${e.needed - e.into}§7 more ${e.def.shardName} to level up"
        return Tooltip(
            cell,
            listOf(
                "${rarityHex(e.def.rarity)}${e.def.name}",
                "§7${e.def.rarity.lowercase().replaceFirstChar { it.uppercase() }} · ${e.def.category}",
                "§7Shard: §f${e.def.shardName}",
                "§7Level §f${e.level}§7/§f10",
                needed,
            ),
        )
    }

    private fun rarityHex(r: String): String = when (r.uppercase()) {
        "UNCOMMON" -> "§a"; "RARE" -> "§9"; "EPIC" -> "§5"; "LEGENDARY" -> "§6"; else -> "§f"
    }

    private fun clip(s: String, maxW: Int): String {
        val font = Minecraft.getInstance().font
        if (font.width(s) <= maxW) return s
        var t = s
        while (t.isNotEmpty() && font.width("$t…") > maxW) t = t.dropLast(1)
        return "$t…"
    }
}
