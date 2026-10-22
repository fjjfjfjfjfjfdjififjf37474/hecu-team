package com.hecuteam.registry;

import com.hecuteam.HECUTeam;
import com.hecuteam.entity.HecuSoldierEntity;
import com.hecuteam.entity.HecuVehicleEntity;
import com.hecuteam.entity.SoldierRole;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class HECUEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, HECUTeam.MODID);
    public static final RegistryObject<EntityType<HecuSoldierEntity>> FRIENDLY_SHOTGUNNER = soldier("friendly_shotgunner", SoldierRole.SHOTGUNNER, false);
    public static final RegistryObject<EntityType<HecuSoldierEntity>> HOSTILE_SHOTGUNNER = soldier("hostile_shotgunner", SoldierRole.SHOTGUNNER, true);
    public static final RegistryObject<EntityType<HecuSoldierEntity>> FRIENDLY_MACHINE_GUNNER = soldier("friendly_machine_gunner", SoldierRole.MACHINE_GUNNER, false);
    public static final RegistryObject<EntityType<HecuSoldierEntity>> HOSTILE_MACHINE_GUNNER = soldier("hostile_machine_gunner", SoldierRole.MACHINE_GUNNER, true);
    public static final RegistryObject<EntityType<HecuSoldierEntity>> FRIENDLY_STORMTROOPER = soldier("friendly_stormtrooper", SoldierRole.STORMTROOPER, false);
    public static final RegistryObject<EntityType<HecuSoldierEntity>> HOSTILE_STORMTROOPER = soldier("hostile_stormtrooper", SoldierRole.STORMTROOPER, true);
    public static final RegistryObject<EntityType<HecuSoldierEntity>> FRIENDLY_SQUAD_LEADER = soldier("friendly_squad_leader", SoldierRole.SQUAD_LEADER, false);
    public static final RegistryObject<EntityType<HecuSoldierEntity>> HOSTILE_SQUAD_LEADER = soldier("hostile_squad_leader", SoldierRole.SQUAD_LEADER, true);
    public static final RegistryObject<EntityType<HecuSoldierEntity>> FRIENDLY_MEDIC = soldier("friendly_medic", SoldierRole.MEDIC, false);
    public static final RegistryObject<EntityType<HecuSoldierEntity>> HOSTILE_MEDIC = soldier("hostile_medic", SoldierRole.MEDIC, true);
    public static final RegistryObject<EntityType<HecuVehicleEntity>> FRIENDLY_APACHE = vehicle("friendly_apache", HecuVehicleEntity.Vehicle.APACHE, false, 2.5F, 2.0F);
    public static final RegistryObject<EntityType<HecuVehicleEntity>> HOSTILE_APACHE = vehicle("hostile_apache", HecuVehicleEntity.Vehicle.APACHE, true, 2.5F, 2.0F);
    public static final RegistryObject<EntityType<HecuVehicleEntity>> FRIENDLY_BRADLEY = vehicle("friendly_bradley", HecuVehicleEntity.Vehicle.BRADLEY, false, 2.8F, 2.0F);
    public static final RegistryObject<EntityType<HecuVehicleEntity>> HOSTILE_BRADLEY = vehicle("hostile_bradley", HecuVehicleEntity.Vehicle.BRADLEY, true, 2.8F, 2.0F);

    private static RegistryObject<EntityType<HecuSoldierEntity>> soldier(String id, SoldierRole role, boolean hostile) {
        return ENTITY_TYPES.register(id, () -> EntityType.Builder.of((EntityType<HecuSoldierEntity> type, net.minecraft.world.level.Level level) -> new HecuSoldierEntity(type, level, role, hostile), MobCategory.MONSTER).sized(0.6F, 1.95F).clientTrackingRange(10).build(id));
    }
    private static RegistryObject<EntityType<HecuVehicleEntity>> vehicle(String id, HecuVehicleEntity.Vehicle v, boolean hostile, float w, float h) {
        return ENTITY_TYPES.register(id, () -> EntityType.Builder.of((EntityType<HecuVehicleEntity> type, net.minecraft.world.level.Level level) -> new HecuVehicleEntity(type, level, v, hostile), MobCategory.MONSTER).sized(w, h).clientTrackingRange(12).fireImmune().build(id));
    }
    @Mod.EventBusSubscriber(modid = HECUTeam.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class Attributes {
        @SubscribeEvent public static void registerAttributes(EntityAttributeCreationEvent event) {
            event.put(FRIENDLY_SHOTGUNNER.get(), HecuSoldierEntity.createAttributes().build());
            event.put(HOSTILE_SHOTGUNNER.get(), HecuSoldierEntity.createAttributes().build());
            event.put(FRIENDLY_MACHINE_GUNNER.get(), HecuSoldierEntity.createAttributes().build());
            event.put(HOSTILE_MACHINE_GUNNER.get(), HecuSoldierEntity.createAttributes().build());
            event.put(FRIENDLY_STORMTROOPER.get(), HecuSoldierEntity.createAttributes().build());
            event.put(HOSTILE_STORMTROOPER.get(), HecuSoldierEntity.createAttributes().build());
            event.put(FRIENDLY_SQUAD_LEADER.get(), HecuSoldierEntity.createAttributes().build());
            event.put(HOSTILE_SQUAD_LEADER.get(), HecuSoldierEntity.createAttributes().build());
            event.put(FRIENDLY_MEDIC.get(), HecuSoldierEntity.createAttributes().build());
            event.put(HOSTILE_MEDIC.get(), HecuSoldierEntity.createAttributes().build());
            event.put(FRIENDLY_APACHE.get(), HecuVehicleEntity.createAttributes(300).build());
            event.put(HOSTILE_APACHE.get(), HecuVehicleEntity.createAttributes(300).build());
            event.put(FRIENDLY_BRADLEY.get(), HecuVehicleEntity.createAttributes(400).build());
            event.put(HOSTILE_BRADLEY.get(), HecuVehicleEntity.createAttributes(400).build());
        }
    }
}
