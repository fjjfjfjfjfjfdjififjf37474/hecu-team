package com.hecuteam.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Simplified HECU vehicles with distinct Apache and Bradley combat behavior. */
public class HecuVehicleEntity extends PathfinderMob {
    public enum Vehicle { APACHE, BRADLEY }

    private final Vehicle vehicle;
    private final boolean hostile;
    private int fireCooldown;
    private int movementTick;

    public boolean isHostileVariant() { return hostile; }
    public Vehicle getVehicleKind() { return vehicle; }

    public HecuVehicleEntity(EntityType<? extends HecuVehicleEntity> type, Level level, Vehicle vehicle, boolean hostile) {
        super(type, level);
        this.vehicle = vehicle;
        this.hostile = hostile;
        this.noPhysics = vehicle == Vehicle.APACHE;
        this.xpReward = 20;
    }

    public static AttributeSupplier.Builder createAttributes(double health) {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, health)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.FOLLOW_RANGE, 64)
                .add(Attributes.ARMOR, 12)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, this::validTarget));
    }

    private boolean validTarget(LivingEntity target) {
        if (!target.isAlive() || target == this || target instanceof Animal) return false;
        if (target instanceof HecuVehicleEntity other) return other.hostile != hostile;
        if (target instanceof HecuSoldierEntity soldier) return soldier.isHostileVariant() != hostile;
        if (target instanceof Player && !hostile) return false;
        return hostile || target instanceof Monster;
    }

    @Override public boolean isAlliedTo(Entity other) {
        if (other == this) return true;
        if (other instanceof HecuVehicleEntity vehicleEntity && vehicleEntity.hostile == hostile) return true;
        if (other instanceof HecuSoldierEntity soldier && soldier.isHostileVariant() == hostile) return true;
        return super.isAlliedTo(other);
    }

    @Override public boolean canAttack(LivingEntity target) {
        return validTarget(target) && super.canAttack(target);
    }

    @Override public void tick() {
        super.tick();
        if (level().isClientSide) return;
        movementTick++;
        if (fireCooldown > 0) fireCooldown--;

        LivingEntity target = getTarget();
        if (vehicle == Vehicle.APACHE) tickApache(target);
        else tickBradley(target);

        if (target != null && target.isAlive()) {
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (distanceTo(target) < 64 && hasLineOfSight(target) && fireCooldown <= 0) {
                attackTarget(target);
            }
        }
    }

    private void tickApache(LivingEntity target) {
        noPhysics = true;
        // Hover well above the terrain; descend somewhat when engaging a target.
        double desiredY = target == null
                ? Math.max(level().getSeaLevel() + 18.0, 30.0)
                : Math.max(target.getY() + 10.0, level().getSeaLevel() + 8.0);
        double dy = clamp((desiredY - getY()) * 0.018, -0.16, 0.16);
        double dx = 0.0;
        double dz = 0.0;
        if (target != null && movementTick % 3 == 0) {
            Vec3 toward = target.position().subtract(position());
            double horizontal = Math.sqrt(toward.x * toward.x + toward.z * toward.z);
            // Circle/approach at range instead of sitting directly over the target.
            if (horizontal > 18.0) {
                dx = clamp(toward.x * 0.006, -0.10, 0.10);
                dz = clamp(toward.z * 0.006, -0.10, 0.10);
            } else if (horizontal < 11.0) {
                dx = clamp(-toward.x * 0.008, -0.10, 0.10);
                dz = clamp(-toward.z * 0.008, -0.10, 0.10);
            } else {
                dx = clamp(-toward.z * 0.004, -0.07, 0.07);
                dz = clamp(toward.x * 0.004, -0.07, 0.07);
            }
        }
        setDeltaMovement(getDeltaMovement().scale(0.78).add(dx, dy, dz));
    }

    private void tickBradley(LivingEntity target) {
        noPhysics = false;
        if (target == null) return;
        double distance = distanceTo(target);
        if (distance > 19.0) {
            getNavigation().moveTo(target, 0.75);
        } else if (distance < 8.0 && movementTick % 10 == 0) {
            // Give the cannon some room rather than driving into infantry.
            Vec3 away = position().subtract(target.position()).normalize();
            getNavigation().moveTo(getX() + away.x * 5.0, getY(), getZ() + away.z * 5.0, 0.7);
        } else if (movementTick % 20 == 0) {
            getNavigation().stop();
        }
    }

    private void attackTarget(LivingEntity target) {
        boolean alliesNearTarget = level().getEntitiesOfClass(LivingEntity.class,
                target.getBoundingBox().inflate(vehicle == Vehicle.APACHE ? 5.0 : 3.0),
                entity -> entity != target && isAlliedTo(entity)).size() > 0;
        boolean allyInFiringLane = hasAllyInFiringLane(target);

        if (vehicle == Vehicle.APACHE) {
            if (!alliesNearTarget && !allyInFiringLane) {
                // Rocket strike: stronger direct hit plus limited splash damage.
                target.hurt(damageSources().mobAttack(this), 24.0F);
                for (LivingEntity nearby : level().getEntitiesOfClass(LivingEntity.class,
                        target.getBoundingBox().inflate(4.0), entity -> entity != target && validTarget(entity) && !isAlliedTo(entity))) {
                    nearby.hurt(damageSources().mobAttack(this), 10.0F);
                }
                fireCooldown = 50;
            } else {
                // Use the Apache's machine gun when a rocket could endanger nearby allies.
                if (!allyInFiringLane) target.hurt(damageSources().mobAttack(this), 7.0F);
                fireCooldown = 8;
            }
        } else {
            // Bradley cannon: hold fire if a friendly unit is in the line or blast area.
            if (!alliesNearTarget && !allyInFiringLane) {
                target.hurt(damageSources().mobAttack(this), 22.0F);
                for (LivingEntity nearby : level().getEntitiesOfClass(LivingEntity.class,
                        target.getBoundingBox().inflate(2.5), entity -> entity != target && validTarget(entity) && !isAlliedTo(entity))) {
                    nearby.hurt(damageSources().mobAttack(this), 8.0F);
                }
            }
            fireCooldown = 28;
        }
    }

    private boolean hasAllyInFiringLane(LivingEntity target) {
        Vec3 start = position().add(0.0, getBbHeight() * 0.65, 0.0);
        Vec3 end = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
        AABB searchBox = getBoundingBox().expandTowards(end.subtract(start)).inflate(1.5);
        return level().getEntitiesOfClass(LivingEntity.class, searchBox,
                entity -> entity != this && entity != target && isAlliedTo(entity))
                .stream().anyMatch(entity -> distanceToSegmentSquared(entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0), start, end) < 2.25);
    }

    private static double distanceToSegmentSquared(Vec3 point, Vec3 start, Vec3 end) {
        Vec3 segment = end.subtract(start);
        double lengthSquared = segment.lengthSqr();
        if (lengthSquared < 1.0E-7) return point.distanceToSqr(start);
        double t = clamp(point.subtract(start).dot(segment) / lengthSquared, 0.0, 1.0);
        return point.distanceToSqr(start.add(segment.scale(t)));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
