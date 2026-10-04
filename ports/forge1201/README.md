# Fission Reactor Alerts: Forge 1.20.1 candidate

This is an isolated Forge 1.20.1 cell for the NeoForge 1.21.1 add-on. It keeps the alert contract: observe formed fission reactors only, alert once on `reactorDamage` changing from zero to positive, re-arm only at zero, read reactor state without writing it, and send to claimed FTB Teams or to players within 64 blocks when unclaimed or FTB is absent.

| Component | Target |
|---|---|
| Minecraft | 1.20.1 |
| Forge | 47.4.13 |
| Java | 17 |
| Mekanism + Generators | 10.4.16.80 / mod version 10.4.16 |
| FTB Chunks / Teams / Library | 2001.3.8 / 2001.3.2 / 2001.2.13, optional |

The integration target was checked against the official Mekanism Generators 1.20.1 source and published JAR. Its method is `tick(Level):boolean`; the bytecode calls `handleDamage(Level)` before returning and exposes `reactorDamage` as a public `double`. The mixin uses that exact descriptor and a required single injection.

The Forge 1.20.1 metadata uses `mandatory` fields in `META-INF/mods.toml`; the GameTest uses Forge's `net.minecraftforge.gametest` annotations, the plural `structures/` folder, and DataVersion 3465. FTB API references live in a bridge loaded only after both optional mods are present, preserving nearby-player fallback without FTB. LegacyForge’s mod dependency configurations are non-transitive, so Architectury 9.2.14 is declared explicitly for the optional FTB runtime.

## Validation status

- Pure alert-latch fixture compiled with `javac --release 17` and passed onset, duplicate suppression, and zero re-arm checks.
- Direct Mixin 0.8.5 annotation processing passed with `remap=false` and generated an empty refmap, as expected for this mod-owned target/method.
- Forge main/GameTest compilation, reobfuscated JAR inspection, and GameTestServer runs are pending a shared heavy-build slot. Run the build and both GameTest routes described in `../../../fission-buildrequest.json` once the parent allocates it.
