package org.chatterjay.netprobe;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Netprobe.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TrafficHUD {

    @SubscribeEvent
    public static void onDebugText(CustomizeGuiOverlayEvent.DebugText event) {
        if (!ChunkMeter.isDebugVisible()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        ChunkTrafficTracker cTracker = ChunkTrafficTracker.INSTANCE;
        ChunkPos currentChunk = mc.player.chunkPosition();
        BlockTrafficTracker.INSTANCE.expireOldEntries(30000);

        // 标题
        event.getLeft().add("");
        event.getLeft().add(ChatFormatting.GOLD + I18n.get("netprobe.f3.title"));

        // 系统网卡 & 模组对比
        double sysKBps = NetworkStats.getKBytesPerSecond();
        double decoderBps = PacketTrafficTracker.INSTANCE.getDecoderBytesPerSecond();
        double decoderKBps = decoderBps / 1024.0;
        double estActualKBps = decoderKBps / 1.3;

        if (NetworkStats.isAvailable()) {
            String pct = sysKBps > 0 ? String.format("%.0f", decoderKBps / sysKBps * 100) : "0";
            event.getLeft().add(I18n.get("netprobe.f3.system_card") + fmtKB(sysKBps) + I18n.get("netprobe.f3.system_card_unit"));
            event.getLeft().add(I18n.get("netprobe.f3.mod_measure") + fmtKB(decoderKBps)
                    + I18n.get("netprobe.f3.mod_measure_body") + fmtKB(estActualKBps)
                    + I18n.get("netprobe.f3.mod_measure_tail") + pct + "%]");
        } else {
            event.getLeft().add(I18n.get("netprobe.f3.system_loading"));
        }
        event.getLeft().add(I18n.get("netprobe.f3.total_traffic") + formatBytes(PacketTrafficTracker.INSTANCE.getDecoderTotalBytes()));

        // 区块/方块详情
        event.getLeft().add(ChatFormatting.GRAY + I18n.get("netprobe.f3.chunk_section"));
        event.getLeft().add(I18n.get("netprobe.f3.chunk_detail_pkt") + formatBytes(cTracker.getChunkPacketBytes())
                + I18n.get("netprobe.f3.chunk_detail_plus") + formatBytes(cTracker.getBlockUpdateBytes())
                + I18n.get("netprobe.f3.chunk_detail_eq") + formatBytes(cTracker.getTotalBytes()));
        event.getLeft().add(I18n.get("netprobe.f3.chunk_count") + cTracker.getChunkCount());

        // 当前区块
        long curTotal = cTracker.getTotalBytes(currentChunk);
        long curLast = cTracker.getLastBytes(currentChunk);
        event.getLeft().add(I18n.get("netprobe.f3.current_chunk_prefix") + currentChunk.x + "," + currentChunk.z
                + I18n.get("netprobe.f3.current_chunk_mid") + formatBytes(curTotal)
                + I18n.get("netprobe.f3.current_chunk_last") + formatBytes(curLast));

        // Top区块
        List<Map.Entry<ChunkPos, long[]>> top = cTracker.getTopChunks(3);
        if (!top.isEmpty()) {
            StringBuilder sb = new StringBuilder(ChatFormatting.GRAY + I18n.get("netprobe.f3.top_chunks"));
            for (int i = 0; i < top.size(); i++) {
                if (i > 0) sb.append("  ");
                ChunkPos p = top.get(i).getKey();
                sb.append("[").append(p.x).append(",").append(p.z).append("]").append(formatBytes(top.get(i).getValue()[0]));
            }
            event.getLeft().add(sb.toString());
        }

        // 方块追踪
        event.getLeft().add(I18n.get("netprobe.f3.block_summary") + BlockTrafficTracker.INSTANCE.getBlockCount()
                + I18n.get("netprobe.f3.block_summary_unit") + formatBytes(BlockTrafficTracker.INSTANCE.getTotalBytes()));

        // 指向方块
        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos bp = ((BlockHitResult) mc.hitResult).getBlockPos();
            event.getLeft().add(I18n.get("netprobe.f3.target_block_prefix") + bp.getX() + "," + bp.getY() + "," + bp.getZ()
                    + I18n.get("netprobe.f3.target_block_mid") + formatBytes(BlockTrafficTracker.INSTANCE.getTotalBytes(bp))
                    + I18n.get("netprobe.f3.target_block_last") + formatBytes(BlockTrafficTracker.INSTANCE.getLastBytes(bp))
                    + " " + BlockTrafficTracker.INSTANCE.getUpdateCount(bp) + I18n.get("netprobe.f3.target_block_updates"));
        }
    }

    private static String fmtKB(double kb) {
        return String.format("%.0f", kb);
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}
