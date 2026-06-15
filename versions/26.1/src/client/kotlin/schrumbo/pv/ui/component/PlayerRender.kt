package schrumbo.pv.ui.component

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.world.entity.LivingEntity

/**
 * Draws a 3D [entity] (centered, looking forward) inside a bordered slot, with an optional
 * floating [nametag] (raw §-formatted) above the head like an in-world name tag.
 */
class PlayerRender(
    override val width: Int,
    override val height: Int,
    private val entity: LivingEntity,
    private val nametag: String? = null,
    private val status: String? = null,
    private val background: Int? = null,
    private val borderColor: Int? = null,
) : Component() {
    override fun render(ctx: GuiGraphicsExtractor, x: Int, y: Int, mouseX: Int, mouseY: Int) {
        background?.let { ctx.fill(x, y, x + width, y + height, it) }
        borderColor?.let { ctx.border(x, y, width, height, it) }

        val pad = 6
        val x0 = x + pad
        val y0 = y + pad + font.lineHeight
        val x1 = x + width - pad
        val y1 = y + height - pad - font.lineHeight
        val scale = ((y1 - y0) * 0.40f).toInt().coerceAtLeast(10)
        val cx = (x0 + x1) / 2f
        val cy = (y0 + y1) / 2f
        InventoryScreen.extractEntityInInventoryFollowsMouse(
            ctx, x0, y0, x1, y1, scale, 0f, cx, cy, entity,
        )

        nametag?.let { ctx.text(font, it, x + (width - font.width(it)) / 2, y + pad, 0xFFFFFFFF.toInt(), true) }
        status?.let { ctx.text(font, it, x + (width - font.width(it)) / 2, y + height - pad - font.lineHeight + 2, 0xFFFFFFFF.toInt(), true) }
    }
}
