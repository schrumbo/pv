package schrumbo.pv.ui

import schrumbo.pv.data.SkillType

/** Catacombs / Dungeons entrance head (meowdding-repo `pv/skull_textures.dungeons`). */
private const val DUNGEON_HEAD =
    "ewogICJ0aW1lc3RhbXAiIDogMTYwMjAzOTQ4MTEzMywKICAicHJvZmlsZUlkIiA6ICJjNTBhZmE4YWJlYjk0ZTQ1OTRiZjFiNDI1YTk4MGYwMiIsCiAgInByb2ZpbGVOYW1lIiA6ICJUd29FQmFlIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzE3YjhlZjdlOGUwYWYxOTM5OWNjMzI0ODVmMmVlNDdiMmJiNGQ1ODAyYzcxYjMwMjFmNGQ1ZDZiNWYzYWZlMDMiCiAgICB9CiAgfQp9"

/**
 * Top-level pages, in tab order, each shown as an item icon in the tab bar. All pages are populated.
 * Skills are their own categories (Enchanting deliberately has none — it stays only in the General
 * skill grid).
 */
enum class Page(val title: String, val icon: String, val skullTexture: String? = null) {
    GENERAL("General", "paper"),
    MINING("Mining", "stone_pickaxe"),
    FORAGING("Foraging", "jungle_sapling"),
    FARMING("Farming", "golden_hoe"),
    FISHING("Fishing", "fishing_rod"),
    HUNTING("Hunting", "lead"),
    COMBAT("Combat", "stone_sword"),
    CATACOMBS("Catacombs", "wither_skeleton_skull", DUNGEON_HEAD),
    RIFT("Rift", "ender_eye"),
    INVENTORY("Loadout", "chest"),
    COLLECTIONS("Collections", "item_frame"),
    PETS("Pets", "bone");

    companion object {
        /** The dedicated page for a skill, or null for skills that don't get their own category. */
        fun forSkill(skill: SkillType): Page? = when (skill) {
            SkillType.MINING -> MINING
            SkillType.FORAGING -> FORAGING
            SkillType.FARMING -> FARMING
            SkillType.FISHING -> FISHING
            SkillType.HUNTING -> HUNTING
            else -> null
        }
    }
}
