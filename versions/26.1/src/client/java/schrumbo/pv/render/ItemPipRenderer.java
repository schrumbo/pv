package schrumbo.pv.render;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Picture-in-picture renderer that draws scalable item icons for the profile viewer. */
public class ItemPipRenderer extends PictureInPictureRenderer<ItemPipRenderState> {

    private static final int ITEM_SIZE = 16;

    private final TrackingItemStackRenderState itemRenderState = new TrackingItemStackRenderState();

    public ItemPipRenderer(MultiBufferSource.BufferSource bufferSource) {
        super(bufferSource);
    }

    @Override
    public Class<ItemPipRenderState> getRenderStateClass() {
        return ItemPipRenderState.class;
    }

    @Override
    protected String getTextureLabel() {
        return "pv_item_pip";
    }

    @Override
    protected void renderToTexture(ItemPipRenderState state, PoseStack matrices) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        double guiScale = client.getWindow().getGuiScale();
        int hudWidth = state.cols() * ITEM_SIZE;
        int hudHeight = state.rows() * ITEM_SIZE;

        float scale = state.renderScale();
        float pps = (float) guiScale * scale;
        float texCenterX = Math.round(hudWidth * scale) * (float) guiScale / 2.0f;
        float texCenterY = Math.round(hudHeight * scale) * (float) guiScale / 2.0f;
        float modelScale = pps * 16.0f;

        matrices.scale(1.0f, -1.0f, -1.0f);

        Lighting lighting = client.gameRenderer.getLighting();

        renderPass(client, state, matrices, lighting, pps, texCenterX, texCenterY, modelScale, true);
        bufferSource.endBatch();

        renderPass(client, state, matrices, lighting, pps, texCenterX, texCenterY, modelScale, false);
        bufferSource.endBatch();
    }

    private void renderPass(Minecraft client, ItemPipRenderState state, PoseStack matrices,
                            Lighting lighting,
                            float pps, float texCenterX, float texCenterY,
                            float modelScale, boolean blockLight) {
        lighting.setupFor(blockLight ? Lighting.Entry.ITEMS_3D : Lighting.Entry.ITEMS_FLAT);

        ItemStack[] items = state.items();

        for (int i = 0; i < items.length && i < state.rows() * state.cols(); i++) {
            if (items[i] == null || items[i].isEmpty()) continue;

            client.getItemModelResolver().updateForLiving(
                    itemRenderState, items[i], ItemDisplayContext.GUI,
                    client.player
            );

            if (itemRenderState.usesBlockLight() != blockLight) continue;

            int col = i % state.cols();
            int row = i / state.cols();
            int slotX = col * ITEM_SIZE;
            int slotY = row * ITEM_SIZE;
            float itemCenterX = slotX + 8.0f;
            float itemCenterY = slotY + 8.0f;

            float snappedX = (Math.round(itemCenterX * pps) - texCenterX) / modelScale;
            float snappedY = (texCenterY - Math.round(itemCenterY * pps)) / modelScale;

            matrices.pushPose();
            matrices.translate(snappedX, snappedY, 0.0f);

            itemRenderState.submit(matrices, new DirectRenderCollector(matrices, bufferSource),
                    15728880, OverlayTexture.NO_OVERLAY, 0);

            matrices.popPose();
        }
    }

    @Override
    protected float getTranslateY(int height, int windowScaleFactor) {
        return height / 2.0f;
    }
}
