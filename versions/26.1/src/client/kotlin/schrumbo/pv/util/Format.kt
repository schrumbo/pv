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

    /** Epoch-millis → "12 Jun 2021" (UTC); 0/blank → "—". */
    fun date(ms: Long): String {
        if (ms <= 0) return "—"
        val d = java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneOffset.UTC).toLocalDate()
        val month = d.month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH)
        return "${d.dayOfMonth} $month ${d.year}"
    }
}
