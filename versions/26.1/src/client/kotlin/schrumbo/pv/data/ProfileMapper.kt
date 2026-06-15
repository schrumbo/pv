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
            bestiaryKills = bestiaryKills(member),
            collections = collections(member),
            mining = mining(member),
            trophy = trophyFish(member),
            jacobs = jacobs(member),
            attributes = attributes(member),
            foraging = foraging(member),
            rift = rift(member),
            pets = pets(member),
            containers = containers(member),
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
        val catacombs = Leveling.skill(
            cata?.num("experience")?.toLong() ?: 0L, Leveling.CATACOMBS_XP,
            overflow = true, overflowStep = Leveling.DUNGEON_OVERFLOW_STEP,
        )

        val classesJson = dj?.obj("player_classes")
        val classes = listOf(
            Triple("healer", "Healer", "potion"),
            Triple("mage", "Mage", "blaze_rod"),
            Triple("berserk", "Berserk", "iron_sword"),
            Triple("archer", "Archer", "bow"),
            Triple("tank", "Tank", "leather_chestplate"),
        ).map { (key, display, icon) ->
            val xp = classesJson?.obj(key)?.num("experience")?.toLong() ?: 0L
            val level = Leveling.skill(xp, Leveling.CATACOMBS_XP, overflow = true, overflowStep = Leveling.DUNGEON_OVERFLOW_STEP)
            DungeonClass(display, icon, level)
        }
        val classAverage = if (classes.isEmpty()) 0.0 else classes.sumOf { it.level.fractional } / classes.size

        val normalTiers = cata?.obj("tier_completions")
        val masterTiers = master?.obj("tier_completions")
        val floors = (0..7).map { f ->
            FloorStat(
                floor = f,
                completions = normalTiers?.num(f.toString())?.toLong() ?: 0L,
                masterCompletions = masterTiers?.num(f.toString())?.toLong() ?: 0L,
                normalBest = floorRun(cata, f),
                masterBest = floorRun(master, f),
            )
        }
        val totalRuns = (normalTiers?.num("total")?.toLong() ?: 0L) + (masterTiers?.num("total")?.toLong() ?: 0L)
        return DungeonData(
            catacombs = catacombs,
            classes = classes,
            classAverage = classAverage,
            selectedClass = dj?.str("selected_dungeon_class"),
            floors = floors,
            secrets = dj?.num("secrets")?.toLong() ?: 0L,
            totalRuns = totalRuns,
            highestFloorNormal = cata?.num("highest_tier_completed")?.toInt() ?: 0,
            highestFloorMaster = master?.num("highest_tier_completed")?.toInt() ?: 0,
        )
    }

    /** Best run for [floor] in a catacombs-type object: combined score, its timestamp, fastest S+ time. */
    private fun floorRun(type: JsonObject?, floor: Int): FloorRun? {
        val best = type?.obj("best_runs")?.array(floor.toString())?.firstOrNull()?.asJsonObject
        val sPlus = type?.obj("fastest_time_s_plus")?.num(floor.toString())?.toLong()
        if (best == null && sPlus == null) return null
        val score = best?.let {
            (it.num("score_exploration") ?: 0.0) + (it.num("score_speed") ?: 0.0) +
                (it.num("score_skill") ?: 0.0) + (it.num("score_bonus") ?: 0.0)
        }?.toInt() ?: 0
        return FloorRun(score, best?.num("timestamp")?.toLong() ?: 0L, sPlus)
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

    /** The `bestiary.kills` map (mob key → count); only numeric entries, the rest dropped. */
    private fun bestiaryKills(member: JsonObject): Map<String, Long> {
        val kills = member.obj("bestiary")?.obj("kills") ?: return emptyMap()
        val out = HashMap<String, Long>()
        for ((key, value) in kills.entrySet()) {
            if (value.isJsonPrimitive && value.asJsonPrimitive.isNumber) out[key] = value.asLong
        }
        return out
    }

    /** The `collection` map (collection key → collected amount); only numeric entries. */
    private fun collections(member: JsonObject): Map<String, Long> {
        val coll = member.obj("collection") ?: return emptyMap()
        val out = HashMap<String, Long>()
        for ((key, value) in coll.entrySet()) {
            if (value.isJsonPrimitive && value.asJsonPrimitive.isNumber) out[key] = value.asLong
        }
        return out
    }

    private val TIER_SUFFIX = Regex("_(bronze|silver|gold|diamond)$")

    private val CROP_NAMES = mapOf(
        "WHEAT" to "Wheat", "CARROT_ITEM" to "Carrot", "POTATO_ITEM" to "Potato",
        "PUMPKIN" to "Pumpkin", "MELON" to "Melon", "SUGAR_CANE" to "Sugar Cane",
        "CACTUS" to "Cactus", "NETHER_STALK" to "Nether Wart", "MUSHROOM_COLLECTION" to "Mushroom",
        "INK_SACK:3" to "Cocoa Beans",
    )

    private fun mining(member: JsonObject): MiningData {
        val mc = member.obj("mining_core")
        val glacite = member.obj("glacite_player_data")
        val crystals = mc?.obj("crystals")?.entrySet()?.map { (key, value) ->
            CrystalState(prettify(key.removeSuffix("_crystal")), value.asJsonObject.str("state") ?: "NOT_FOUND")
        } ?: emptyList()
        val corpses = glacite?.obj("corpses_looted")?.entrySet()
            ?.map { (key, value) -> prettify(key) to (value.asLong) } ?: emptyList()
        return MiningData(
            mithril = mc?.num("powder_mithril")?.toLong() ?: 0L,
            mithrilTotal = mc?.num("powder_mithril_total")?.toLong() ?: 0L,
            gemstone = mc?.num("powder_gemstone")?.toLong() ?: 0L,
            gemstoneTotal = mc?.num("powder_gemstone_total")?.toLong() ?: 0L,
            glacite = mc?.num("powder_glacite")?.toLong() ?: 0L,
            glaciteTotal = mc?.num("powder_glacite_total")?.toLong() ?: 0L,
            tokens = mc?.num("tokens")?.toLong() ?: 0L,
            crystals = crystals,
            corpses = corpses,
            fossilsDonated = glacite?.array("fossils_donated")?.size() ?: 0,
            mineshafts = glacite?.num("mineshafts_entered")?.toLong() ?: 0L,
        )
    }

    private fun trophyFish(member: JsonObject): TrophyData {
        val tf = member.obj("trophy_fish") ?: return TrophyData(0L, emptyList())
        val bases = tf.entrySet()
            .filter { (k, v) -> k != "total_caught" && k != "rewards" && v.isJsonPrimitive && v.asJsonPrimitive.isNumber }
            .map { it.key }
            .filter { !TIER_SUFFIX.containsMatchIn(it) }
            .distinct()
        val fish = bases.map { base ->
            TrophyFish(
                name = prettify(base),
                bronze = tf.num("${base}_bronze")?.toLong() ?: 0L,
                silver = tf.num("${base}_silver")?.toLong() ?: 0L,
                gold = tf.num("${base}_gold")?.toLong() ?: 0L,
                diamond = tf.num("${base}_diamond")?.toLong() ?: 0L,
            )
        }.sortedByDescending { it.total }
        return TrophyData(tf.num("total_caught")?.toLong() ?: 0L, fish)
    }

    private fun jacobs(member: JsonObject): JacobsData {
        val jc = member.obj("jacobs_contest")
        val medals = jc?.obj("medals_inv")
        val unique = jc?.obj("unique_brackets")
        val perks = jc?.obj("perks")
        val pbs = HashMap<String, Long>()
        jc?.obj("contests")?.entrySet()?.forEach { (key, value) ->
            val crop = key.substringAfterLast(':')
            val collected = value.asJsonObject.num("collected")?.toLong() ?: 0L
            pbs[crop] = maxOf(pbs[crop] ?: 0L, collected)
        }
        return JacobsData(
            gold = medals?.num("gold")?.toInt() ?: 0,
            silver = medals?.num("silver")?.toInt() ?: 0,
            bronze = medals?.num("bronze")?.toInt() ?: 0,
            contests = jc?.obj("contests")?.size() ?: 0,
            uniqueGold = unique?.array("gold")?.size() ?: 0,
            uniqueSilver = unique?.array("silver")?.size() ?: 0,
            uniqueBronze = unique?.array("bronze")?.size() ?: 0,
            doubleDrops = perks?.num("double_drops")?.toInt() ?: 0,
            farmingCap = perks?.num("farming_level_cap")?.toInt() ?: 0,
            pbs = pbs.entries.sortedByDescending { it.value }
                .map { CropPB(CROP_NAMES[it.key] ?: prettify(it.key), it.value) },
        )
    }

    private fun attributes(member: JsonObject): AttributesData {
        val stacks = member.obj("attributes")?.obj("stacks")
        val attrs = stacks?.entrySet()
            ?.map { (key, value) -> NamedLevel(prettify(key), value.asInt) }
            ?.sortedByDescending { it.level } ?: emptyList()
        val owned = member.obj("shards")?.array("owned")
        val shardsOwned = owned?.sumOf { it.asJsonObject.num("amount_owned")?.toLong() ?: 0L } ?: 0L
        return AttributesData(attrs, shardsOwned, owned?.size() ?: 0)
    }

    private fun foraging(member: JsonObject): ForagingData {
        val fc = member.obj("foraging_core")
        val gifts = member.obj("foraging")?.obj("tree_gifts")
        val claimed = gifts?.obj("milestone_tier_claimed")
        val trees = gifts?.entrySet()
            ?.filter { (k, v) -> k != "milestone_tier_claimed" && v.isJsonPrimitive && v.asJsonPrimitive.isNumber }
            ?.map { (key, value) -> TreeGift(prettify(key), value.asLong, claimed?.num(key)?.toInt() ?: 0) }
            ?.sortedByDescending { it.gifts } ?: emptyList()
        return ForagingData(
            whispers = fc?.num("forests_whispers")?.toLong() ?: 0L,
            whispersSpent = fc?.num("forests_whispers_spent")?.toLong() ?: 0L,
            dailyTrees = fc?.num("daily_trees_cut")?.toLong() ?: 0L,
            trees = trees,
        )
    }

    private fun rift(member: JsonObject): RiftData {
        val r = member.obj("rift")
        val stats = member.obj("player_stats")?.obj("rift")
        return RiftData(
            motes = member.obj("currencies")?.num("motes_purse")?.toLong() ?: 0L,
            lifetimeMotes = stats?.num("lifetime_motes_earned")?.toLong() ?: 0L,
            enigmaSouls = r?.obj("enigma")?.array("found_souls")?.size() ?: 0,
            galleryTrophies = r?.obj("gallery")?.array("secured_trophies")?.size() ?: 0,
            witherEyes = r?.obj("wither_cage")?.array("killed_eyes")?.size() ?: 0,
            catsFound = r?.obj("dead_cats")?.array("found_cats")?.size() ?: 0,
            areas = r?.num("lifetime_purchased_boundaries")?.toInt() ?: 0,
            hasMontezuma = r?.obj("dead_cats")?.obj("montezuma") != null,
        )
    }

    /** Decodes the player's item containers, keeping only those holding at least one item. */
    private fun containers(member: JsonObject): List<NamedContainer> {
        val inv = member.obj("inventory") ?: return emptyList()
        fun decode(obj: JsonObject?): List<ItemStack> =
            obj?.str("data")?.let { InventoryDecoder.decode(it) } ?: emptyList()

        val out = mutableListOf<NamedContainer>()
        fun add(name: String, items: List<ItemStack>) {
            if (items.any { !it.isEmpty }) out += NamedContainer(name, items)
        }

        add("Inventory", decode(inv.obj("inv_contents")))
        add("Armor", decode(inv.obj("inv_armor")).reversed())
        add("Equipment", decode(inv.obj("equipment_contents")).reversed())
        add("Ender Chest", decode(inv.obj("ender_chest_contents")))
        val bag = inv.obj("bag_contents")
        add("Accessories", decode(bag?.obj("talisman_bag")))
        add("Potion Bag", decode(bag?.obj("potion_bag")))
        add("Fishing Bag", decode(bag?.obj("fishing_bag")))
        add("Quiver", decode(bag?.obj("quiver")))
        add("Personal Vault", decode(inv.obj("personal_vault_contents")))
        val backpacks = inv.obj("backpack_contents")?.entrySet()
            ?.sortedBy { it.key.toIntOrNull() ?: 0 }
            ?.flatMap { (_, v) -> if (v.isJsonObject) InventoryDecoder.decode(v.asJsonObject.str("data") ?: "") else emptyList() }
            ?: emptyList()
        add("Backpacks", backpacks)
        return out
    }

    private fun pets(member: JsonObject): List<PetEntry> {
        val pets = member.obj("pets_data")?.array("pets") ?: member.array("pets") ?: return emptyList()
        return pets.filter { it.isJsonObject }.map { it.asJsonObject }
            .map { pet ->
                PetEntry(
                    type = pet.str("type") ?: "?",
                    tier = pet.str("tier") ?: "COMMON",
                    exp = pet.num("exp") ?: 0.0,
                    active = pet.bool("active"),
                    heldItem = pet.str("heldItem"),
                    skin = pet.str("skin"),
                )
            }
    }

    /** `volcanic_stonefish` → `Volcanic Stonefish`. */
    private fun prettify(key: String): String =
        key.split('_').joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

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
