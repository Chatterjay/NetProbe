package org.chatterjay.netprobe;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod.EventBusSubscriber(modid = Netprobe.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ChunkOverlayRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger("NetProbe-Overlay");

    private static final int MAX_DISTANCE = 256;
    private static final double MAX_HEIGHT = 20.0;

    private static boolean loggedEvent = false;
    private static boolean loggedMixin = false;
    private static int frameCounter = 0;

    private static boolean shouldLog() {
        return frameCounter % 60 == 0;
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        if (!loggedEvent) {
            LOGGER.info("===== NetProbe 渲染路径: AFTER_PARTICLES (事件 PoseStack) =====");
            loggedEvent = true;
        }
        if (!ChunkMeter.isOverlayVisible()) return;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) return;

        frameCounter++;
        Camera cam = event.getCamera();
        Frustum frustum = event.getFrustum();
        float partialTick = event.getPartialTick();

        // 使用事件的 PoseStack (管线自带的摄像机旋转 R)
        // 手动加上摄像机平移 T(-camPos)，得到完整的视图矩阵 V = R * T
        Vec3 camPos = cam.getPosition();
        PoseStack poseStack = event.getPoseStack();
        var mvStack = RenderSystem.getModelViewStack();
        mvStack.pushPose();
        mvStack.last().pose().set(poseStack.last().pose());
        mvStack.last().pose().translate(-(float)camPos.x, -(float)camPos.y, -(float)camPos.z);
        RenderSystem.applyModelViewMatrix();

        // 渲染状态统一在这里管理 (depth test = off 让覆盖层始终可见)
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();

        try {
            doTestRendering(cam, "A-Event");
            doHeatmap(cam, partialTick, frustum, level, "A-Event");
        } finally {
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();

            mvStack.popPose();
            RenderSystem.applyModelViewMatrix();
        }
    }

    // ====================================================================
    // 渲染路径 B: Mixin at renderLevel TAIL (停用)
    // ====================================================================

    public static void renderOverlay(PoseStack poseStack, Camera camera, float partialTick, Frustum frustum) {
        if (!loggedMixin) {
            LOGGER.info("===== [渲染路径B] Mixin TAIL 已触发 (跳过) =====");
            loggedMixin = true;
        }
    }

    // ====================================================================
    // 测试渲染: 白色(0,64,0) + 红色(8,64,0) + 坐标轴
    // 使用世界坐标, 矩阵已由 setupMatrices 设置
    // ====================================================================

    private static void doTestRendering(Camera camera, String source) {
        try {
            RenderSystem.setShader(GameRenderer::getPositionColorShader);

            // 白色方块 世界(0,64,0)
            BufferBuilder bb = new BufferBuilder(512);
            bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            bb.vertex(-4, 64, -4).color(1f, 1f, 1f, 1f).endVertex();
            bb.vertex(-4, 64, 4).color(1f, 1f, 1f, 1f).endVertex();
            bb.vertex(4, 64, 4).color(1f, 1f, 1f, 1f).endVertex();
            bb.vertex(4, 64, -4).color(1f, 1f, 1f, 1f).endVertex();
            BufferUploader.drawWithShader(bb.end());

            // 红色方块 世界(8,64,0)
            bb = new BufferBuilder(512);
            bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            bb.vertex(4, 64, -4).color(1f, 0f, 0f, 1f).endVertex();
            bb.vertex(4, 64, 4).color(1f, 0f, 0f, 1f).endVertex();
            bb.vertex(12, 64, 4).color(1f, 0f, 0f, 1f).endVertex();
            bb.vertex(12, 64, -4).color(1f, 0f, 0f, 1f).endVertex();
            BufferUploader.drawWithShader(bb.end());

            // 坐标轴 世界(0,64,0)
            bb = new BufferBuilder(256);
            bb.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
            bb.vertex(-30, 64, 0).color(1f, 0f, 0f, 1f).endVertex();
            bb.vertex(30, 64, 0).color(1f, 0f, 0f, 1f).endVertex();
            bb.vertex(0, 60, 0).color(0f, 1f, 0f, 1f).endVertex();
            bb.vertex(0, 80, 0).color(0f, 1f, 0f, 1f).endVertex();
            bb.vertex(0, 64, -30).color(0f, 0f, 1f, 1f).endVertex();
            bb.vertex(0, 64, 30).color(0f, 0f, 1f, 1f).endVertex();
            BufferUploader.drawWithShader(bb.end());

            // 高可见度紫色方块 世界(0, 80, 0) — 在空中, 肯定能看到
            bb = new BufferBuilder(512);
            bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            bb.vertex(-8, 80, -8).color(1f, 0f, 1f, 0.8f).endVertex();
            bb.vertex(-8, 80, 8).color(1f, 0f, 1f, 0.8f).endVertex();
            bb.vertex(8, 80, 8).color(1f, 0f, 1f, 0.8f).endVertex();
            bb.vertex(8, 80, -8).color(1f, 0f, 1f, 0.8f).endVertex();
            BufferUploader.drawWithShader(bb.end());

            if (shouldLog()) {
                LOGGER.info("[{}] 测试方块: 白(0,64)+红(8,64)+紫(0,80)+轴", source);
            }

        } catch (Exception e) {
            LOGGER.error("[{}] 测试渲染异常: {}", source, e.getMessage());
        }
    }

    // ====================================================================
    // 热力图入口
    // ====================================================================

    private static void doHeatmap(Camera camera, float partialTick,
                                   Frustum frustum, ClientLevel level, String source) {
        ChunkTrafficTracker tracker = ChunkTrafficTracker.INSTANCE;
        if (tracker.getTotalBytes() <= 0) {
            if (shouldLog()) LOGGER.info("[{}][热力图] 无流量数据", source);
            return;
        }

        List<Map.Entry<ChunkPos, long[]>> chunks = tracker.getTopChunks(50);
        if (chunks.isEmpty()) {
            if (shouldLog()) LOGGER.info("[{}][热力图] 无区块数据", source);
            return;
        }

        long maxBytes = chunks.get(0).getValue()[0];
        if (maxBytes <= 0) {
            if (shouldLog()) LOGGER.info("[{}][热力图] maxBytes=0", source);
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        ChunkPos playerChunk = mc.player.chunkPosition();
        float gameTime = level.getGameTime() + partialTick;

        int totalBeforeFilter = chunks.size();
        int filteredByDistance = 0;
        int filteredByFrustum = 0;

        java.util.ArrayList<RenderChunk> visible = new java.util.ArrayList<>();

        for (Map.Entry<ChunkPos, long[]> entry : chunks) {
            ChunkPos pos = entry.getKey();
            long total = entry.getValue()[0];
            long last = entry.getValue()[1];

            int dx = Math.abs(pos.x - playerChunk.x);
            int dz = Math.abs(pos.z - playerChunk.z);
            if (dx * 16 > MAX_DISTANCE || dz * 16 > MAX_DISTANCE) {
                filteredByDistance++;
                continue;
            }

            var aabb = new AABB(
                    pos.getMinBlockX(), level.getMinBuildHeight(), pos.getMinBlockZ(),
                    pos.getMaxBlockX(), level.getMaxBuildHeight(), pos.getMaxBlockZ());
            if (frustum != null && !frustum.isVisible(aabb)) {
                filteredByFrustum++;
                continue;
            }

            int sy = level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getMinBlockX() + 8, pos.getMinBlockZ() + 8);

            float ratio = (float) total / maxBytes;
            double h = 2.0 + ratio * MAX_HEIGHT;
            boolean isTop = entry == chunks.get(0);

            visible.add(new RenderChunk(pos, total, last, sy, ratio, h, isTop));
        }

        if (visible.isEmpty()) {
            if (shouldLog()) {
                LOGGER.info("[{}][热力图] 可见区块为空: 总数={} 距离过滤={} 视锥体过滤={}",
                        source, totalBeforeFilter, filteredByDistance, filteredByFrustum);
            }
            return;
        }

        if (shouldLog()) {
            LOGGER.info("[{}][热力图] 可见区块数: {} (总数据源 {} 区块)",
                    source, visible.size(), chunks.size());
        }

        int mode = ChunkMeter.getOverlayMode();

        try {
            if (mode == 1) {
                renderColumns(visible, gameTime);
            } else if (mode == 2) {
                renderFlatQuads(visible, gameTime);
            } else if (mode == 3) {
                renderWireframe(visible, gameTime);
            }
            renderLabels(visible, font, camera, partialTick);
        } catch (Exception e) {
            LOGGER.error("[{}][热力图] 渲染异常: {} {}",
                    source, e.getClass().getSimpleName(), e.getMessage() != null ? e.getMessage() : "");
        }
    }

    // ===== Mode 1: 3D Columns =====
    private static void renderColumns(java.util.ArrayList<RenderChunk> chunks, float gameTime) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        for (RenderChunk rc : chunks) {
            float pulse = rc.isTop ? (float)(Math.sin(gameTime * 0.15) * 0.2 + 0.8f) : 1.0f;
            int col = heatColor(rc.ratio, pulse);

            double mx = rc.pos.getMinBlockX(), Mx = rc.pos.getMaxBlockX() + 1;
            double mz = rc.pos.getMinBlockZ(), Mz = rc.pos.getMaxBlockZ() + 1;
            double fy = rc.surfaceY + 0.1;
            double ty = fy + rc.height;

            BufferBuilder bb = new BufferBuilder(8192);
            bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

            // north wall
            for (double[] v : new double[][]{
                {mx, fy, mz, col}, {Mx, fy, mz, col}, {Mx, ty, mz, col}, {mx, ty, mz, col}
            }) { bb.vertex((float)v[0], (float)v[1], (float)v[2]).color((int)v[3]).endVertex(); }
            // south
            for (double[] v : new double[][]{
                {mx, fy, Mz, col}, {Mx, fy, Mz, col}, {Mx, ty, Mz, col}, {mx, ty, Mz, col}
            }) { bb.vertex((float)v[0], (float)v[1], (float)v[2]).color((int)v[3]).endVertex(); }
            // east
            for (double[] v : new double[][]{
                {Mx, fy, mz, col}, {Mx, fy, Mz, col}, {Mx, ty, Mz, col}, {Mx, ty, mz, col}
            }) { bb.vertex((float)v[0], (float)v[1], (float)v[2]).color((int)v[3]).endVertex(); }
            // west
            for (double[] v : new double[][]{
                {mx, fy, mz, col}, {mx, fy, Mz, col}, {mx, ty, Mz, col}, {mx, ty, mz, col}
            }) { bb.vertex((float)v[0], (float)v[1], (float)v[2]).color((int)v[3]).endVertex(); }
            // top
            for (double[] v : new double[][]{
                {mx, ty, mz, col}, {Mx, ty, mz, col}, {Mx, ty, Mz, col}, {mx, ty, Mz, col}
            }) { bb.vertex((float)v[0], (float)v[1], (float)v[2]).color((int)v[3]).endVertex(); }
            // bottom
            int gc = col & 0x00FFFFFF | 0x60000000;
            for (double[] v : new double[][]{
                {mx, rc.surfaceY, mz, gc}, {mx, rc.surfaceY, Mz, gc}, {Mx, rc.surfaceY, Mz, gc}, {Mx, rc.surfaceY, mz, gc}
            }) { bb.vertex((float)v[0], (float)v[1], (float)v[2]).color((int)v[3]).endVertex(); }

            BufferUploader.drawWithShader(bb.end());
        }
    }

    // ===== Mode 2: Flat quads =====
    private static void renderFlatQuads(java.util.ArrayList<RenderChunk> chunks, float gameTime) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        for (RenderChunk rc : chunks) {
            float pulse = rc.isTop ? (float)(Math.sin(gameTime * 0.15) * 0.2 + 0.8f) : 1.0f;
            int col = heatColor(rc.ratio, pulse);

            double mx = rc.pos.getMinBlockX(), Mx = rc.pos.getMaxBlockX() + 1;
            double mz = rc.pos.getMinBlockZ(), Mz = rc.pos.getMaxBlockZ() + 1;
            double y = rc.surfaceY + 0.5;

            BufferBuilder bb = new BufferBuilder(256);
            bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            for (double[] v : new double[][]{
                {mx, y, mz, col}, {mx, y, Mz, col}, {Mx, y, Mz, col}, {Mx, y, mz, col}
            }) { bb.vertex((float)v[0], (float)v[1], (float)v[2]).color((int)v[3]).endVertex(); }
            BufferUploader.drawWithShader(bb.end());
        }
    }

    // ===== Mode 3: Wireframe =====
    private static void renderWireframe(java.util.ArrayList<RenderChunk> chunks, float gameTime) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        for (RenderChunk rc : chunks) {
            float pulse = rc.isTop ? (float)(Math.sin(gameTime * 0.15) * 0.2 + 0.8f) : 1.0f;
            int bc = borderColor(rc.ratio, pulse);

            double mx = rc.pos.getMinBlockX(), Mx = rc.pos.getMaxBlockX() + 1;
            double mz = rc.pos.getMinBlockZ(), Mz = rc.pos.getMaxBlockZ() + 1;
            double y = rc.surfaceY + 0.5;
            double ty = y + 3.0;

            BufferBuilder bb = new BufferBuilder(512);
            bb.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
            // bottom rect
            for (double[] v : new double[][]{
                {mx, y, mz, bc}, {Mx, y, mz, bc}, {Mx, y, mz, bc}, {Mx, y, Mz, bc},
                {Mx, y, Mz, bc}, {mx, y, Mz, bc}, {mx, y, Mz, bc}, {mx, y, mz, bc}
            }) { bb.vertex((float)v[0], (float)v[1], (float)v[2]).color((int)v[3]).endVertex(); }
            // top rect
            for (double[] v : new double[][]{
                {mx, ty, mz, bc}, {Mx, ty, mz, bc}, {Mx, ty, mz, bc}, {Mx, ty, Mz, bc},
                {Mx, ty, Mz, bc}, {mx, ty, Mz, bc}, {mx, ty, Mz, bc}, {mx, ty, mz, bc}
            }) { bb.vertex((float)v[0], (float)v[1], (float)v[2]).color((int)v[3]).endVertex(); }
            // verticals
            for (double[] v : new double[][]{
                {mx, y, mz, bc}, {mx, ty, mz, bc}, {Mx, y, mz, bc}, {Mx, ty, mz, bc},
                {Mx, y, Mz, bc}, {Mx, ty, Mz, bc}, {mx, y, Mz, bc}, {mx, ty, Mz, bc}
            }) { bb.vertex((float)v[0], (float)v[1], (float)v[2]).color((int)v[3]).endVertex(); }
            BufferUploader.drawWithShader(bb.end());
        }
    }

    // ===== Text labels (最多显示 15 个，防 OOM) =====
    private static final int MAX_LABELS = 15;

    private static void renderLabels(java.util.ArrayList<RenderChunk> chunks,
                                     Font font, Camera camera, float partialTick) {
        try {
            // V = R * T(-cp): mulPose 先于 translate
            PoseStack ps = new PoseStack();
            Vec3 cp = camera.getPosition();
            ps.mulPose(camera.rotation());
            ps.translate(-cp.x, -cp.y, -cp.z);

            BufferBuilder bb = new BufferBuilder(65536);
            MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(bb);

            int count = 0;
            for (RenderChunk rc : chunks) {
                if (count++ >= MAX_LABELS) break;
                String label = String.format("累:%s 单:%s", formatBytes(rc.total), formatBytes(rc.last));
                double lx = rc.pos.getMinBlockX() + 8;
                double lz = rc.pos.getMinBlockZ() + 8;
                double ly = rc.surfaceY + rc.height + 1.0;

                ps.pushPose();
                ps.translate(lx, ly, lz);
                ps.mulPose(camera.rotation());
                ps.scale(0.03f, -0.03f, 0.03f);

                float tw = font.width(label);
                font.drawInBatch(label, -tw / 2, 0, 0xFFFFFFFF, false,
                        ps.last().pose(), bufferSource, Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);
                ps.popPose();
            }
            bufferSource.endBatch();
        } catch (Exception e) {
            LOGGER.error("[热力图-标签] 异常: {}", e.getMessage());
        }
    }

    // ===== Helpers =====

    private static int heatColor(float ratio, float pulse) {
        int r, g;
        if (ratio < 0.5f) {
            float t = ratio / 0.5f;
            r = (int) (t * 255);
            g = 255;
        } else {
            float t = (ratio - 0.5f) / 0.5f;
            r = 255;
            g = (int) ((1 - t) * 255);
        }
        int a = (int) (Math.min(200 + ratio * 55, 255) * pulse);
        return (Math.min(a, 255) << 24) | (r << 16) | (g << 8) | 0;
    }

    private static int borderColor(float ratio, float pulse) {
        int r, g;
        if (ratio < 0.5f) {
            float t = ratio / 0.5f;
            r = (int) (t * 200);
            g = 200;
        } else {
            float t = (ratio - 0.5f) / 0.5f;
            r = 200;
            g = (int) ((1 - t) * 200);
        }
        int a = (int) (245 + ratio * 10);
        return (Math.min(a, 255) << 24) | (r << 16) | (g << 8) | 0;
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private static class RenderChunk {
        final ChunkPos pos;
        final long total, last;
        final int surfaceY;
        final float ratio;
        final double height;
        final boolean isTop;

        RenderChunk(ChunkPos pos, long total, long last, int surfaceY, float ratio, double height, boolean isTop) {
            this.pos = pos;
            this.total = total;
            this.last = last;
            this.surfaceY = surfaceY;
            this.ratio = ratio;
            this.height = height;
            this.isTop = isTop;
        }
    }
}
