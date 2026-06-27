package org.chatterjay.netprobe;

import java.util.concurrent.atomic.AtomicLong;

public class PacketTrafficTracker {

    public static final PacketTrafficTracker INSTANCE = new PacketTrafficTracker();

    private final AtomicLong totalDecoderBytes = new AtomicLong(0);
    private long ratePrevBytes = 0;
    private long ratePrevTime = System.currentTimeMillis();
    private double rateBps = 0;

    private PacketTrafficTracker() {}

    public void addPacketBytes(int bytes) {
        totalDecoderBytes.addAndGet(bytes);
    }

    public long getDecoderTotalBytes() { return totalDecoderBytes.get(); }

    public double getDecoderBytesPerSecond() {
        long now = System.currentTimeMillis();
        long curBytes = totalDecoderBytes.get();
        long dt = now - ratePrevTime;
        if (dt > 800) {
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
