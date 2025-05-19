package org.malek.minmod.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.malek.minmod.Minmod;
import org.malek.minmod.client.renderer.PlaneEntityRenderer;

public class MinmodClient implements ClientModInitializer {

    public static final Logger LOGGER = LogManager.getLogger("minmod-client");

    @Override
    public void onInitializeClient() {
        try {
            // Log initialization
            LOGGER.info("Minmod client initializing...");

            // Register tooltip callback for plane item
            registerTooltips();

            // Register plane entity renderer
            EntityRendererRegistry.register(Minmod.PLANE_ENTITY_TYPE, PlaneEntityRenderer::new);
            
            LOGGER.info("Minmod client initialized successfully!");
        } catch (Exception e) {
            LOGGER.error("Failed to initialize MinmodClient", e);
        }
    }
    
    private void registerTooltips() {
        try {
            ItemTooltipCallback.EVENT.register((stack, context, lines) -> {
                if (stack.getItem() == Minmod.PLANE_ITEM) {
                    // Additional tooltip info would be added here
                }
            });
        } catch (Exception e) {
            LOGGER.error("Failed to register tooltips", e);
        }
    }
}
