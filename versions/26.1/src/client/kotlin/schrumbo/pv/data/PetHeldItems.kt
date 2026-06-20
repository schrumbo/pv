package schrumbo.pv.data

/** Resolves a pet held-item id into a readable name and its rarity colour code (§). */
object PetHeldItems {

    data class Held(val name: String, val rarityCode: String)

    private val RARITY = listOf("COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC")

    /** Names that don't follow the `<skill>_SKILL_BOOST` / prettify rules. */
    private val NAMES = mapOf(
        "TIER_BOOST" to "Tier Boost",
        "TEXTBOOK" to "Textbook",
        "QUICK_CLAW" to "Quick Claw",
        "SHARPENED_CLAWS" to "Sharpened Claws",
        "HARDENED_SCALES" to "Hardened Scales",
        "BIG_TEETH" to "Big Teeth",
        "LUCKY_CLOVER" to "Lucky Clover",
        "IRON_CLAWS" to "Iron Claws",
        "GOLD_CLAWS" to "Gold Claws",
        "WASHED_UP_SOUVENIR" to "Washed-up Souvenir",
        "DWARF_TURTLE_SHELMET" to "Dwarf Turtle Shelmet",
        "MINOS_RELIC" to "Minos Relic",
        "GREEN_BANDANA" to "Green Bandana",
        "ALL_SKILLS_SUPER_BOOST" to "All Skills Super Boost",
        "EXP_SHARE" to "Exp Share",
    )

    fun resolve(id: String): Held {
        var key = id.removePrefix("PET_ITEM_")
        val rarity = RARITY.firstOrNull { key.endsWith("_$it") }
        if (rarity != null) key = key.removeSuffix("_$rarity")
        val name = NAMES[key] ?: ruleName(key)
        return Held(name, code(rarity))
    }

    private fun ruleName(key: String): String = when {
        key.endsWith("_SKILL_BOOST") -> prettify(key.removeSuffix("_SKILL_BOOST")) + " Exp Boost"
        else -> prettify(key)
    }

    private fun code(rarity: String?): String = when (rarity) {
        "UNCOMMON" -> "§a"; "RARE" -> "§9"; "EPIC" -> "§5"; "LEGENDARY" -> "§6"; "MYTHIC" -> "§d"; else -> "§f"
    }

    private fun prettify(s: String): String =
        s.split('_').joinToString(" ") { it.lowercase().replaceFirstChar { c -> c.uppercase() } }
}
