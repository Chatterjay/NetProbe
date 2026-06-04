package org.chatterjay.netprobe;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Netprobe.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ChunkMeterCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("chunkmeter")
                .executes(ctx -> {
                    ChunkMeter.toggleHud();
                    sendMessage(ChunkMeter.isHudVisible()
                            ? "ChunkMeter HUD 已显示"
                            : "ChunkMeter HUD 已隐藏");
                    return 1;
                })
                .then(Commands.literal("reset")
                        .executes(ctx -> {
                            ChunkTrafficTracker.INSTANCE.reset();
                            sendMessage("ChunkMeter 统计数据已重置");
                            return 1;
                        })
                )
                .then(Commands.literal("toggle")
                        .executes(ctx -> {
                            ChunkMeter.toggleHud();
                            sendMessage(ChunkMeter.isHudVisible()
                                    ? "ChunkMeter HUD 已显示"
                                    : "ChunkMeter HUD 已隐藏");
                            return 1;
                        })
                )
                .then(Commands.literal("overlay")
                        .executes(ctx -> {
                            ChunkMeter.toggleOverlay();
                            String status = ChunkMeter.isOverlayVisible() ? "已显示" : "已隐藏";
                            sendMessage("区块热力图" + status + " | 当前模式: " + ChunkMeter.getOverlayModeName());
                            return 1;
                        })
                )
                .then(Commands.literal("mode")
                        .executes(ctx -> {
                            ChunkMeter.cycleOverlayMode();
                            sendMessage("切换渲染模式: " + ChunkMeter.getOverlayModeName());
                            return 1;
                        })
                )
        );
    }

    private static void sendMessage(String msg) {
        Minecraft.getInstance().gui.getChat().addMessage(
                Component.literal(ChatFormatting.GOLD + "[ChunkMeter] " + ChatFormatting.WHITE + msg)
        );
    }
}
