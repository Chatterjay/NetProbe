package org.chatterjay.netprobe;

import net.minecraft.core.BlockPos;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BlockTrafficTracker {

    public static final BlockTrafficTracker INSTANCE = new BlockTrafficTracker();

    // BlockPos → [累计流量, 更新次数]
    private final ConcurrentHashMap<BlockPos, long[]> blockTraffic = new ConcurrentHashMap<>();

    private BlockTrafficTracker() {}

    public void recordBlock(BlockPos pos, long bytes) {
        blockTraffic.merge(pos, new long[]{bytes, 1}, (a, b) -> {
            a[0] += bytes;
            a[1]++;
            return a;
        });
    }

    public long getTotalBytes() {
        return blockTraffic.values().stream().mapToLong(a -> a[0]).sum();
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

    public void reset() {
        blockTraffic.clear();
    }
}
