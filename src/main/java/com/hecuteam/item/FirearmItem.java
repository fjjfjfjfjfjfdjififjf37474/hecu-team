package com.hecuteam.item;

import com.hecuteam.registry.HECUItems;
import com.hecuteam.registry.HECUSounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import org.joml.Vector3f;

import java.util.function.Supplier;

/** Server-authoritative hitscan firearm. Sneak + right click reloads; MP5 launches a grenade
 * when a grenade is held in the offhand. Magazines are stored per ItemStack. */
public class FirearmItem extends Item {
    private static final String TAG_LOADED = "Loaded";
    private static final String TAG_RELOADING = "Reloading";
    private static final int RELOAD_TICKS = 24;
    private final int capacity;
    private final float damage;
    private final int cooldown;
    private final Supplier<Item> ammo;
    private final boolean shotgun;
    private final boolean grenadeLauncher;
    private final boolean laserSight;

    public FirearmItem(Properties props, int capacity, float damage, int cooldown,
                       Supplier<Item> ammo, boolean grenadeLauncher) {
        this(props, capacity, damage, cooldown, ammo, false, grenadeLauncher, false);
    }

    public FirearmItem(Properties props, int capacity, float damage, int cooldown,
                       Supplier<Item> ammo, boolean shotgun, boolean grenadeLauncher, boolean laserSight) {
        super(props);
        this.capacity = capacity;
        this.damage = damage;
        this.cooldown = cooldown;
        this.ammo = ammo;
        this.shotgun = shotgun;
        this.grenadeLauncher = grenadeLauncher;
        this.laserSight = laserSight;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            if (grenadeLauncher && player.getOffhandItem().is(HECUItems.GRENADE.get())) {
                if (!level.isClientSide) launchGrenade(level, player);
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
            if (getLoaded(stack) >= capacity) {
                if (!level.isClientSide) player.displayClientMessage(Component.literal("Магазин уже полный: " + capacity + "/" + capacity), true);
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
            if (!level.isClientSide && !hasAmmo(player)) {
                player.displayClientMessage(Component.literal("Нет подходящих патронов в инвентаре"), true);
                return InteractionResultHolder.fail(stack);
            }
            stack.getOrCreateTag().putBoolean(TAG_RELOADING, true);
            if (!level.isClientSide) level.playSound(null, player.blockPosition(), HECUSounds.WEAPON_RELOAD_START.get(), SoundSource.PLAYERS, 0.75F, 1.0F);
            player.startUsingItem(hand);
            if (!level.isClientSide) player.displayClientMessage(Component.literal("Перезарядка..."), true);
            return InteractionResultHolder.consume(stack);
        }

        // Normal right-click no longer fires: shooting is handled by the left-click client input packet.
        return InteractionResultHolder.pass(stack);
    }

    public boolean isAutomaticWeapon() {
        return isAutomatic();
    }

    /** Visual reload profile used by the client-side first-person animation. */
    public boolean isShotgunReload() { return shotgun; }
    public boolean hasGrenadeLauncher() { return grenadeLauncher; }
    public boolean hasLaserSight() { return laserSight; }
    public int getMagazineCapacity() { return capacity; }

    /** Called only from the server-side network handler after a left-click input request. */
    public void fireFromAttack(Player player, ItemStack stack, boolean automaticRequest) {
        Level level = player.level();
        if (level.isClientSide || player.isShiftKeyDown() || stack.getItem() != this) return;
        if (stack.getOrCreateTag().getBoolean(TAG_RELOADING)) return;
        if (automaticRequest != isAutomatic()) return;
        if (player.getCooldowns().isOnCooldown(this)) return;

        int loaded = getLoaded(stack);
        if (loaded <= 0) {
            player.displayClientMessage(Component.literal("Магазин пуст — Shift + ПКМ для перезарядки"), true);
            level.playSound(null, player.blockPosition(), HECUSounds.WEAPON_EMPTY.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
            player.getCooldowns().addCooldown(this, 8);
            return;
        }

        if (shotgun) {
            for (int i = 0; i < 8; i++) fireRay(level, player, damage / 2.6F, 0.075);
        } else {
            fireRay(level, player, damage, isAutomatic() ? 0.012 : 0.0);
        }
        playFireEffects(level, player);
        stack.getOrCreateTag().putInt(TAG_LOADED, loaded - 1);
        player.getCooldowns().addCooldown(this, cooldown);
        player.swing(InteractionHand.MAIN_HAND, true);
    }

    private boolean isAutomatic() {
        return !shotgun && (cooldown <= 4);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.CROSSBOW;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingUseDuration) {
        if (!(user instanceof Player player)) return;
        int elapsed = getUseDuration(stack) - remainingUseDuration;
        if (stack.getOrCreateTag().getBoolean(TAG_RELOADING)) {
            if (!level.isClientSide && elapsed >= RELOAD_TICKS) {
                reload(player, stack);
                stack.getOrCreateTag().remove(TAG_RELOADING);
                player.stopUsingItem();
                level.playSound(null, player.blockPosition(), HECUSounds.WEAPON_RELOAD_END.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
            }
            return;
        }
        if (level.isClientSide || player.isShiftKeyDown()) return;
        if (elapsed % cooldown != 0) return;
        int loaded = getLoaded(stack);
        if (loaded <= 0) {
            player.displayClientMessage(Component.literal("Магазин пуст — Shift + ПКМ для перезарядки"), true);
            player.stopUsingItem();
            return;
        }
        fireRay(level, player, damage, 0.012);
        playFireEffects(level, player);
        stack.getOrCreateTag().putInt(TAG_LOADED, loaded - 1);
        player.swing(InteractionHand.MAIN_HAND, true);
    }

    private boolean hasAmmo(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ammo.get()) && !stack.isEmpty()) return true;
        }
        return false;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        if (stack.hasTag() && stack.getTag().getBoolean(TAG_RELOADING)) {
            stack.getTag().remove(TAG_RELOADING);
        }
        super.releaseUsing(stack, level, user, timeLeft);
    }

    private void reload(Player player, ItemStack weapon) {
        int loaded = getLoaded(weapon);
        int needed = capacity - loaded;
        if (needed <= 0) {
            player.displayClientMessage(Component.literal("Магазин уже полный: " + loaded + "/" + capacity), true);
            return;
        }
        int taken = 0;
        for (ItemStack inv : player.getInventory().items) {
            if (inv.is(ammo.get()) && taken < needed) {
                int amount = Math.min(needed - taken, inv.getCount());
                taken += amount;
                inv.shrink(amount);
            }
        }
        if (taken > 0) {
            weapon.getOrCreateTag().putInt(TAG_LOADED, loaded + taken);
            player.displayClientMessage(Component.literal("Перезарядка: " + (loaded + taken) + "/" + capacity), true);
        } else {
            player.displayClientMessage(Component.literal("Нет подходящих патронов в инвентаре"), true);
        }
    }

    private int getLoaded(ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().contains(TAG_LOADED)) {
            stack.getOrCreateTag().putInt(TAG_LOADED, capacity);
        }
        return Math.max(0, Math.min(capacity, stack.getOrCreateTag().getInt(TAG_LOADED)));
    }

    private void fireRay(Level level, Player player, float shotDamage, double spread) {
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getViewVector(1.0F);
        if (spread > 0) {
            direction = direction.add((level.random.nextDouble() - 0.5) * spread,
                    (level.random.nextDouble() - 0.5) * spread, (level.random.nextDouble() - 0.5) * spread).normalize();
        }
        Vec3 end = start.add(direction.scale(80.0));
        BlockHitResult blockHit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 finalEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        AABB search = player.getBoundingBox().expandTowards(finalEnd.subtract(start)).inflate(1.0);
        EntityHitResult entityHit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                level, player, start, finalEnd, search,
                entity -> entity instanceof LivingEntity && entity != player && entity.isPickable());
        if (entityHit != null && entityHit.getEntity() instanceof LivingEntity target) {
            target.hurt(level.damageSources().playerAttack(player), shotDamage);
        }
    }

    private void playFireEffects(Level level, Player player) {
        float pitch = shotgun ? 0.72F : (isAutomatic() ? 1.05F : 0.88F);
        level.playSound(null, player.blockPosition(), shotgun ? HECUSounds.WEAPON_SHOTGUN.get()
                        : (isAutomatic() ? HECUSounds.WEAPON_AUTO.get() : HECUSounds.WEAPON_PISTOL.get()),
                SoundSource.PLAYERS, shotgun ? 1.0F : 0.85F, pitch);
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 muzzle = eye.add(look.scale(0.65)).add(0, -0.08, 0);
        if (level instanceof net.minecraft.server.level.ServerLevel server) {
            server.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, shotgun ? 5 : 2,
                    0.025, 0.025, 0.025, 0.015);
            server.sendParticles(ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z, 1,
                    0.015, 0.015, 0.015, 0.005);
        }
    }

    private void launchGrenade(Level level, Player player) {
        ItemStack offhand = player.getOffhandItem();
        if (!offhand.is(HECUItems.GRENADE.get())) return;
        offhand.shrink(1);
        Vec3 look = player.getLookAngle();
        PrimedTnt grenade = new PrimedTnt(level, player.getX() + look.x, player.getEyeY() - 0.15, player.getZ() + look.z, player);
        grenade.setFuse(45);
        grenade.setDeltaMovement(look.scale(1.15).add(0, 0.12, 0));
        level.addFreshEntity(grenade);
        level.playSound(null, player.blockPosition(), HECUSounds.GRENADE_LAUNCHER.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
        player.getCooldowns().addCooldown(this, 25);
        player.swing(InteractionHand.MAIN_HAND, true);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide && selected && entity instanceof Player player && player.getMainHandItem() == stack && player.tickCount % 10 == 0) {
            player.displayClientMessage(Component.literal("Патроны: " + getLoaded(stack) + "/" + capacity + "  |  ЛКМ — огонь | Shift + ПКМ — перезарядка"), true);
        }
        if (laserSight && level.isClientSide && selected && entity instanceof Player player && player.getMainHandItem() == stack && player.tickCount % 2 == 0) {
            Vec3 start = player.getEyePosition();
            Vec3 end = start.add(player.getViewVector(1.0F).scale(30.0));
            BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            Vec3 point = hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
            Vec3 delta = point.subtract(start);
            int steps = Math.max(1, (int) (delta.length() * 2));
            DustParticleOptions red = new DustParticleOptions(new Vector3f(1.0F, 0.02F, 0.02F), 0.55F);
            for (int i = 1; i <= steps; i++) {
                Vec3 p = start.add(delta.scale((double) i / steps));
                level.addParticle(red, p.x, p.y, p.z, 0, 0, 0);
            }
        }
    }
}
