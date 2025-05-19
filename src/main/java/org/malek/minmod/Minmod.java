package org.malek.minmod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.malek.minmod.entity.PlaneEntity;
import org.malek.minmod.item.PlaneItem;

public class Minmod implements ModInitializer {

    public static final Logger LOGGER = LogManager.getLogger("minmod");
    public static final String MOD_ID = "minmod";
    
    // Register the plane entity type
    public static final EntityType<PlaneEntity> PLANE_ENTITY_TYPE = Registry.register(
        Registries.ENTITY_TYPE,
        new Identifier(MOD_ID, "plane"),
        FabricEntityTypeBuilder.create(SpawnGroup.MISC, PlaneEntity::new)
            .dimensions(EntityDimensions.fixed(1.5f, 0.5f))
            .build()
    );

    // Item Registration
    public static final PlaneItem PLANE_ITEM = new PlaneItem(new Item.Settings().maxCount(1));

    @Override
    public void onInitialize() {
        LOGGER.info("MinMod initializing - preparing plane functionality!");
        
        try {
            // Register items
            registerItems();
            LOGGER.info("MinMod initialized successfully!");
        } catch (Exception e) {
            LOGGER.error("Failed to initialize MinMod", e);
        }
    }
    
    private void registerItems() {
        try {
            Registry.register(Registries.ITEM, new Identifier(MOD_ID, "plane_item"), PLANE_ITEM);
            
            // Add the plane item to the tools item group
            ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(content -> {
                content.add(PLANE_ITEM);
            });
            
            LOGGER.info("Items registered successfully!");
        } catch (Exception e) {
            LOGGER.error("Failed to register items", e);
        }
    }
}
