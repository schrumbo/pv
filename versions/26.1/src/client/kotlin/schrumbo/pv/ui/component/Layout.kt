package schrumbo.pv.ui.component

import net.minecraft.client.gui.GuiGraphicsExtractor

/** Empty gap of fixed size, used for spacing. */
class Spacer(override val width: Int, override val height: Int = 0) : Component() {
    override fun render(ctx: GuiGraphicsExtractor, x: Int, y: Int, mouseX: Int, mouseY: Int) {}
}

/** A fixed-size filled rectangle with an optional hard-edged border. */
class Box(
    override val width: Int,
    override val height: Int,
    private val background: Int? = null,
    private val borderColor: Int? = null,
) : Component() {
    override fun render(ctx: GuiGraphicsExtractor, x: Int, y: Int, mouseX: Int, mouseY: Int) {
        background?.let { ctx.fill(x, y, x + width, y + height, it) }
        borderColor?.let { ctx.border(x, y, width, height, it) }
    }
}

/** A fixed-size box that draws an optional background/border and places one child by alignment. */
class Frame(
    override val width: Int,
    override val height: Int,
    private val child: Component,
    private val background: Int? = null,
    private val borderColor: Int? = null,
    private val hAlign: HAlign = HAlign.CENTER,
    private val vAlign: VAlign = VAlign.CENTER,
) : Component() {
    override fun render(ctx: GuiGraphicsExtractor, x: Int, y: Int, mouseX: Int, mouseY: Int) {
        background?.let { ctx.fill(x, y, x + width, y + height, it) }
        borderColor?.let { ctx.border(x, y, width, height, it) }
        val cx = when (hAlign) {
            HAlign.START -> x
            HAlign.CENTER -> x + (width - child.width) / 2
            HAlign.END -> x + (width - child.width)
        }
        val cy = when (vAlign) {
            VAlign.TOP -> y
            VAlign.CENTER -> y + (height - child.height) / 2
            VAlign.BOTTOM -> y + (height - child.height)
        }
        child.render(ctx, cx, cy, mouseX, mouseY)
    }
}

/** Wraps a single child with uniform [padding] and an optional background/border. */
class Card(
    private val content: Component,
    private val padding: Int = 8,
    private val background: Int? = null,
    private val borderColor: Int? = null,
) : Component() {
    override val width get() = content.width + padding * 2
    override val height get() = content.height + padding * 2

    override fun render(ctx: GuiGraphicsExtractor, x: Int, y: Int, mouseX: Int, mouseY: Int) {
        background?.let { ctx.fill(x, y, x + width, y + height, it) }
        borderColor?.let { ctx.border(x, y, width, height, it) }
        content.render(ctx, x + padding, y + padding, mouseX, mouseY)
    }
}

/** Lays children out left-to-right with [spacing] between them, aligned vertically by [align]. */
class Row(
    private val children: List<Component>,
    private val spacing: Int = 4,
    private val align: VAlign = VAlign.TOP,
) : Component() {
    constructor(vararg children: Component, spacing: Int = 4, align: VAlign = VAlign.TOP) :
        this(children.toList(), spacing, align)

    override val width get() =
        children.sumOf { it.width } + spacing * (children.size - 1).coerceAtLeast(0)
    override val height get() = children.maxOfOrNull { it.height } ?: 0

    override fun render(ctx: GuiGraphicsExtractor, x: Int, y: Int, mouseX: Int, mouseY: Int) {
        var cx = x
        val h = height
        for (child in children) {
            val cy = when (align) {
                VAlign.TOP -> y
                VAlign.CENTER -> y + (h - child.height) / 2
                VAlign.BOTTOM -> y + (h - child.height)
            }
            child.render(ctx, cx, cy, mouseX, mouseY)
            cx += child.width + spacing
        }
    }
}

/** Pins [left] to the start and [right] to the end of a fixed [width]; height is the taller child. */
class SpaceBetween(
    override val width: Int,
    private val left: Component,
    private val right: Component,
) : Component() {
    override val height get() = maxOf(left.height, right.height)

    override fun render(ctx: GuiGraphicsExtractor, x: Int, y: Int, mouseX: Int, mouseY: Int) {
        val h = height
        left.render(ctx, x, y + (h - left.height) / 2, mouseX, mouseY)
        right.render(ctx, x + width - right.width, y + (h - right.height) / 2, mouseX, mouseY)
    }
}

/** Lays children out top-to-bottom with [spacing] between them, aligned horizontally by [align]. */
class Column(
    private val children: List<Component>,
    private val spacing: Int = 4,
    private val align: HAlign = HAlign.START,
) : Component() {
    constructor(vararg children: Component, spacing: Int = 4, align: HAlign = HAlign.START) :
        this(children.toList(), spacing, align)

    override val width get() = children.maxOfOrNull { it.width } ?: 0
    override val height get() =
        children.sumOf { it.height } + spacing * (children.size - 1).coerceAtLeast(0)

    override fun render(ctx: GuiGraphicsExtractor, x: Int, y: Int, mouseX: Int, mouseY: Int) {
        var cy = y
        val w = width
        for (child in children) {
            val cx = when (align) {
                HAlign.START -> x
                HAlign.CENTER -> x + (w - child.width) / 2
                HAlign.END -> x + (w - child.width)
            }
            child.render(ctx, cx, cy, mouseX, mouseY)
            cy += child.height + spacing
        }
    }
}
