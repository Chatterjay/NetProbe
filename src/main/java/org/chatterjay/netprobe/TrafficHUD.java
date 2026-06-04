package org.chatterjay.netprobe;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.ChunkPos;

import java.util.List;
import java.util.Map;

public class TrafficHUD {

    private static final int LINE_HEIGHT = 10;
    private static final int PADDING = 4;
    private static final int BG_COLOR = 0x80000000;

    public static void renderOverlay(GuiGraphics guiGraphics) {
        if (!ChunkMeter.isHudVisible()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.font == null) return;

        ChunkTrafficTracker tracker = ChunkTrafficTracker.INSTANCE;
        Font font = mc.font;

        var pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(0, 0, 200);

        var player = mc.player;
        int px = (int) Math.floor(player.getX());
        int py = (int) Math.floor(player.getY());
        int pz = (int) Math.floor(player.getZ());
        ChunkPos currentChunk = player.chunkPosition();

        List<Map.Entry<ChunkPos, long[]>> top = tracker.getTopChunks(5);

        String overlayStatus = ChunkMeter.isOverlayVisible()
                ? ChatFormatting.GREEN + " [热力图:" + ChunkMeter.getOverlayModeName() + "]" : "";
        String title = ChatFormatting.GOLD + "ChunkMeter" + overlayStatus;
        String posLine = String.format("位置: %d, %d, %d  |  区块: [%d, %d]",
                px, py, pz, currentChunk.x, currentChunk.z);
        String stats = String.format("区块数: %d  |  总流量: %s",
                tracker.getChunkCount(), formatBytes(tracker.getTotalBytes()));

        // Calculate width
        int textWidth = font.width(title);
        textWidth = Math.max(textWidth, font.width(posLine));
        textWidth = Math.max(textWidth, font.width(stats));
        for (Map.Entry<ChunkPos, long[]> entry : top) {
            String dir = directionFrom(currentChunk, entry.getKey());
            String line = String.format("#%d [%d, %d] 累:%s 单:%s  %s",
                    top.indexOf(entry) + 1, entry.getKey().x, entry.getKey().z,
                    formatBytes(entry.getValue()[0]), formatBytes(entry.getValue()[1]), dir);
            textWidth = Math.max(textWidth, font.width(line));
        }

        // Layout
        int x = PADDING;
        int y = PADDING;
        int bgWidth = textWidth + PADDING * 2 + 4;
        int lines = 3; // title, pos, stats
        if (!top.isEmpty()) {
            lines += 1 + top.size(); // header + entries
        }
        int bgHeight = lines * LINE_HEIGHT + PADDING * 2;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.fill(x - 2, y - 2, x + bgWidth, y + bgHeight, BG_COLOR);
        RenderSystem.disableBlend();

        y += PADDING;
        guiGraphics.drawString(font, title, x, y, 0xFFFFFF, false);
        y += LINE_HEIGHT;
        guiGraphics.drawString(font, posLine, x, y, 0xFFFFFF, false);
        y += LINE_HEIGHT;
        guiGraphics.drawString(font, stats, x, y, 0xFFFFFF, false);
        y += LINE_HEIGHT;

        if (!top.isEmpty()) {
            guiGraphics.drawString(font, ChatFormatting.GRAY + "--- Top 5 ---", x, y, 0xFFFFFF, false);
            y += LINE_HEIGHT;
            int rank = 1;
            for (Map.Entry<ChunkPos, long[]> entry : top) {
                String dir = directionFrom(currentChunk, entry.getKey());
                String line = String.format("#%d [%d, %d] 累:%s 单:%s  %s",
                        rank++, entry.getKey().x, entry.getKey().z,
                        formatBytes(entry.getValue()[0]), formatBytes(entry.getValue()[1]), dir);
                guiGraphics.drawString(font, ChatFormatting.AQUA + line, x, y, 0xFFFFFF, false);
                y += LINE_HEIGHT;
            }
        }

        pose.popPose();
    }

    private static String directionFrom(ChunkPos origin, ChunkPos target) {
        int dx = target.x - origin.x;
        int dz = target.z - origin.z;

        String dirX;
        String dirZ;
        if (dx > 0) {
            dirX = "东";
        } else if (dx < 0) {
            dirX = "西";
        } else {
            dirX = "";
        }
        if (dz > 0) {
            dirZ = "南";
        } else if (dz < 0) {
            dirZ = "北";
        } else {
            dirZ = "";
        }

        if (dx == 0 && dz == 0) return ChatFormatting.GREEN + "★ 当前";

        String arrow;
        if (dx == 0) {
            arrow = dz < 0 ? "↑" : "↓";
        } else if (dz == 0) {
            arrow = dx > 0 ? "→" : "←";
        } else if (dx > 0 && dz < 0) {
            arrow = "↗";
        } else if (dx > 0 && dz > 0) {
            arrow = "↘";
        } else if (dx < 0 && dz < 0) {
            arrow = "↖";
        } else {
            arrow = "↙";
        }

        return String.format("%s%s %s", dirZ, dirX, arrow);
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
