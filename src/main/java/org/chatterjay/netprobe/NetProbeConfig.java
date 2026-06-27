package org.chatterjay.netprobe;

import net.neoforged.neoforge.common.ModConfigSpec;

public class NetProbeConfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue refreshInterval;
    public static final ModConfigSpec.DoubleValue renderHeight;
    public static final ModConfigSpec.BooleanValue labelSeeThrough;
    public static final ModConfigSpec.DoubleValue overlayAlpha;
    public static final ModConfigSpec.ConfigValue<String> colorLow;
    public static final ModConfigSpec.ConfigValue<String> colorMid;
    public static final ModConfigSpec.ConfigValue<String> colorHigh;
    public static final ModConfigSpec.LongValue normalMax;
    public static final ModConfigSpec.LongValue warningMax;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("overlay");
        labelSeeThrough = builder
                .comment("Render labels through blocks (always visible)")
                .define("labelSeeThrough", true);
        refreshInterval = builder
                .comment("Minimum ticks between overlay updates (20 ticks = 1 second)")
                .defineInRange("refreshInterval", 10, 1, 100);
        renderHeight = builder
                .comment("Text label height above block (in blocks)")
                .defineInRange("renderHeight", 1.5, 0.5, 5.0);
        overlayAlpha = builder
                .comment("Block overlay face opacity (0.0 - 1.0)")
                .defineInRange("overlayAlpha", 0.12, 0.0, 1.0);

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

        SPEC = builder.build();
    }
}
