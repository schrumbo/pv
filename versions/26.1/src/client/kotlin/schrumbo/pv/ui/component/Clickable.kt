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
 * Per-frame slot for a rich hover preview (e.g. a storage page's item grid). A [PreviewOnHover] writes
 * the component + cursor position here while hovered; the screen draws it on top after the page so it
 * floats above everything. Mirrors [ClickRegistry].
 */
object HoverPreview {
    var content: Component? = null
    var x = 0
    var y = 0
    fun reset() { content = null }
}

/** Renders [child]; while hovered, publishes [preview] (built lazily) to [HoverPreview] at the cursor. */
class PreviewOnHover(private val child: Component, private val preview: () -> Component) : Component() {
    override val width get() = child.width
    override val height get() = child.height

    override fun render(ctx: GuiGraphicsExtractor, x: Int, y: Int, mouseX: Int, mouseY: Int) {
        child.render(ctx, x, y, mouseX, mouseY)
        if (mouseX in x until x + width && mouseY in y until y + height) {
            HoverPreview.content = preview()
            HoverPreview.x = Hover.screenX
            HoverPreview.y = Hover.screenY
        }
    }
}

/**
 * Wraps a [child] and registers its rect for click handling. On hover it draws a [hoverFill] wash
 * across its (padded) area *before* the child, so the child's text and PiP item icons composite on
 * top and stay fully visible. [pad] grows the hit/hover area.
 */
class Clickable(
    private val child: Component,
    private val hoverFill: Int? = null,
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
        if (hovered && hoverFill != null) ctx.fill(rx, ry, rx + rw, ry + rh, hoverFill)
        child.render(ctx, x, y, mouseX, mouseY)
        ClickRegistry.regions += ClickRegistry.Region(rx, ry, rw, rh, onClick)
    }
}
