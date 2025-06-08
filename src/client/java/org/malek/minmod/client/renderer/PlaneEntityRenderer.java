package org.malek.minmod.client.renderer;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.malek.minmod.Minmod;
import org.malek.minmod.client.model.PlaneEntityModel;
import org.malek.minmod.entity.PlaneEntity;

public class PlaneEntityRenderer extends EntityRenderer<PlaneEntity> {
    public static final EntityModelLayer PLANE_MODEL_LAYER = new EntityModelLayer(new Identifier(Minmod.MOD_ID, "plane_entity"), "main");
    private final PlaneEntityModel model;
    private static final Identifier TEXTURE = new Identifier(Minmod.MOD_ID, "textures/entity/plane_entity.png");

    public PlaneEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.model = new PlaneEntityModel(context.getPart(PLANE_MODEL_LAYER));
        this.shadowRadius = 0.5f; // Adjust shadow size if needed
    }

    @Override
    public void render(PlaneEntity entity, float entityYaw, float partialTicks, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light) {
        matrixStack.push();
        // Apply transformations for the entity's orientation
        matrixStack.translate(0.0D, -0.6D, 0.0D); // Corrected Y translation to make wheel bottoms align with ground
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - entityYaw)); // Yaw
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(entity.getPitch(partialTicks))); // Pitch

        // If your model has parts that should animate based on entity state, call model.setAngles here
        // this.model.setAngles(entity, 0.0F, 0.0F, entity.age + partialTicks, 0.0F, 0.0F);

        VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(this.model.getLayer(getTexture(entity)));
        this.model.render(matrixStack, vertexConsumer, light, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);

        matrixStack.pop();
        super.render(entity, entityYaw, partialTicks, matrixStack, vertexConsumerProvider, light);
    }

    @Override
    public Identifier getTexture(PlaneEntity entity) {
        return TEXTURE;
    }
}
