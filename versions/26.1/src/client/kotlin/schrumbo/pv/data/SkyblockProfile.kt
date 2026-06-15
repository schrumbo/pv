package schrumbo.pv.data

import net.minecraft.world.item.ItemStack
import schrumbo.pv.util.Leveling

/**
 * The eight skills that make up the (overflow) skill average, plus the cosmetic skills.
 * [cap] is the base level cap; [inAverage] marks the skills counted toward the average.
 * [apiKey] is the field name under `player_data.experience`.
 */
enum class SkillType(
    val display: String,
    val apiKey: String,
    val cap: Int,
    val inAverage: Boolean,
    val icon: String,
) {
    FARMING("Farming", "SKILL_FARMING", 60, true, "golden_hoe"),
    MINING("Mining", "SKILL_MINING", 60, true, "stone_pickaxe"),
    COMBAT("Combat", "SKILL_COMBAT", 60, true, "stone_sword"),
    FORAGING("Foraging", "SKILL_FORAGING", 54, true, "jungle_sapling"),
    FISHING("Fishing", "SKILL_FISHING", 50, true, "fishing_rod"),
    ENCHANTING("Enchanting", "SKILL_ENCHANTING", 60, true, "enchanting_table"),
    ALCHEMY("Alchemy", "SKILL_ALCHEMY", 50, true, "brewing_stand"),
    TAMING("Taming", "SKILL_TAMING", 60, true, "wolf_spawn_egg"),
    CARPENTRY("Carpentry", "SKILL_CARPENTRY", 50, false, "crafting_table"),
    RUNECRAFTING("Runecrafting", "SKILL_RUNECRAFTING", 25, false, "magma_cream"),
    SOCIAL("Social", "SKILL_SOCIAL", 25, false, "emerald"),
    HUNTING("Hunting", "SKILL_HUNTING", 50, true, "lead"),
}

/** The six slayer bosses, keyed by their `slayer_bosses` field name, with an icon item. */
enum class SlayerType(val display: String, val apiKey: String, val icon: String) {
    ZOMBIE("Revenant", "zombie", "rotten_flesh"),
    SPIDER("Tarantula", "spider", "cobweb"),
    WOLF("Sven", "wolf", "mutton"),
    ENDERMAN("Voidgloom", "enderman", "ender_pearl"),
    BLAZE("Inferno", "blaze", "blaze_powder"),
    VAMPIRE("Riftstalker", "vampire", "redstone"),
}

/** A resolved skill: its type plus the computed (overflow) level. */
data class SkillEntry(val type: SkillType, val level: Leveling.Level)

/**
 * A resolved slayer: its type, computed level, and total boss kills per tier.
 * [tierKills] is ordered T1..T5 (index 0 = tier 1); a boss without a tier reports 0 there.
 */
data class SlayerEntry(val type: SlayerType, val level: Leveling.Level, val tierKills: List<Long>)

/**
 * A single Skyblock profile for one player: cosmetic name, game mode, Skyblock level,
 * Catacombs level, all skills with the overflow skill average, and slayers.
 */
data class SkyblockProfile(
    val cuteName: String,
    val gameMode: String?,
    val skyblockLevel: Leveling.Level,
    val catacombs: Leveling.Level,
    val skills: List<SkillEntry>,
    val skillAverage: Double,
    val slayers: List<SlayerEntry>,
    val armor: List<ItemStack>,
)
