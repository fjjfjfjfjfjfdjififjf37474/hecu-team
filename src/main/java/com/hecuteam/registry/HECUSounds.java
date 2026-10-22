package com.hecuteam.registry;

import com.hecuteam.HECUTeam;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** Custom synthesized sound effects for the HECU weapons. */
public final class HECUSounds {
    private HECUSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, HECUTeam.MODID);

    public static final RegistryObject<SoundEvent> WEAPON_PISTOL = register("weapon_pistol");
    public static final RegistryObject<SoundEvent> WEAPON_AUTO = register("weapon_auto");
    public static final RegistryObject<SoundEvent> WEAPON_SHOTGUN = register("weapon_shotgun");
    public static final RegistryObject<SoundEvent> WEAPON_RELOAD_START = register("weapon_reload_start");
    public static final RegistryObject<SoundEvent> WEAPON_RELOAD_END = register("weapon_reload_end");
    public static final RegistryObject<SoundEvent> WEAPON_EMPTY = register("weapon_empty");
    public static final RegistryObject<SoundEvent> GRENADE_LAUNCHER = register("grenade_launcher");

    private static RegistryObject<SoundEvent> register(String name) {
        ResourceLocation id = new ResourceLocation(HECUTeam.MODID, name);
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    public static void register(IEventBus bus) {
        SOUNDS.register(bus);
    }
}
