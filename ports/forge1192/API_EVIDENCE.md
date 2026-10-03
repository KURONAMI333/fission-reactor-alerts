# Forge 1.19.2 API and dependency evidence

Observed 2026-10-03 against the official Mekanism release artifacts, the selected official FTB Maven artifacts, and cached Forge/Minecraft artifacts. No Gradle build or game process was run.

## Target versions and binaries

| Component | Selected artifact | SHA-256 |
|---|---|---|
| Mekanism core | CurseForge file 4644795, `mekanism-268560-4644795.jar`, mod version `10.3.9` | `da09560b04b4088df7d6ebf4d962da8b9e040b62bff346e03027da7427678e59` |
| Mekanism Generators | CurseForge file 4644798, `mekanism-generators-268566-4644798.jar`, mod version `10.3.9` | `2572caf9f1f6af6568585b8981ebfc961a35585e5a6db53f614d1ff69a9fd4d8` |
| FTB Chunks Forge | Maven `dev.ftb.mods:ftb-chunks-forge:1902.4.7-build.420` | `bdc1469541893b4ab894446834f305c13c9d30041811096c028f8f54e6844435` |
| FTB Teams Forge | Maven `dev.ftb.mods:ftb-teams-forge:1902.2.14-build.123` | `f6ab37d059b59adfb2222037c62ea10e826ae1a0f41ea61d89ab26e1929f01cd` |
| FTB Library Forge | Maven `dev.ftb.mods:ftb-library-forge:1902.4.2-build.701` | `a3e14e6f62a122bbbacc6af1a31831bf3d6c915d46eac357c389c7b623cb93d5` |
| Architectury Forge | Maven `dev.architectury:architectury-forge:6.5.90` | `286ee9a0a34ddc44342790ba02a849962d918fa7377a5a37a9c5e9478a4754cb` |
| Forge mapped compile API | Forge `1.19.2-43.5.2`, Parchment `2022.11.27` | `28a58929247f1f341218fcea50eca51ddfa8c806f0650c3c8eca0bedf4626b07` |

All seven table hashes were recomputed from the exact artifact bytes with SHA-256. The machine-readable record, including the absolute artifact paths, is `verification/fission-forge1192/dependency-sha256.json`.

The selected development target is Forge 1.19.2-43.5.2, and the compile API hash below is for that exact mapped artifact. The three official FTB pack manifests observed during selection use Forge 43.4.5 or 43.3.13, so this cell does **not** claim exact loader-patch parity with those packs. Mekanism's `mods.toml` declares Minecraft exactly 1.19.2 and Forge `[43.2.8,)`; Generators requires Mekanism `[10.3.9]`. The cached official client `version.json` reports Java 17, world DataVersion 3120, resource-pack format 9, and data-pack format 10; the raw metadata is `../../../raw-metadata/minecraft-1.19.2-version.json` (SHA-256 `a2865b7b47b8b887b418e71c662ea13d905d0223c804dc488d7977825ba9d343`). This mod's `src/main/resources/pack.mcmeta` describes its resource assets, so its `pack_format` is **9**. Data-pack format **10** is the target-version fact, but this mod does not ship a data-pack `pack.mcmeta`; it must not replace the resource format field. The GameTest structure declares world DataVersion **3120**. No pack format 13 value is used.

## Mekanism hook target

`javap -p -s` on file 4644798 shows `public boolean tick(Level)` with descriptor `(Lnet/minecraft/world/level/Level;)Z` and `public double reactorDamage` with descriptor `D`. `javap -p -c` shows the call to `handleDamage(Level)` at bytecode offset 65; the method returns at offset 190. The `RETURN` injection therefore observes the updated value after Mekanism's damage handling. The target method belongs to Mekanism, not Minecraft's mapped namespace, so both `@Mixin` and `@Inject` use `remap = false`; `require = 1` makes a missing hook fail fast.

The core artifact's multiblock base provides the formed-state and bounds/identity access used by the tracker (`isFormed`, `getMinPos`, `getMaxPos`, `inventoryID`). Reactor damage and heat are only read. Main sources compiled with `javac --release 17` against this exact Mekanism pair and the mapped Forge 1.19.2 jar.

## Optional FTB bridge API

The 1.19.2 API differs from the 1.20.1 cell and was read from the exact downloaded jars with `javap`:

- `dev.ftb.mods.ftbchunks.data.FTBChunksAPI.isManagerLoaded()` and static `getManager()`.
- `ClaimedChunkManager.getChunk(ChunkDimPos)` returns `dev.ftb.mods.ftbchunks.data.ClaimedChunk`.
- `ClaimedChunk.getTeamData().getTeam()` returns `dev.ftb.mods.ftbteams.data.Team`; `Team.getOnlineMembers()` and `Team.getName()` supply recipients and display name.
- `FTBTeamsAPI.getPlayerTeam(ServerPlayer)` and `FTBChunksAPI.claimAsPlayer(ServerPlayer, ResourceKey<Level>, ChunkPos, boolean)` are used by the FTB-present GameTest.

The Maven POMs and in-jar `mods.toml` files were inspected. FTB Chunks requires Architectury at least 6.5.77, FTB Teams at least 6.3.49, and FTB Library at least 6.2.43. They require each other at lower compatible minimums and require Forge 43.2.6 or newer. The cell pins Architectury 6.5.90 and the matching 1.19.2 FTB artifacts. FTB's Forge dependencies are optional in this add-on. Main-class FTB references live only in a bridge loaded reflectively after Forge confirms both mods, so the nearby-player route remains loadable without FTB.

## Forge and GameTest API checks

The exact mapped Forge 43.5.2 artifact exposes the Forge `@Mod` annotation, `ModList`, Forge GameTest annotations, `ServerPlayer`'s constructor including `ProfilePublicKey`, and `PlayerList.placeNewPlayer(Connection, ServerPlayer)`. The GameTest sources were adapted to 1.19.2 APIs (`Registry.BLOCK`, `Entity.getLevel`, `GameTestHelper.fail`); GameTest-source compilation against the mapped artifact and exact FTB jars passed with Java 17. The minimal structure retains the Forge `structures/` resource path and has its root DataVersion changed from the copied 1.20.1 value 3465 to 3120.

## Not yet verified

Parchment mappings are enabled through the ParchmentMC Librarian ForgeGradle plugin declared after ForgeGradle in `build.gradle`; `settings.gradle` includes the official Parchment Maven plugin repository. This follows the plugin DSL flow in the [Librarian ForgeGradle guide](https://github.com/ParchmentMC/Librarian/blob/dev/docs/FORGEGRADLE.md). Gradle plugin resolution and the ForgeGradle build remain unrun here.

The Gradle source configuration directs Mixin AP to `build/generated/mixin` with the same explicit `-AoutRefMapFile` option exercised by the direct AP probe, then includes that generated directory as main resources. This Gradle task wiring has not yet been built. The direct AP probe passed; because the target and injector are both `remap = false`, its empty refmap requires no cross-namespace mappings.

The GameTests require explicit Gradle property `-PgameTestMode=full` or `-PgameTestMode=without-ftb`; Gradle rejects any other value, selects FTB runtime jars only in `full`, and passes the selected mode to the GameTest server. Every GameTest asserts the add-on, Mekanism, and Generators are loaded plus the exact presence/absence of all four FTB hosts. In `without-ftb`, the claimed-team GameTest is an explicitly logged intentional skip after all four absences are asserted; the nearby-player test still sends and checks the actual fallback system-chat packet. None of these runtime assertions has been executed yet.

The following remain pending the parent's shared Windows build slot: ForgeGradle configuration/build, Gradle's mapped dependency resolution and Mixin annotation processor, reobfuscated shipped-JAR inspection, and both GameTest modes. The source and targeted `javac` results do not establish production JAR or runtime correctness.
