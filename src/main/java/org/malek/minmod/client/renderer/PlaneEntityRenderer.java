package org.malek.minmod.client.renderer;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.util.Identifier;
import net.minecraft.item.ItemStack;
import org.malek.minmod.Minmod;
import org.malek.minmod.entity.PlaneEntity;

public class PlaneEntityRenderer extends EntityRenderer<PlaneEntity> {
    private final ItemRenderer itemRenderer;
    private final ItemStack planeStack = new ItemStack(Minmod.PLANE_ITEM);

    public PlaneEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(PlaneEntity entity, float yaw, float partialTicks, MatrixStack matrixStack, VertexConsumerProvider buffer, int packedLight) {
        matrixStack.push();
        matrixStack.translate(0.0D, 0.25D, 0.0D);
        itemRenderer.renderItem(planeStack, ModelTransformationMode.GROUND, packedLight, OverlayTexture.DEFAULT_UV, matrixStack, buffer, entity.getWorld(), entity.getId());
        matrixStack.pop();
        super.render(entity, yaw, partialTicks, matrixStack, buffer, packedLight);
    }

    @Override
    public Identifier getTexture(PlaneEntity entity) { // Changed method name from getTextureLocation
        // No texture because rendering as item
        return null;
    }
}
