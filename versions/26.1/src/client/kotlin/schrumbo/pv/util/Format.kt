package schrumbo.pv.util

/** Number formatting helpers for the UI. */
object Format {

    /** Compact XP/coins: 1_234 → "1,234", 12_300_000 → "12.3M", 4_500_000_000 → "4.5B". */
    fun compact(value: Long): String = when {
        value < 1_000 -> value.toString()
        value < 1_000_000 -> trim(value / 1_000.0) + "K"
        value < 1_000_000_000 -> trim(value / 1_000_000.0) + "M"
        else -> trim(value / 1_000_000_000.0) + "B"
    }

    private fun trim(v: Double): String {
        val s = "%.1f".format(v)
        return if (s.endsWith(".0")) s.dropLast(2) else s
    }
}
