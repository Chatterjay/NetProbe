package org.chatterjay.netprobe;

import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = Netprobe.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class ChunkMeter {

    private static boolean debugVisible = false;
    private static boolean blockOverlayVisible = false;

    public static void toggleDebug() { debugVisible = !debugVisible; }
    public static boolean isDebugVisible() { return debugVisible; }

    public static void toggleBlockOverlay() { blockOverlayVisible = !blockOverlayVisible; }
    public static boolean isBlockOverlayVisible() { return blockOverlayVisible; }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        ChunkTrafficTracker.INSTANCE.reset();
        BlockTrafficTracker.INSTANCE.reset();
        PacketTrafficTracker.INSTANCE.reset();
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        BlockPos pos = event.getPos();
        if (pos != null) BlockTrafficTracker.INSTANCE.markBroken(pos);
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        BlockPos pos = event.getPos();
        if (pos != null) BlockTrafficTracker.INSTANCE.markBroken(pos);
    }
}
