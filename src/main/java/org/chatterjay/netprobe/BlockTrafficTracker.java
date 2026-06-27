package org.chatterjay.netprobe;

import net.minecraft.core.BlockPos;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Comparator;

public class BlockTrafficTracker {

    public static final BlockTrafficTracker INSTANCE = new BlockTrafficTracker();

    // BlockPos → [累计流量, 更新次数, 最后更新时间戳ms, 单次流量]
    private final ConcurrentHashMap<BlockPos, long[]> blockTraffic = new ConcurrentHashMap<>();
    // 被标记为已破坏的位置 → 标记时间戳
    private final ConcurrentHashMap<BlockPos, Long> brokenPositions = new ConcurrentHashMap<>();

    private BlockTrafficTracker() {}

    public void recordBlock(BlockPos pos, long bytes) {
        brokenPositions.remove(pos);
        long now = System.currentTimeMillis();
        blockTraffic.merge(pos, new long[]{bytes, 1, now, bytes}, (a, b) -> {
            a[0] += bytes;
            a[1]++;
            a[2] = now;
            a[3] = bytes;
            return a;
        });
    }

    /** 标记方块已被破坏，30s 内无新流量则清除 */
    public void markBroken(BlockPos pos) {
        brokenPositions.put(pos, System.currentTimeMillis());
    }

    /** 清除超过 maxAgeMs 无新流量的已破坏方块 */
    public void cleanupBroken(long maxAgeMs) {
        long cutoff = System.currentTimeMillis() - maxAgeMs;
        brokenPositions.entrySet().removeIf(e -> {
            if (e.getValue() > cutoff) return false;
            blockTraffic.remove(e.getKey());
            return true;
        });
    }

    /** 清除超过 maxAgeMs 未更新的条目 */
    public void expireStale(long maxAgeMs) {
        long cutoff = System.currentTimeMillis() - maxAgeMs;
        blockTraffic.values().removeIf(v -> v[2] < cutoff);
    }

    /** 保留最近更新的 maxEntries 个条目，超出时淘汰最旧的 */
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
