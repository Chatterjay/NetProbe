package org.chatterjay.netprobe;

import net.minecraft.world.level.ChunkPos;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ChunkTrafficTracker {

    public static final ChunkTrafficTracker INSTANCE = new ChunkTrafficTracker();

    private final ConcurrentHashMap<ChunkPos, long[]> chunkTraffic = new ConcurrentHashMap<>();
    private final AtomicLong totalBytes = new AtomicLong(0);
    private final AtomicLong chunkPacketBytes = new AtomicLong(0);
    private final AtomicLong blockUpdateBytes = new AtomicLong(0);
    private final AtomicInteger chunkCount = new AtomicInteger(0);

    private long ratePrevBytes = 0;
    private long ratePrevTime = 0;
    private double rateBytesPerSec = 0;

    private ChunkTrafficTracker() {
        ratePrevTime = System.currentTimeMillis();
    }

    public double getBytesPerSecond() {
        long now = System.currentTimeMillis();
        long curBytes = totalBytes.get();
        long dt = now - ratePrevTime;
        if (dt > 500) {
            rateBytesPerSec = (double) (curBytes - ratePrevBytes) / dt * 1000.0;
            ratePrevBytes = curBytes;
            ratePrevTime = now;
        }
        return rateBytesPerSec;
    }

    public void recordChunk(ChunkPos pos, long bytes) {
        chunkTraffic.merge(pos, new long[]{bytes, bytes}, (a, b) -> {
            a[0] += bytes;
            a[1] = bytes;
            return a;
        });
        totalBytes.addAndGet(bytes);
        chunkPacketBytes.addAndGet(bytes);
        chunkCount.incrementAndGet();
    }

    public void addBlockBytes(ChunkPos pos, long bytes) {
        chunkTraffic.merge(pos, new long[]{bytes, bytes}, (a, b) -> {
            a[0] += bytes;
            a[1] = bytes;
            return a;
        });
        totalBytes.addAndGet(bytes);
        blockUpdateBytes.addAndGet(bytes);
    }

    public long getChunkPacketBytes() { return chunkPacketBytes.get(); }
    public long getBlockUpdateBytes() { return blockUpdateBytes.get(); }

    public long getTotalBytes() {
        return totalBytes.get();
    }

    public long getTotalBytes(ChunkPos pos) {
        long[] data = chunkTraffic.get(pos);
        return data != null ? data[0] : 0;
    }

    public int getChunkCount() {
        return chunkCount.get();
    }

    public long getLastBytes(ChunkPos pos) {
        long[] data = chunkTraffic.get(pos);
        return data != null ? data[1] : 0;
    }

    public List<Map.Entry<ChunkPos, long[]>> getTopChunks(int n) {
        List<Map.Entry<ChunkPos, long[]>> entries = new ArrayList<>(chunkTraffic.entrySet());
        entries.sort((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0]));
        return entries.subList(0, Math.min(n, entries.size()));
    }

    public void reset() {
        chunkTraffic.clear();
        totalBytes.set(0);
        chunkPacketBytes.set(0);
        blockUpdateBytes.set(0);
        chunkCount.set(0);
    }
}
