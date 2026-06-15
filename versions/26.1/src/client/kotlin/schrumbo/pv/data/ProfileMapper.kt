package schrumbo.pv.data

import com.google.gson.JsonObject
import net.minecraft.world.item.ItemStack
import schrumbo.pv.util.Leveling

/** Maps the raw `/v2/skyblock/profiles` payload into [SkyblockProfile]s. */
object ProfileMapper {

    /** All profiles the player is a member of, plus the index of the selected one (or 0). */
    fun mapAll(profilesResponse: JsonObject, uuid: String): Pair<List<SkyblockProfile>, Int> {
        val profiles = profilesResponse.array("profiles") ?: return emptyList<SkyblockProfile>() to 0
        val members = profiles.map { it.asJsonObject }.filter { it.obj("members")?.has(uuid) == true }
        if (members.isEmpty()) return emptyList<SkyblockProfile>() to 0

        val selected = members.indexOfFirst { it.bool("selected") }.coerceAtLeast(0)
        val mapped = members.mapNotNull { mapOne(it, uuid) }
        return mapped to selected.coerceIn(0, (mapped.size - 1).coerceAtLeast(0))
    }

    private fun mapOne(profile: JsonObject, uuid: String): SkyblockProfile? {
        val member = profile.obj("members")?.obj(uuid) ?: return null
        val skills = skills(member)
        val dungeons = dungeons(member)
        return SkyblockProfile(
            cuteName = profile.str("cute_name") ?: "?",
            gameMode = profile.str("game_mode"),
            skyblockLevel = skyblockLevel(member),
            catacombs = dungeons.catacombs,
            dungeons = dungeons,
            skills = skills,
            skillAverage = average(skills),
            slayers = slayers(member),
            armor = armor(member),
            bank = profile.obj("banking")?.num("balance")?.toLong() ?: 0L,
            purse = (member.obj("currencies")?.num("coin_purse") ?: member.num("coin_purse"))?.toLong() ?: 0L,
            firstJoin = member.obj("profile")?.num("first_join")?.toLong() ?: 0L,
            fairySouls = (member.obj("fairy_soul")?.num("total_collected") ?: member.num("fairy_souls_collected"))?.toInt() ?: 0,
        )
    }

    private fun dungeons(member: JsonObject): DungeonData {
        val dj = member.obj("dungeons")
        val types = dj?.obj("dungeon_types")
        val cata = types?.obj("catacombs")
        val master = types?.obj("master_catacombs")
        val catacombs = Leveling.skill(cata?.num("experience")?.toLong() ?: 0L, Leveling.CATACOMBS_XP, overflow = false)

        val classesJson = dj?.obj("player_classes")
        val classes = listOf(
            "healer" to "Healer", "mage" to "Mage", "berserk" to "Berserk", "archer" to "Archer", "tank" to "Tank",
        ).map { (key, display) ->
            val xp = classesJson?.obj(key)?.num("experience")?.toLong() ?: 0L
            DungeonClass(display, Leveling.skill(xp, Leveling.CATACOMBS_XP, overflow = false))
        }
        val classAverage = if (classes.isEmpty()) 0.0 else classes.sumOf { it.level.fractional } / classes.size

        val normalTiers = cata?.obj("tier_completions")
        val masterTiers = master?.obj("tier_completions")
        val floors = (0..7).map { f ->
            FloorStat(f, normalTiers?.num(f.toString())?.toLong() ?: 0L, masterTiers?.num(f.toString())?.toLong() ?: 0L)
        }
        return DungeonData(catacombs, classes, classAverage, dj?.str("selected_dungeon_class"), floors)
    }

    private fun armor(member: JsonObject): List<ItemStack> {
        val data = member.obj("inventory")?.obj("inv_armor")?.str("data")
            ?: member.obj("inv_armor")?.str("data")
            ?: return emptyList()
        return InventoryDecoder.decode(data)
    }

    private fun skyblockLevel(member: JsonObject): Leveling.Level {
        val xp = member.obj("leveling")?.num("experience")?.toLong() ?: 0L
        val level = (xp / 100).toInt()
        val into = (xp % 100)
        return Leveling.Level(level, xp / 100.0, xp, Int.MAX_VALUE, false, into / 100.0, 100 - into)
    }

    private fun skills(member: JsonObject): List<SkillEntry> {
        val exp = member.obj("player_data")?.obj("experience") ?: member.obj("experience")
        return SkillType.entries.map { type ->
            val xp = exp?.num(type.apiKey)?.toLong() ?: 0L
            val runecrafting = type == SkillType.RUNECRAFTING
            val table = if (runecrafting) Leveling.RUNECRAFTING_XP else Leveling.SKILL_XP
            val level = Leveling.skill(xp, table, overflow = !runecrafting && type != SkillType.SOCIAL)
            SkillEntry(type, level)
        }
    }

    private fun slayers(member: JsonObject): List<SlayerEntry> {
        val bosses = member.obj("slayer")?.obj("slayer_bosses") ?: member.obj("slayer_bosses")
        return SlayerType.entries.map { type ->
            val boss = bosses?.obj(type.apiKey)
            val xp = boss?.num("xp")?.toLong() ?: 0L
            val tierKills = (0..4).map { tier -> boss?.num("boss_kills_tier_$tier")?.toLong() ?: 0L }
            SlayerEntry(type, Leveling.slayer(xp, type.apiKey), tierKills)
        }
    }

    private fun average(skills: List<SkillEntry>): Double {
        val counted = skills.filter { it.type.inAverage }
        if (counted.isEmpty()) return 0.0
        return counted.sumOf { it.level.fractional } / counted.size
    }

    private fun JsonObject.obj(name: String): JsonObject? =
        get(name)?.takeIf { it.isJsonObject }?.asJsonObject

    private fun JsonObject.array(name: String) =
        get(name)?.takeIf { it.isJsonArray }?.asJsonArray

    private fun JsonObject.num(name: String): Double? =
        get(name)?.takeIf { it.isJsonPrimitive }?.asDouble

    private fun JsonObject.str(name: String): String? =
        get(name)?.takeIf { it.isJsonPrimitive }?.asString

    private fun JsonObject.bool(name: String): Boolean =
        get(name)?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false
}
