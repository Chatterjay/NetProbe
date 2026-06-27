# NetProbe

A client-side Minecraft mod for real-time inbound network traffic monitoring.

**Minecraft 1.21.1 · NeoForge**

## Features

- **Real-time traffic monitor** — View network traffic directly in the F3 debug screen
- **Multi-layer measurement**:
  - **System-level**: Reads physical NIC RX rate via `netstat -e`
  - **Minecraft network layer**: Captures all packet bytes via `PacketDecoder` mixin
  - **Chunk/Block analysis**: Tracks chunk data payloads, block entity NBT, and section block updates
- **Block traffic overlay** — In-world colored heatmap showing traffic per block
- **Target block tracking** — Point your crosshair at any block to see its traffic
- **See-through labels** — Configurable text visibility through blocks
- **Dual-language** — Chinese and English (auto-follows game language)
- **Server-free** — Client-side only, no server installation needed

## Commands

| Command | Description |
|---------|-------------|
| `/chunkmeter` | Toggle F3 debug overlay |
| `/chunkmeter reset` | Reset all statistics |
| `/chunkmeter inspect` | Show detailed traffic data in chat |
| `/chunkmeter overlay` | Toggle block traffic world overlay |
| `/chunkmeter debug` | Toggle debug logging |

Press **F3** to open the debug screen and view NetProbe data (requires `/chunkmeter` to enable first).

## F3 Display Legend

```
=== NetProbe Traffic ===
System NIC: 2500 KB/s  (physical adapter RX)
Mod: 500 KB/s (raw) >> ~385 KB/s (est. compressed) [20%]
Total MC traffic: 64.5 MB
--- Chunk / Block Analysis ---
Chunk packets: 9.7 MB  + Block updates: 255.9 KB  = 10.0 MB
Chunk records: 634
Current chunk [-12,0]: total 470.4 KB  last 105 B
Top chunks: [-12,0]470.4 KB  [-11,0]12.3 KB
Tracked blocks: 39  total 432.2 KB
Target block [-44,64,9]: total 143.9 KB  last 5 B  12 updates
```

- **System NIC** = Physical NIC RX rate (via `netstat -e`)
- **Mod** = Raw captured Minecraft packet bytes
- **Est. compressed** = Raw ÷ 1.3 (zlib compression ratio), approximates actual wire traffic
- **Percentage** = Mod measurement / System NIC ratio
- **total** = Cumulative traffic for that chunk or block
- **last** = Estimated size of the most recent packet

## Configuration

Accessible via NeoForge mod list → NetProbe → Config.

| Setting | Default | Description |
|---------|---------|-------------|
| Refresh Interval | 10 ticks | Minimum ticks between overlay updates |
| Label Height | 1.5 | Text label height above block (in blocks) |
| Overlay Alpha | 0.12 | Block overlay face opacity |
| Label See Through | true | Render labels through blocks |
| Normal Color | 00FF00 | Color for normal traffic overlays |
| Warning Color | FFFF00 | Color for warning traffic overlays |
| High Color | FF0000 | Color for high traffic overlays |
| Normal Max | 100 B | Traffic threshold for normal |
| Warning Max | 500 B | Traffic threshold for warning |

## Technical Implementation

Uses Sponge Mixin to inject into `PacketDecoder` for total packet capture,
`ClientboundLevelChunkWithLightPacket` for chunk data recording,
`ClientboundBlockEntityDataPacket` for NBT payload measurement,
`ClientboundSectionBlocksUpdatePacket` and `ClientboundBlockUpdatePacket` for block updates,
and `Level.blockEntityChanged` for block entity change detection.
System NIC rate is polled via `netstat -e`.

## License

GNU AGPL 3.0
