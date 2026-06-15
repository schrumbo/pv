package schrumbo.pv.ui.page

import schrumbo.pv.data.PetEntry
import schrumbo.pv.data.PetRegistry
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
import schrumbo.pv.util.Format

/** Pets page: every owned pet as its real skull, rarity-coloured, with a level bar; active pet first. */
object PetsPage {

    private val TIER_COLORS = mapOf(
        "COMMON" to 0xFFFFFFFF.toInt(),
        "UNCOMMON" to 0xFF55FF55.toInt(),
        "RARE" to 0xFF5599FF.toInt(),
        "EPIC" to 0xFFAA44FF.toInt(),
        "LEGENDARY" to 0xFFFFB534.toInt(),
        "MYTHIC" to 0xFFFF77DD.toInt(),
        "DIVINE" to 0xFF44DDEE.toInt(),
    )

    private val TIER_RANK = listOf("COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC", "DIVINE")

    fun build(p: SkyblockProfile, width: Int): Component {
        val active = p.pets.firstOrNull { it.active }
        val activeName = active?.let { PetRegistry.displayName(it.type) } ?: "none"
        val sorted = p.pets.sortedWith(
            compareByDescending<PetEntry> { it.active }
                .thenByDescending { TIER_RANK.indexOf(it.tier) }
                .thenByDescending { PetRegistry.level(it).level },
        )
        return Column(
            PageKit.pageHeader("Pets", "· ${p.pets.size} pets · active: $activeName", width),
            grid(sorted, width),
            spacing = 8,
        )
    }

    private fun grid(pets: List<PetEntry>, width: Int): Component {
        if (pets.isEmpty()) return Text("No pets", Theme.TEXT_MUTED)
        val cellW = PageKit.cellW(width, 3)
        return PageKit.grid(pets.map { petCell(it, cellW) }, width, cols = 3, rowGap = 6)
    }

    private fun petCell(pet: PetEntry, cellW: Int): Component {
        val lvl = PetRegistry.level(pet)
        val tierColor = TIER_COLORS[pet.tier] ?: Theme.TEXT
        val innerW = cellW - 18 - 5
        val name = PetRegistry.displayName(pet.type)
        val levelLabel = if (lvl.maxed) "Lv ${lvl.level} ✦" else "Lv ${lvl.level}"
        val body = Column(
            SpaceBetween(
                innerW,
                Text(PageKit.clip((if (pet.active) "▶ " else "") + name, innerW - 42), if (pet.active) Theme.GREEN else tierColor),
                Text(levelLabel, if (lvl.maxed) Theme.GOLD else Theme.TEXT_MUTED),
            ),
            ProgressBar(innerW, 3, lvl.progress, tierColor, Theme.SURFACE_ALT),
            spacing = 2,
        )
        val cell = Row(Item(PetRegistry.icon(pet.type), 18, tooltip = false), body, spacing = 5, align = VAlign.CENTER)
        val held = pet.heldItem?.let {
            "§7Held: §f" + it.removePrefix("PET_ITEM_").split('_').joinToString(" ") { w -> w.replaceFirstChar { c -> c.uppercase() } }
        }
        return Tooltip(
            cell,
            listOfNotNull(
                "${tierHex(pet.tier)}$name",
                "§7${pet.tier.lowercase().replaceFirstChar { it.uppercase() }}",
                "§7Level §f${lvl.level}§7/§f${lvl.maxLevel}  §8(${Format.compact(pet.exp.toLong())} xp)",
                held,
            ),
        )
    }

    private fun tierHex(tier: String): String = when (tier) {
        "UNCOMMON" -> "§a"
        "RARE" -> "§9"
        "EPIC" -> "§5"
        "LEGENDARY" -> "§6"
        "MYTHIC" -> "§d"
        "DIVINE" -> "§b"
        else -> "§f"
    }
}
