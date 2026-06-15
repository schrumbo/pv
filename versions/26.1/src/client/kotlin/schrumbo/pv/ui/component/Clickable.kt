package schrumbo.pv.ui.component

import net.minecraft.client.gui.GuiGraphicsExtractor

/**
 * Per-frame collector of click regions emitted by [Clickable] components. The screen resets it
 * before rendering the page, then resolves a click against the regions recorded last frame.
 * Coordinates are in the content's local space (the screen applies the page transform on lookup).
 */
object ClickRegistry {
    class Region(val x: Int, val y: Int, val w: Int, val h: Int, val action: () -> Unit)

    val regions = mutableListOf<Region>()

    fun reset() = regions.clear()

    /** Fires the topmost region containing [lx]/[ly]; returns whether anything was hit. */
    fun fire(lx: Int, ly: Int): Boolean {
        for (r in regions.asReversed()) {
            if (lx >= r.x && lx < r.x + r.w && ly >= r.y && ly < r.y + r.h) {
                r.action()
                return true
            }
        }
        return false
    }
}

/** Wraps a [child], registers its rect for click handling, and outlines it with [hoverBorder] on hover. */
class Clickable(
    private val child: Component,
    private val hoverBorder: Int? = null,
    private val onClick: () -> Unit,
) : Component() {
    override val width get() = child.width
    override val height get() = child.height

    override fun render(ctx: GuiGraphicsExtractor, x: Int, y: Int, mouseX: Int, mouseY: Int) {
        child.render(ctx, x, y, mouseX, mouseY)
        ClickRegistry.regions += ClickRegistry.Region(x, y, width, height, onClick)
        if (hoverBorder != null && mouseX in x until x + width && mouseY in y until y + height) {
            ctx.border(x, y, width, height, hoverBorder)
        }
    }
}
