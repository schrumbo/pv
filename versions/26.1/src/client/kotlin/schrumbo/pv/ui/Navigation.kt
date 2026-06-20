package schrumbo.pv.ui

import schrumbo.pv.data.SkillType

/**
 * Top-level pages, in tab order, each shown as an item icon in the tab bar. All pages are populated.
 * Skills are their own categories (Enchanting deliberately has none — it stays only in the General
 * skill grid).
 */
enum class Page(val title: String, val icon: String) {
    GENERAL("General", "paper"),
    MINING("Mining", "netherite_pickaxe"),
    FORAGING("Foraging", "netherite_axe"),
    FARMING("Farming", "diamond_hoe"),
    FISHING("Fishing", "fishing_rod"),
    HUNTING("Hunting", "lead"),
    BESTIARY("Bestiary", "zombie_head"),
    CATACOMBS("Catacombs", "wither_skeleton_skull"),
    RIFT("Rift", "ender_eye"),
    INVENTORY("Loadout", "shulker_box"),
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
