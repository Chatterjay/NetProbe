package org.chatterjay.netprobe.mixin;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.PacketDecoder;
import org.chatterjay.netprobe.PacketTrafficTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(PacketDecoder.class)
public class PacketDecoderMixin {

    @Inject(method = "decode", at = @At("HEAD"))
    private void netprobe$onDecodeHead(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out, CallbackInfo ci) {
        PacketTrafficTracker.INSTANCE.addPacketBytes(buf.readableBytes());
    }
}
