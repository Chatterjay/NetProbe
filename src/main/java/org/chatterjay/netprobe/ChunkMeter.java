package org.chatterjay.netprobe;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Netprobe.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ChunkMeter {

    private static boolean hudVisible = true;
    private static boolean overlayVisible = false;
    private static int overlayMode = 1; // 1=柱子, 2=色块, 3=线框

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        ChunkTrafficTracker.INSTANCE.reset();
    }

    public static void toggleHud() {
        hudVisible = !hudVisible;
    }

    public static boolean isHudVisible() {
        return hudVisible;
    }

    public static void toggleOverlay() {
        overlayVisible = !overlayVisible;
    }

    public static boolean isOverlayVisible() {
        return overlayVisible;
    }

    public static void cycleOverlayMode() {
        overlayMode = (overlayMode % 3) + 1;
    }

    public static int getOverlayMode() {
        return overlayMode;
    }

    public static String getOverlayModeName() {
        if (overlayMode == 1) return "3D彩色柱子";
        if (overlayMode == 2) return "地表色块";
        return "线框网格";
    }
}
