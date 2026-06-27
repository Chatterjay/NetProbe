# 更新日志

## 0.2.0 (2026-06-27)

### 移植到 1.21.1 NeoForge
- 从 1.20.1 Forge 迁移至 1.21.1 NeoForge（ModDevGradle）
- `ForgeConfigSpec` 替换为 `ModConfigSpec`
- 移除 Cloth Config 依赖，NeoForge 自动生成配置界面

### 新功能
- **方块实体变更检测**：新增 `LevelBlockEntityChangedMixin`，当 `BlockEntity.setChanged()` 时触发 `sendBlockUpdated()`，使罐子、机器等方块实体内容变化的追踪成为可能
- **单方块更新追踪**：新增 `ClientboundBlockUpdateMixin` 捕获单个方块更新包
- **穿透显示文本**：新增配置项，可切换标签文字是否穿透方块可见
- **回环检测**：单人模式下（无物理网卡流量）显示压缩比而非异常百分比
- **调试模式**：`/chunkmeter debug` 切换数据包捕获的调试日志

### 改进
- **防抖系统**：每位块 1 秒防抖窗口，防止快速 BE 数据包导致闪烁
- **压缩比显示**：单人模式显示 `[压缩 Nx]` 代替 `[N%]`
- **配置本地化**：配置键使用 NeoForge 的 `{modid}.configuration.{key}` 格式，支持翻译

## 0.1.0 (2026-06-27)

- 新增方块流量半透明覆盖层渲染（6 面彩色方块）
- 新增流量热力色系统：绿→黄→红平滑渐变，可配置阈值
- 新增方块数据按数量/时间双重淘汰机制，损坏方块保留 30s 后清理
- 新增 Cloth Config 配置界面（Forge 模组菜单按钮可用）
- 可配置项：刷新间隔、标签高度、覆盖层不透明度、低/中/高颜色、正常/警告阈值
- 新增方块破坏/放置事件追踪，数据持久化
- 新增覆盖层数据缓存，修复刷新期间的闪烁问题

## 0.0.1 (2026-06-07)

- 初始发布
- `PacketDecoder` mixin 实现 Minecraft 网络层全流量捕获
- 区块数据载荷追踪 (`ClientboundLevelChunkWithLightPacket`)
- 方块实体 NBT 大小测量 (`ClientboundBlockEntityDataPacket`)
- 区块截面更新追踪 (`ClientboundSectionBlocksUpdatePacket`)
- F3 调试屏幕集成，实时显示流量数据
- 系统级网卡速率读取（Windows `netstat -e` 轮询）
- 指向方块流量检查（准星瞄准自动显示）
- 指令系统：`/chunkmeter` 切换显示、重置统计、数据检查
- 多语言支持：中文和英文
- 纯客户端模组，无需服务端安装
