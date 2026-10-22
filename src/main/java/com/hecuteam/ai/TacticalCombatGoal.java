package com.hecuteam.ai;

import com.hecuteam.entity.HecuSoldierEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import com.hecuteam.registry.HECUSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.EnumSet;

/** Tactical ranged combat: spacing, lateral flanking, cover attempts, and ally-safe firing. */
public class TacticalCombatGoal extends Goal {
    private final HecuSoldierEntity mob;
    private final double speed, range, damage;
    private int shotCooldown;
    private int strafeDirection = 1;
    private int strafeTicks;
    private int coverCheckCooldown;
    private Vec3 coverPosition;

    public TacticalCombatGoal(HecuSoldierEntity mob, double speed, double range, double damage) {
        this.mob = mob;
        this.speed = speed;
        this.range = range;
        this.damage = damage;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override public boolean canUse() {
        return mob.getTarget() != null && mob.getTarget().isAlive() && mob.isValidEnemy(mob.getTarget());
    }

    @Override public boolean canContinueToUse() { return canUse(); }
    @Override public void start() { shotCooldown = 10; strafeTicks = 0; coverCheckCooldown = 0; coverPosition = null; }

    @Override public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null) return;
        mob.getLookControl().setLookAt(target, 30, 30);
        double distance = mob.distanceTo(target);

        if (coverCheckCooldown > 0) coverCheckCooldown--;
        if (mob.getHealth() < mob.getMaxHealth() * 0.45F && coverCheckCooldown == 0) {
            coverPosition = findCover(target);
            coverCheckCooldown = 50;
        }
        if (coverPosition != null && mob.getHealth() < mob.getMaxHealth() * 0.60F) {
            if (mob.distanceToSqr(coverPosition.x, coverPosition.y, coverPosition.z) > 2.25) {
                mob.getNavigation().moveTo(coverPosition.x, coverPosition.y, coverPosition.z, speed * 1.15);
            } else {
                mob.getNavigation().stop();
                // Stay behind cover while hurt; retry a flanking move when healed.
                if (mob.getHealth() >= mob.getMaxHealth() * 0.60F) coverPosition = null;
            }
        } else {
            coverPosition = null;
            if (distance > range * 0.85) {
                mob.getNavigation().moveTo(target, speed);
            } else if (distance < range * 0.45) {
                Vec3 away = mob.position().subtract(target.position()).normalize();
                mob.getNavigation().moveTo(mob.getX() + away.x * 5, mob.getY(), mob.getZ() + away.z * 5, speed);
            } else {
                if (++strafeTicks > 25) { strafeDirection *= -1; strafeTicks = 0; }
                Vec3 toward = target.position().subtract(mob.position()).normalize();
                Vec3 side = new Vec3(-toward.z, 0, toward.x).scale(strafeDirection * 0.45);
                if (mob.getRandom().nextInt(5) == 0) mob.getNavigation().moveTo(mob.getX() + side.x * 3, mob.getY(), mob.getZ() + side.z * 3, speed);
                else if (mob.getNavigation().isDone()) mob.getNavigation().stop();
            }
        }

        if (shotCooldown > 0) shotCooldown--;
        if (distance <= range && shotCooldown <= 0 && mob.hasLineOfSight(target) && isShotClearOfAllies(target)) {
            float shotDamage = (float) damage;
            if (mob.getRole() == com.hecuteam.entity.SoldierRole.SHOTGUNNER) {
                // A short-range buckshot burst: strong up close, weaker at the edge of the weapon's range.
                shotDamage *= distance < 6.0 ? 4.5F : 2.0F;
            }
            target.hurt(mob.damageSources().mobAttack(mob), shotDamage);
            Vec3 muzzle = mob.getEyePosition().add(mob.getLookAngle().scale(0.45));
            if (mob.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 2, 0.025, 0.025, 0.025, 0.005);
            }
            float pitch = 0.82F + mob.getRandom().nextFloat() * 0.28F;
            mob.level().playSound(null, mob.blockPosition(),
                    mob.getRole() == com.hecuteam.entity.SoldierRole.SHOTGUNNER ? HECUSounds.WEAPON_SHOTGUN.get()
                    : (mob.getRole() == com.hecuteam.entity.SoldierRole.SQUAD_LEADER ? HECUSounds.WEAPON_PISTOL.get()
                    : HECUSounds.WEAPON_AUTO.get()), SoundSource.HOSTILE,
                    mob.getRole() == com.hecuteam.entity.SoldierRole.MACHINE_GUNNER ? 0.62F : 0.78F, pitch);
            shotCooldown = switch (mob.getRole()) {
                case SHOTGUNNER -> 24;
                case MACHINE_GUNNER -> 3;
                case STORMTROOPER -> 4;
                case SQUAD_LEADER -> 12;
                case MEDIC -> 10;
            };
            mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        }
    }

    /** Finds a nearby reachable-looking position where a block breaks line of sight to the target. */
    private Vec3 findCover(LivingEntity target) {
        Vec3 targetEye = target.getEyePosition();
        for (int radius = 3; radius <= 6; radius++) {
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2.0 * i / 12.0) + mob.getRandom().nextDouble() * 0.18;
                double x = mob.getX() + Math.cos(angle) * radius;
                double z = mob.getZ() + Math.sin(angle) * radius;
                BlockPos feet = BlockPos.containing(x, mob.getY(), z);
                if (!mob.level().getBlockState(feet).isAir() || mob.level().getBlockState(feet.below()).isAir()) continue;
                Vec3 candidateEye = new Vec3(x, feet.getY() + 1.5, z);
                HitResult hit = mob.level().clip(new ClipContext(candidateEye, targetEye, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mob));
                if (hit.getType() == HitResult.Type.BLOCK) return new Vec3(x, feet.getY(), z);
            }
        }
        return null;
    }

    /** Prevents hits when a friendly soldier or the commanding player is standing in the shot lane. */
    private boolean isShotClearOfAllies(LivingEntity target) {
        Vec3 start = mob.getEyePosition();
        Vec3 end = target.getEyePosition();
        Vec3 line = end.subtract(start);
        double lengthSqr = line.lengthSqr();
        if (lengthSqr < 0.001) return true;
        var searchBox = mob.getBoundingBox().expandTowards(line).inflate(0.8);
        for (Entity candidate : mob.level().getEntities(mob, searchBox, e -> e.isAlive() && e != target && mob.isAlliedTo(e))) {
            Vec3 toCandidate = candidate.position().add(0, candidate.getBbHeight() * 0.5, 0).subtract(start);
            double t = Math.max(0.0, Math.min(1.0, toCandidate.dot(line) / lengthSqr));
            Vec3 closest = start.add(line.scale(t));
            double safeRadius = Math.max(0.45, candidate.getBbWidth() * 0.65);
            if (candidate.position().add(0, candidate.getBbHeight() * 0.5, 0).distanceToSqr(closest) < safeRadius * safeRadius) return false;
        }
        return true;
    }
}
