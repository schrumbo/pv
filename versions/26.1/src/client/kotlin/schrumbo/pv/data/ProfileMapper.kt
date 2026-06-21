package schrumbo.pv.data

import com.google.gson.JsonObject
import net.minecraft.world.item.ItemStack
import schrumbo.pv.util.Leveling

/** Maps the raw `/v2/skyblock/profiles` payload into [SkyblockProfile]s. */
object ProfileMapper {

    /** All profiles the player is a member of, plus the index of the selected one (or 0). */
    fun mapAll(profilesResponse: JsonObject, uuid: String, commissions: Long = 0L): Pair<List<SkyblockProfile>, Int> {
        val profiles = profilesResponse.array("profiles") ?: return emptyList<SkyblockProfile>() to 0
        val members = profiles.filter { it.isJsonObject }.map { it.asJsonObject }.filter { it.obj("members")?.has(uuid) == true }
        if (members.isEmpty()) return emptyList<SkyblockProfile>() to 0

        val selected = members.indexOfFirst { it.bool("selected") }.coerceAtLeast(0)
        val mapped = members.mapNotNull { mapOne(it, uuid, commissions) }
        return mapped to selected.coerceIn(0, (mapped.size - 1).coerceAtLeast(0))
    }

    private fun mapOne(profile: JsonObject, uuid: String, commissions: Long): SkyblockProfile? {
        val member = profile.obj("members")?.obj(uuid) ?: return null
        val skills = skills(member)
        val dungeons = dungeons(member)
        val gpd = member.obj("garden_player_data")
        val gear = gearItems(member)
        return SkyblockProfile(
            profileId = profile.str("profile_id") ?: "",
            cuteName = profile.str("cute_name") ?: "?",
            gameMode = profile.str("game_mode"),
            skyblockLevel = skyblockLevel(member),
            catacombs = dungeons.catacombs,
            dungeons = dungeons,
            skills = skills,
            skillAverage = average(skills),
            slayers = slayers(member),
            bestiaryKills = bestiaryKills(member),
            combat = combat(member),
            collections = collections(member),
            mining = mining(member, commissions),
            trophy = trophyFish(member),
            jacobs = jacobs(member),
            attributes = attributes(member),
            foraging = foraging(member),
            rift = rift(member),
            pets = pets(member),
            containers = containers(member),
            backpacks = backpacks(member),
            sacks = sacks(member),
            wardrobeSlot = (member.obj("inventory")?.num("wardrobe_equipped_slot") ?: member.num("wardrobe_equipped_slot"))?.toInt() ?: -1,
            armor = armor(member),
            equipment = equipment(member),
            miningTools = scanGear(gear, MINING_TOOLS),
            miningArmor = bestArmor(gear),
            miningEquipment = bestEquipment(gear),
            magicalPower = member.obj("accessory_bag_storage")?.num("highest_magical_power")?.toInt() ?: 0,
            selectedPower = member.obj("accessory_bag_storage")?.str("selected_power"),
            bank = profile.obj("banking")?.num("balance")?.toLong() ?: 0L,
            purse = (member.obj("currencies")?.num("coin_purse") ?: member.num("coin_purse"))?.toLong() ?: 0L,
            firstJoin = member.obj("profile")?.num("first_join")?.toLong() ?: 0L,
            fairySouls = (member.obj("fairy_soul")?.num("total_collected") ?: member.num("fairy_souls_collected"))?.toInt() ?: 0,
            greenhouse = GreenhouseData(
                copper = gpd?.num("copper")?.toInt() ?: 0,
                larvaConsumed = gpd?.num("larva_consumed")?.toInt() ?: 0,
                discovered = gpd?.array("discovered_greenhouse_crops")?.mapNotNull { it.asString }?.toSet() ?: emptySet(),
                analyzed = gpd?.array("analyzed_greenhouse_crops")?.mapNotNull { it.asString }?.toSet() ?: emptySet(),
            ),
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
        val best = type?.obj("best_runs")?.array(floor.toString())?.firstOrNull()?.takeIf { it.isJsonObject }?.asJsonObject
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

    private fun equipment(member: JsonObject): List<ItemStack> {
        val data = member.obj("inventory")?.obj("equipment_contents")?.str("data") ?: return emptyList()
        return InventoryDecoder.decode(data).reversed()
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

    private val KUUDRA_TIERS = listOf(
        "none" to "Basic", "hot" to "Hot", "burning" to "Burning", "fiery" to "Fiery", "infernal" to "Infernal",
    )
    private val DOJO_TESTS = listOf(
        "mob_kb" to "Force", "wall_jump" to "Stamina", "archer" to "Mastery", "sword_swap" to "Discipline",
        "snake" to "Swiftness", "fireball" to "Control", "lock_head" to "Tenacity",
    )

    /** Mob kills/deaths (`player_stats`) + Crimson Isle (`nether_island_player_data`). */
    private fun combat(member: JsonObject): CombatData {
        val ps = member.obj("player_stats")
        fun counts(obj: JsonObject?): List<MobCount> = obj?.entrySet()
            ?.filter { (k, v) -> k != "total" && v.isJsonPrimitive && v.asJsonPrimitive.isNumber }
            ?.map { (k, v) -> MobCount(prettify(k), v.asLong) }
            ?.filter { it.count > 0 }
            ?.sortedByDescending { it.count }
            ?: emptyList()
        val kills = counts(ps?.obj("kills"))
        val deaths = counts(ps?.obj("deaths"))
        val mobs = MobStats(kills, deaths, kills.sumOf { it.count }, deaths.sumOf { it.count })

        val nether = member.obj("nether_island_player_data")
        val kuudraObj = nether?.obj("kuudra_completed_tiers")
        val kuudra = KUUDRA_TIERS.map { (id, name) ->
            KuudraTier(id, name, kuudraObj?.num(id)?.toInt() ?: 0, kuudraObj?.num("highest_wave_$id")?.toInt() ?: 0)
        }
        val dojoObj = nether?.obj("dojo")
        val dojo = DOJO_TESTS.map { (id, name) ->
            DojoTest(id, name, dojoObj?.num("dojo_points_$id")?.toInt() ?: 0, dojoObj?.num("dojo_time_$id")?.toInt() ?: -1)
        }
        val crimson = CrimsonIsleData(
            selectedFaction = when (nether?.str("selected_faction")) {
                "mages" -> "Mage"; "barbarians" -> "Barbarian"; else -> null
            },
            mageReputation = nether?.num("mages_reputation")?.toInt() ?: 0,
            barbarianReputation = nether?.num("barbarians_reputation")?.toInt() ?: 0,
            kuudra = kuudra,
            dojo = dojo,
        )
        return CombatData(mobs, crimson)
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

    /** All Crystal Hollows / Glacite crystals, in display order (nucleus run, gemstones, glacite). */
    private val CRYSTAL_ORDER = listOf(
        "jade", "amethyst", "topaz", "sapphire", "amber", "ruby",
        "jasper", "opal", "aquamarine", "citrine", "peridot", "onyx",
    )

    /** The five crystals placed for a Crystal Nucleus run; the min of their placements = runs done. */
    private val NUCLEUS_CRYSTALS = listOf("jade", "amethyst", "topaz", "sapphire", "amber")

    private fun mining(member: JsonObject, commissions: Long): MiningData {
        val mc = member.obj("mining_core")
        val glacite = member.obj("glacite_player_data")
        val crystalData = mc?.obj("crystals")
        val crystals = CRYSTAL_ORDER.map { key ->
            CrystalState(prettify(key), crystalData?.obj("${key}_crystal")?.str("state") ?: "NOT_FOUND")
        }
        val corpses = glacite?.obj("corpses_looted")?.entrySet()
            ?.map { (key, value) -> prettify(key) to (value.asLong) } ?: emptyList()
        val nucleusRuns = NUCLEUS_CRYSTALS
            .map { crystalData?.obj("${it}_crystal")?.num("total_placed")?.toLong() ?: 0L }
            .minOrNull() ?: 0L
        return MiningData(
            mithril = mc?.num("powder_mithril")?.toLong() ?: 0L,
            mithrilTotal = mc?.num("powder_mithril_total")?.toLong() ?: 0L,
            mithrilSpent = mc?.num("powder_spent_mithril")?.toLong() ?: 0L,
            gemstone = mc?.num("powder_gemstone")?.toLong() ?: 0L,
            gemstoneTotal = mc?.num("powder_gemstone_total")?.toLong() ?: 0L,
            gemstoneSpent = mc?.num("powder_spent_gemstone")?.toLong() ?: 0L,
            glacite = mc?.num("powder_glacite")?.toLong() ?: 0L,
            glaciteTotal = mc?.num("powder_glacite_total")?.toLong() ?: 0L,
            glaciteSpent = mc?.num("powder_spent_glacite")?.toLong() ?: 0L,
            tokens = mc?.num("tokens")?.toLong() ?: 0L,
            nodes = treeNodes(member, "mining", mc),
            crystals = crystals,
            corpses = corpses,
            donatedFossils = glacite?.array("fossils_donated")?.mapNotNull { it.asString }?.toSet() ?: emptySet(),
            mineshafts = glacite?.num("mineshafts_entered")?.toLong() ?: 0L,
            nucleusRuns = nucleusRuns,
            commissions = commissions,
            hotmLevel = hotmTier(
                member.obj("skill_tree")?.obj("experience")?.num("mining")?.toLong()
                    ?: mc?.num("experience")?.toLong() ?: 0L,
            ),
        )
    }

    // Authoritative mining-gear ids from meowdding-repo (repo/pv/gear/mining.json). The armor and
    // equipment lists are ordered by progression (worst → best) so the list index doubles as a score.
    private val MINING_TOOLS = setOf(
        "TITANIUM_PICKAXE", "RUSTY_TITANIUM_PICKAXE", "JUNGLE_PICKAXE", "WOOD_PICKAXE", "LAPIS_PICKAXE",
        "BANDAGED_MITHRIL_PICKAXE", "MITHRIL_PICKAXE", "REFINED_MITHRIL_PICKAXE", "IRON_PICKAXE", "GOLD_PICKAXE",
        "ZOMBIE_PICKAXE", "ALPHA_PICK", "PROMISING_PICKAXE", "DIAMOND_PICKAXE", "PICKONIMBUS",
        "REFINED_TITANIUM_PICKAXE", "FRACTURED_MITHRIL_PICKAXE", "STONE_PICKAXE", "ROOKIE_PICKAXE",
        "MITHRIL_DRILL_1", "MITHRIL_DRILL_2", "TITANIUM_DRILL_1", "TITANIUM_DRILL_2", "TITANIUM_DRILL_3",
        "TITANIUM_DRILL_4", "GEMSTONE_DRILL_1", "GEMSTONE_DRILL_2", "GEMSTONE_DRILL_3", "GEMSTONE_DRILL_4",
        "DIVAN_DRILL", "GEMSTONE_GAUNTLET", "CHISEL", "REINFORCED_CHISEL", "GLACITE_CHISEL", "PERFECT_CHISEL",
    )

    private val MINING_ARMOR = listOf(
        "MINER_OUTFIT_HELMET", "MINER_OUTFIT_CHESTPLATE", "MINER_OUTFIT_LEGGINGS", "MINER_OUTFIT_BOOTS",
        "LAPIS_ARMOR_HELMET", "LAPIS_ARMOR_CHESTPLATE", "LAPIS_ARMOR_LEGGINGS", "LAPIS_ARMOR_BOOTS",
        "TANK_MINER_HELMET", "TANK_MINER_CHESTPLATE", "TANK_MINER_LEGGINGS", "TANK_MINER_BOOTS",
        "HARDENED_DIAMOND_HELMET", "HARDENED_DIAMOND_CHESTPLATE", "HARDENED_DIAMOND_LEGGINGS", "HARDENED_DIAMOND_BOOTS",
        "MINERAL_HELMET", "MINERAL_CHESTPLATE", "MINERAL_LEGGINGS", "MINERAL_BOOTS",
        "GLOSSY_MINERAL_HELMET", "GLOSSY_MINERAL_CHESTPLATE", "GLOSSY_MINERAL_LEGGINGS", "GLOSSY_MINERAL_BOOTS",
        "GOBLIN_HELMET", "GOBLIN_CHESTPLATE", "GOBLIN_LEGGINGS", "GOBLIN_BOOTS",
        "GLACITE_HELMET", "GLACITE_CHESTPLATE", "GLACITE_LEGGINGS", "GLACITE_BOOTS",
        "HEAT_HELMET", "HEAT_CHESTPLATE", "HEAT_LEGGINGS", "HEAT_BOOTS",
        "ARMOR_OF_YOG_HELMET", "ARMOR_OF_YOG_CHESTPLATE", "ARMOR_OF_YOG_LEGGINGS", "ARMOR_OF_YOG_BOOTS",
        "FLAME_BREAKER_HELMET", "FLAME_BREAKER_CHESTPLATE", "FLAME_BREAKER_LEGGINGS", "FLAME_BREAKER_BOOTS",
        "SORROW_HELMET", "SORROW_CHESTPLATE", "SORROW_LEGGINGS", "SORROW_BOOTS",
        "DIVAN_HELMET", "DIVAN_CHESTPLATE", "DIVAN_LEGGINGS", "DIVAN_BOOTS",
    )

    /** Mining equipment id lists per slot, in display order: necklace, cloak, belt, gloves. */
    private val MINING_EQUIPMENT = listOf(
        listOf("MITHRIL_NECKLACE", "TITANIUM_NECKLACE", "AMBER_NECKLACE", "DIVAN_PENDANT"),
        listOf("ANCIENT_CLOAK", "MITHRIL_CLOAK", "TITANIUM_CLOAK", "SAPPHIRE_CLOAK"),
        listOf("MITHRIL_BELT", "TITANIUM_BELT", "JADE_BELT"),
        listOf("GLOWSTONE_GAUNTLET", "VANQUISHED_GLOWSTONE_GAUNTLET", "MITHRIL_GAUNTLET", "TITANIUM_GAUNTLET",
            "AMETHYST_GAUNTLET", "DWARVEN_HANDWARMERS"),
    )

    private val ARMOR_SLOTS = listOf("HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS")

    /**
     * Every (stack, id) pair across the player's inventory, equipped slots, ender chest, personal vault,
     * backpacks and wardrobe — the search space for gear scanning. Wardrobe sets are flattened in.
     */
    private fun gearItems(member: JsonObject): List<Pair<ItemStack, String?>> {
        val inv = member.obj("inventory") ?: return emptyList()
        val sources = mutableListOf<String>()
        inv.obj("inv_contents")?.str("data")?.let { sources += it }
        inv.obj("inv_armor")?.str("data")?.let { sources += it }
        inv.obj("equipment_contents")?.str("data")?.let { sources += it }
        inv.obj("ender_chest_contents")?.str("data")?.let { sources += it }
        inv.obj("personal_vault_contents")?.str("data")?.let { sources += it }
        inv.obj("backpack_contents")?.entrySet()?.forEach { (_, v) ->
            if (v.isJsonObject) v.asJsonObject.str("data")?.let { sources += it }
        }
        val items = sources.flatMap { InventoryDecoder.decodeWithIds(it) }.toMutableList()
        member.obj("loadout")?.obj("armor")?.entrySet()?.forEach { (_, set) ->
            val o = set.takeIf { it.isJsonObject }?.asJsonObject ?: return@forEach
            ARMOR_SLOTS.forEach { piece -> o.obj(piece)?.str("data")?.let { items += InventoryDecoder.decodeWithIds(it) } }
        }
        return items.filter { !it.first.isEmpty }
    }

    /** Distinct items from [gear] whose id is in [ids], one per id. */
    private fun scanGear(gear: List<Pair<ItemStack, String?>>, ids: Set<String>): List<ItemStack> =
        gear.filter { it.second in ids }.distinctBy { it.second }.map { it.first }

    /** Best armor piece per slot (helmet, chestplate, leggings, boots); empty slot = no piece owned. */
    private fun bestArmor(gear: List<Pair<ItemStack, String?>>): List<ItemStack> =
        ARMOR_SLOTS.map { slot ->
            gear.filter { it.second?.endsWith(slot) == true && it.second in MINING_ARMOR }
                .maxByOrNull { MINING_ARMOR.indexOf(it.second) }?.first ?: ItemStack.EMPTY
        }

    /** Best equipment per slot (necklace, cloak, belt, gloves); empty slot = no piece owned. */
    private fun bestEquipment(gear: List<Pair<ItemStack, String?>>): List<ItemStack> =
        MINING_EQUIPMENT.map { list ->
            gear.filter { it.second in list }.maxByOrNull { list.indexOf(it.second) }?.first ?: ItemStack.EMPTY
        }

    /** Cumulative HotM xp → tier (1–10). */
    private val HOTM_XP = longArrayOf(3000, 12000, 37000, 97000, 197000, 347000, 557000, 847000, 1247000)
    private fun hotmTier(exp: Long): Int = 1 + HOTM_XP.count { exp >= it }

    /**
     * Perk-tree node levels (`id -> level`) for a skill. The unified `skill_tree.nodes.<skill>` is
     * preferred; mining falls back to the legacy `mining_core.nodes`. Toggle flags are dropped.
     */
    private fun treeNodes(member: JsonObject, skill: String, legacy: JsonObject? = null): Map<String, Int> {
        val src = member.obj("skill_tree")?.obj("nodes")?.obj(skill) ?: legacy?.obj("nodes")
        val out = HashMap<String, Int>()
        src?.entrySet()?.forEach { (key, value) ->
            if (key.startsWith("toggle_")) return@forEach
            if (value.isJsonPrimitive && value.asJsonPrimitive.isNumber) out[key] = value.asInt
        }
        return out
    }

    private fun trophyFish(member: JsonObject): TrophyData {
        val stats = member.obj("player_stats")
        val itemsFished = stats?.obj("items_fished")
        val tf = member.obj("trophy_fish")
            ?: return TrophyData(0L, emptyList(), itemsFished?.num("total")?.toLong() ?: 0L,
                itemsFished?.num("treasure")?.toLong() ?: 0L, itemsFished?.num("large_treasure")?.toLong() ?: 0L,
                stats?.num("sea_creature_kills")?.toLong() ?: 0L)
        val bases = tf.entrySet()
            .filter { (k, v) -> k != "total_caught" && k != "rewards" && v.isJsonPrimitive && v.asJsonPrimitive.isNumber }
            .map { it.key }
            .filter { !TIER_SUFFIX.containsMatchIn(it) }
            .distinct()
        val fish = bases.map { base ->
            TrophyFish(
                key = base,
                name = prettify(base),
                bronze = tf.num("${base}_bronze")?.toLong() ?: 0L,
                silver = tf.num("${base}_silver")?.toLong() ?: 0L,
                gold = tf.num("${base}_gold")?.toLong() ?: 0L,
                diamond = tf.num("${base}_diamond")?.toLong() ?: 0L,
            )
        }.sortedByDescending { it.total }
        return TrophyData(
            totalCaught = tf.num("total_caught")?.toLong() ?: 0L,
            fish = fish,
            itemsFished = itemsFished?.num("total")?.toLong() ?: 0L,
            treasure = itemsFished?.num("treasure")?.toLong() ?: 0L,
            largeTreasure = itemsFished?.num("large_treasure")?.toLong() ?: 0L,
            seaCreatures = stats?.num("sea_creature_kills")?.toLong() ?: 0L,
        )
    }

    private fun jacobs(member: JsonObject): JacobsData {
        val jc = member.obj("jacobs_contest")
        val medals = jc?.obj("medals_inv")
        val unique = jc?.obj("unique_brackets")
        val perks = jc?.obj("perks")
        val pbs = HashMap<String, Long>()
        // Diamond/Platinum medals are never deposited into `medals_inv` (only bronze/silver/gold are
        // spendable). Like SkyCrypt, count earned diamond/platinum from each contest's claimed medal.
        val earned = HashMap<String, Int>()
        jc?.obj("contests")?.entrySet()?.forEach { (key, value) ->
            val o = value.takeIf { it.isJsonObject }?.asJsonObject ?: return@forEach
            val crop = key.substringAfterLast(':')
            val collected = o.num("collected")?.toLong() ?: 0L
            pbs[crop] = maxOf(pbs[crop] ?: 0L, collected)
            o.str("claimed_medal")?.let { earned[it] = (earned[it] ?: 0) + 1 }
        }
        return JacobsData(
            diamond = earned["diamond"] ?: medals?.num("diamond")?.toInt() ?: 0,
            platinum = earned["platinum"] ?: medals?.num("platinum")?.toInt() ?: 0,
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
        // Syphoned shard total per attribute id (id → count); drives the attribute level via the rarity
        // cost table. Keys upper-cased to match AttributeRegistry ids.
        val stacks = HashMap<String, Int>()
        member.obj("attributes")?.obj("stacks")?.entrySet()?.forEach { (k, v) ->
            if (v.isJsonPrimitive && v.asJsonPrimitive.isNumber) stacks[k.uppercase()] = v.asInt
        }
        val owned = member.obj("shards")?.array("owned")
        val shardsOwned = owned?.sumOf { it.takeIf { e -> e.isJsonObject }?.asJsonObject?.num("amount_owned")?.toLong() ?: 0L } ?: 0L
        return AttributesData(stacks, shardsOwned, owned?.size() ?: 0)
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
            nodes = treeNodes(member, "foraging"),
            hotfLevel = fc?.num("hotf_level")?.toInt() ?: 0,
        )
    }

    private fun rift(member: JsonObject): RiftData {
        val r = member.obj("rift")
        val stats = member.obj("player_stats")?.obj("rift")
        val inv = r?.obj("inventory")
        fun decode(o: JsonObject?): List<ItemStack> = o?.str("data")?.let { InventoryDecoder.decode(it) } ?: emptyList()
        return RiftData(
            motes = member.obj("currencies")?.num("motes_purse")?.toLong() ?: 0L,
            lifetimeMotes = stats?.num("lifetime_motes_earned")?.toLong() ?: 0L,
            enigmaSouls = r?.obj("enigma")?.array("found_souls")?.size() ?: 0,
            galleryTrophies = r?.obj("gallery")?.array("secured_trophies")?.size() ?: 0,
            securedTrophies = r?.obj("gallery")?.array("secured_trophies")
                ?.mapNotNull { it.takeIf { e -> e.isJsonObject }?.asJsonObject }
                ?.mapNotNull { t -> t.str("type")?.let { it to (t.num("timestamp")?.toLong() ?: 0L) } }
                ?.toMap() ?: emptyMap(),
            witherEyes = r?.obj("wither_cage")?.array("killed_eyes")?.size() ?: 0,
            catsFound = r?.obj("dead_cats")?.array("found_cats")?.size() ?: 0,
            hasMontezuma = r?.obj("dead_cats")?.obj("montezuma") != null,
            visits = stats?.num("visits")?.toLong() ?: 0L,
            burgers = r?.obj("castle")?.num("grubber_stacks")?.toInt() ?: 0,
            porthals = r?.array("lifetime_purchased_boundaries")?.mapNotNull { it.asString } ?: emptyList(),
            armor = decode(inv?.obj("inv_armor")).reversed(),
            equipment = decode(inv?.obj("equipment_contents")).reversed(),
            inventory = decode(inv?.obj("inv_contents")),
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

        // Inventory feeds the Loadout tab; Armor/Equipment are shown there too (no standalone tabs).
        add("Inventory", decode(inv.obj("inv_contents")))
        add("Ender Chest", decode(inv.obj("ender_chest_contents")))
        val wardrobe = wardrobeItems(member)
        if (wardrobe.isNotEmpty()) out += NamedContainer("Wardrobe", wardrobe)
        val bag = inv.obj("bag_contents")
        add("Accessories", decode(bag?.obj("talisman_bag")))
        add("Potion Bag", decode(bag?.obj("potion_bag")))
        add("Fishing Bag", decode(bag?.obj("fishing_bag")))
        add("Quiver", decode(bag?.obj("quiver")))
        // Candy + Carnival bags always show (even empty) — they're part of the Misc category.
        val shared = member.obj("shared_inventory")
        out += NamedContainer("Candy Bag", decode(shared?.obj("candy_inventory_contents")))
        out += NamedContainer("Carnival Mask Bag", decode(shared?.obj("carnival_mask_inventory_contents")))
        add("Personal Vault", decode(inv.obj("personal_vault_contents")))

        // Hunting box — owned shards as a chest-style grid of skulls, amount shown as the stack size.
        val box = member.obj("shards")?.array("owned")
            ?.mapNotNull { it.takeIf { e -> e.isJsonObject }?.asJsonObject }
            ?.mapNotNull { o ->
                val key = o.str("type")?.uppercase()?.removePrefix("SHARD_") ?: return@mapNotNull null
                val amt = o.num("amount_owned")?.toInt() ?: 0
                if (amt > 0) key to amt else null
            }
            ?.sortedByDescending { it.second }
            ?.map { (key, amt) -> AttributeRegistry.boxStack(key, amt) }
            ?: emptyList()
        add("Hunting Box", box)
        return out
    }

    /**
     * Wardrobe sets from `loadout.armor`: a map of 1-based slot id → `{HELMET,CHESTPLATE,LEGGINGS,
     * BOOTS}`, each a gzipped 1-item inventory. Flattened to 36 slots per page (4 rows × 9: all
     * helmets, then chestplates, leggings, boots) so each column reads as one set.
     */
    private fun wardrobeItems(member: JsonObject): List<ItemStack> {
        val armor = member.obj("loadout")?.obj("armor") ?: return emptyList()
        val maxSlot = armor.entrySet().mapNotNull { it.key.toIntOrNull() }.maxOrNull() ?: return emptyList()
        val pages = (maxSlot + 8) / 9
        val out = mutableListOf<ItemStack>()
        for (page in 0 until pages) {
            val start = page * 9 + 1
            for (piece in listOf("HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS")) {
                for (slotId in start until start + 9) {
                    val data = armor.obj(slotId.toString())?.obj(piece)?.str("data")
                    out += data?.let { InventoryDecoder.decode(it).firstOrNull() } ?: ItemStack.EMPTY
                }
            }
        }
        return out
    }

    /** `inventory.sacks_counts` (Skyblock item id → amount), keeping only positive counts. */
    private fun sacks(member: JsonObject): Map<String, Long> {
        val counts = member.obj("inventory")?.obj("sacks_counts") ?: return emptyMap()
        val out = HashMap<String, Long>()
        for ((k, v) in counts.entrySet()) {
            if (v.isJsonPrimitive && v.asJsonPrimitive.isNumber && v.asLong > 0) out[k] = v.asLong
        }
        return out
    }

    /** Backpack type id → full slot count (the stored contents may be trimmed shorter than this). */
    private val BACKPACK_SLOTS = mapOf(
        "SMALL_BACKPACK" to 9, "MEDIUM_BACKPACK" to 18, "LARGE_BACKPACK" to 27,
        "GREATER_BACKPACK" to 36, "JUMBO_BACKPACK" to 45,
    )

    /** Each owned backpack (even empty ones) with its icon and full slot count from its type. */
    private fun backpacks(member: JsonObject): List<Backpack> {
        val inv = member.obj("inventory") ?: return emptyList()
        val contents = inv.obj("backpack_contents") ?: return emptyList()
        val icons = inv.obj("backpack_icons")
        return contents.entrySet().sortedBy { it.key.toIntOrNull() ?: 0 }.mapNotNull { (key, v) ->
            if (!v.isJsonObject) return@mapNotNull null
            val items = InventoryDecoder.decode(v.asJsonObject.str("data") ?: "")
            val iconData = icons?.obj(key)?.str("data")
            val icon = iconData?.let { InventoryDecoder.decodeIcon(it) }?.takeIf { !it.isEmpty }
                ?: ItemStack(net.minecraft.world.item.Items.CHEST)
            val slots = BACKPACK_SLOTS[iconData?.let { InventoryDecoder.iconId(it) }]
                ?: ((items.size + 8) / 9 * 9).coerceAtLeast(9)
            Backpack(icon, items, slots)
        }
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
