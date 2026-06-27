package org.chatterjay.netprobe;

import net.minecraft.core.BlockPos;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Comparator;

public class BlockTrafficTracker {

    public static final BlockTrafficTracker INSTANCE = new BlockTrafficTracker();

    private final ConcurrentHashMap<BlockPos, long[]> blockTraffic = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<BlockPos, Long> brokenPositions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<BlockPos, Long> lastRecordTime = new ConcurrentHashMap<>();

    private BlockTrafficTracker() {}

    public boolean isRecentDuplicate(BlockPos pos, long windowMs) {
        long now = System.currentTimeMillis();
        Long prev = lastRecordTime.put(pos, now);
        return prev != null && now - prev <= windowMs;
    }

    public void recordBlock(BlockPos pos, long bytes) {
        recordBlock(pos, bytes, 0);
    }

    public void recordBlock(BlockPos pos, long bytes, long debounceMs) {
        brokenPositions.remove(pos);
        long now = System.currentTimeMillis();
        Long prev = lastRecordTime.get(pos);
        if (debounceMs > 0 && prev != null && now - prev <= debounceMs) {
            blockTraffic.merge(pos, new long[]{bytes, 0, now, bytes}, (a, b) -> new long[]{
                a[0] + b[0],
                a[1],
                now,
                b[0]
            });
        } else {
            lastRecordTime.put(pos, now);
            blockTraffic.merge(pos, new long[]{bytes, 1, now, bytes}, (a, b) -> new long[]{
                a[0] + b[0],
                a[1] + 1,
                now,
                b[0]
            });
        }
    }

    public void markBroken(BlockPos pos) {
        brokenPositions.put(pos, System.currentTimeMillis());
    }

    public void cleanupBroken(long maxAgeMs) {
        long cutoff = System.currentTimeMillis() - maxAgeMs;
        brokenPositions.entrySet().removeIf(e -> {
            if (e.getValue() > cutoff) return false;
            blockTraffic.remove(e.getKey());
            return true;
        });
    }

    public void expireStale(long maxAgeMs) {
        long cutoff = System.currentTimeMillis() - maxAgeMs;
        blockTraffic.values().removeIf(v -> v[2] < cutoff);
    }

    public void trimToSize(int maxEntries) {
        if (blockTraffic.size() <= maxEntries) return;
        List<Map.Entry<BlockPos, long[]>> entries = new ArrayList<>(blockTraffic.entrySet());
        entries.sort(Comparator.comparingLong(e -> e.getValue()[2]));
        for (int i = 0, n = entries.size() - maxEntries; i < n; i++) {
            blockTraffic.remove(entries.get(i).getKey());
        }
    }

    public long getTotalBytes() {
        return blockTraffic.values().stream().mapToLong(a -> a[0]).sum();
    }

    public long getTotalBytes(BlockPos pos) {
        long[] data = blockTraffic.get(pos);
        return data != null ? data[0] : 0;
    }

    public long getUpdateCount(BlockPos pos) {
        long[] data = blockTraffic.get(pos);
        return data != null ? data[1] : 0;
    }

    public long getLastBytes(BlockPos pos) {
        long[] data = blockTraffic.get(pos);
        return data != null ? data[3] : 0;
    }

    public int getBlockCount() {
        return blockTraffic.size();
    }

    public List<Map.Entry<BlockPos, long[]>> getTopBlocks(int n) {
        List<Map.Entry<BlockPos, long[]>> entries = new ArrayList<>(blockTraffic.entrySet());
        entries.sort((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0]));
        return entries.subList(0, Math.min(n, entries.size()));
    }

    public void remove(BlockPos pos) {
        blockTraffic.remove(pos);
    }

    public void reset() {
        blockTraffic.clear();
        brokenPositions.clear();
        lastRecordTime.clear();
    }
}
