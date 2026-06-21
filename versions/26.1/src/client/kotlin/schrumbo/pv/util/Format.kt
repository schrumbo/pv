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

    /** Floored short form (no decimals/comma): 999 → "999", 1_500 → "1k", 4_100_000 → "4m". Exact in tooltip. */
    fun short(value: Long): String = when {
        value < 1_000 -> value.toString()
        value < 1_000_000 -> "${value / 1_000}k"
        value < 1_000_000_000 -> "${value / 1_000_000}m"
        else -> "${value / 1_000_000_000}b"
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

    /** Epoch-millis → "3 May '25" (UTC); 0/blank → "—". Compact for dense tables. */
    fun shortDate(ms: Long): String {
        if (ms <= 0) return "—"
        val d = java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneOffset.UTC).toLocalDate()
        val month = d.month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH)
        return "${d.dayOfMonth} $month '%02d".format(d.year % 100)
    }

    /** 1..n → Roman numerals (e.g. 9 → "IX"); 0 or negative → "0". */
    fun roman(n: Int): String {
        if (n <= 0) return "0"
        val values = intArrayOf(1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1)
        val symbols = arrayOf("M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I")
        val sb = StringBuilder()
        var x = n
        for (i in values.indices) while (x >= values[i]) { sb.append(symbols[i]); x -= values[i] }
        return sb.toString()
    }

    /** Run duration in millis → "m:ss" (e.g. 152119 → "2:32"); non-positive → "—". */
    fun duration(ms: Long): String {
        if (ms <= 0) return "—"
        val total = (ms + 500) / 1000
        return "${total / 60}:%02d".format(total % 60)
    }
}
