package org.malek.minmod.client.gui;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.malek.minmod.entity.PlaneEntity;

public class PlaneSpeedHudOverlay implements HudRenderCallback {
    @Override
    public void onHudRender(DrawContext drawContext, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        if (player != null && player.getVehicle() instanceof PlaneEntity plane) {
            TextRenderer textRenderer = client.textRenderer;
            String speedText = String.format("Speed: %.2f", plane.getCurrentSpeed());
            String takeoffSpeedText = String.format("Takeoff: %.2f", PlaneEntity.getTakeoffSpeedThreshold());
            String stallSpeedText = String.format("Stall: %.2f", PlaneEntity.getStallSpeedThreshold());

            int screenWidth = drawContext.getScaledWindowWidth();
            // int screenHeight = drawContext.getScaledWindowHeight(); // Not used yet

            drawContext.drawTextWithShadow(textRenderer, Text.literal(speedText), 10, 10, 0xFFFFFF);
            drawContext.drawTextWithShadow(textRenderer, Text.literal(takeoffSpeedText), 10, 20, 0xFFFFFF);
            drawContext.drawTextWithShadow(textRenderer, Text.literal(stallSpeedText), 10, 30, 0xFFFFFF);
        }
    }
}

