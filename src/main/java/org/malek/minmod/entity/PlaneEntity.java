package org.malek.minmod.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.damage.DamageSource;
import org.malek.minmod.Minmod;
import org.apache.logging.log4j.LogManager; // Added import
import org.apache.logging.log4j.Logger;    // Added import

import java.util.List;

public class PlaneEntity extends Entity {
    public static final Logger LOGGER = LogManager.getLogger("MinModPlaneEntity"); // Added logger

    // Movement Parameters
    private float currentSpeed = 0.0f;
    private static final float MAX_SPEED = 1.8f; // Max speed in blocks/tick
    private static final float ACCELERATION = 0.025f;
    private static final float BRAKE_FORCE = 0.06f;
    private static final float NATURAL_DECELERATION_AIR = 0.008f;
    private static final float NATURAL_DECELERATION_GROUND = 0.025f;
    private static final float YAW_CONTROL_SENSITIVITY = 2.8f; // Degrees per tick
    private static final float PITCH_INTERPOLATION_FACTOR = 0.1f; // How quickly plane's pitch follows player's look
    private static final float MAX_PITCH = 45.0f; // Max nose up/down degrees
    private static final float MIN_PITCH = -45.0f;

    private static final float TAKEOFF_SPEED_THRESHOLD = 0.75f; // Speed needed to start lifting
    private static final float STALL_SPEED_THRESHOLD = TAKEOFF_SPEED_THRESHOLD * 0.8f; // Speed below which plane stalls
    private static final float LIFT_COEFFICIENT = 0.035f; // How much lift per unit of speed over threshold
    private static final float GRAVITY_PULL = 0.045f; // Custom gravity when flying
    private static final float STALL_DESCENT_RATE = 0.03f; // Additional downward pull when stalled
    private static final float INITIAL_LIFT_BOOST = 0.15f; // Small boost to get off ground

    public PlaneEntity(EntityType<? extends PlaneEntity> type, World world) {
        super(type, world);
        this.noClip = false; // Allow collisions
        this.setNoGravity(true); // We handle gravity manually
        this.intersectionChecked = true; // Use getBoundingBox() for raycasting (like boats)
    }

    @Override
    public boolean isCollidable() {
        return true; // Explicitly state that this entity is collidable
    }

    public boolean canHit() {
        return !this.isRemoved(); // Explicitly allow the entity to be "hit" by ray tracing
    }

    public boolean canBeCollidedWith() {
        return !this.isRemoved(); // Allow the entity to be collided with by other entities
    }

    @Override
    public void tick() {
        super.tick(); // Handles basic entity ticking, including passenger updates via isVehicle check

        Entity controllingPassenger = this.getControllingPassenger();
        Vec3d positionAtTickStart = this.getPos(); // Store position at the very start of tick logic

        if (controllingPassenger instanceof LivingEntity rider) {
            float speedAtTickStart = currentSpeed; // Speed from end of last tick / start of this tick's logic

            // --- Rotation ---
            float yawChange = -rider.sidewaysSpeed * YAW_CONTROL_SENSITIVITY;
            this.setYaw(this.getYaw() + yawChange);
            float targetRiderPitch = rider.getPitch() * 0.65f;
            float currentPlanePitch = this.getPitch();
            float newPitch = currentPlanePitch + (targetRiderPitch - currentPlanePitch) * PITCH_INTERPOLATION_FACTOR;
            this.setPitch(Math.max(MIN_PITCH, Math.min(MAX_PITCH, newPitch)));

            // --- Tentative Speed Update from Player Input & Drag ---
            float tentativeCurrentSpeed = speedAtTickStart;
            boolean attemptedPositiveAcceleration = false;
            if (rider.forwardSpeed > 0.0F) { // W key
                tentativeCurrentSpeed += ACCELERATION * rider.forwardSpeed;
                attemptedPositiveAcceleration = true;
            } else if (rider.forwardSpeed < 0.0F) { // S key (brakes)
                tentativeCurrentSpeed += BRAKE_FORCE * rider.forwardSpeed;
            }

            if (this.isOnGround()) {
                tentativeCurrentSpeed -= NATURAL_DECELERATION_GROUND;
            } else {
                tentativeCurrentSpeed -= NATURAL_DECELERATION_AIR;
            }
            tentativeCurrentSpeed = Math.max(0, Math.min(tentativeCurrentSpeed, MAX_SPEED));

            // --- Calculate Velocity for movement based on tentativeCurrentSpeed ---
            Vec3d forwardDirForVel = Vec3d.fromPolar(this.getPitch(), this.getYaw());
            Vec3d velocityForMove = forwardDirForVel.multiply(tentativeCurrentSpeed);
            double vx = velocityForMove.getX();
            double vy = velocityForMove.getY();
            double vz = velocityForMove.getZ();

            // --- Lift and Gravity based on tentativeCurrentSpeed ---
            if (!this.isOnGround()) {
                if (tentativeCurrentSpeed > TAKEOFF_SPEED_THRESHOLD) {
                    vy += (tentativeCurrentSpeed - TAKEOFF_SPEED_THRESHOLD) * LIFT_COEFFICIENT;
                } else if (tentativeCurrentSpeed < STALL_SPEED_THRESHOLD && tentativeCurrentSpeed > 0) { // Added > 0 check for stall
                    vy -= STALL_DESCENT_RATE * (1.0f - (tentativeCurrentSpeed / STALL_SPEED_THRESHOLD));
                }
                vy -= GRAVITY_PULL;
            } else { // On ground behavior
                if (tentativeCurrentSpeed > TAKEOFF_SPEED_THRESHOLD && rider.forwardSpeed > 0.0F && this.getPitch() < -2.0f) {
                    vy += INITIAL_LIFT_BOOST;
                } else {
                    vy = Math.max(0, vy); // Prevent sinking from pitch if not taking off
                }
            }
            this.setVelocity(vx, vy, vz);

            // --- Perform Movement ---
            this.move(MovementType.SELF, this.getVelocity());

            // --- Determine final currentSpeed for this tick based on outcome ---
            Vec3d positionAfterMove = this.getPos(); // Position after this tick's move call
            Vec3d actualDisplacementThisTick = positionAfterMove.subtract(positionAtTickStart);

            if (attemptedPositiveAcceleration && this.isOnGround()) {
                Vec3d horizontalForwardDir = Vec3d.fromPolar(0, this.getYaw()).normalize();
                double actualForwardDistance = actualDisplacementThisTick.dotProduct(horizontalForwardDir);
                double minForwardMovementThreshold = 0.01; // If moved less than this, consider blocked

                if (rider.forwardSpeed > 0.0F && actualForwardDistance < minForwardMovementThreshold && tentativeCurrentSpeed > speedAtTickStart) {
                    // Blocked and was trying to accelerate: currentSpeed should be based on speedAtTickStart + drag, no acceleration benefit
                    currentSpeed = speedAtTickStart; // Revert to speed before this tick's acceleration attempt
                    if (this.isOnGround()) { // Re-apply drag for this tick as acceleration didn't count
                        currentSpeed -= NATURAL_DECELERATION_GROUND;
                    }
                    currentSpeed = Math.max(0, Math.min(currentSpeed, MAX_SPEED)); // Re-clamp
                } else {
                    // Not blocked, or no positive acceleration that would increase speed was effectively attempted:
                    currentSpeed = tentativeCurrentSpeed;
                }
            } else {
                // Not attempting positive acceleration, or not on ground:
                currentSpeed = tentativeCurrentSpeed;
            }

        } else { // No passenger or passenger not LivingEntity
            if (this.isOnGround()) {
                currentSpeed -= NATURAL_DECELERATION_GROUND * 1.5f; // Faster stop on ground
            } else {
                currentSpeed -= NATURAL_DECELERATION_AIR;
            }
            currentSpeed = Math.max(0, currentSpeed);

            Vec3d oldVel = this.getVelocity();
            double vy = oldVel.y;
            if (!this.isOnGround()) {
                vy -= GRAVITY_PULL;
                if (currentSpeed < STALL_SPEED_THRESHOLD) { // Also apply stall descent if no passenger and slow
                    vy -= STALL_DESCENT_RATE * (1.0f - (currentSpeed / STALL_SPEED_THRESHOLD));
                }
            } else {
                vy = 0; // Stick to ground
            }

            if (currentSpeed > 0) {
                Vec3d forwardDirection = Vec3d.fromPolar(this.getPitch(), this.getYaw());
                Vec3d newHorizontalVel = forwardDirection.multiply(currentSpeed);
                this.setVelocity(newHorizontalVel.x, vy, newHorizontalVel.z);
            } else {
                this.setVelocity(oldVel.x * 0.8, vy, oldVel.z * 0.8); // Dampen horizontal if no speed
            }

            if (this.isOnGround() && this.getVelocity().lengthSquared() < 0.0001) {
                this.setVelocity(Vec3d.ZERO);
                currentSpeed = 0;
            }
        }

        this.velocityModified = true; // Important to tell the game we've changed velocity

        // Apply calculated velocity, handle collisions, and update onGround status
        this.move(MovementType.SELF, this.getVelocity());

        // After moving, if on ground, ensure Y velocity is truly zero to prevent sinking/bouncing
        if (this.isOnGround()) {
            this.setVelocity(this.getVelocity().x, 0, this.getVelocity().z);
        }
    }

    @Override
    protected void initDataTracker() {
        // No data to track for now
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.contains("CurrentSpeed", NbtCompound.FLOAT_TYPE)) {
            currentSpeed = nbt.getFloat("CurrentSpeed");
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putFloat("CurrentSpeed", currentSpeed);
    }

    @Override
    public boolean isPushable() {
        return true; // Like a boat
    }

    @Override
    public EntityDimensions getDimensions(EntityPose pose) {
        return EntityDimensions.fixed(1.5f, 0.5f);
    }

    // Make the plane rideable
    public boolean canBeControlledByRider() {
        return true;
    }

    @Override
    public LivingEntity getControllingPassenger() {
        List<Entity> passengers = this.getPassengerList();
        if (passengers.isEmpty()) {
            return null;
        }
        Entity firstPassenger = passengers.get(0);
        return (firstPassenger instanceof LivingEntity) ? (LivingEntity) firstPassenger : null;
    }

    // Prevent passengers from being ejected easily on collision
    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengerList().isEmpty();
    }

    public double getMountedHeightOffset() {
        return 0.1; // Adjust passenger seating position if needed
    }

    @Override
    protected void updatePassengerPosition(Entity passenger, Entity.PositionUpdater positionUpdater) {
        if (this.hasPassenger(passenger)) {
            // Calculate the Y position for the passenger.
            // this.getMountedHeightOffset() is the offset from the plane's origin to the seat.
            double yPos = this.getY() + this.getMountedHeightOffset();

            // Use the positionUpdater to set the passenger's position.
            // For a simple plane, the passenger is centered on the plane's X and Z.
            positionUpdater.accept(passenger, this.getX(), yPos, this.getZ());

            // Sync passenger's yaw with the plane's yaw.
            // This ensures the passenger (and their camera) faces the same direction as the plane horizontally.
            float planeYaw = this.getYaw();
            passenger.setYaw(planeYaw);
            passenger.setBodyYaw(planeYaw); // Sync body yaw as well for better player model rendering.

            // Passenger's pitch is typically controlled by their own input (mouse look).
            // We are not explicitly setting passenger pitch here, allowing them to look up and down freely.
        }
    }

    // Getter for currentSpeed for HUD
    public float getCurrentSpeed() {
        return this.currentSpeed;
    }

    // Static getter for TAKEOFF_SPEED_THRESHOLD for HUD
    public static float getTakeoffSpeedThreshold() {
        return TAKEOFF_SPEED_THRESHOLD;
    }

    // Static getter for STALL_SPEED_THRESHOLD for HUD
    public static float getStallSpeedThreshold() {
        return STALL_SPEED_THRESHOLD;
    }

    public void removePassenger(Entity passenger) {
        if (this.isOnGround()) {
            super.removePassenger(passenger);
            return;
        }

        World world = this.getWorld();
        boolean safeToDismount = false;
        // Check current block and 3 blocks down
        for (int i = 0; i <= 3; i++) {
            BlockPos posBeneath = this.getBlockPos().down(i);
            if (!world.getBlockState(posBeneath).isAir() && world.getBlockState(posBeneath).isSolidBlock(world, posBeneath)) {
                safeToDismount = true;
                break;
            }
        }

        if (safeToDismount) {
            super.removePassenger(passenger);
        } else {
            if (passenger instanceof PlayerEntity && !world.isClient()) {
                ((PlayerEntity) passenger).sendMessage(Text.literal("Too high to dismount!"), true);
            }
            // If not safe, do not call super.removePassenger(passenger), thus preventing dismount.
            if (!world.isClient()) {
                passenger.startRiding(this, true);
            }
        }
    }

    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        // Forceful logging at the start
        LOGGER.error("!!! PlaneEntity INTERACT method CALLED. Player: {} ({}), Hand: {}, World is client: {}", player.getName().getString(), player.getUuidAsString(), hand, this.getWorld().isClient);
        System.out.println("!!! PlaneEntity INTERACT method CALLED VIA STDOUT. Player: " + player.getName().getString() + ", World is client: " + this.getWorld().isClient);

        if (player.isSneaking()) {
            LOGGER.info("Player is sneaking. Passing interaction.");
            return ActionResult.PASS;
        }
        if (this.getPassengerList().isEmpty()) {
            LOGGER.info("Plane is empty.");
            if (!this.getWorld().isClient) {
                LOGGER.info("Executing startRiding on server for player {}.", player.getName().getString());
                boolean success = player.startRiding(this);
                LOGGER.info("startRiding call returned: {}", success);
                if(success) return ActionResult.SUCCESS;
                else return ActionResult.FAIL;
            }
            return ActionResult.SUCCESS;
        } else {
            LOGGER.info("Plane is not empty. Current passengers: {}. First passenger: {}", this.getPassengerList().size(), (this.getPassengerList().isEmpty() ? "N/A" : this.getPassengerList().get(0).getName().getString()));
        }
        return ActionResult.PASS;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        // Forceful logging at the start
        LOGGER.error("!!! PlaneEntity DAMAGE method CALLED. Source: {}, Amount: {}, World is client: {}", source.getName(), amount, this.getWorld().isClient);
        System.out.println("!!! PlaneEntity DAMAGE method CALLED VIA STDOUT. Source: " + source.getName() + ", World is client: " + this.getWorld().isClient);

        if (this.getWorld().isClient || this.isRemoved()) {
            LOGGER.info("Damage check: Is client or removed. Returning false.");
            return false;
        }

        Entity attacker = source.getAttacker();
        if (attacker instanceof PlayerEntity attackerPlayer) {
            LOGGER.info("Damage check: Attacker is PlayerEntity: {}", attackerPlayer.getName().getString());
            if (!attackerPlayer.getAbilities().creativeMode) {
                LOGGER.info("Damage check: Player {} not in creative. Dropping item.", attackerPlayer.getName().getString());
                this.dropStack(new ItemStack(Minmod.PLANE_ITEM));
            } else {
                LOGGER.info("Damage check: Player {} in creative. Not dropping item.", attackerPlayer.getName().getString());
            }
        } else {
            LOGGER.info("Damage check: Attacker is not PlayerEntity or is null. Actual attacker: {}", (attacker != null ? attacker.getClass().getName() : "null"));
        }

        this.discard();
        LOGGER.info("PlaneEntity discarded.");
        return true;
    }
}
