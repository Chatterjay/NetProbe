# 更新日志

## 0.0.1 (2026-06-07)

- 初始发布
- PacketDecoder mixin 实现 Minecraft 网络层全流量捕获
- 区块数据载荷追踪 (ClientboundLevelChunkWithLightPacket)
- 方块实体 NBT 大小测量 (ClientboundBlockEntityDataPacket)
- 区块截面更新追踪 (ClientboundSectionBlocksUpdatePacket)
- F3 调试屏幕集成，实时显示流量数据
- 系统级网卡速率读取 (Windows netstat 轮询)
- 指向方块流量检查（准星瞄准自动显示）
- 指令系统: `/chunkmeter` 切换显示、重置统计、数据检查
- 多语言支持：中文和英文
- 纯客户端模组，无需服务端安装
