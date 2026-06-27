package org.chatterjay.netprobe.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.SectionPos;
import org.chatterjay.netprobe.BlockTrafficTracker;
import org.chatterjay.netprobe.ChunkTrafficTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientboundSectionBlocksUpdatePacket.class)
public class SectionBlocksUpdateMixin {

    @Shadow private SectionPos sectionPos;
    @Shadow private short[] positions;
    @Shadow private BlockState[] states;

    @Inject(method = "handle", at = @At("HEAD"))
    private void netprobe$onHandle(CallbackInfo ci) {
        if (positions == null || positions.length == 0) return;
        int estSize = 5;
        ChunkPos chunkPos = new ChunkPos(sectionPos.x(), sectionPos.z());
        int totalForChunk = positions.length * estSize;
        for (int i = 0; i < positions.length; i++) {
            short packed = positions[i];
            BlockPos pos = new BlockPos(
                sectionPos.relativeToBlockX(packed),
                sectionPos.relativeToBlockY(packed),
                sectionPos.relativeToBlockZ(packed)
            );
            BlockTrafficTracker.INSTANCE.recordBlock(pos, estSize);
        }
        ChunkTrafficTracker.INSTANCE.addBlockBytes(chunkPos, totalForChunk);
    }
}
