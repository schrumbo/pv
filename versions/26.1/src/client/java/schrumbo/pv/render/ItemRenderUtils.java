package schrumbo.pv.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2f;

/** Renders items through the PiP pipeline so icons scale cleanly. */
public final class ItemRenderUtils {

    private static final int ITEM_SIZE = 16;

    private ItemRenderUtils() {}

    /** Renders a single item at a screen position. */
    public static void renderItem(GuiGraphicsExtractor context, ItemStack stack, int x, int y) {
        renderItem(context, stack, x, y, 1.0f);
    }

    /** Renders a single item at a screen position with [scale]. */
    public static void renderItem(GuiGraphicsExtractor context, ItemStack stack, int x, int y, float scale) {
        renderItems(context, new ItemStack[]{stack}, x, y, 1, 1, scale);
    }

    /** Renders a grid of items at a screen position. */
    public static void renderItems(GuiGraphicsExtractor context, ItemStack[] items, int x, int y, int cols, int rows) {
        renderItems(context, items, x, y, cols, rows, 1.0f);
    }

    /** Renders a grid of items at a screen position with [scale]. */
    public static void renderItems(GuiGraphicsExtractor context, ItemStack[] items, int x, int y,
                                   int cols, int rows, float scale) {
        int hudW = cols * ITEM_SIZE;
        int hudH = rows * ITEM_SIZE;
        int scaledW = Math.round(hudW * scale);
        int scaledH = Math.round(hudH * scale);

        ScreenRectangle scissor = context.scissorStack.peek();
        Matrix3x2f pose = new Matrix3x2f(context.pose());

        ItemPipRenderState state = new ItemPipRenderState(
                items, rows, cols, scale, pose, scissor,
                x, y, x + scaledW, y + scaledH
        );
        context.guiRenderState.addPicturesInPictureState(state);
    }
}
