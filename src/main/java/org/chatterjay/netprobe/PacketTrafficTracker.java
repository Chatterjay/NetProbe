package org.chatterjay.netprobe;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 从 PacketDecoder 采集的 Minecraft 网络层总字节统计
 */
public class PacketTrafficTracker {

    public static final PacketTrafficTracker INSTANCE = new PacketTrafficTracker();

    private final AtomicLong totalDecoderBytes = new AtomicLong(0);
    private long ratePrevBytes = 0;
    private long ratePrevTime = System.currentTimeMillis();
    private double rateBps = 0;

    private PacketTrafficTracker() {}

    /** 从 PacketDecoder 累加字节 */
    public void addPacketBytes(int bytes) {
        totalDecoderBytes.addAndGet(bytes);
    }

    public long getDecoderTotalBytes() { return totalDecoderBytes.get(); }

    /** 计算每秒速率 (20 tick ≈ 1s 更新一次) */
    public double getDecoderBytesPerSecond() {
        long now = System.currentTimeMillis();
        long curBytes = totalDecoderBytes.get();
        long dt = now - ratePrevTime;
        if (dt > 800) { // ~20 game ticks
            rateBps = (double) (curBytes - ratePrevBytes) / dt * 1000.0;
            ratePrevBytes = curBytes;
            ratePrevTime = now;
        }
        return rateBps;
    }

    public void reset() {
        totalDecoderBytes.set(0);
        ratePrevBytes = 0;
        ratePrevTime = System.currentTimeMillis();
        rateBps = 0;
    }
}
