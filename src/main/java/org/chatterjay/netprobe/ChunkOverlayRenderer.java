package org.chatterjay.netprobe;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 世界渲染已停用，流量通过左上角 HUD 和 F3 调试界面显示。
 */
@Mod.EventBusSubscriber(modid = Netprobe.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ChunkOverlayRenderer {

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        // 不再渲染世界覆盖层
    }

    public static void renderOverlay(PoseStack poseStack, Camera camera, float partialTick, Frustum frustum) {
    }
}
