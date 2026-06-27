package org.chatterjay.netprobe;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

@OnlyIn(Dist.CLIENT)
public class NetProbeClientSetup {

    public static void init() {
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (mc, parent) -> createConfigScreen(parent)));
    }

    private static Screen createConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("netprobe.config.title"));

        builder.setGlobalized(true);
        builder.setGlobalizedExpanded(false);
        ConfigEntryBuilder eb = builder.entryBuilder();

        // --- Overlay ---
        ConfigCategory overlay = builder.getOrCreateCategory(
                Component.translatable("netprobe.config.overlay"));

        overlay.addEntry(eb.startIntField(
                        Component.translatable("netprobe.config.refreshInterval"),
                        NetProbeConfig.refreshInterval.get())
                .setDefaultValue(10)
                .setMin(1).setMax(100)
                .setSaveConsumer(NetProbeConfig.refreshInterval::set)
                .build());

        overlay.addEntry(eb.startDoubleField(
                        Component.translatable("netprobe.config.renderHeight"),
                        NetProbeConfig.renderHeight.get())
                .setDefaultValue(1.04)
                .setMin(0.5).setMax(5.0)
                .setSaveConsumer(v -> NetProbeConfig.renderHeight.set(v))
                .build());

        overlay.addEntry(eb.startDoubleField(
                        Component.translatable("netprobe.config.overlayAlpha"),
                        NetProbeConfig.overlayAlpha.get())
                .setDefaultValue(0.15)
                .setMin(0.0).setMax(1.0)
                .setSaveConsumer(v -> NetProbeConfig.overlayAlpha.set(v))
                .build());

        // --- Colors ---
        ConfigCategory colors = builder.getOrCreateCategory(
                Component.translatable("netprobe.config.colors"));

        colors.addEntry(eb.startStrField(
                        Component.translatable("netprobe.config.colorLow"),
                        NetProbeConfig.colorLow.get())
                .setDefaultValue("00FF00")
                .setSaveConsumer(NetProbeConfig.colorLow::set)
                .build());

        colors.addEntry(eb.startStrField(
                        Component.translatable("netprobe.config.colorMid"),
                        NetProbeConfig.colorMid.get())
                .setDefaultValue("FFFF00")
                .setSaveConsumer(NetProbeConfig.colorMid::set)
                .build());

        colors.addEntry(eb.startStrField(
                        Component.translatable("netprobe.config.colorHigh"),
                        NetProbeConfig.colorHigh.get())
                .setDefaultValue("FF0000")
                .setSaveConsumer(NetProbeConfig.colorHigh::set)
                .build());

        // --- Thresholds ---
        ConfigCategory thresholds = builder.getOrCreateCategory(
                Component.translatable("netprobe.config.thresholds"));

        thresholds.addEntry(eb.startLongField(
                        Component.translatable("netprobe.config.normalMax"),
                        NetProbeConfig.normalMax.get())
                .setDefaultValue(100L)
                .setMin(1L).setMax(Long.MAX_VALUE)
                .setSaveConsumer(NetProbeConfig.normalMax::set)
                .build());

        thresholds.addEntry(eb.startLongField(
                        Component.translatable("netprobe.config.warningMax"),
                        NetProbeConfig.warningMax.get())
                .setDefaultValue(500L)
                .setMin(1L).setMax(Long.MAX_VALUE)
                .setSaveConsumer(NetProbeConfig.warningMax::set)
                .build());

        builder.setSavingRunnable(() -> NetProbeConfig.CLIENT_SPEC.save());
        return builder.build();
    }
}
