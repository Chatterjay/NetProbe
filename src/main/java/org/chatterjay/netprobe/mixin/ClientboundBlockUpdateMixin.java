package org.chatterjay.netprobe.mixin;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.world.level.ChunkPos;
import org.chatterjay.netprobe.BlockTrafficTracker;
import org.chatterjay.netprobe.ChunkTrafficTracker;
import org.chatterjay.netprobe.Netprobe;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientboundBlockUpdatePacket.class)
public class ClientboundBlockUpdateMixin {

    private static final Logger LOG = LogUtils.getLogger();
    private static int debugCount = 0;

    @Shadow private BlockPos pos;

    @Inject(method = "handle", at = @At("HEAD"))
    private void netprobe$onHandle(CallbackInfo ci) {
        int estSize = 3;
        BlockTrafficTracker.INSTANCE.recordBlock(pos, estSize);
        ChunkTrafficTracker.INSTANCE.addBlockBytes(new ChunkPos(pos), estSize);
        if (Netprobe.isDebugMode() && ++debugCount % 100 == 1) {
            LOG.info("[NetProbe] BlockUpdate #{} pos=[{},{},{}] size={}", debugCount, pos.getX(), pos.getY(), pos.getZ(), estSize);
        }
    }
}
