package com.hecuteam.registry;

import com.hecuteam.HECUTeam;
import com.hecuteam.item.FirearmItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = HECUTeam.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class HECUItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, HECUTeam.MODID);
    public static final RegistryObject<Item> SHOTGUN_AMMO = ammo("spas12_shells");
    public static final RegistryObject<Item> M249_AMMO = ammo("m249_556_ammo");
    public static final RegistryObject<Item> MP5_AMMO = ammo("mp5_9mm_ammo");
    public static final RegistryObject<Item> DEAGLE_AMMO = ammo("deagle_50ae_ammo");
    public static final RegistryObject<Item> GRENADE = ammo("mp5_grenade");
    public static final RegistryObject<Item> SPAS12 = gun("spas12", 8, 9.0F, 18, SHOTGUN_AMMO, true, false, false);
    public static final RegistryObject<Item> M249 = gun("m249", 50, 5.5F, 3, M249_AMMO, false, false, false);
    public static final RegistryObject<Item> MP5 = gun("mp5_m203", 50, 5.0F, 3, MP5_AMMO, false, true, false);
    public static final RegistryObject<Item> DESERT_EAGLE = gun("desert_eagle_laser", 8, 12.0F, 12, DEAGLE_AMMO, false, false, true);
    public static final RegistryObject<Item> EGG_F_SHOTGUNNER = egg("friendly_shotgunner_spawn_egg", HECUEntities.FRIENDLY_SHOTGUNNER, 0x5C594F, 0xC5B58B);
    public static final RegistryObject<Item> EGG_H_SHOTGUNNER = egg("hostile_shotgunner_spawn_egg", HECUEntities.HOSTILE_SHOTGUNNER, 0x3D463F, 0xA18D69);
    public static final RegistryObject<Item> EGG_F_M249 = egg("friendly_machine_gunner_spawn_egg", HECUEntities.FRIENDLY_MACHINE_GUNNER, 0x5C594F, 0x3F5C35);
    public static final RegistryObject<Item> EGG_H_M249 = egg("hostile_machine_gunner_spawn_egg", HECUEntities.HOSTILE_MACHINE_GUNNER, 0x3D463F, 0x5E6B3D);
    public static final RegistryObject<Item> EGG_F_STORM = egg("friendly_stormtrooper_spawn_egg", HECUEntities.FRIENDLY_STORMTROOPER, 0x5C594F, 0x6A7B4E);
    public static final RegistryObject<Item> EGG_H_STORM = egg("hostile_stormtrooper_spawn_egg", HECUEntities.HOSTILE_STORMTROOPER, 0x3D463F, 0x74824C);
    public static final RegistryObject<Item> EGG_F_LEADER = egg("friendly_squad_leader_spawn_egg", HECUEntities.FRIENDLY_SQUAD_LEADER, 0x5C594F, 0xC5B58B);
    public static final RegistryObject<Item> EGG_H_LEADER = egg("hostile_squad_leader_spawn_egg", HECUEntities.HOSTILE_SQUAD_LEADER, 0x3D463F, 0xA18D69);
    public static final RegistryObject<Item> EGG_F_MEDIC = egg("friendly_medic_spawn_egg", HECUEntities.FRIENDLY_MEDIC, 0x5C594F, 0xB5BDB0);
    public static final RegistryObject<Item> EGG_H_MEDIC = egg("hostile_medic_spawn_egg", HECUEntities.HOSTILE_MEDIC, 0x3D463F, 0xB5BDB0);
    public static final RegistryObject<Item> EGG_F_APACHE = vehicleEgg("friendly_apache_spawn_egg", HECUEntities.FRIENDLY_APACHE, 0x4B5443, 0x777D69);
    public static final RegistryObject<Item> EGG_H_APACHE = vehicleEgg("hostile_apache_spawn_egg", HECUEntities.HOSTILE_APACHE, 0x30382E, 0x777D69);
    public static final RegistryObject<Item> EGG_F_BRADLEY = vehicleEgg("friendly_bradley_spawn_egg", HECUEntities.FRIENDLY_BRADLEY, 0x4B5443, 0x777D69);
    public static final RegistryObject<Item> EGG_H_BRADLEY = vehicleEgg("hostile_bradley_spawn_egg", HECUEntities.HOSTILE_BRADLEY, 0x30382E, 0x777D69);
    private static RegistryObject<Item> vehicleEgg(String name, RegistryObject<net.minecraft.world.entity.EntityType<com.hecuteam.entity.HecuVehicleEntity>> entity, int background, int spots) { return ITEMS.register(name, () -> new SpawnEggItem(entity.get(), background, spots, new Item.Properties())); }
    private static RegistryObject<Item> egg(String name, RegistryObject<net.minecraft.world.entity.EntityType<com.hecuteam.entity.HecuSoldierEntity>> entity, int background, int spots) { return ITEMS.register(name, () -> new SpawnEggItem(entity.get(), background, spots, new Item.Properties())); }
    private static RegistryObject<Item> ammo(String name) { return ITEMS.register(name, () -> new Item(new Item.Properties())); }
    private static RegistryObject<Item> gun(String name, int cap, float dmg, int cd, RegistryObject<Item> ammo, boolean shotgun, boolean gl, boolean laser) {
        return ITEMS.register(name, () -> new FirearmItem(new Item.Properties().stacksTo(1), cap, dmg, cd, ammo::get, shotgun, gl, laser));
    }

    @SubscribeEvent
    public static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(SPAS12.get()); event.accept(M249.get()); event.accept(MP5.get()); event.accept(DESERT_EAGLE.get());
            event.accept(SHOTGUN_AMMO.get()); event.accept(M249_AMMO.get()); event.accept(MP5_AMMO.get()); event.accept(DEAGLE_AMMO.get()); event.accept(GRENADE.get());
            event.accept(EGG_F_SHOTGUNNER.get()); event.accept(EGG_H_SHOTGUNNER.get());
            event.accept(EGG_F_M249.get()); event.accept(EGG_H_M249.get());
            event.accept(EGG_F_STORM.get()); event.accept(EGG_H_STORM.get());
            event.accept(EGG_F_LEADER.get()); event.accept(EGG_H_LEADER.get());
            event.accept(EGG_F_MEDIC.get()); event.accept(EGG_H_MEDIC.get());
            event.accept(EGG_F_APACHE.get()); event.accept(EGG_H_APACHE.get());
            event.accept(EGG_F_BRADLEY.get()); event.accept(EGG_H_BRADLEY.get());
        }
    }
}
