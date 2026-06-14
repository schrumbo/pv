package schrumbo.pv.ui.screen

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import schrumbo.pv.ui.Theme

/** Root profile-viewer screen. Currently a placeholder shell hosting the tab bar. */
class PvScreen(private val target: String) : Screen(Component.literal("Profile Viewer")) {

    override fun isPauseScreen(): Boolean = false

    override fun extractRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        context.fill(0, 0, width, height, Theme.BACKDROP)

        val panelW = minOf(width - 40, 760)
        val panelH = minOf(height - 40, 480)
        val px = (width - panelW) / 2
        val py = (height - panelH) / 2

        context.fill(px, py, px + panelW, py + panelH, Theme.SURFACE)
        border(context, px, py, panelW, panelH, Theme.BORDER)

        val headerH = 22
        context.fill(px, py, px + panelW, py + headerH, Theme.SURFACE_ALT)
        context.fill(px, py + headerH, px + panelW, py + headerH + 1, Theme.BORDER)
        context.text(font, "Profile Viewer", px + 8, py + 7, Theme.TEXT, true)
        val sub = target.ifEmpty { "self" }
        context.text(font, sub, px + panelW - 8 - font.width(sub), py + 7, Theme.TEXT_MUTED, true)

        val tabY = py + headerH + 6
        context.text(font, "General", px + 10, tabY, Theme.ACCENT, true)
        context.fill(px + 10, tabY + 11, px + 10 + font.width("General"), tabY + 12, Theme.ACCENT)

        val msg = "General page — work in progress"
        context.text(font, msg, px + (panelW - font.width(msg)) / 2, py + panelH / 2, Theme.TEXT_MUTED, true)
    }

    private fun border(c: GuiGraphicsExtractor, x: Int, y: Int, w: Int, h: Int, color: Int) {
        c.fill(x, y, x + w, y + 1, color)
        c.fill(x, y + h - 1, x + w, y + h, color)
        c.fill(x, y, x + 1, y + h, color)
        c.fill(x + w - 1, y, x + w, y + h, color)
    }
}
