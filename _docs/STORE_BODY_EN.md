<!-- Summary 正本は _docs/STORE_SUMMARY_EN.txt。公開時はこのコメント行を本文に含めない -->
<!-- GitHub URL は公開予定の意図URL。repo 未作成の段階では 404 -->

One chat message the instant a Mekanism fission reactor starts taking damage — sent to the team that claimed it, or to players standing nearby — so the meltdown is never the first warning you get.

Mekanism's fission reactor can run away while nobody is watching: a redstone glitch, a fuel/waste imbalance, a chunk-load edge case. The built-in answer — Logic Adapter, redstone, Industrial Alarm — only works if you wired every reactor by hand. This addon watches every formed reactor on the server for you.

**Features**

- Fires the moment a formed fission reactor's damage rises above zero — the earliest warning signal Mekanism exposes before a meltdown
- One alert per incident, not per tick. The latch holds while the reactor stays damaged and re-arms once it recovers to zero, so a second incident alerts again
- The message carries the reactor's position, damage % and temperature, so you can jump straight to it
- **Claim-aware delivery**: with FTB Chunks + FTB Teams installed, the alert reaches the online members of the team(s) that claimed the reactor's chunks — the players who can actually fix it
- Without claims — or without FTB — players within 64 blocks of the reactor get it instead
- Every alert is also written to the server log for later review
- Server-side and read-only: it observes reactor state, never writes to it

**Dependencies**

- [Mekanism](https://www.curseforge.com/minecraft/mc-mods/mekanism) + [Mekanism Generators](https://www.curseforge.com/minecraft/mc-mods/mekanism-generators) — required; this is an addon for their fission reactor
- [FTB Chunks](https://www.curseforge.com/minecraft/mc-mods/ftb-chunks) + [FTB Teams](https://www.curseforge.com/minecraft/mc-mods/ftb-teams) — optional. With both installed, alerts route to the claiming team; without them the proximity fallback covers every reactor
- The two FTB mods are CurseForge-only, so a Modrinth-only setup cannot resolve them — the mod works without them regardless

**Scope and limitations**

- Alerts are chat messages. No siren, no overlay, no redstone output — Mekanism's own Logic Adapter + Industrial Alarm still exists if you want a physical alarm
- Only FTB Chunks claims are consulted for team delivery; other claim/protection mods are not
- The server needs it. Installing on a client alone does nothing

<p><a href="https://www.patreon.com/KURONAMI333"><img src="https://raw.githubusercontent.com/KURONAMI333/music-disc-maker/6a0a895769575a1a58fd1fb6dfb15e259bcefccb/_docs/support/patreon.png" width="440" height="156" alt="Support my mods on Patreon"></a> <a href="https://x.com/kuronami333"><img src="https://raw.githubusercontent.com/KURONAMI333/music-disc-maker/6a0a895769575a1a58fd1fb6dfb15e259bcefccb/_docs/support/x.png" width="300" height="156" alt="Follow @kuronami333 on X"></a></p>

Bugs and questions: comment on the CurseForge page, or DM @kuronami333 on X.

The mod icon adapts Mekanism Generators' `fission_reactor_port_input` block texture (MIT license). See NOTICE.md in the source repository for attribution and third-party terms.

All Rights Reserved. Free to put in any modpack, on any platform, monetised or not - no permission needed, no credit required. Source: https://github.com/KURONAMI333/fission-reactor-alerts
