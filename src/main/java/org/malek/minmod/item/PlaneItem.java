package org.malek.minmod.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.malek.minmod.entity.PlaneEntity;
import org.malek.minmod.Minmod;

import java.util.List;

public class PlaneItem extends Item {
    private static final Logger LOGGER = LogManager.getLogger("minmod-plane");
    
    public PlaneItem(Settings settings) {
        super(settings);
    }
    
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        
        try {
            if (!world.isClient) {
                // Give a temporary flight boost when used
                player.setVelocity(player.getRotationVector().multiply(1.5));
                player.fallDistance = 0.0F;
                
                // Set cooldown
                player.getItemCooldownManager().set(this, 20);
            }
            return TypedActionResult.success(stack);
        } catch (Exception e) {
            LOGGER.error("Error when using PlaneItem", e);
            return TypedActionResult.pass(stack);
        }
    }
    
    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        try {
            tooltip.add(Text.literal("Right-click a block to place and ride the plane!"));
        } catch (Exception e) {
            LOGGER.error("Error appending tooltip for PlaneItem", e);
        }
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        if (!world.isClient) {
            Vec3d pos = context.getHitPos(); // New way, public access
            PlaneEntity plane = new PlaneEntity(Minmod.PLANE_ENTITY_TYPE, world);
            plane.refreshPositionAndAngles(pos.x, pos.y + 0.5, pos.z,
                context.getPlayer() != null ? context.getPlayer().getYaw() : 0, 0);
            world.spawnEntity(plane);
            if (context.getPlayer() != null) {
                context.getStack().decrement(1);
                context.getPlayer().startRiding(plane);
            }
        }
        return ActionResult.success(world.isClient);
    }
}
