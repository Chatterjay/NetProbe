package org.chatterjay.netprobe.mixin;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.SectionPos;
import org.chatterjay.netprobe.BlockTrafficTracker;
import org.chatterjay.netprobe.ChunkTrafficTracker;
import org.chatterjay.netprobe.Netprobe;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientboundSectionBlocksUpdatePacket.class)
public class SectionBlocksUpdateMixin {

    private static final Logger LOG = LogUtils.getLogger();
    private static int debugCount = 0;

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
        if (Netprobe.isDebugMode() && ++debugCount % 10 == 1) {
            short p0 = positions[0];
            LOG.info("[NetProbe] SectionUpdate #{} section=[{},{},{}] count={} first=[{},{},{}]",
                debugCount, sectionPos.x(), sectionPos.y(), sectionPos.z(), positions.length,
                sectionPos.relativeToBlockX(p0), sectionPos.relativeToBlockY(p0), sectionPos.relativeToBlockZ(p0));
        }
    }
}
