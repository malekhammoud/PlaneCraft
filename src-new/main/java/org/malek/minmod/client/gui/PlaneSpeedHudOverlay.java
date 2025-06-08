package org.malek.minmod.client.gui;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.malek.minmod.entity.PlaneEntity;

import java.util.List;
import java.util.function.Predicate;

public class PlaneSpeedHudOverlay implements HudRenderCallback {
    private static final int HUD_TEXT_COLOR = 0xFFFFFF; // White
    private static final int ALTITUDE_BAR_COLOR = 0xFF00AACC; // Light Blue
    private static final int ALTITUDE_BG_COLOR = 0x80333333; // Semi-transparent dark gray
    private static final int SONAR_BG_COLOR = 0x80000000; // Semi-transparent black
    private static final int SONAR_LINE_COLOR = 0xFF00FF00; // Bright Green
    private static final int SONAR_GRID_COLOR = 0x80008000; // Semi-transparent Green
    private static final int SONAR_BLIP_COLOR = 0xFFFF0000; // Bright Red for generic blips
    private static final double SONAR_RANGE = 48.0; // Blocks

    @Override
    public void onHudRender(DrawContext drawContext, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        if (player != null && player.getVehicle() instanceof PlaneEntity plane) {
            TextRenderer textRenderer = client.textRenderer;
            int screenWidth = drawContext.getScaledWindowWidth();
            int screenHeight = drawContext.getScaledWindowHeight();

            // Existing Speed Info
            String speedText = String.format("Speed: %.2f", plane.getCurrentSpeed());
            String takeoffSpeedText = String.format("Takeoff: %.2f", PlaneEntity.getTakeoffSpeedThreshold());
            String stallSpeedText = String.format("Stall: %.2f", PlaneEntity.getStallSpeedThreshold());

            drawContext.drawTextWithShadow(textRenderer, Text.literal(speedText), 10, 10, HUD_TEXT_COLOR);
            drawContext.drawTextWithShadow(textRenderer, Text.literal(takeoffSpeedText), 10, 20, HUD_TEXT_COLOR);
            drawContext.drawTextWithShadow(textRenderer, Text.literal(stallSpeedText), 10, 30, HUD_TEXT_COLOR);

            // Display Coal Level
            /*
            int coalLevel = plane.getCoalLevel();
            int maxCoalLevel = PlaneEntity.getMaxCoalLevel();

            String coalText = String.format("Coal: %d / %d", coalLevel, maxCoalLevel);
            // Change color if fuel is low (e.g., less than 10%)
            int coalColor = (coalLevel <= maxCoalLevel * 0.1) ? 0xFF5555 : HUD_TEXT_COLOR; // Red if low, else white
            drawContext.drawTextWithShadow(textRenderer, Text.literal(coalText), 10, 40, coalColor);
             */

            // GPS Info - Adjusted Y position to accommodate coal level text
            drawGpsInfo(drawContext, textRenderer, plane);

            // Altitude Indicator
            drawAltitudeIndicator(drawContext, client, plane, screenWidth, screenHeight);

            // SONAR Display
            drawSonarDisplay(drawContext, client, player, plane, screenWidth, screenHeight, tickDelta);
        }
    }

    private void drawGpsInfo(DrawContext drawContext, TextRenderer textRenderer, PlaneEntity plane) {
        final int x = 10;
        // Adjusted baseY to make space for the Coal Level text
        final int baseY = 50;
        String posXText = String.format("X: %.1f", plane.getX());
        String posYText = String.format("Y: %.1f (Alt)", plane.getY());
        String posZText = String.format("Z: %.1f", plane.getZ());

        drawContext.drawTextWithShadow(textRenderer, Text.literal(posXText), x, baseY, HUD_TEXT_COLOR);
        drawContext.drawTextWithShadow(textRenderer, Text.literal(posYText), x, baseY + 10, HUD_TEXT_COLOR);
        drawContext.drawTextWithShadow(textRenderer, Text.literal(posZText), x, baseY + 20, HUD_TEXT_COLOR);
    }

    private void drawAltitudeIndicator(DrawContext drawContext, MinecraftClient client, PlaneEntity plane, int screenWidth, int screenHeight) {
        int barWidth = 15;
        int barHeight = 100;
        int x = screenWidth - barWidth - 10;
        int y = (screenHeight - barHeight) / 2;

        // Background
        drawContext.fill(x, y, x + barWidth, y + barHeight, ALTITUDE_BG_COLOR);

        // Altitude representation
        float altitude = (float) plane.getY();
        float maxDisplayAltitude = 320.0f; // Max build height + a bit
        float minDisplayAltitude = -64.0f; // Min world depth
        float altitudeRange = maxDisplayAltitude - minDisplayAltitude;

        float normalizedAltitude = MathHelper.clamp((altitude - minDisplayAltitude) / altitudeRange, 0.0f, 1.0f);
        int fillHeight = (int) (normalizedAltitude * barHeight);

        drawContext.fill(x, y + barHeight - fillHeight, x + barWidth, y + barHeight, ALTITUDE_BAR_COLOR);

        // Scale markers (simple example)
        for (int i = 0; i <= 4; i++) {
            int markerY = y + (barHeight * i / 4);
            drawContext.drawHorizontalLine(x - 2, x, markerY, HUD_TEXT_COLOR);
        }
        if (client != null) { // Added null check for client
            drawContext.drawTextWithShadow(client.textRenderer, Text.literal(String.format("%.0fm", altitude)), x - 35, y + barHeight / 2 - 4, HUD_TEXT_COLOR);
        }
    }

    private void drawSonarDisplay(DrawContext drawContext, MinecraftClient client, ClientPlayerEntity player, PlaneEntity plane, int screenWidth, int screenHeight, float tickDelta) {
        int sonarSize = 80;
        int sonarRadius = sonarSize / 2;
        int centerX = screenWidth / 2;
        int centerY = screenHeight - sonarRadius - 10; // Positioned at bottom-center

        int x1 = centerX - sonarRadius;
        int y1 = centerY - sonarRadius;
        int x2 = centerX + sonarRadius;
        int y2 = centerY + sonarRadius;

        // Background
        drawContext.fill(x1, y1, x2, y2, SONAR_BG_COLOR);

        // Grid lines
        drawContext.drawHorizontalLine(x1, x2, centerY, SONAR_GRID_COLOR);
        drawContext.drawVerticalLine(centerX, y1, y2, SONAR_GRID_COLOR);

        // Concentric circles
        int numCircles = 3;
        for (int i = 1; i <= numCircles; i++) {
            int radius = sonarRadius * i / numCircles;
            drawContext.drawHorizontalLine(centerX - radius, centerX + radius, centerY - radius, SONAR_GRID_COLOR); // Top
            drawContext.drawHorizontalLine(centerX - radius, centerX + radius, centerY + radius, SONAR_GRID_COLOR); // Bottom
            drawContext.drawVerticalLine(centerX - radius, centerY - radius, centerY + radius, SONAR_GRID_COLOR);   // Left
            drawContext.drawVerticalLine(centerX + radius, centerY - radius, centerY + radius, SONAR_GRID_COLOR);   // Right
        }
        // Sweeping line & Entity Blips
        if (client != null && client.world != null) {
            long time = client.world.getTime();
            float angle = (time + tickDelta) * 6.0f % 360.0f;
            float radians = (float) Math.toRadians(angle - 90);

            int lineEndX = centerX + (int) (Math.cos(radians) * sonarRadius);
            int lineEndY = centerY + (int) (Math.sin(radians) * sonarRadius);

            // Simple line simulation with fill
            if (Math.abs(lineEndX - centerX) > Math.abs(lineEndY - centerY)) { // More horizontal
                int startX = Math.min(centerX, lineEndX);
                int endX = Math.max(centerX, lineEndX);
                if (lineEndX - centerX != 0) {
                    float slope = (float)(lineEndY - centerY) / (lineEndX - centerX);
                    for (int lx = startX; lx <= endX; lx++) {
                        int ly = Math.round(centerY + slope * (lx - centerX));
                        drawContext.fill(lx, ly, lx + 1, ly + 1, SONAR_LINE_COLOR);
                    }
                } else {
                    drawContext.fill(startX, centerY, endX + 1, centerY + 1, SONAR_LINE_COLOR);
                }
            } else { // More vertical or perfectly diagonal
                int startY = Math.min(centerY, lineEndY);
                int endY = Math.max(centerY, lineEndY);
                if (lineEndY - centerY != 0) {
                    float slope = (float)(lineEndX - centerX) / (lineEndY - centerY);
                    for (int ly = startY; ly <= endY; ly++) {
                        int lx = Math.round(centerX + slope * (ly - centerY));
                        drawContext.fill(lx, ly, lx + 1, ly + 1, SONAR_LINE_COLOR);
                    }
                } else {
                    drawContext.fill(centerX, startY, centerX + 1, endY + 1, SONAR_LINE_COLOR);
                }
            }

            // Entity Blips
            Predicate<net.minecraft.entity.Entity> sonarPredicate = (e) ->
                e != null && e.isAlive() && e != player && !(e instanceof net.minecraft.entity.decoration.ItemFrameEntity) &&
                !(e instanceof net.minecraft.entity.decoration.painting.PaintingEntity) &&
                !(e instanceof net.minecraft.entity.ItemEntity);

            List<net.minecraft.entity.Entity> nearbyEntities = client.world.getOtherEntities(plane, plane.getBoundingBox().expand(SONAR_RANGE), sonarPredicate);
            Vec3d planePos = plane.getPos();
            float planeYawRad = -plane.getYaw() * MathHelper.RADIANS_PER_DEGREE; // Negative for standard counter-clockwise rotation from +X

            for (net.minecraft.entity.Entity entity : nearbyEntities) {
                Vec3d entityPos = entity.getPos();
                Vec3d relativePosWorld = entityPos.subtract(planePos);

                // Ignore entities too far above or below for a 2D sonar representation
                if (Math.abs(relativePosWorld.y) > SONAR_RANGE / 2) continue;

                // Rotate relative position to plane's local coordinates (X right, Z forward)
                Vec3d localPos = relativePosWorld.rotateY(planeYawRad);

                double distSq = localPos.x * localPos.x + localPos.z * localPos.z;
                if (distSq <= SONAR_RANGE * SONAR_RANGE) {
                    int blipX = centerX + (int)((localPos.x / SONAR_RANGE) * sonarRadius);
                    int blipY = centerY - (int)((localPos.z / SONAR_RANGE) * sonarRadius);

                    double distToCenter = Math.sqrt(Math.pow(blipX - centerX, 2) + Math.pow(blipY - centerY, 2));
                    if (distToCenter <= sonarRadius) {
                         drawContext.fill(blipX - 1, blipY - 1, blipX + 1, blipY + 1, SONAR_BLIP_COLOR);
                    }
                }
            }
        }
    }
}
