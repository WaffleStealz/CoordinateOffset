# CoordinateOffset — Folia fork notes

This is a **Folia fork** of [joshuaprince/CoordinateOffset](https://github.com/joshuaprince/CoordinateOffset), maintained by WaffleStealz.

Upstream license: **AGPL-3.0**. Keep source available and preserve attribution.

Plugin name on the server: `CoordinateOffset` (see `paper-plugin.yml`).

## Folia changes (so far)

| Area | Upstream | Fork |
|------|----------|------|
| Plugin metadata | No Folia support | `folia-supported: true`, name `CoordinateOffset` |
| Post-teleport chunk refresh delay | `BukkitScheduler.runTaskLater` | `EntityScheduler.runDelayed` via `FoliaSupport` |
| Rate-limit flush timer | `BukkitScheduler.runTaskTimer` | `AsyncScheduler.runAtFixedRate` |
| `assertMainThread` | Requires `Bukkit.isPrimaryThread()` | No-op (no global main thread on Folia) |
| Chunk refresh driver | Global `ServerTickEndEvent` | Per-player `EntityScheduler` timer |

## Still to validate on a live Folia server

- Join / quit / respawn / world change / nether portals with offsets
- Short teleport offset regenerate + chunk resync
- Dig/place/combat with nonzero offset
- World border obfuscator
- Bamboo/dripstone collision fix (NMS)
- Soft-depend plugins (GrimAC, Geyser — still expected to be problematic)

## Build

```bash
cd CoordinateOffset
./gradlew :folia:shadowJar
```

Artifact always written to:

- `CoordinateOffset/target/CoordinateOffset-<version>.jar`
- `CoordinateOffset/target/CoordinateOffset-SNAPSHOT.jar` (stable name from `assemble`)

Requires **PacketEvents** installed on the server.
