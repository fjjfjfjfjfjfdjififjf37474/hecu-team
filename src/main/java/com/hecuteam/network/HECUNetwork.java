package com.hecuteam.network;

import com.hecuteam.HECUTeam;
import com.hecuteam.item.FirearmItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class HECUNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(HECUTeam.MODID, "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private static int packetId = 0;

    private HECUNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(FireRequest.class, packetId++)
                .encoder((msg, buf) -> buf.writeBoolean(msg.automatic))
                .decoder(buf -> new FireRequest(buf.readBoolean()))
                .consumerMainThread((msg, ctx) -> {
                    ServerPlayer player = ctx.get().getSender();
                    if (player != null && player.isAlive() && !player.isSpectator()) {
                        ItemStack held = player.getMainHandItem();
                        if (held.getItem() instanceof FirearmItem firearm) firearm.fireFromAttack(player, held, msg.automatic);
                    }
                    ctx.get().setPacketHandled(true);
                }).add();
    }

    public static void requestFire(boolean automatic) {
        CHANNEL.sendToServer(new FireRequest(automatic));
    }

    public static final class FireRequest {
        final boolean automatic;
        public FireRequest(boolean automatic) { this.automatic = automatic; }
    }
}
