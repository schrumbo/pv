package schrumbo.pv.render;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2f;

/** Render state for PiP-based item rendering. */
public record ItemPipRenderState(
        ItemStack[] items,
        int rows,
        int cols,
        float renderScale,
        Matrix3x2f pose,
        ScreenRectangle scissorArea,
        int x0, int y0, int x1, int y1
) implements PictureInPictureRenderState {

    @Override
    public float scale() {
        return 16.0f * renderScale;
    }

    @Override
    public Matrix3x2f pose() {
        return pose;
    }

    @Override
    public ScreenRectangle scissorArea() {
        return scissorArea;
    }

    @Override
    public ScreenRectangle bounds() {
        return PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea);
    }
}
