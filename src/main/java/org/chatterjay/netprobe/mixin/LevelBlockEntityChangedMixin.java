package org.chatterjay.netprobe.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Level.class)
public class LevelBlockEntityChangedMixin {

    @Inject(method = "blockEntityChanged", at = @At("HEAD"))
    private void netprobe$onBlockEntityChanged(BlockPos pos, CallbackInfo ci) {
        Level self = (Level)(Object)this;
        if (self.isClientSide()) return;

        BlockEntity be = self.getBlockEntity(pos);
        if (be != null && be.getUpdatePacket() != null) {
            BlockState state = self.getBlockState(pos);
            ((ServerLevel)self).sendBlockUpdated(pos, state, state, 3);
        }
    }
}
