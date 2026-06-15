package schrumbo.pv.util

import com.google.gson.JsonObject

/** Builds a legacy §-formatted rank nametag (e.g. `§b[MVP§c+§b] §bSchrumbo`) from `/player` data. */
object HypixelRank {

    private val COLORS = mapOf(
        "BLACK" to "0", "DARK_BLUE" to "1", "DARK_GREEN" to "2", "DARK_AQUA" to "3",
        "DARK_RED" to "4", "DARK_PURPLE" to "5", "GOLD" to "6", "GRAY" to "7",
        "DARK_GRAY" to "8", "BLUE" to "9", "GREEN" to "a", "AQUA" to "b",
        "RED" to "c", "LIGHT_PURPLE" to "d", "YELLOW" to "e", "WHITE" to "f",
    )

    fun nametag(player: JsonObject?, name: String): String {
        if (player == null) return "§7$name"
        player.get("prefix")?.asString?.let { return "$it §f$name" }

        val staff = player.get("rank")?.asString?.takeIf { it != "NORMAL" }
        val plus = "§" + (COLORS[player.get("rankPlusColor")?.asString] ?: "c")
        val (prefix, color) = when {
            staff == "ADMIN" -> "§c[ADMIN]" to "§c"
            staff == "GAME_MASTER" -> "§2[GM]" to "§2"
            staff == "YOUTUBER" -> "§c[§fYOUTUBE§c]" to "§c"
            staff == "MODERATOR" -> "§2[MOD]" to "§2"
            staff == "HELPER" -> "§9[HELPER]" to "§9"
            player.get("monthlyPackageRank")?.asString == "SUPERSTAR" -> "§6[MVP$plus++§6]" to "§6"
            else -> when (player.get("newPackageRank")?.asString ?: player.get("packageRank")?.asString) {
                "MVP_PLUS" -> "§b[MVP$plus+§b]" to "§b"
                "MVP" -> "§b[MVP]" to "§b"
                "VIP_PLUS" -> "§a[VIP§6+§a]" to "§a"
                "VIP" -> "§a[VIP]" to "§a"
                else -> "" to "§7"
            }
        }
        return if (prefix.isEmpty()) "$color$name" else "$prefix $color$name"
    }
}
