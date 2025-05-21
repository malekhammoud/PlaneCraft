package org.malek.minmod.client.model;

import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import org.malek.minmod.entity.PlaneEntity;

public class PlaneEntityModel extends EntityModel<PlaneEntity> {
    private final ModelPart base;
    private final ModelPart body;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart tailFin;
    private final ModelPart horizontalStabilizer;
    private final ModelPart propellerSpinner;
    private final ModelPart propellerBlade1;
    private final ModelPart propellerBlade2;
    private final ModelPart frontWheelStrut;
    private final ModelPart frontWheel;
    private final ModelPart rearLeftWheelStrut;
    private final ModelPart rearLeftWheel;
    private final ModelPart rearRightWheelStrut;
    private final ModelPart rearRightWheel;

    public PlaneEntityModel(ModelPart root) {
        this.base = root;
        this.body = root.getChild("body");
        this.leftWing = root.getChild("left_wing");
        this.rightWing = root.getChild("right_wing");
        this.tailFin = root.getChild("tail_fin");
        this.horizontalStabilizer = root.getChild("horizontal_stabilizer");
        this.propellerSpinner = root.getChild("propeller_spinner");
        this.propellerBlade1 = this.propellerSpinner.getChild("propeller_blade1");
        this.propellerBlade2 = this.propellerSpinner.getChild("propeller_blade2");
        this.frontWheelStrut = root.getChild("front_wheel_strut");
        this.frontWheel = root.getChild("front_wheel");
        this.rearLeftWheelStrut = root.getChild("rear_left_wheel_strut");
        this.rearLeftWheel = root.getChild("rear_left_wheel");
        this.rearRightWheelStrut = root.getChild("rear_right_wheel_strut");
        this.rearRightWheel = root.getChild("rear_right_wheel");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();

        ModelTransform commonPivot = ModelTransform.pivot(0.0F, 22.0F, 0.0F);

        modelPartData.addChild("body", ModelPartBuilder.create()
                        .uv(0, 0).cuboid(-4.0F, -2.0F, -12.0F, 8.0F, 4.0F, 24.0F),
                commonPivot);

        modelPartData.addChild("left_wing", ModelPartBuilder.create()
                        .uv(0, 28).cuboid(4.0F, -1.0F, -8.0F, 16.0F, 2.0F, 8.0F),
                commonPivot);

        modelPartData.addChild("right_wing", ModelPartBuilder.create()
                        .uv(0, 38).cuboid(-20.0F, -1.0F, -8.0F, 16.0F, 2.0F, 8.0F),
                commonPivot);

        modelPartData.addChild("tail_fin", ModelPartBuilder.create()
                        .uv(0, 48).cuboid(-1.0F, -7.0F, 10.0F, 2.0F, 6.0F, 2.0F),
                commonPivot);

        modelPartData.addChild("horizontal_stabilizer", ModelPartBuilder.create()
                        .uv(8, 48).cuboid(-6.0F, -0.5F, 9.0F, 12.0F, 1.0F, 4.0F),
                commonPivot);

        ModelPartData spinner = modelPartData.addChild("propeller_spinner", ModelPartBuilder.create()
                        .uv(40, 48).cuboid(-1.0F, -1.0F, -13.0F, 2.0F, 2.0F, 1.0F),
                commonPivot);

        spinner.addChild("propeller_blade1", ModelPartBuilder.create()
                        .uv(40, 51).cuboid(0.0F, -3.0F, -12.5F, 1.0F, 6.0F, 1.0F),
                ModelTransform.NONE);

        spinner.addChild("propeller_blade2", ModelPartBuilder.create()
                        .uv(44, 51).cuboid(-3.0F, 0.0F, -12.5F, 6.0F, 1.0F, 1.0F),
                ModelTransform.NONE);

        modelPartData.addChild("front_wheel_strut", ModelPartBuilder.create()
                        .uv(0, 56).cuboid(-0.5F, 0.0F, -10.0F, 1.0F, 1.0F, 1.0F),
                commonPivot);

        modelPartData.addChild("front_wheel", ModelPartBuilder.create()
                        .uv(4, 56).cuboid(-0.5F, 1.0F, -10.5F, 1.0F, 2.0F, 2.0F),
                commonPivot);

        modelPartData.addChild("rear_left_wheel_strut", ModelPartBuilder.create()
                        .uv(8, 56).cuboid(11.5F, 0.0F, -4.0F, 1.0F, 1.0F, 1.0F),
                commonPivot);

        modelPartData.addChild("rear_left_wheel", ModelPartBuilder.create()
                        .uv(12, 56).cuboid(11.5F, 1.0F, -4.5F, 1.0F, 2.0F, 2.0F),
                commonPivot);

        modelPartData.addChild("rear_right_wheel_strut", ModelPartBuilder.create()
                        .uv(16, 56).cuboid(-12.5F, 0.0F, -4.0F, 1.0F, 1.0F, 1.0F),
                commonPivot);

        modelPartData.addChild("rear_right_wheel", ModelPartBuilder.create()
                        .uv(20, 56).cuboid(-12.5F, 1.0F, -4.5F, 1.0F, 2.0F, 2.0F),
                commonPivot);

        return TexturedModelData.of(modelData, 64, 64);
    }

    @Override
    public void setAngles(PlaneEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        this.propellerSpinner.yaw = animationProgress * 0.5F;
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
        body.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        leftWing.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        rightWing.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        tailFin.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        horizontalStabilizer.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        propellerSpinner.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        frontWheelStrut.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        frontWheel.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        rearLeftWheelStrut.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        rearLeftWheel.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        rearRightWheelStrut.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        rearRightWheel.render(matrices, vertices, light, overlay, red, green, blue, alpha);
    }
}

