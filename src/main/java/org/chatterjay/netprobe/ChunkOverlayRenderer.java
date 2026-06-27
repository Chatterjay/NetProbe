package org.chatterjay.netprobe;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
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

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        BlockTrafficTracker.INSTANCE.trimToSize(500);
        BlockTrafficTracker.INSTANCE.cleanupBroken(30000);
        BlockTrafficTracker.INSTANCE.expireStale(30000);

        if (!ChunkMeter.isBlockOverlayVisible()) return;

        List<Map.Entry<BlockPos, long[]>> entries = BlockTrafficTracker.INSTANCE.getTopBlocks(500);

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
            float a = 0.28f;

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

        RenderSystem.enableCull();
        // depth test stays disabled for text layer

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
            poseStack.translate(pos.getX() + 0.5, pos.getY() + 1.04, pos.getZ() + 0.5);
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

    /** Map bytes to ARGB heat color: green(low) → yellow → red(high) on log scale */
    private static int getHeatColor(long bytes) {
        double t = Math.log10(Math.max(bytes, 1)) / Math.log10(1024); // 0 at 1B, 1 at 1KB
        t = Math.min(t / 1.5, 1.0); // spread so 1KB is orange, need ~10KB for full red
        if (t < 0.5) {
            float s = (float)(t * 2);
            int r = (int)(255 * s);
            return (r << 16) | 0x00FF00;
        } else {
            float s = (float)((t - 0.5) * 2);
            int g = (int)(255 * (1 - s));
            return 0xFF0000 | (g << 8);
        }
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}
