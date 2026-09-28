<!-- Summary 正本は _docs/STORE_SUMMARY_EN.txt。公開時はこのコメント行を本文に含めない -->
<!-- GitHub URL は公開予定の意図URL。repo 未作成の段階では 404 -->

A Mekanism fission reactor can start taking damage while nobody is watching — a redstone glitch, a fuel imbalance, a chunk-load edge case. By the time the meltdown is visible, the reactor is already gone.

This addon watches every formed reactor on the server. The instant damage rises above zero — the earliest warning Mekanism exposes — it sends one chat message with the reactor's position, damage and temperature. Once the reactor returns to zero damage the alert re-arms, so the next incident warns again instead of staying silent.

With FTB Chunks and FTB Teams installed, the alert goes to the online members of the team that claimed the reactor's chunks — the players who can actually fix it. Without claims it goes to players within 64 blocks of the reactor, and every alert is also written to the server log. The mod only reads reactor state; it never changes it.

This is an addon for Mekanism + Mekanism Generators' fission reactor. FTB Chunks and FTB Teams are optional for team routing and are published on CurseForge only.

The icon adapts Mekanism Generators' `fission_reactor_port_input` block texture (MIT license). See NOTICE.md in the source repository for attribution and third-party terms.

<p><a href="https://www.patreon.com/KURONAMI333"><img src="https://raw.githubusercontent.com/KURONAMI333/music-disc-maker/6a0a895769575a1a58fd1fb6dfb15e259bcefccb/_docs/support/patreon.png" width="440" height="156" alt="Support my mods on Patreon"></a> <a href="https://x.com/kuronami333"><img src="https://raw.githubusercontent.com/KURONAMI333/music-disc-maker/6a0a895769575a1a58fd1fb6dfb15e259bcefccb/_docs/support/x.png" width="300" height="156" alt="Follow @kuronami333 on X"></a></p>

Bugs and questions: comment on this page, or DM @kuronami333 on X.

All Rights Reserved. Free to put in any modpack, on any platform, monetised or not - no permission needed, no credit required. Source: https://github.com/KURONAMI333/fission-reactor-alerts
