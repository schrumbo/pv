package schrumbo.pv.ui.component

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor

/**
 * A self-measuring UI element. Each component reports a fixed [width]/[height] (containers derive
 * theirs from children) and draws itself at an absolute position. Hard edges only — no rounding.
 */
abstract class Component {
    abstract val width: Int
    abstract val height: Int

    /** Draws this component at an absolute position. [mouseX]/[mouseY] enable hover (e.g. tooltips). */
    abstract fun render(ctx: GuiGraphicsExtractor, x: Int, y: Int, mouseX: Int, mouseY: Int)

    protected val font: Font get() = Minecraft.getInstance().font
}

/** Horizontal placement of children within a column or of content within a box. */
enum class HAlign { START, CENTER, END }

/** Vertical placement of children within a row. */
enum class VAlign { TOP, CENTER, BOTTOM }

/** Draws a one-pixel hard-edged border around the given rectangle. */
internal fun GuiGraphicsExtractor.border(x: Int, y: Int, w: Int, h: Int, color: Int) {
    fill(x, y, x + w, y + 1, color)
    fill(x, y + h - 1, x + w, y + h, color)
    fill(x, y, x + 1, y + h, color)
    fill(x + w - 1, y, x + w, y + h, color)
}
