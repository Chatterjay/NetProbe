package org.chatterjay.netprobe;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import org.joml.Matrix4f;

import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = Netprobe.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class ChunkOverlayRenderer {

    private static long lastRefreshTime = 0;
    private static List<Map.Entry<BlockPos, long[]>> cachedEntries = List.of();

    private static final RenderType OVERLAY = RenderType.create(
            "netprobe_overlay",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            256, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorShader))
                    .setTransparencyState(new RenderStateShard.TransparencyStateShard(
                            "translucent",
                            () -> {
                                RenderSystem.enableBlend();
                                RenderSystem.defaultBlendFunc();
                            },
                            () -> {
                                RenderSystem.disableBlend();
                                RenderSystem.defaultBlendFunc();
                            }))
                    .setDepthTestState(new RenderStateShard.DepthTestStateShard("always", 519))
                    .setCullState(new RenderStateShard.CullStateShard(false))
                    .createCompositeState(true));

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
        if (entries.isEmpty()) return;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        Font font = mc.font;

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);
        PoseStack.Pose poseEntry = poseStack.last();
        Matrix4f matrix = poseEntry.pose();

        if (NetProbeConfig.labelSeeThrough.get()) {
            RenderSystem.disableDepthTest();
        }

        OVERLAY.setupRenderState();
        VertexConsumer consumer = bufferSource.getBuffer(OVERLAY);
        for (Map.Entry<BlockPos, long[]> entry : entries) {
            long[] data = entry.getValue();
            long last = data[3];
            if (last <= 0) continue;

            BlockPos pos = entry.getKey();
            int color = getHeatColor(last);
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;
            int a = (int) (NetProbeConfig.overlayAlpha.get() * 255);

            renderBoxFill(consumer, matrix, pos, r, g, b, a);
        }
        bufferSource.endBatch(OVERLAY);
        OVERLAY.clearRenderState();

        for (Map.Entry<BlockPos, long[]> entry : entries) {
            long[] data = entry.getValue();
            long total = data[0];
            long last = data[3];
            long count = data[1];
            if (total <= 0 && last <= 0) continue;
            if (last <= 0) continue;

            BlockPos pos = entry.getKey();
            int heatColor = getHeatColor(last);
            int textColor = 0xFF000000 | heatColor;

            String line1 = formatBytes(total);
            String line2 = formatBytes(last) + " | " + count;

            poseStack.pushPose();
            poseStack.translate(pos.getX() + 0.5, pos.getY() + NetProbeConfig.renderHeight.get(), pos.getZ() + 0.5);
            poseStack.mulPose(camera.rotation());
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
            poseStack.scale(-0.025f, -0.025f, 0.025f);

            Matrix4f mat = poseStack.last().pose();

            Font.DisplayMode displayMode = NetProbeConfig.labelSeeThrough.get()
                    ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL;

            font.drawInBatch(line1,
                    -font.width(line1) / 2f, 0f,
                    textColor, false, mat, bufferSource,
                    displayMode, 0, LightTexture.FULL_BRIGHT);

            font.drawInBatch(line2,
                    -font.width(line2) / 2f, font.lineHeight + 2,
                    0xFFAAAAAA, false, mat, bufferSource,
                    displayMode, 0, LightTexture.FULL_BRIGHT);

            poseStack.popPose();
        }

        // font.drawInBatch 只缓存顶点，flush 必须在 restore depth test 之前
        bufferSource.endBatch();

        if (NetProbeConfig.labelSeeThrough.get()) {
            RenderSystem.enableDepthTest();
        }

        poseStack.popPose();
    }

    private static void renderBoxFill(VertexConsumer consumer, Matrix4f pose,
                                       BlockPos pos, int r, int g, int b, int a) {
        float x0 = pos.getX() + 0.02f, y0 = pos.getY() + 0.02f, z0 = pos.getZ() + 0.02f;
        float x1 = pos.getX() + 0.98f, y1 = pos.getY() + 0.98f, z1 = pos.getZ() + 0.98f;

        quad(consumer, pose, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, r, g, b, a);
        quad(consumer, pose, x0, y0, z0, x0, y0, z1, x1, y0, z1, x1, y0, z0, r, g, b, a);
        quad(consumer, pose, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, r, g, b, a);
        quad(consumer, pose, x0, y0, z1, x0, y1, z1, x1, y1, z1, x1, y0, z1, r, g, b, a);
        quad(consumer, pose, x0, y0, z0, x0, y1, z0, x0, y1, z1, x0, y0, z1, r, g, b, a);
        quad(consumer, pose, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, r, g, b, a);
    }

    private static void quad(VertexConsumer consumer, Matrix4f pose,
                              float x1, float y1, float z1,
                              float x2, float y2, float z2,
                              float x3, float y3, float z3,
                              float x4, float y4, float z4,
                              int r, int g, int b, int a) {
        consumer.addVertex(pose, x1, y1, z1).setColor(r, g, b, a);
        consumer.addVertex(pose, x2, y2, z2).setColor(r, g, b, a);
        consumer.addVertex(pose, x3, y3, z3).setColor(r, g, b, a);
        consumer.addVertex(pose, x4, y4, z4).setColor(r, g, b, a);
    }

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
