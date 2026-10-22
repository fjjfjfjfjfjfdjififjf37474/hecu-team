package com.hecuteam.client;

import com.hecuteam.item.FirearmItem;
import com.hecuteam.network.HECUNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = "hecuteam")
public final class FirearmInputHandler {
    private static boolean wasAttackDown;

    private FirearmInputHandler() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null) {
            wasAttackDown = false;
            return;
        }
        ItemStack held = mc.player.getMainHandItem();
        boolean attackDown = mc.options.keyAttack.isDown();
        if (held.getItem() instanceof FirearmItem firearm && attackDown) {
            if (firearm.isAutomaticWeapon()) {
                HECUNetwork.requestFire(true);
            } else if (!wasAttackDown) {
                HECUNetwork.requestFire(false);
            }
        }
        wasAttackDown = attackDown;
    }
}
