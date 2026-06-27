package org.chatterjay.netprobe.mixin;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

@Mixin(ClientboundBlockEntityDataPacket.class)
public class BlockEntityDataMixin {

    private static final Logger LOG = LogUtils.getLogger();
    private static int debugCount = 0;

    @Shadow private BlockPos pos;
    @Shadow private CompoundTag tag;

    @Inject(method = "handle", at = @At("HEAD"))
    private void netprobe$onHandle(CallbackInfo ci) {
        int size = 0;
        if (tag != null) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos);
                net.minecraft.nbt.NbtIo.write(tag, dos);
                size = baos.size();
            } catch (Exception ignored) {}
        }
        int finalSize = Math.max(size, 1);

        BlockTrafficTracker.INSTANCE.recordBlock(pos, finalSize, 1000);
        ChunkTrafficTracker.INSTANCE.addBlockBytes(new ChunkPos(pos), finalSize);
        if (Netprobe.isDebugMode() && ++debugCount % 10 == 1) {
            LOG.info("[NetProbe] BlockEntityData #{} pos=[{},{},{}] size={}", debugCount, pos.getX(), pos.getY(), pos.getZ(), finalSize);
        }
    }
}
