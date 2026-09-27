# Fission Reactor Alerts (FRA)

> Server-side early warning for Mekanism fission reactors: when a formed reactor starts taking damage, the claiming team receives one chat alert; without a claim, nearby players receive it. Never touches reactor state.

[![License: All Rights Reserved](https://img.shields.io/badge/License-All_Rights_Reserved-red.svg)](LICENSE)
Homepage (intended CurseForge page, not yet published): https://www.curseforge.com/minecraft/mc-mods/fission-reactor-alerts
Bugs and questions: comment on the CurseForge page, or DM @kuronami333 on X (https://x.com/kuronami333).

---

## Supported Loaders / Versions

| Minecraft | NeoForge | Forge | Fabric |
|---|:---:|:---:|:---:|
| 1.21.1 | ✅ | — | — |

Requires **Mekanism** + **Mekanism Generators**. Team delivery is optional via **FTB Chunks** + **FTB Teams**.

---

## Why?

Mekanism's fission reactor can run away and melt down while nobody is looking — a redstone glitch, a fuel/waste imbalance, or a chunk-load edge case is all it takes (upstream issues mekanism/Mekanism#8373, #8482). The built-in answer is the Logic Adapter + redstone + Industrial Alarm, but that only works if you remembered to wire every reactor by hand.

This mod watches every formed fission reactor on the server for you. The moment one starts taking damage — the earliest signal Mekanism gives before a meltdown — the responsible team gets a single chat alert with the reactor's position, damage % and temperature. Once the reactor recovers to zero damage the latch re-arms, so a second incident alerts again. No duplicate spam, no reactor changes.

## How it works

- A mixin observes `FissionReactorMultiblockData#tick` (server side only, read-only).
- Alerts fire on the `damage 0 → >0` transition; the latch holds until damage returns to 0.
- Recipients: if any reactor chunk is claimed with FTB Chunks, the online members of the owning team(s); otherwise players within 64 blocks.
- The server log records every alert for audit.

## Installation

1. Install NeoForge 1.21.1
2. Install Mekanism + Mekanism Generators (required), FTB Chunks + FTB Teams (optional, for team alerts)
3. Drop `fission-reactor-alerts-X.Y.Z.jar` into `mods/` (server side is enough; harmless on clients)

## License

[All Rights Reserved](LICENSE) — modpack inclusion welcome, no credit required.

## Credits

- Author: KURONAMI
- The mod icon adapts Mekanism Generators' `fission_reactor_port_input` block texture. See [NOTICE.md](NOTICE.md) for attribution and third-party terms.
