package org.chatterjay.netprobe;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = Netprobe.MODID, value = Dist.CLIENT)
public class NetProbeClientSetup {

    @SubscribeEvent
    static void setup(FMLClientSetupEvent event) {
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class,
                ChunkOverlayRenderer::onRenderLevelStage);
        NeoForge.EVENT_BUS.addListener(CustomizeGuiOverlayEvent.DebugText.class,
                TrafficHUD::onDebugText);
        NeoForge.EVENT_BUS.addListener(RegisterClientCommandsEvent.class,
                ChunkMeterCommand::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class,
                ChunkMeter::onPlayerLogin);
        NeoForge.EVENT_BUS.addListener(BlockEvent.BreakEvent.class,
                ChunkMeter::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(BlockEvent.EntityPlaceEvent.class,
                ChunkMeter::onBlockPlace);
    }
}
