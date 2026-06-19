package schrumbo.pv.ui.component

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.item.ItemStack
import schrumbo.pv.render.ItemRenderUtils
import net.minecraft.network.chat.Component as McComponent

/** A single line of text. */
class Text(
    private val text: String,
    private val color: Int,
    private val shadow: Boolean = true,
) : Component() {
    override val width get() = font.width(text)
    override val height get() = font.lineHeight

    override fun render(ctx: GuiGraphicsExtractor, x: Int, y: Int, mouseX: Int, mouseY: Int) {
        ctx.text(font, text, x, y, color, shadow)
    }
}

/** A hard-edged progress bar filled to [fraction] (0..1) of its width. */
class ProgressBar(
    override val width: Int,
    override val height: Int,
    private val fraction: Double,
    private val foreground: Int,
    private val background: Int,
) : Component() {
    override fun render(ctx: GuiGraphicsExtractor, x: Int, y: Int, mouseX: Int, mouseY: Int) {
        ctx.fill(x, y, x + width, y + height, background)
        val filled = (width * fraction.coerceIn(0.0, 1.0)).toInt()
        if (filled > 0) ctx.fill(x, y, x + filled, y + height, foreground)
    }
}

/**
 * An item icon rendered via the vanilla item pipeline (handles textured skulls). [decorations] draws
 * the real stack count + durability/cooldown like an inventory slot; [corner] draws a custom compact
 * label in the stack-count corner (e.g. a level or a catch count). Shows the tooltip on hover unless
 * [tooltip] is disabled.
 */
class Item(
    private val stack: ItemStack,
    private val size: Int = 16,
    private val tooltip: Boolean = true,
    private val decorations: Boolean = false,
    private val corner: String? = null,
) : Component() {
    override val width get() = size
    override val height get() = size

    override fun render(ctx: GuiGraphicsExtractor, x: Int, y: Int, mouseX: Int, mouseY: Int) {
        if (!stack.isEmpty) {
            // Vanilla item rendering (handles textured player-head skulls + async skin resolution);
            // pose-scaled for non-16px icons. Crisp because items are model geometry, not bitmaps.
            if (size == 16) {
                ctx.item(stack, x, y)
                if (decorations) ctx.itemDecorations(font, stack, x, y)
            } else {
                // Non-16 icons go through the PiP pipeline: it renders the item model at the target
                // size, so textured skulls stay crisp instead of being raster-upscaled (pixelated).
                ItemRenderUtils.renderItem(ctx, stack, x, y, size / 16f)
            }
        }
        corner?.let {
            val tx = x + size - font.width(it) - 1
            ctx.text(font, it, tx, y + size - font.lineHeight + 1, 0xFFFFFFFF.toInt(), true)
        }
        if (tooltip && mouseX in x until x + size && mouseY in y until y + size) {
            ctx.setTooltipForNextFrame(font, stack, Hover.screenX, Hover.screenY)
        }
    }
}

/** Wraps a [child] and shows a multi-line (§-formatted) tooltip while hovered. */
class Tooltip(private val child: Component, private val lines: List<String>) : Component() {
    override val width get() = child.width
    override val height get() = child.height

    override fun render(ctx: GuiGraphicsExtractor, x: Int, y: Int, mouseX: Int, mouseY: Int) {
        child.render(ctx, x, y, mouseX, mouseY)
        if (mouseX in x until x + width && mouseY in y until y + height) {
            ctx.setComponentTooltipForNextFrame(font, lines.map { McComponent.literal(it) }, Hover.screenX, Hover.screenY)
        }
    }
}

/** Draws a raw legacy-formatted string (§ color codes interpreted by the font). */
class RichText(private val text: String) : Component() {
    override val width get() = font.width(text)
    override val height get() = font.lineHeight

    override fun render(ctx: GuiGraphicsExtractor, x: Int, y: Int, mouseX: Int, mouseY: Int) {
        ctx.text(font, text, x, y, 0xFFFFFFFF.toInt(), true)
    }
}
