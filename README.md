# NetProbe

一个客户端侧 Minecraft Forge 模组，用于实时监控入站网络流量。

## 功能

- **实时流量监控** — 在 F3 调试屏幕中直接查看网络流量
- **多层测量**：
  - 系统级：通过 `netstat -e` 读取物理网卡接收速率
  - Minecraft 网络层：通过 `PacketDecoder` mixin 捕获所有数据包字节
  - 区块/方块分析：追踪区块数据载荷、方块实体 NBT 和区块截面更新
- **指向方块追踪** — 将准星对准方块，查看其流量消耗
- **多语言支持** — 支持中文和英文（自动跟随游戏语言）
- **无需服务端安装** — 纯客户端模组

## 指令

| 指令 | 说明 |
|------|------|
| `/chunkmeter` | 切换 F3 调试叠加层 |
| `/chunkmeter reset` | 重置所有统计数据 |
| `/chunkmeter inspect` | 在聊天框显示详细流量数据 |

按 **F3** 打开调试屏幕查看 NetProbe 数据（需先用 `/chunkmeter` 开启）。

## F3 显示说明

```
=== NetProbe 流量监控 ===
系统网卡: 2500 KB/s  (物理网卡接收速率)
模组测: 500 KB/s (原始) >> ~385 KB/s (压缩后估算) [20%]
Minecraft总流量: 64.5 MB
--- 区块/方块分析 ---
区块数据包: 9.7 MB  + 方块更新: 255.9 KB  = 10.0 MB
区块记录数: 634
当前区块 [-12,0]: 累 470.4 KB  单 105 B
Top区块: [-12,0]470.4 KB  [-11,0]12.3 KB
追踪方块: 39 个  共 432.2 KB
指向方块 [-44,64,9]: 累 143.9 KB  单 5 B  12次更新
```

**说明：**
- **系统网卡** = 物理网卡接收速率（通过 `netstat -e` 读取）
- **模组测** = 捕获的原始 Minecraft 数据包字节
- **压缩后估算** = 原始值 ÷ 1.3（zlib 压缩率），接近实际网线流量
- **百分比** = 模组测 / 系统网卡，反映 Minecraft 流量占比
- **累** = 该区块或方块的累计流量
- **单** = 最近一次数据包的估算大小

## 技术实现

通过 Mixin 注入 `PacketDecoder` 捕获所有入站包字节、`ClientboundLevelChunkWithLightPacket` 记录区块数据、`ClientboundBlockEntityDataPacket` 测量 NBT、`ClientboundSectionBlocksUpdatePacket` 追踪方块更新。系统网卡速率通过 `netstat -e` 轮询获取。

## 许可

GNU AGPL 3.0
