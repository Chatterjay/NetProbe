# Changelog

## 0.2.0 (2026-06-27)

### Port to 1.21.1 NeoForge
- Migrated from 1.20.1 Forge to 1.21.1 NeoForge (ModDevGradle)
- Updated event bus: `MinecraftForge.EVENT_BUS` → `NeoForge.EVENT_BUS`
- Replaced `ForgeConfigSpec` with `ModConfigSpec`
- Removed Cloth Config dependency; NeoForge generates config GUI automatically

### New Features
- **Block entity change detection**: New `LevelBlockEntityChangedMixin` triggers `sendBlockUpdated()` when `BlockEntity.setChanged()` is called, enabling tracking of block entities (jars, machines, etc.) that change contents without block state updates
- **Single block update tracking**: New `ClientboundBlockUpdateMixin` captures individual `ClientboundBlockUpdatePacket`
- **See-through labels**: New config option to toggle text visibility through blocks
- **Loopback detection**: When running in singleplayer (no physical NIC traffic detected), shows compression ratio instead of inflated percentage
- **Debug mode**: `/chunkmeter debug` toggles debug logging for packet capture diagnostics

### Improvements
- **Debounce system**: 1-second debounce per block position prevents flickering from rapid BE data packets
- **Updated localization**: All config keys properly localized for NeoForge's `{modid}.configuration.{key}` format
- **Compression ratio display**: Shows `[comp Nx]` instead of `[N%]` in singleplayer mode

## 0.1.0 (2026-06-27)

- Block traffic overlay (6-face colored quads)
- Heat color system: green → yellow → red gradient with configurable thresholds
- Block data eviction: dual count/time mechanism, broken blocks cleaned after 30s
- Config GUI via Cloth Config (Forge)
- Configurable: refresh interval, label height, overlay alpha, low/mid/high colors, normal/warning thresholds
- Block break/place event tracking with data persistence
- Overlay data caching to eliminate refresh flickering

## 0.0.1 (2026-06-07)

- Initial release
- `PacketDecoder` mixin for full Minecraft network layer capture
- Chunk data payload tracking (`ClientboundLevelChunkWithLightPacket`)
- Block entity NBT measurement (`ClientboundBlockEntityDataPacket`)
- Section block update tracking (`ClientboundSectionBlocksUpdatePacket`)
- F3 debug screen integration
- System NIC rate polling (Windows `netstat -e`)
- Target block traffic inspection (crosshair auto-detect)
- Command system: toggle display, reset stats, inspect data
- Chinese and English localization
- Client-side only, no server installation required
