# Fission Reactor Alerts: Forge 1.19.2 candidate

This isolated Forge cell adapts the NeoForge 1.21.1 add-on. It observes formed Mekanism fission reactors after `handleDamage`, sends one alert on the first positive damage value, suppresses repeats until damage returns to zero, and never writes reactor state. Alerts go to online members of every team claiming a reactor chunk; if FTB Chunks/Teams are absent or the reactor is unclaimed, nearby players within 64 blocks receive the alert.

| Component | Target |
|---|---|
| Minecraft | 1.19.2 |
| Forge | 43.5.2 (Java 17) |
| Mekanism + Generators | 10.3.9.13 (CurseForge files 4644795 / 4644798) |
| Optional FTB Chunks / Teams / Library | 1902.4.7-build.420 / 1902.2.14-build.123 / 1902.4.2-build.701 |
| Optional Architectury | 6.5.90 |

API evidence and exact artifact hashes are recorded in `API_EVIDENCE.md`; the server targets are checked against official jars. `GameTest` coverage includes the damage latch, actual claimed-team packet delivery, and the nearby-player fallback.

## Validation status

- Direct Java 17 compilation of main and GameTest sources, Mixin annotation processing, and the pure damage-latch fixture have passed. ForgeGradle production reobfuscation and GameTestServer runs have not been performed.
- The parent has stopped heavy builds pending a shared Windows build slot. Use the exact source snapshot and the work packet root’s `FISSION_FORGE1192_BUILDREQUEST.json` after the parent allocates that slot. Do not treat source preparation as a completed Forge 1.19.2 port.
