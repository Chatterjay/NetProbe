package org.chatterjay.netprobe;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Netprobe.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ChunkMeter {

    private static boolean debugVisible = false;

    public static void toggleDebug() { debugVisible = !debugVisible; }
    public static boolean isDebugVisible() { return debugVisible; }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        ChunkTrafficTracker.INSTANCE.reset();
        BlockTrafficTracker.INSTANCE.reset();
        PacketTrafficTracker.INSTANCE.reset();
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        BlockTrafficTracker.INSTANCE.remove(event.getPos());
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        BlockTrafficTracker.INSTANCE.remove(event.getPos());
    }
}
