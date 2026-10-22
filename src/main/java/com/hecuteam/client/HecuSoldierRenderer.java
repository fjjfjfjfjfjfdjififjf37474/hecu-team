package com.hecuteam.client;

import com.hecuteam.HECUTeam;
import com.hecuteam.entity.HecuSoldierEntity;
import com.hecuteam.entity.SoldierRole;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.EntityRenderersEvent;

/** Uses Minecraft's humanoid rig with original HECU-style pixel textures and role-specific gear colors. */
public class HecuSoldierRenderer extends HumanoidMobRenderer<HecuSoldierEntity, HumanoidModel<HecuSoldierEntity>> {
    public HecuSoldierRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.45F);
    }

    @Override
    public ResourceLocation getTextureLocation(HecuSoldierEntity entity) {
        String side = entity.isHostileVariant() ? "hostile_" : "friendly_";
        String role = switch (entity.getRole()) {
            case SHOTGUNNER -> "shotgunner";
            case MACHINE_GUNNER -> "machine_gunner";
            case STORMTROOPER -> "stormtrooper";
            case SQUAD_LEADER -> "squad_leader";
            case MEDIC -> "medic";
        };
        return new ResourceLocation(HECUTeam.MODID, "textures/entity/" + side + role + ".png");
    }

    @Mod.EventBusSubscriber(modid = HECUTeam.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class Registration {
        @SubscribeEvent
        public static void register(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(com.hecuteam.registry.HECUEntities.FRIENDLY_SHOTGUNNER.get(), HecuSoldierRenderer::new);
            event.registerEntityRenderer(com.hecuteam.registry.HECUEntities.HOSTILE_SHOTGUNNER.get(), HecuSoldierRenderer::new);
            event.registerEntityRenderer(com.hecuteam.registry.HECUEntities.FRIENDLY_MACHINE_GUNNER.get(), HecuSoldierRenderer::new);
            event.registerEntityRenderer(com.hecuteam.registry.HECUEntities.HOSTILE_MACHINE_GUNNER.get(), HecuSoldierRenderer::new);
            event.registerEntityRenderer(com.hecuteam.registry.HECUEntities.FRIENDLY_STORMTROOPER.get(), HecuSoldierRenderer::new);
            event.registerEntityRenderer(com.hecuteam.registry.HECUEntities.HOSTILE_STORMTROOPER.get(), HecuSoldierRenderer::new);
            event.registerEntityRenderer(com.hecuteam.registry.HECUEntities.FRIENDLY_SQUAD_LEADER.get(), HecuSoldierRenderer::new);
            event.registerEntityRenderer(com.hecuteam.registry.HECUEntities.HOSTILE_SQUAD_LEADER.get(), HecuSoldierRenderer::new);
            event.registerEntityRenderer(com.hecuteam.registry.HECUEntities.FRIENDLY_MEDIC.get(), HecuSoldierRenderer::new);
            event.registerEntityRenderer(com.hecuteam.registry.HECUEntities.HOSTILE_MEDIC.get(), HecuSoldierRenderer::new);
        }
    }
}
