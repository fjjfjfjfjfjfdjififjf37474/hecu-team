package com.hecuteam.client;

import com.hecuteam.HECUTeam;
import com.hecuteam.item.FirearmItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Weapon-specific first-person reload motion. */
@Mod.EventBusSubscriber(modid = HECUTeam.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ReloadAnimationHandler {
    private static final int RELOAD_TICKS = 24;

    private ReloadAnimationHandler() {}

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return;
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || !player.isUsingItem()) return;

        ItemStack used = player.getUseItem();
        if (!(used.getItem() instanceof FirearmItem firearm)
                || !used.getOrCreateTag().getBoolean("Reloading")) return;

        int elapsed = Mth.clamp(72000 - player.getUseItemRemainingTicks(), 0, RELOAD_TICKS);
        float t = elapsed / (float) RELOAD_TICKS;
        float dip = Mth.sin(t * (float) Math.PI);
        float settle = Mth.clamp((t - 0.72F) / 0.28F, 0.0F, 1.0F);
        float wobble = Mth.sin(t * (float) Math.PI * 3.0F) * (1.0F - settle);

        PoseStack pose = event.getPoseStack();
        if (firearm.isShotgunReload()) {
            // SPAS-12: repeated, slightly staggered shell-loading movement.
            float shellBeat = Mth.sin(t * (float) Math.PI * 6.0F);
            pose.translate(0.015F * shellBeat, -0.12F * dip - 0.018F * Math.abs(shellBeat), 0.12F * dip);
            pose.mulPose(Axis.XP.rotationDegrees(12.0F * dip + 2.5F * shellBeat));
            pose.mulPose(Axis.ZP.rotationDegrees(-7.0F * dip + shellBeat * 2.0F));
        } else if (firearm.hasLaserSight()) {
            // Desert Eagle: heavier, short downward movement for a magazine swap.
            float snap = Mth.sin(t * (float) Math.PI * 2.0F) * (1.0F - settle);
            pose.translate(0.025F * snap, -0.22F * dip, 0.16F * dip);
            pose.mulPose(Axis.XP.rotationDegrees(22.0F * dip));
            pose.mulPose(Axis.YP.rotationDegrees(5.0F * dip));
            pose.mulPose(Axis.ZP.rotationDegrees(-9.0F * dip + 2.0F * snap));
        } else if (firearm.hasGrenadeLauncher()) {
            // MP5/M203: a quick, compact SMG reload with a slight side shift.
            pose.translate(-0.035F * dip + 0.008F * wobble, -0.16F * dip, 0.11F * dip);
            pose.mulPose(Axis.XP.rotationDegrees(17.0F * dip));
            pose.mulPose(Axis.YP.rotationDegrees(-7.0F * dip));
            pose.mulPose(Axis.ZP.rotationDegrees(8.0F * dip + wobble * 2.0F));
        } else {
            // M249: heavier belt-fed weapon, deeper drop and slower-looking settle.
            float heavyWobble = wobble * 0.045F;
            pose.translate(0.025F * dip, -0.23F * dip + heavyWobble, 0.17F * dip);
            pose.mulPose(Axis.XP.rotationDegrees(20.0F * dip));
            pose.mulPose(Axis.YP.rotationDegrees(4.0F * dip));
            pose.mulPose(Axis.ZP.rotationDegrees(-11.0F * dip + heavyWobble * 25.0F));
        }
    }
}
