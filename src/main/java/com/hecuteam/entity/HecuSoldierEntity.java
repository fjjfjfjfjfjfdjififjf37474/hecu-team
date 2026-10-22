package com.hecuteam.entity;

import com.hecuteam.ai.TacticalCombatGoal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import com.hecuteam.registry.HECUItems;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import java.util.UUID;

/** HECU infantry with persistent command/squad assignment and support behavior. */
public class HecuSoldierEntity extends PathfinderMob {
    private final SoldierRole role;
    private final boolean hostile;
    private UUID commander;
    private UUID squadLeader;
    private int medicCooldown;

    public HecuSoldierEntity(EntityType<? extends HecuSoldierEntity> type, Level level, SoldierRole role, boolean hostile) {
        super(type, level);
        this.role = role;
        this.hostile = hostile;
        this.xpReward = 8;
        // Equip a role-appropriate HECU weapon so the standard humanoid renderer displays it in hand.
        switch (role) {
            case SHOTGUNNER -> setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(HECUItems.SPAS12.get()));
            case MACHINE_GUNNER -> setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(HECUItems.M249.get()));
            case STORMTROOPER -> setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(HECUItems.MP5.get()));
            case SQUAD_LEADER, MEDIC -> setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(HECUItems.DESERT_EAGLE.get()));
        }
    }

    public SoldierRole getRole() { return role; }
    public boolean isHostileVariant() { return hostile; }
    public UUID getCommander() { return commander; }
    public void setCommander(UUID id) { commander = id; }
    public UUID getSquadLeader() { return squadLeader; }
    public void setSquadLeader(UUID id) { squadLeader = id; }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.MOVEMENT_SPEED, 0.30)
                .add(Attributes.FOLLOW_RANGE, 40.0).add(Attributes.ARMOR, 4.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.15);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new TacticalCombatGoal(this, 1.0, role.range, role.damage));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 12.0F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, this::isValidEnemy));
    }

    /** Hostile HECU attack living targets except animals and their own faction/squad. Friendly HECU engage monsters only. */
    public boolean isValidEnemy(LivingEntity target) {
        if (target == this || !target.isAlive() || target instanceof Animal) return false;
        if (target instanceof HecuSoldierEntity soldier && isSameFactionOrSquad(soldier)) return false;
        if (target instanceof HecuVehicleEntity vehicle && vehicle.isHostileVariant() == hostile) return false;
        if (commander != null && target.getUUID().equals(commander)) return false;
        if (target instanceof Player && !hostile) return false;
        return hostile || target instanceof Monster;
    }

    private boolean isSameFactionOrSquad(HecuSoldierEntity other) {
        if (other.hostile == hostile) return true;
        if (commander != null && commander.equals(other.commander)) return true;
        if (squadLeader != null && squadLeader.equals(other.squadLeader)) return true;
        return getUUID().equals(other.squadLeader) || other.getUUID().equals(squadLeader);
    }

    @Override public boolean isAlliedTo(Entity other) {
        if (other == this) return true;
        if (other instanceof Player player && commander != null && commander.equals(player.getUUID())) return true;
        if (other instanceof HecuSoldierEntity soldier && isSameFactionOrSquad(soldier)) return true;
        if (other instanceof HecuVehicleEntity vehicle && vehicle.isHostileVariant() == hostile) return true;
        return super.isAlliedTo(other);
    }

    @Override public boolean canAttack(LivingEntity target) { return isValidEnemy(target) && super.canAttack(target); }

    @Override public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer serverPlayer && !hostile) {
            commander = player.getUUID();
            squadLeader = null;
            setTarget(null);
            getNavigation().stop();
            setCustomName(Component.literal(roleName() + " — " + player.getName().getString()));
            player.displayClientMessage(Component.literal(roleName() + " присоединился к вашей команде."), true);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    private String roleName() {
        return switch (role) {
            case SHOTGUNNER -> "Shotgunner";
            case MACHINE_GUNNER -> "Machine Gunner";
            case STORMTROOPER -> "Stormtrooper";
            case SQUAD_LEADER -> "Squad Leader";
            case MEDIC -> "Medic";
        };
    }

    private void healNearbySquadmates() {
        if (role != SoldierRole.MEDIC || medicCooldown > 0) return;
        HecuSoldierEntity patient = level().getEntitiesOfClass(HecuSoldierEntity.class, getBoundingBox().inflate(12.0), s ->
                s != this && s.isAlive() && isSameFactionOrSquad(s) && s.getHealth() < s.getMaxHealth() - 1.0F)
                .stream().min((a, b) -> Float.compare(a.getHealth() / a.getMaxHealth(), b.getHealth() / b.getMaxHealth())).orElse(null);
        if (patient != null) {
            patient.heal(4.0F);
            medicCooldown = 40;
            patient.getCommandSenderWorld().broadcastEntityEvent(patient, (byte) 7);
        }
    }

    private void recruitNearbySoldiers() {
        if (role != SoldierRole.SQUAD_LEADER || tickCount % 40 != 0) return;
        for (HecuSoldierEntity soldier : level().getEntitiesOfClass(HecuSoldierEntity.class, getBoundingBox().inflate(18.0))) {
            if (soldier == this || soldier.hostile != hostile || soldier.commander != null || soldier.squadLeader != null) continue;
            // A soldier already assigned to another squad should never be stolen by a nearby leader.
            soldier.squadLeader = getUUID();
        }
    }

    private void followCommanderOrLeader() {
        if (getTarget() != null || tickCount % 10 != 0) return;
        if (commander != null) {
            Player owner = level().getPlayerByUUID(commander);
            if (owner != null && owner.isAlive() && distanceToSqr(owner) > 100.0) getNavigation().moveTo(owner, 1.08);
            else if (owner != null && distanceToSqr(owner) < 9.0) getNavigation().stop();
            return;
        }
        if (squadLeader != null) {
            HecuSoldierEntity leader = level().getEntitiesOfClass(HecuSoldierEntity.class, getBoundingBox().inflate(64.0), s -> s.getUUID().equals(squadLeader)).stream().findFirst().orElse(null);
            if (leader != null && leader.isAlive() && distanceToSqr(leader) > 36.0) getNavigation().moveTo(leader, 1.0);
        }
    }

    @Override public void tick() {
        super.tick();
        if (medicCooldown > 0) medicCooldown--;
        if (!level().isClientSide) {
            recruitNearbySoldiers();
            followCommanderOrLeader();
            healNearbySquadmates();
        }
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (commander != null) tag.putUUID("HECUCommander", commander);
        if (squadLeader != null) tag.putUUID("HECUSquadLeader", squadLeader);
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        commander = tag.hasUUID("HECUCommander") ? tag.getUUID("HECUCommander") : null;
        squadLeader = tag.hasUUID("HECUSquadLeader") ? tag.getUUID("HECUSquadLeader") : null;
    }
}
