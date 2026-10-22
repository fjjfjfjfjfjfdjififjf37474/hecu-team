package com.hecuteam;

import com.hecuteam.registry.HECUEntities;
import com.hecuteam.registry.HECUItems;
import com.hecuteam.registry.HECUSounds;
import com.hecuteam.network.HECUNetwork;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(HECUTeam.MODID)
public class HECUTeam {
    public static final String MODID = "hecuteam";
    public HECUTeam() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        HECUItems.ITEMS.register(bus);
        HECUEntities.ENTITY_TYPES.register(bus);
        HECUSounds.register(bus);
        HECUNetwork.register();
    }
}
