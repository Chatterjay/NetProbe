package org.chatterjay.netprobe;

import net.minecraft.world.level.ChunkPos;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ChunkTrafficTracker {

    public static final ChunkTrafficTracker INSTANCE = new ChunkTrafficTracker();

    // long[] = [累计流量, 单次流量]
    private final ConcurrentHashMap<ChunkPos, long[]> chunkTraffic = new ConcurrentHashMap<>();
    private final AtomicLong totalBytes = new AtomicLong(0);
    private final AtomicInteger chunkCount = new AtomicInteger(0);

    private ChunkTrafficTracker() {}

    public void recordChunk(ChunkPos pos, long bytes) {
        chunkTraffic.merge(pos, new long[]{bytes, bytes}, (a, b) -> {
            a[0] += bytes;  // 累计
            a[1] = bytes;   // 单次
            return a;
        });
        totalBytes.addAndGet(bytes);
        chunkCount.incrementAndGet();
    }

    public long getTotalBytes() {
        return totalBytes.get();
    }

    public int getChunkCount() {
        return chunkCount.get();
    }

    public long getLastBytes(ChunkPos pos) {
        long[] data = chunkTraffic.get(pos);
        return data != null ? data[1] : 0;
    }

    /** 按累计流量排序，每个 entry 的 value 为 long[] = [累计流量, 单次流量] */
    public List<Map.Entry<ChunkPos, long[]>> getTopChunks(int n) {
        List<Map.Entry<ChunkPos, long[]>> entries = new ArrayList<>(chunkTraffic.entrySet());
        entries.sort((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0]));
        return entries.subList(0, Math.min(n, entries.size()));
    }

    public void reset() {
        chunkTraffic.clear();
        totalBytes.set(0);
        chunkCount.set(0);
    }
}
