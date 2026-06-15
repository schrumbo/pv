package schrumbo.pv.ui

import schrumbo.pv.data.SkillType

/** Top-level pages, in tab order. Only [GENERAL] is populated for now; the rest are placeholders. */
enum class Page(val title: String) {
    GENERAL("General"),
    SKILLS("Skills"),
    INVENTORY("Inventory"),
    PETS("Pets"),
    DUNGEONS("Dungeons"),
    COLLECTIONS("Collections"),
    BESTIARY("Bestiary"),
    RIFT("Rift"),
    CRIMSON_ISLE("Crimson Isle"),
    MISC("Misc"),
}

/** Sub-pages of the Skills page, each backed by a skill the General page can deep-link into. */
enum class SkillTab(val title: String, val skill: SkillType) {
    MINING("Mining", SkillType.MINING),
    FORAGING("Foraging", SkillType.FORAGING),
    FARMING("Farming", SkillType.FARMING),
    FISHING("Fishing", SkillType.FISHING),
    ENCHANTING("Enchanting", SkillType.ENCHANTING),
    HUNTING("Hunting", SkillType.HUNTING);

    companion object {
        fun forSkill(skill: SkillType): SkillTab? = entries.firstOrNull { it.skill == skill }
    }
}
