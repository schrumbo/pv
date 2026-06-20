package schrumbo.pv.ui.page

import schrumbo.pv.data.FishingRegistry
import schrumbo.pv.data.SkillType
import schrumbo.pv.data.SkyblockProfile
import schrumbo.pv.data.TrophyData
import schrumbo.pv.data.TrophyFish
import schrumbo.pv.ui.Theme
import schrumbo.pv.ui.component.Column
import schrumbo.pv.ui.component.Component
import schrumbo.pv.ui.component.Frame
import schrumbo.pv.ui.component.HAlign
import schrumbo.pv.ui.component.Item
import schrumbo.pv.ui.component.Row
import schrumbo.pv.ui.component.Spacer
import schrumbo.pv.ui.component.Text
import schrumbo.pv.ui.component.Tooltip
import schrumbo.pv.ui.component.VAlign
import schrumbo.pv.util.Format

/** Fishing page: trophy-fish heads (per tier, count as stack size) and fishing stats. */
object FishingPage {

    private const val DIAMOND = 0xFF4DD0E1.toInt()
    private const val GOLD = 0xFFFFB534.toInt()
    private const val SILVER = 0xFFD8D8D8.toInt()
    private const val BRONZE = 0xFFCD7F32.toInt()

    /** Tier rows, top (diamond) to bottom (bronze). */
    private val TIERS = listOf("diamond" to DIAMOND, "gold" to GOLD, "silver" to SILVER, "bronze" to BRONZE)

    fun build(p: SkyblockProfile, width: Int): Component {
        val t = p.trophy
        val fishBlock = fish(t)
        return Column(
            PageKit.skillHeader(p, SkillType.FISHING, width),
            Frame(width, fishBlock.height, fishBlock, hAlign = HAlign.CENTER, vAlign = VAlign.TOP),
            stats(t, width),
            spacing = 8,
        )
    }

    /** One column per fish; four heads bottom-up (bronze → diamond), catch count as the stack size. */
    private fun fish(t: TrophyData): Component {
        if (t.fish.isEmpty()) return Text("No trophy fish caught", Theme.TEXT_MUTED)
        // Leading spacer: the per-tier count is drawn left of each head and would otherwise be
        // scissor-clipped on the first column when it overflows the icon (big six/seven-digit counts).
        return Row(listOf(Spacer(6, 0)) + t.fish.map { fishColumn(it) }, spacing = 3)
    }

    private fun fishColumn(f: TrophyFish): Component {
        val heads = TIERS.map { (tier, _) ->
            val count = when (tier) {
                "diamond" -> f.diamond; "gold" -> f.gold; "silver" -> f.silver; else -> f.bronze
            }
            val head = FishingRegistry.head(f.key, tier)
            if (head.isEmpty) return@map Spacer(18, 18)
            val icon = Item(head, 18, tooltip = false, corner = if (count > 0) Format.compact(count) else null)
            Tooltip(icon, listOf("§f${f.name}", "§7${tier.replaceFirstChar { it.uppercase() }}: §f${Format.compact(count)}", "§7Total: §f${Format.compact(f.total)}"))
        }
        return Column(heads, spacing = 2)
    }

    private fun stats(t: TrophyData, width: Int): Component = Column(
        PageKit.tileRow(
            width,
            listOf(
                "Items Fished" to (Format.compact(t.itemsFished) to Theme.ACCENT),
                "Treasure" to (Format.compact(t.treasure) to Theme.GOLD),
                "Large Treasure" to (Format.compact(t.largeTreasure) to Theme.GOLD),
            ),
        ),
        PageKit.tileRow(
            width,
            listOf(
                "Sea Creatures" to (Format.compact(t.seaCreatures) to Theme.GREEN),
                "Dolphin Milestone" to (dolphinTier(t.seaCreatures) to DIAMOND),
                "Trophy Caught" to (Format.compact(t.totalCaught) to Theme.TEXT),
            ),
        ),
        spacing = 8,
    )

    /** Dolphin pet rarity milestone from total sea-creature kills. */
    private fun dolphinTier(kills: Long): String = when {
        kills >= 100_000 -> "Legendary"
        kills >= 25_000 -> "Epic"
        kills >= 5_000 -> "Rare"
        kills >= 1_000 -> "Uncommon"
        kills >= 250 -> "Common"
        else -> "—"
    }
}
