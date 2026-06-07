package org.chatterjay.netprobe.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.ChunkPos;
import org.chatterjay.netprobe.BlockTrafficTracker;
import org.chatterjay.netprobe.ChunkTrafficTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(ClientboundBlockEntityDataPacket.class)
public class BlockEntityDataMixin {

    private static final long DEDUPE_WINDOW_MS = 100;
    private static final ConcurrentHashMap<BlockPos, long[]> RECENT_PACKETS = new ConcurrentHashMap<>();

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
        if (isDuplicate(pos, finalSize)) return;

        BlockTrafficTracker.INSTANCE.recordBlock(pos, finalSize);
        ChunkTrafficTracker.INSTANCE.addBlockBytes(new ChunkPos(pos), finalSize);
    }

    private static boolean isDuplicate(BlockPos pos, int size) {
        long now = System.currentTimeMillis();
        long[] prev = RECENT_PACKETS.put(pos, new long[]{size, now});
        return prev != null && (int) prev[0] == size && now - prev[1] <= DEDUPE_WINDOW_MS;
    }
}
