package org.chatterjay.netprobe.mixin;

import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.world.level.ChunkPos;
import org.chatterjay.netprobe.ChunkTrafficTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientboundLevelChunkWithLightPacket.class)
public class ChunkPacketMixin {

    @Shadow private int x;
    @Shadow private int z;
    @Shadow private ClientboundLevelChunkPacketData chunkData;

    @Inject(method = "handle", at = @At("HEAD"))
    private void netprobe$onHandle(CallbackInfo ci) {
        int size = 0;
        if (chunkData != null) {
            byte[] buf = ((ClientboundLevelChunkPacketDataAccessor) chunkData).getBuffer();
            size = buf != null ? buf.length : 0;
        }
        ChunkTrafficTracker.INSTANCE.recordChunk(
                new ChunkPos(this.x, this.z),
                size
        );
    }
}
