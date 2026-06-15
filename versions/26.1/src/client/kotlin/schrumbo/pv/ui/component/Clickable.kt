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

/**
 * Wraps a [child] and registers its rect for click handling. On hover it draws a 2px [hoverRail]
 * down the left edge — never over the content, so scaled PiP item icons stay visible (a full
 * background fill composites above PiP items and would hide them). [pad] grows the hit/hover area.
 */
class Clickable(
    private val child: Component,
    private val hoverRail: Int? = null,
    private val pad: Int = 0,
    private val onClick: () -> Unit,
) : Component() {
    override val width get() = child.width
    override val height get() = child.height

    override fun render(ctx: GuiGraphicsExtractor, x: Int, y: Int, mouseX: Int, mouseY: Int) {
        val rx = x - pad
        val ry = y - pad
        val rw = width + pad * 2
        val rh = height + pad * 2
        val hovered = mouseX in rx until rx + rw && mouseY in ry until ry + rh
        child.render(ctx, x, y, mouseX, mouseY)
        if (hovered && hoverRail != null) ctx.fill(rx, ry, rx + 2, ry + rh, hoverRail)
        ClickRegistry.regions += ClickRegistry.Region(rx, ry, rw, rh, onClick)
    }
}
