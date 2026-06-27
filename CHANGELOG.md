# 更新日志

## 0.1.0 (2026-06-27)

- 新增方块流量半透明覆盖层渲染（6面彩色方块）
- 新增流量热力色系统：绿→黄→红平滑渐变，可配置阈值
- 新增方块数据按数量/时间双重淘汰机制，损坏方块保留30s后清理
- 新增 Cloth Config 配置界面（Forge Mods 菜单按钮可用）
- 可配置项：刷新间隔、标签高度、覆盖层不透明度、低/中/高颜色、正常/警告阈值
- 新增方块破坏/放置事件追踪，数据持久化
- 新增覆盖层数据缓存，修复刷新期间的闪烁问题
- Cloth Config 设为强制客户端依赖

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
