package org.chatterjay.netprobe;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(Netprobe.MODID)
public class Netprobe {

    public static final String MODID = "netprobe";
    private static final Logger LOGGER = LogUtils.getLogger();

        public Netprobe(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, NetProbeConfig.SPEC);
        NetworkStats.start();
        LOGGER.info("NetProbe loaded");
    }
}
