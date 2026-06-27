package org.chatterjay.netprobe;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class NetProbeConfig {

    public static final ForgeConfigSpec CLIENT_SPEC;

    public static final ForgeConfigSpec.IntValue refreshInterval;
    public static final ForgeConfigSpec.DoubleValue renderHeight;
    public static final ForgeConfigSpec.DoubleValue overlayAlpha;
    public static final ForgeConfigSpec.ConfigValue<String> colorLow;
    public static final ForgeConfigSpec.ConfigValue<String> colorMid;
    public static final ForgeConfigSpec.ConfigValue<String> colorHigh;
    public static final ForgeConfigSpec.LongValue normalMax;
    public static final ForgeConfigSpec.LongValue warningMax;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("overlay");
        refreshInterval = builder
                .comment("Minimum ticks between overlay updates (20 ticks = 1 second)")
                .defineInRange("refreshInterval", 10, 1, 100);
        renderHeight = builder
                .comment("Text label height above block (in blocks)")
                .defineInRange("renderHeight", 1.04, 0.5, 5.0);
        overlayAlpha = builder
                .comment("Block overlay face opacity (0.0 - 1.0)")
                .defineInRange("overlayAlpha", 0.15, 0.0, 1.0);

        builder.push("colors");
        colorLow = builder
                .comment("Normal traffic color (hex RRGGBB)")
                .define("colorLow", "00FF00");
        colorMid = builder
                .comment("Warning traffic color (hex RRGGBB)")
                .define("colorMid", "FFFF00");
        colorHigh = builder
                .comment("High traffic color (hex RRGGBB)")
                .define("colorHigh", "FF0000");
        builder.pop();

        builder.push("thresholds");
        normalMax = builder
                .comment("Traffic below this (bytes) is considered normal")
                .defineInRange("normalMax", 100L, 1L, Long.MAX_VALUE);
        warningMax = builder
                .comment("Traffic below this (bytes) is warning, above is high")
                .defineInRange("warningMax", 500L, 1L, Long.MAX_VALUE);
        builder.pop();

        builder.pop();

        CLIENT_SPEC = builder.build();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);
    }
}
