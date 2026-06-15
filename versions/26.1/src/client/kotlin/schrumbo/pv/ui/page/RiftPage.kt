package schrumbo.pv.ui.page

import schrumbo.pv.data.RiftData
import schrumbo.pv.data.SkyblockProfile
import schrumbo.pv.ui.Theme
import schrumbo.pv.ui.component.Column
import schrumbo.pv.ui.component.Component
import schrumbo.pv.ui.component.SpaceBetween
import schrumbo.pv.ui.component.Text
import schrumbo.pv.util.Format

/** Rift page: motes, enigma souls, and area/exploration progress. */
object RiftPage {

    private const val MOTE = 0xFFB070FF.toInt()

    fun build(p: SkyblockProfile, width: Int): Component {
        val r = p.rift
        return Column(
            PageKit.pageHeader("The Rift", "· ${Format.compact(r.motes)} motes", width),
            PageKit.tileRow(
                width,
                listOf(
                    "Motes Purse" to (Format.compact(r.motes) to MOTE),
                    "Lifetime Motes" to (Format.compact(r.lifetimeMotes) to Theme.TEXT),
                    "Enigma Souls" to ("${r.enigmaSouls}" to Theme.ACCENT),
                    "Areas Unlocked" to ("${r.areas}" to Theme.TEXT),
                ),
            ),
            PageKit.section("PROGRESS", width, progress(r, width)),
            spacing = 10,
        )
    }

    private fun progress(r: RiftData, width: Int): Component {
        val cellW = PageKit.cellW(width, 2)
        val rows = listOf(
            row(cellW, "Enigma Souls", "${r.enigmaSouls}"),
            row(cellW, "Gallery Trophies", "${r.galleryTrophies}"),
            row(cellW, "Wither Eyes Killed", "${r.witherEyes}"),
            row(cellW, "Cats Found", "${r.catsFound}"),
            row(cellW, "Montezuma (Death pet)", if (r.hasMontezuma) "Owned" else "—"),
        )
        return PageKit.grid(rows, width, cols = 2)
    }

    private fun row(cellW: Int, name: String, value: String): Component =
        SpaceBetween(cellW, Text(name, Theme.TEXT), Text(value, Theme.TEXT_MUTED))
}
