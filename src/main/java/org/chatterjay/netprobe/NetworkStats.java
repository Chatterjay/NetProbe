package org.chatterjay.netprobe;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * 从系统获取真实网卡流量 (KB/s)
 * 通过 netstat -e 采样主物理网卡的接收字节，每秒计算速率
 * 解析方式: 逐行扫描找到第一个超过 4 位的数字 (即接收字节)
 */
public class NetworkStats {

    private static volatile double currentKBps = -1;
    private static volatile boolean running = false;
    private static Thread poller;

    public static synchronized void start() {
        if (running) return;
        running = true;
        poller = new Thread(() -> {
            long prevRx = -1;
            long prevTime = 0;
            while (running) {
                try {
                    long rx = readRxBytes();
                    if (rx >= 0) {
                        long now = System.currentTimeMillis();
                        if (prevRx >= 0) {
                            long dt = now - prevTime;
                            if (dt > 500) {
                                currentKBps = (double) (rx - prevRx) / dt * 1000.0 / 1024.0;
                            }
                        }
                        prevRx = rx;
                        prevTime = now;
                    }
                } catch (Exception ignored) {}
                try { Thread.sleep(1000); } catch (InterruptedException e) { break; }
            }
        }, "NetProbe-NetStats");
        poller.setDaemon(true);
        poller.start();
        currentKBps = 0.0;
    }

    public static synchronized void stop() {
        running = false;
        if (poller != null) { poller.interrupt(); poller = null; }
        currentKBps = -1;
    }

    public static boolean isAvailable() { return currentKBps >= 0; }

    /** 获取当前系统接收速率 (KB/s) */
    public static double getKBytesPerSecond() {
        if (currentKBps < -0.5) start();
        return Math.max(0, currentKBps);
    }

    /** 保持兼容 */
    public static double getBytesPerSecond() {
        return getKBytesPerSecond() * 1024.0;
    }

    /** 从 netstat -e 输出中提取第一个超过 4 位的数字 (接收字节) */
    private static long readRxBytes() {
        try {
            Process p = Runtime.getRuntime().exec("netstat -e");
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                long val = extractFirstNumber(line);
                if (val >= 0) {
                    br.close();
                    return val;
                }
            }
            br.close();
        } catch (Exception ignored) {}
        return -1;
    }

    /** 从字符串中提取第一个 > 999 的数字 */
    private static long extractFirstNumber(String s) {
        StringBuilder digits = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c >= '0' && c <= '9') {
                digits.append(c);
            } else if (digits.length() > 0) {
                if (digits.length() > 4) {
                    try { return Long.parseLong(digits.toString()); } catch (NumberFormatException ignored) {}
                }
                digits.setLength(0);
            }
        }
        if (digits.length() > 4) {
            try { return Long.parseLong(digits.toString()); } catch (NumberFormatException ignored) {}
        }
        return -1;
    }
}
