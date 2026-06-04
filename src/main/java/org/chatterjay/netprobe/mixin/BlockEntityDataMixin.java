package org.chatterjay.netprobe.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import org.chatterjay.netprobe.BlockTrafficTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

@Mixin(ClientboundBlockEntityDataPacket.class)
public class BlockEntityDataMixin {

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
        BlockTrafficTracker.INSTANCE.recordBlock(pos, Math.max(size, 1));
    }
}
