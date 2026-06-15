package schrumbo.pv.ui

/** Central dark palette. Hard edges only — no rounded corners anywhere in this mod. */
object Theme {
    const val BACKDROP = 0xC8000000.toInt()
    const val SURFACE = 0xFF15161A.toInt()
    const val SURFACE_ALT = 0xFF1E2026.toInt()
    const val BORDER = 0xFF2C2F36.toInt()
    const val TEXT = 0xFFE6E6E6.toInt()
    const val TEXT_MUTED = 0xFF8A8F98.toInt()
    const val ACCENT = 0xFF4DA3FF.toInt()
    const val WARN = 0xFFE0654F.toInt()
    const val GREEN = 0xFF5BD16A.toInt()
    const val GOLD = 0xFFFFB534.toInt()
    const val CATA = 0xFFE0533D.toInt()
    const val CATA_TRACK = 0xFF3A221D.toInt()

    /** Translucent white wash drawn behind a hovered clickable so its PiP icons stay on top. */
    const val HOVER = 0x22FFFFFF
}
