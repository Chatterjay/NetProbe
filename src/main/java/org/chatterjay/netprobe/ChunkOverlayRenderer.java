package org.chatterjay.netprobe;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Netprobe.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ChunkOverlayRenderer {

    private static long lastRefreshTime = 0;
    private static List<Map.Entry<BlockPos, long[]>> cachedEntries = List.of();

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        BlockTrafficTracker.INSTANCE.trimToSize(500);
        BlockTrafficTracker.INSTANCE.cleanupBroken(30000);
        BlockTrafficTracker.INSTANCE.expireStale(30000);

        if (!ChunkMeter.isBlockOverlayVisible()) return;

        long now = System.currentTimeMillis();
        if (now - lastRefreshTime >= NetProbeConfig.refreshInterval.get() * 50L) {
            lastRefreshTime = now;
            cachedEntries = BlockTrafficTracker.INSTANCE.getTopBlocks(500);
        }

        List<Map.Entry<BlockPos, long[]>> entries = cachedEntries;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        Font font = mc.font;

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        // --- Block face overlays ---
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = poseStack.last().pose();

        for (Map.Entry<BlockPos, long[]> entry : entries) {
            long[] data = entry.getValue();
            long last = data[3];
            if (last <= 0) continue;

            BlockPos pos = entry.getKey();
            int color = getHeatColor(last);
            float r = ((color >> 16) & 0xFF) / 255f;
            float g = ((color >> 8) & 0xFF) / 255f;
            float b = (color & 0xFF) / 255f;
            float a = NetProbeConfig.overlayAlpha.get().floatValue();

            float x0 = pos.getX() + 0.02f;
            float y0 = pos.getY() + 0.02f;
            float z0 = pos.getZ() + 0.02f;
            float x1 = pos.getX() + 0.98f;
            float y1 = pos.getY() + 0.98f;
            float z1 = pos.getZ() + 0.98f;

            // Top Y+
            builder.vertex(matrix, x0, y1, z0).color(r, g, b, a).endVertex();
            builder.vertex(matrix, x0, y1, z1).color(r, g, b, a).endVertex();
            builder.vertex(matrix, x1, y1, z1).color(r, g, b, a).endVertex();
            builder.vertex(matrix, x1, y1, z0).color(r, g, b, a).endVertex();
            // Bottom Y-
            builder.vertex(matrix, x1, y0, z0).color(r, g, b, a).endVertex();
            builder.vertex(matrix, x1, y0, z1).color(r, g, b, a).endVertex();
            builder.vertex(matrix, x0, y0, z1).color(r, g, b, a).endVertex();
            builder.vertex(matrix, x0, y0, z0).color(r, g, b, a).endVertex();
            // North -Z
            builder.vertex(matrix, x0, y1, z0).color(r, g, b, a).endVertex();
            builder.vertex(matrix, x0, y0, z0).color(r, g, b, a).endVertex();
            builder.vertex(matrix, x1, y0, z0).color(r, g, b, a).endVertex();
            builder.vertex(matrix, x1, y1, z0).color(r, g, b, a).endVertex();
            // South +Z
            builder.vertex(matrix, x1, y1, z1).color(r, g, b, a).endVertex();
            builder.vertex(matrix, x1, y0, z1).color(r, g, b, a).endVertex();
            builder.vertex(matrix, x0, y0, z1).color(r, g, b, a).endVertex();
            builder.vertex(matrix, x0, y1, z1).color(r, g, b, a).endVertex();
            // West -X
            builder.vertex(matrix, x0, y1, z0).color(r, g, b, a).endVertex();
            builder.vertex(matrix, x0, y1, z1).color(r, g, b, a).endVertex();
            builder.vertex(matrix, x0, y0, z1).color(r, g, b, a).endVertex();
            builder.vertex(matrix, x0, y0, z0).color(r, g, b, a).endVertex();
            // East +X
            builder.vertex(matrix, x1, y1, z1).color(r, g, b, a).endVertex();
            builder.vertex(matrix, x1, y1, z0).color(r, g, b, a).endVertex();
            builder.vertex(matrix, x1, y0, z0).color(r, g, b, a).endVertex();
            builder.vertex(matrix, x1, y0, z1).color(r, g, b, a).endVertex();
        }

        tesselator.end();

        // --- Text labels ---
        for (Map.Entry<BlockPos, long[]> entry : entries) {
            long[] data = entry.getValue();
            long total = data[0];
            long last = data[3];
            long count = data[1];
            if (total <= 0 && last <= 0) continue;
            if (last <= 0) continue; // skip blocks with no recent traffic for text

            BlockPos pos = entry.getKey();
            int heatColor = getHeatColor(last);
            int textColor = 0xFF000000 | heatColor;

            String line1 = formatBytes(total);
            String line2 = formatBytes(last) + " | " + count;

            poseStack.pushPose();
            poseStack.translate(pos.getX() + 0.5, pos.getY() + NetProbeConfig.renderHeight.get(), pos.getZ() + 0.5);
            poseStack.mulPose(camera.rotation());
            poseStack.scale(-0.025f, -0.025f, 0.025f);

            Matrix4f mat = poseStack.last().pose();

            font.drawInBatch(line1,
                    -font.width(line1) / 2f, 0f,
                    textColor, false, mat, bufferSource,
                    Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT);

            font.drawInBatch(line2,
                    -font.width(line2) / 2f, font.lineHeight + 2,
                    0xFFAAAAAA, false, mat, bufferSource,
                    Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT);

            poseStack.popPose();
        }

        poseStack.popPose();
        bufferSource.endBatch();
        RenderSystem.enableDepthTest();
    }

    public static void renderOverlay(PoseStack poseStack, Camera camera, float partialTick, Frustum frustum) {
    }

    /** Map bytes to color using configured thresholds: normal → warning → high */
    private static int getHeatColor(long bytes) {
        long n = NetProbeConfig.normalMax.get();
        long w = NetProbeConfig.warningMax.get();
        int cLow = parseHex(NetProbeConfig.colorLow.get());
        int cMid = parseHex(NetProbeConfig.colorMid.get());
        int cHigh = parseHex(NetProbeConfig.colorHigh.get());

        if (bytes <= n) {
            return cLow;
        } else if (bytes <= w) {
            float t = (float)(bytes - n) / Math.max(w - n, 1);
            return lerpColor(cLow, cMid, t);
        } else {
            // smooth transition beyond warning, capped at 10x warningMax for full red
            float t = Math.min((float)(bytes - w) / (w * 9f + 1f), 1.0f);
            return lerpColor(cMid, cHigh, t);
        }
    }

    private static int parseHex(String hex) {
        try { return Integer.parseInt(hex, 16); } catch (NumberFormatException e) { return 0x00FF00; }
    }

    private static int lerpColor(int from, int to, float t) {
        int fr = (from >> 16) & 0xFF, fg = (from >> 8) & 0xFF, fb = from & 0xFF;
        int tr = (to >> 16) & 0xFF, tg = (to >> 8) & 0xFF, tb = to & 0xFF;
        int r = fr + (int)((tr - fr) * t);
        int g = fg + (int)((tg - fg) * t);
        int b = fb + (int)((tb - fb) * t);
        return (r << 16) | (g << 8) | b;
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}
