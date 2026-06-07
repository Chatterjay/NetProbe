package org.chatterjay.netprobe;

import net.minecraft.core.BlockPos;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BlockTrafficTracker {

    public static final BlockTrafficTracker INSTANCE = new BlockTrafficTracker();

    // BlockPos → [累计流量, 更新次数, 最后更新时间戳ms, 单次流量]
    private final ConcurrentHashMap<BlockPos, long[]> blockTraffic = new ConcurrentHashMap<>();

    private BlockTrafficTracker() {}

    public void recordBlock(BlockPos pos, long bytes) {
        long now = System.currentTimeMillis();
        blockTraffic.merge(pos, new long[]{bytes, 1, now, bytes}, (a, b) -> {
            a[0] += bytes;
            a[1]++;
            a[2] = now;
            a[3] = bytes;
            return a;
        });
    }

    /** 移除超过指定毫秒未更新的记录 */
    public void expireOldEntries(long maxAgeMs) {
        long cutoff = System.currentTimeMillis() - maxAgeMs;
        blockTraffic.values().removeIf(v -> v[2] < cutoff);
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

    /** 按累计流量排序，返回 [(BlockPos, [累计流量, 更新次数])] */
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
    }
}
