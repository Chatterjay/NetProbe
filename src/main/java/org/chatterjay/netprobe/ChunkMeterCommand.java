package org.chatterjay.netprobe;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = Netprobe.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class ChunkMeterCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("chunkmeter")
                .executes(ctx -> {
                    ChunkMeter.toggleDebug();
                    sendMessage(ChunkMeter.isDebugVisible()
                            ? I18n.get("netprobe.cmd.toggle_on")
                            : I18n.get("netprobe.cmd.toggle_off"));
                    return 1;
                })
                .then(Commands.literal("toggle")
                        .executes(ctx -> {
                            ChunkMeter.toggleDebug();
                            sendMessage(ChunkMeter.isDebugVisible()
                                    ? I18n.get("netprobe.cmd.toggle_on")
                                    : I18n.get("netprobe.cmd.toggle_off"));
                            return 1;
                        })
                )
                .then(Commands.literal("reset")
                        .executes(ctx -> {
                            ChunkTrafficTracker.INSTANCE.reset();
                            BlockTrafficTracker.INSTANCE.reset();
                            PacketTrafficTracker.INSTANCE.reset();
                            sendMessage(I18n.get("netprobe.cmd.reset"));
                            return 1;
                        })
                )
                .then(Commands.literal("inspect")
                        .executes(ctx -> {
                            inspect();
                            return 1;
                        })
                )
                .then(Commands.literal("blockoverlay")
                        .executes(ctx -> {
                            ChunkMeter.toggleBlockOverlay();
                            sendMessage(ChunkMeter.isBlockOverlayVisible()
                                    ? I18n.get("netprobe.cmd.blockoverlay_on")
                                    : I18n.get("netprobe.cmd.blockoverlay_off"));
                            return 1;
                        })
                )
        );
    }

    private static void inspect() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ChunkPos chunk = mc.player.chunkPosition();
        ChunkTrafficTracker ct = ChunkTrafficTracker.INSTANCE;
        BlockTrafficTracker bt = BlockTrafficTracker.INSTANCE;
        PacketTrafficTracker pt = PacketTrafficTracker.INSTANCE;

        sendMsg("§6=== NetProbe 数据检查 ===");
        sendMsg("§7当前区块: §f[" + chunk.x + ", " + chunk.z + "]  §7累:§f" + formatBytes(ct.getTotalBytes(chunk)) + "  §7单:§f" + formatBytes(ct.getLastBytes(chunk)));
        sendMsg("§7区块总量: §f" + formatBytes(ct.getTotalBytes()) + "  §7(区块包:§f" + formatBytes(ct.getChunkPacketBytes()) + "§7 + 方块更新:§f" + formatBytes(ct.getBlockUpdateBytes()) + "§7)");
        sendMsg("§7网络总流量: §f" + formatBytes(pt.getDecoderTotalBytes()) + "  §7区块记录: §f" + ct.getChunkCount());

        List<Map.Entry<BlockPos, long[]>> blocksInChunk = new ArrayList<>();
        for (Map.Entry<BlockPos, long[]> e : bt.getTopBlocks(50)) {
            BlockPos bp = e.getKey();
            if (ChunkPos.asLong(bp.getX() >> 4, bp.getZ() >> 4) == chunk.toLong()) {
                blocksInChunk.add(e);
            }
        }
        if (!blocksInChunk.isEmpty()) {
            sendMsg("§7当前区块追踪方块 (§f" + blocksInChunk.size() + "§7个):");
            int n = 0;
            for (Map.Entry<BlockPos, long[]> e : blocksInChunk) {
                if (n++ >= 5) { sendMsg("  §8... 还有 " + (blocksInChunk.size() - 5) + " 个"); break; }
                BlockPos bp = e.getKey();
                long[] d = e.getValue();
                sendMsg("  §8[" + bp.getX() + "," + bp.getY() + "," + bp.getZ() + "] §f累:" + formatBytes(d[0]) + " §7单:" + formatBytes(d[3]) + " §f" + d[1] + "§7次");
            }
        }

        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos bp = ((BlockHitResult) mc.hitResult).getBlockPos();
            sendMsg("§7指向方块: §8[" + bp.getX() + "," + bp.getY() + "," + bp.getZ() + "] §f累:" + formatBytes(bt.getTotalBytes(bp))
                    + " §7单:" + formatBytes(bt.getLastBytes(bp)) + " §f" + bt.getUpdateCount(bp) + "§7次更新");
        }
        sendMsg("§6======================");
    }

    private static void sendMsg(String msg) {
        Minecraft.getInstance().gui.getChat().addMessage(Component.literal(msg));
    }

    private static void sendMessage(String msg) {
        Minecraft.getInstance().gui.getChat().addMessage(
                Component.literal(ChatFormatting.GOLD + "[NetProbe] " + ChatFormatting.WHITE + msg)
        );
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}
