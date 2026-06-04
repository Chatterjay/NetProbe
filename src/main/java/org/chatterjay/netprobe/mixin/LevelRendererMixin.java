package org.chatterjay.netprobe.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.chatterjay.netprobe.ChunkOverlayRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin at renderLevel TAIL — poseStack is in world space here.
 * ChunkOverlayRenderer.renderOverlay handles camera transform internally.
 */
@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void netprobe$onRenderLevel(PoseStack poseStack, float partialTick, long finishNanoTime,
                                        boolean blockOutlines, Camera camera, GameRenderer gameRenderer,
                                        LightTexture lightTexture, Matrix4f projectionMatrix,
                                        CallbackInfo ci) {
        ChunkOverlayRenderer.renderOverlay(poseStack, camera, partialTick, null);
    }
}
