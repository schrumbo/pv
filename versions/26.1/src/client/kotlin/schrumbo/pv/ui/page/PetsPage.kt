package schrumbo.pv.ui.page

import schrumbo.pv.data.PetEntry
import schrumbo.pv.data.PetHeldItems
import schrumbo.pv.data.PetRegistry
import schrumbo.pv.data.SkyblockProfile
import schrumbo.pv.ui.Theme
import schrumbo.pv.ui.component.Column
import schrumbo.pv.ui.component.Component
import schrumbo.pv.ui.component.Frame
import schrumbo.pv.ui.component.HAlign
import schrumbo.pv.ui.component.Item
import schrumbo.pv.ui.component.Overlay
import schrumbo.pv.ui.component.Row
import schrumbo.pv.ui.component.Text
import schrumbo.pv.ui.component.Tooltip
import schrumbo.pv.ui.component.VAlign
import schrumbo.pv.util.Format

/** Pets page: every pet as its skin/skull with held item, level shown as a stack-size count. */
object PetsPage {

    private val TIER_COLORS = mapOf(
        "COMMON" to 0xFFFFFFFF.toInt(), "UNCOMMON" to 0xFF55FF55.toInt(), "RARE" to 0xFF5599FF.toInt(),
        "EPIC" to 0xFFAA44FF.toInt(), "LEGENDARY" to 0xFFFFB534.toInt(), "MYTHIC" to 0xFFFF77DD.toInt(),
        "DIVINE" to 0xFF44DDEE.toInt(),
    )
    private val TIER_RANK = listOf("COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC", "DIVINE")

    fun build(p: SkyblockProfile, width: Int): Component {
        val sorted = p.pets.sortedWith(
            compareByDescending<PetEntry> { it.active }
                .thenByDescending { TIER_RANK.indexOf(it.tier) }
                .thenByDescending { PetRegistry.level(it).level },
        )
        val totalXp = p.pets.sumOf { it.exp }.toLong()
        return Column(
            PageKit.pageHeader("Pets", "${p.pets.size} pets    ${petScore(p.pets)} pet score    ${Format.compact(totalXp)} total xp", width),
            grid(sorted, width),
            spacing = 8,
        )
    }

    /** Pet score: per unique species, points equal to the rank of its highest owned rarity. */
    private fun petScore(pets: List<PetEntry>): Int {
        val best = HashMap<String, Int>()
        for (pet in pets) {
            val rank = TIER_RANK.indexOf(pet.tier) + 1
            best[pet.type] = maxOf(best[pet.type] ?: 0, rank)
        }
        return best.values.sum()
    }

    private const val ICON = 26
    private const val GAP = 6

    /** Icon-only grid: each pet is just its skull with the level in the corner; details live in the
     *  tooltip. The active pet gets a green-bordered tile. Columns fill the available width. */
    private fun grid(pets: List<PetEntry>, width: Int): Component {
        if (pets.isEmpty()) return Text("No pets", Theme.TEXT_MUTED)
        val cols = ((width + GAP) / (ICON + GAP)).coerceAtLeast(1)
        val cells = pets.map { petCell(it) }
        return Column(cells.chunked(cols).map { Row(it, spacing = GAP) }, spacing = GAP)
    }

    private fun petCell(pet: PetEntry): Component {
        val lvl = PetRegistry.level(pet)
        val base = Item(PetRegistry.iconFor(pet), ICON - 2, tooltip = false, corner = lvl.level.toString())
        // A mini render of the held item over the top-right corner of the pet (incl. custom skulls).
        val held = PetRegistry.heldIcon(pet.heldItem)
        val icon: Component = if (!held.isEmpty) Overlay(base, Item(held, 9, tooltip = false)) else base

        val tierColor = TIER_COLORS[pet.tier] ?: Theme.TEXT
        val bg = (tierColor and 0x00FFFFFF) or 0x33000000
        val border = if (pet.active) Theme.GREEN else tierColor
        val cell = Frame(ICON, ICON, icon, bg, border, HAlign.CENTER, VAlign.CENTER)

        val name = PetRegistry.displayName(pet.type)
        val heldLine = pet.heldItem?.let {
            val h = PetHeldItems.resolve(it)
            "§7Held: ${h.rarityCode}${h.name}"
        }
        return Tooltip(
            cell,
            listOfNotNull(
                "${tierHex(pet.tier)}$name",
                "§7${pet.tier.lowercase().replaceFirstChar { it.uppercase() }}",
                "§7Level §f${lvl.level}§7/§f${lvl.maxLevel}  §8(${Format.compact(pet.exp.toLong())} xp)",
                pet.skin?.let { "§dSkin: §f" + it.split('_').joinToString(" ") { w -> w.replaceFirstChar { c -> c.uppercase() } } },
                heldLine,
            ),
        )
    }

    private fun tierHex(tier: String): String = when (tier) {
        "UNCOMMON" -> "§a"; "RARE" -> "§9"; "EPIC" -> "§5"; "LEGENDARY" -> "§6"
        "MYTHIC" -> "§d"; "DIVINE" -> "§b"; else -> "§f"
    }
}
