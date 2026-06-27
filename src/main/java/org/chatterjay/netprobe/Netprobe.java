package org.chatterjay.netprobe;

import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import org.slf4j.Logger;

@Mod(Netprobe.MODID)
public class Netprobe {

    public static final String MODID = "netprobe";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Netprobe() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
        NetProbeConfig.register();

        if (FMLLoader.getDist() == Dist.CLIENT) {
            NetProbeClientSetup.init();
        }

        NetworkStats.start();
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("NetProbe loaded");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("NetProbe server starting");
    }
}
