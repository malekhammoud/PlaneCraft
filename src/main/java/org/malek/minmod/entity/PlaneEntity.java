package org.malek.minmod.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.malek.minmod.Minmod;

import java.util.List;

public class PlaneEntity extends Entity {
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
    private static final float LIFT_COEFFICIENT = 0.035f; // How much lift per unit of speed over threshold
    private static final float GRAVITY_PULL = 0.045f; // Custom gravity when flying
    private static final float INITIAL_LIFT_BOOST = 0.15f; // Small boost to get off ground

    public PlaneEntity(EntityType<? extends PlaneEntity> type, World world) {
        super(type, world);
        this.noClip = false; // Allow collisions
        this.setNoGravity(true); // We handle gravity manually
    }

    @Override
    public void tick() {
        super.tick(); // Handles basic entity ticking, including passenger updates via isVehicle check

        Entity controllingPassenger = this.getControllingPassenger();

        if (controllingPassenger instanceof LivingEntity) {
            LivingEntity rider = (LivingEntity) controllingPassenger;

            // --- Rotation ---
            // Yaw (turning) controlled by A/D (rider.sidewaysSpeed: +ve for A/left, -ve for D/right)
            float yawChange = -rider.sidewaysSpeed * YAW_CONTROL_SENSITIVITY;
            this.setYaw(this.getYaw() + yawChange);

            // Pitch controlled by mouse look (rider.getPitch(): +ve for looking down)
            // Plane pitch: +ve for nose down, -ve for nose up
            float targetRiderPitch = rider.getPitch() * 0.65f; // Dampen rider's pitch influence
            float currentPlanePitch = this.getPitch();
            float newPitch = currentPlanePitch + (targetRiderPitch - currentPlanePitch) * PITCH_INTERPOLATION_FACTOR;
            this.setPitch(Math.max(MIN_PITCH, Math.min(MAX_PITCH, newPitch))); // Clamp pitch

            // --- Speed and Thrust ---
            if (rider.forwardSpeed > 0.0F) { // W key
                currentSpeed += ACCELERATION * rider.forwardSpeed;
            } else if (rider.forwardSpeed < 0.0F) { // S key (brakes)
                currentSpeed += BRAKE_FORCE * rider.forwardSpeed; // rider.forwardSpeed is negative
            }

            // Natural deceleration/drag
            if (this.isOnGround()) {
                currentSpeed -= NATURAL_DECELERATION_GROUND;
            } else {
                currentSpeed -= NATURAL_DECELERATION_AIR;
            }

            // Clamp speed
            currentSpeed = Math.max(0, Math.min(currentSpeed, MAX_SPEED));

            // --- Calculate Velocity ---
            Vec3d forwardDirection = Vec3d.fromPolar(this.getPitch(), this.getYaw());
            Vec3d velocity = forwardDirection.multiply(currentSpeed);
            double vx = velocity.x;
            double vy = velocity.y; // Base Y velocity from pitch and speed
            double vz = velocity.z;

            // --- Lift and Gravity ---
            if (!this.isOnGround()) {
                // Apply lift if speed is above takeoff threshold
                if (currentSpeed > TAKEOFF_SPEED_THRESHOLD) {
                    vy += (currentSpeed - TAKEOFF_SPEED_THRESHOLD) * LIFT_COEFFICIENT;
                }
                // Apply custom gravity
                vy -= GRAVITY_PULL;
            } else {
                // On ground behavior
                // Check for takeoff: sufficient speed, W pressed, and pitching up
                if (currentSpeed > TAKEOFF_SPEED_THRESHOLD && rider.forwardSpeed > 0.0F && this.getPitch() < -2.0f) {
                    vy += INITIAL_LIFT_BOOST; // Initial jump to get off ground
                } else {
                     // If not taking off, ensure Y velocity doesn't pull down through ground from previous frame's calculation
                    vy = Math.max(0, vy);
                }
            }

            this.setVelocity(vx, vy, vz);

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

        // Update passenger positions with custom logic (especially for yaw)
        for (Entity passenger : this.getPassengerList()) {
            customUpdatePassengerPosition(passenger);
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
        return passengers.isEmpty() ? null : (passengers.get(0) instanceof LivingEntity) ? (LivingEntity) passengers.get(0) : null;
    }

    // Prevent passengers from being ejected easily on collision
    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengerList().size() < 1; // Only one passenger
    }

    public double getMountedHeightOffset() {
        return 0.1; // Adjust passenger seating position if needed
    }

    // To ensure inputs are processed correctly from the passenger
    public void customUpdatePassengerPosition(Entity passenger) { // Renamed from updatePassengerPosition
        if (this.hasPassenger(passenger)) {
            // Standard boat-like positioning, adjust Y offset as needed
            float yOffset;
            if (this.isRemoved()) {
                yOffset = 0.01f;
            } else {
                // Use passenger's dimensions to get height
                double passengerHeightOffset = passenger.getDimensions(passenger.getPose()).height;
                yOffset = (float) (this.getMountedHeightOffset() + passengerHeightOffset);
            }

            // Simple positioning for now, can be refined
            passenger.setPosition(this.getX(), this.getY() + yOffset, this.getZ());

            // Sync passenger rotation with plane's yaw, allow independent pitch
            passenger.setYaw(this.getYaw());
            // passenger.setPitch(this.getPitch()); // Uncomment if you want passenger pitch locked to plane pitch
        }
    }
}
