package schrumbo.pv.data

import com.google.gson.JsonParser

/**
 * One node in a perk tree (HOTM / Foraging) at a fixed grid [col]/[row]. [type] is `P` (perk),
 * `C` (core) or `A` (ability); [max] is the level cap; [kind] is the powder/cost kind.
 */
data class TreeNodeDef(
    val id: String,
    val col: Int,
    val row: Int,
    val name: String,
    val type: String,
    val max: Int,
    val kind: String,
)

/** A node resolved against a player's progress: its definition plus the current [level]. */
data class TreeNode(val def: TreeNodeDef, val level: Int) {
    val unlocked: Boolean get() = level > 0
    val maxed: Boolean get() = level >= def.max
    val progress: Double get() = if (def.max <= 0) 1.0 else (level.toDouble() / def.max).coerceIn(0.0, 1.0)
}

/**
 * A resolved tree: placed [nodes] and grid bounds, plus [extras] — nodes the player owns that the
 * static layout doesn't know (newer than the bundled layout), shown in an overflow row.
 */
data class ResolvedTree(val nodes: List<TreeNode>, val cols: Int, val rows: Int, val extras: List<TreeNode>)

/** Loads the embedded `trees.json` once and resolves player node levels into placed grids. */
object SkillTreeRegistry {

    val hotm: List<TreeNodeDef>
    val foraging: List<TreeNodeDef>

    init {
        val stream = javaClass.getResourceAsStream("/assets/pv/trees.json")
            ?: error("missing trees.json resource")
        val root = stream.reader().use { JsonParser.parseReader(it).asJsonObject }
        hotm = root.getAsJsonArray("hotm").map { node(it.asJsonObject) }
        foraging = root.getAsJsonArray("foraging").map { node(it.asJsonObject) }
    }

    private fun node(o: com.google.gson.JsonObject) = TreeNodeDef(
        id = o.get("id").asString,
        col = o.get("c").asInt,
        row = o.get("r").asInt,
        name = o.get("name").asString,
        type = o.get("t").asString,
        max = o.get("max")?.asInt ?: 1,
        kind = o.get("k")?.asString ?: "",
    )

    /** Resolves [defs] against a player's `id -> level` node map; unknown owned ids become [ResolvedTree.extras]. */
    fun resolve(defs: List<TreeNodeDef>, levels: Map<String, Int>): ResolvedTree {
        val known = defs.mapTo(HashSet()) { it.id }
        val nodes = defs.map { TreeNode(it, levels[it.id] ?: 0) }
        val cols = (defs.maxOfOrNull { it.col } ?: 0) + 1
        val rows = (defs.maxOfOrNull { it.row } ?: 0) + 1
        val extras = levels.filterKeys { it !in known }
            .map { (id, lvl) -> TreeNode(TreeNodeDef(id, -1, -1, prettify(id), "U", maxOf(lvl, 1), ""), lvl) }
            .sortedBy { it.def.name }
        return ResolvedTree(nodes, cols, rows, extras)
    }

    private fun prettify(id: String): String =
        id.split('_').joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
}
