package org.malek.minmod.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes; // Added import
import net.minecraft.entity.player.PlayerEntity;
import org.malek.minmod.Minmod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void minmod_onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        // Cast 'this' to LivingEntity. The mixin system makes 'this' refer to the targeted class instance.
        LivingEntity livingEntity = (LivingEntity) (Object) this;

        // Check if the damage source is fall damage and if the entity is a player
        if (source.isOf(DamageTypes.FALL) && livingEntity instanceof PlayerEntity player) { // Changed to isOf(DamageTypes.FALL)
            // Check if this player's UUID is in the set
            if (Minmod.CANCEL_NEXT_FALL_DAMAGE.contains(player.getUuid())) {
                // Cancel the damage
                cir.setReturnValue(false);
                // Remove the player from the set so this only cancels one instance of fall damage
                Minmod.CANCEL_NEXT_FALL_DAMAGE.remove(player.getUuid());
                Minmod.LOGGER.info("Mixin cancelled fall damage for player {} after exiting plane.", player.getName().getString());
            }
        }
    }
}

