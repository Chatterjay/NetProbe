package org.chatterjay.netprobe;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(Netprobe.MODID)
public class Netprobe {

    public static final String MODID = "netprobe";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean debugMode = false;

    public static boolean isDebugMode() {
        return debugMode;
    }

    public static void setDebugMode(boolean enabled) {
        debugMode = enabled;
    }

    public Netprobe(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, NetProbeConfig.SPEC);

        if (FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
            registerConfigScreen(modContainer);
        }

        NetworkStats.start();
        LOGGER.info("NetProbe loaded");
    }

    private static void registerConfigScreen(ModContainer container) {
        try {
            Class<?> factoryClass = Class.forName("net.neoforged.neoforge.client.gui.IConfigScreenFactory");
            Class<?> configScreenClass = Class.forName("net.neoforged.neoforge.client.gui.ConfigurationScreen");
            Class<?> screenClass = Class.forName("net.minecraft.client.gui.screens.Screen");
            var ctor = configScreenClass.getConstructor(ModContainer.class, screenClass);
            var regMethod = ModContainer.class.getMethod("registerExtensionPoint", Class.class, java.util.function.Supplier.class);

            Object factory = java.lang.reflect.Proxy.newProxyInstance(
                    factoryClass.getClassLoader(),
                    new Class<?>[]{factoryClass},
                    (_proxy, method, args) -> {
                        if ("createScreen".equals(method.getName()) && args != null && args.length == 2) {
                            return ctor.newInstance(container, args[1]);
                        }
                        return null;
                    }
            );
            regMethod.invoke(container, factoryClass, (java.util.function.Supplier<?>) () -> factory);
        } catch (Exception e) {
            LOGGER.warn("Failed to register config screen", e);
        }
    }
}
