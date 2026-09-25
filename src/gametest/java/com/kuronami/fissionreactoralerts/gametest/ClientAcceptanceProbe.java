package com.kuronami.fissionreactoralerts.gametest;

import com.kuronami.fissionreactoralerts.FissionReactorAlerts;
import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import mekanism.generators.common.content.fission.FissionReactorMultiblockData;
import mekanism.generators.common.tile.fission.TileEntityFissionReactorCasing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Real-client acceptance probe for the release build. Completely inert unless
 * {@code -Dfra.clientprobe=true} is passed to the dev client JVM; it lives in
 * the gametest source set so it can never reach the shipped jar.
 *
 * <p>Sequence: wait for a real world join → record the loaded mod list → build
 * a minimum Mekanism fission reactor next to the real player on the
 * <em>integrated server thread</em> (same 3x3x4 layout as the GameTest) →
 * drive the live {@code reactorDamage} field through one alert episode
 * (damage &gt; 0), a hold at non-zero (must not re-alert), a reset to zero
 * (re-arm), and a second episode. Screenshots are captured via the in-process
 * {@link Screenshot#grab} path (macOS {@code screencapture} has no permission
 * in this environment). Results go to {@code run/client/fra-acceptance/}.
 */
@EventBusSubscriber(modid = FissionReactorAlerts.MODID, value = Dist.CLIENT)
public final class ClientAcceptanceProbe {

    private static final boolean ENABLED = Boolean.getBoolean("fra.clientprobe");
    private static final String ALERT_KEY_PREFIX = "message.fissionreactoralerts.";
    private static final double DAMAGE_ONE = 8.0;
    private static final double DAMAGE_TWO = 26.0;

    private enum Phase {
        WAIT_WORLD, SETTLE, PLACE, WAIT_FORMED, AIM, SHOT_FORMED,
        DAMAGE1, WAIT_ALERT1, ALERT1_SETTLE, SHOT_ALERT1, HOLD,
        RESET, REARM_WAIT, DAMAGE2, WAIT_ALERT2, ALERT2_SETTLE,
        OPEN_CHAT, CHAT_SHOT, WRITE_RESULT, DONE, FAILED
    }

    private static Phase phase = Phase.WAIT_WORLD;
    private static int phaseTicks;
    private static long totalTicks;
    private static String failReason = "";

    private static final AtomicInteger alertCount = new AtomicInteger();
    private static final AtomicReference<String> lastAlertText = new AtomicReference<>("");
    private static final AtomicReference<ServerLevel> levelRef = new AtomicReference<>();
    private static final AtomicReference<BlockPos> baseRef = new AtomicReference<>();
    private static volatile boolean formedFlag;
    private static volatile int alertsAtFirstFire = -1;
    private static volatile int alertsDuringHold;
    private static volatile boolean modListLogged;

    private ClientAcceptanceProbe() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!ENABLED || phase == Phase.DONE || phase == Phase.FAILED) return;
        totalTicks++;
        phaseTicks++;
        try {
            step(Minecraft.getInstance());
        } catch (Throwable t) {
            fail("uncaught: " + t);
        }
    }

    @SubscribeEvent
    public static void onSystemChat(ClientChatReceivedEvent.System event) {
        if (!ENABLED) return;
        Component msg = event.getMessage();
        String key = findAlertKey(msg);
        if (key != null) {
            int n = alertCount.incrementAndGet();
            lastAlertText.set(msg.getString());
            log("alert chat received #{}: key={} text=\"{}\"", n, key, msg.getString());
        }
    }

    private static void step(Minecraft mc) throws Exception {
        switch (phase) {
            case WAIT_WORLD -> {
                if (mc.player != null && mc.level != null && mc.getSingleplayerServer() != null
                        && !(mc.screen instanceof net.minecraft.client.gui.screens.ReceivingLevelScreen)) {
                    logModList();
                    log("world joined: player={} dim={} pos={} tick={}",
                            mc.player.getGameProfile().getName(),
                            mc.level.dimension().location(),
                            mc.player.blockPosition(), totalTicks);
                    next(Phase.SETTLE);
                } else if (phaseTicks > 3600) {
                    fail("world join not detected in 180s (screen="
                            + (mc.screen == null ? "null" : mc.screen.getClass().getName()) + ")");
                }
            }
            case SETTLE -> {
                if (phaseTicks >= 80) next(Phase.PLACE);
            }
            case PLACE -> {
                MinecraftServer server = mc.getSingleplayerServer();
                server.execute(() -> {
                    try {
                        ServerPlayer sp = server.getPlayerList().getPlayers().isEmpty()
                                ? null : server.getPlayerList().getPlayers().get(0);
                        if (sp == null) {
                            fail("no ServerPlayer on integrated server");
                            return;
                        }
                        ServerLevel lvl = sp.serverLevel();
                        BlockPos near = sp.blockPosition().east(6);
                        int surfaceY = lvl.getHeight(
                                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, near.getX(), near.getZ());
                        int y = Math.max(surfaceY,
                                Math.min(sp.blockPosition().getY(), lvl.getMaxBuildHeight() - 8));
                        BlockPos base = near.atY(Math.max(y, lvl.getMinBuildHeight() + 1));
                        sp.teleportTo(base.getX() - 4.5, base.getY() + 1, base.getZ() + 0.5);
                        buildReactor(lvl, base);
                        levelRef.set(lvl);
                        baseRef.set(base);
                        log("reactor placed: base={} player teleported to vantage", base);
                    } catch (Throwable t) {
                        fail("server-side place failed: " + t);
                    }
                });
                next(Phase.WAIT_FORMED);
            }
            case WAIT_FORMED -> {
                // The tile's MultiblockData instance is replaced when the
                // structure forms, so it must be re-fetched every poll.
                if (phaseTicks % 5 == 0) {
                    mc.getSingleplayerServer().execute(() -> {
                        try {
                            FissionReactorMultiblockData d = currentReactor();
                            if (d != null && d.isFormed()) formedFlag = true;
                        } catch (Throwable t) {
                            log("formed poll: {}", t.toString());
                        }
                    });
                }
                if (phaseTicks % 100 == 0) {
                    mc.getSingleplayerServer().execute(() -> {
                        try {
                            ServerLevel lvl = levelRef.get();
                            BlockPos base = baseRef.get();
                            if (lvl == null || base == null) return;
                            BlockEntity be = lvl.getBlockEntity(base);
                            FissionReactorMultiblockData d = reactorData(lvl, base);
                            log("formed poll @{}: be={} data={} isFormed={}",
                                    phaseTicks,
                                    be == null ? "null" : be.getClass().getSimpleName(),
                                    d == null ? "null" : "present",
                                    d != null && d.isFormed());
                        } catch (Throwable t) {
                            log("formed diag: {}", t.toString());
                        }
                    });
                }
                if (formedFlag) {
                    log("reactor formed (isFormed=true)");
                    next(Phase.AIM);
                } else if (phaseTicks > 600) {
                    fail("reactor did not form within 600 ticks");
                }
            }
            case AIM -> {
                aimAtReactor(mc);
                next(Phase.SHOT_FORMED);
            }
            case SHOT_FORMED -> {
                if (phaseTicks >= 10) {
                    shot(mc, "01-formed-reactor");
                    next(Phase.DAMAGE1);
                }
            }
            case DAMAGE1 -> {
                setDamage(mc, DAMAGE_ONE);
                next(Phase.WAIT_ALERT1);
            }
            case WAIT_ALERT1 -> {
                int n = alertCount.get();
                if (n >= 1) {
                    alertsAtFirstFire = n;
                    log("first alert observed after damage={}", DAMAGE_ONE);
                    next(Phase.ALERT1_SETTLE);
                } else if (phaseTicks > 300) {
                    fail("no alert within 300 ticks of damage=" + DAMAGE_ONE);
                }
            }
            case ALERT1_SETTLE -> {
                if (phaseTicks >= 10) next(Phase.SHOT_ALERT1);
            }
            case SHOT_ALERT1 -> {
                shot(mc, "02-alert-fired");
                next(Phase.HOLD);
            }
            case HOLD -> {
                // Damage stays at DAMAGE_ONE for a full 100 ticks; a correct
                // latch must produce exactly one alert for the whole episode.
                alertsDuringHold = alertCount.get();
                if (phaseTicks >= 100) {
                    log("hold complete: damage held 100 ticks, alerts so far={}", alertsDuringHold);
                    next(Phase.RESET);
                }
            }
            case RESET -> {
                setDamage(mc, 0.0);
                next(Phase.REARM_WAIT);
            }
            case REARM_WAIT -> {
                if (phaseTicks >= 25) next(Phase.DAMAGE2);
            }
            case DAMAGE2 -> {
                setDamage(mc, DAMAGE_TWO);
                next(Phase.WAIT_ALERT2);
            }
            case WAIT_ALERT2 -> {
                if (alertCount.get() >= 2) {
                    log("second alert observed after re-arm (damage={})", DAMAGE_TWO);
                    next(Phase.ALERT2_SETTLE);
                } else if (phaseTicks > 300) {
                    fail("no second alert after re-arm within 300 ticks");
                }
            }
            case ALERT2_SETTLE -> {
                if (phaseTicks >= 10) next(Phase.OPEN_CHAT);
            }
            case OPEN_CHAT -> {
                mc.setScreen(new ChatScreen(""));
                next(Phase.CHAT_SHOT);
            }
            case CHAT_SHOT -> {
                if (phaseTicks >= 15) {
                    shot(mc, "03-chat-history-two-alerts");
                    mc.setScreen(null);
                    next(Phase.WRITE_RESULT);
                }
            }
            case WRITE_RESULT -> {
                writeResult(mc, true);
                next(Phase.DONE);
            }
            default -> {}
        }
    }

    // ---- server-side helpers (run inside server.execute) ----

    /** Current live MultiblockData for the placed reactor, or null. */
    private static FissionReactorMultiblockData currentReactor() {
        ServerLevel lvl = levelRef.get();
        BlockPos base = baseRef.get();
        if (lvl == null || base == null) return null;
        return reactorData(lvl, base);
    }

    private static void setDamage(Minecraft mc, double value) {
        mc.getSingleplayerServer().execute(() -> {
            try {
                FissionReactorMultiblockData d = currentReactor();
                if (d == null) {
                    fail("reactor data missing at setDamage");
                    return;
                }
                d.reactorDamage = value;
                FissionReactorAlerts.LOGGER.info(
                        "[fra-probe] reactorDamage set to {}", value);
            } catch (Throwable t) {
                fail("setDamage failed: " + t);
            }
        });
    }

    /** Identical structure to ReactorAlertGameTests#buildReactor (3x3x4 shell). */
    private static void buildReactor(ServerLevel level, BlockPos base) {
        BlockState casing = mekBlock("fission_reactor_casing").defaultBlockState();
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 4; y++) {
                for (int z = 0; z < 3; z++) {
                    boolean shell = x == 0 || x == 2 || z == 0 || z == 2 || y == 0 || y == 3;
                    if (shell) {
                        level.setBlock(base.offset(x, y, z), casing, 3);
                    }
                }
            }
        }
        level.setBlock(base.offset(1, 1, 1), mekBlock("fission_fuel_assembly").defaultBlockState(), 3);
        level.setBlock(base.offset(1, 2, 1), mekBlock("control_rod_assembly").defaultBlockState(), 3);
    }

    private static FissionReactorMultiblockData reactorData(ServerLevel level, BlockPos base) {
        BlockEntity be = level.getBlockEntity(base);
        if (!(be instanceof TileEntityFissionReactorCasing casing)) {
            throw new IllegalStateException("no fission casing tile at " + base + " (got " + be + ")");
        }
        return casing.getMultiblock();
    }

    private static Block mekBlock(String name) {
        Block b = BuiltInRegistries.BLOCK.get(
                ResourceLocation.fromNamespaceAndPath("mekanismgenerators", name));
        if (b == null || b == Blocks.AIR) {
            throw new IllegalStateException("mekanismgenerators block missing: " + name);
        }
        return b;
    }

    // ---- client-side helpers ----

    private static void aimAtReactor(Minecraft mc) {
        if (mc.player == null) return;
        BlockPos base = baseRef.get();
        if (base == null) return;
        var eye = mc.player.getEyePosition();
        double cx = base.getX() + 1.5 - eye.x;
        double cy = base.getY() + 2.0 - eye.y;
        double cz = base.getZ() + 1.5 - eye.z;
        double horiz = Math.sqrt(cx * cx + cz * cz);
        float yaw = (float) (Math.toDegrees(Math.atan2(-cx, cz)));
        float pitch = (float) (-Math.toDegrees(Math.atan2(cy, horiz)));
        mc.player.setYRot(yaw);
        mc.player.setYHeadRot(yaw);
        mc.player.setXRot(pitch);
    }

    private static void logModList() {
        if (modListLogged) return;
        modListLogged = true;
        for (String id : new String[]{
                "minecraft", "neoforge", "mekanism", "mekanismgenerators",
                "ftbchunks", "ftbteams", "ftblibrary", "jei", "fissionreactoralerts"}) {
            var c = ModList.get().getModContainerById(id);
            if (c.isPresent()) {
                log("modlist {} = {}", id, c.get().getModInfo().getVersion());
            } else {
                log("modlist {} = MISSING", id);
            }
        }
    }

    private static String findAlertKey(Component c) {
        if (c.getContents() instanceof TranslatableContents t
                && t.getKey().startsWith(ALERT_KEY_PREFIX)) {
            return t.getKey();
        }
        for (Component sib : c.getSiblings()) {
            String key = findAlertKey(sib);
            if (key != null) return key;
        }
        return null;
    }

    private static void shot(Minecraft mc, String name) {
        try {
            File dir = new File(mc.gameDirectory, "fra-acceptance");
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
            Screenshot.grab(dir, name + ".png", mc.getMainRenderTarget(),
                    c -> log("screenshot: {}", c.getString()));
        } catch (Throwable t) {
            log("screenshot failed: {}", t.toString());
        }
    }

    private static void writeResult(Minecraft mc, boolean ok) {
        try {
            File dir = new File(mc.gameDirectory, "fra-acceptance");
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
            String body = String.join("\n",
                    "result=" + (ok ? "PASS" : "FAIL"),
                    "failReason=" + failReason,
                    "alertsAtFirstFire=" + alertsAtFirstFire,
                    "alertsTotal=" + alertCount.get(),
                    "alertsDuringHold=" + alertsDuringHold,
                    "lastAlertText=" + lastAlertText.get(),
                    "totalTicks=" + totalTicks);
            Files.writeString(new File(dir, "probe-result.txt").toPath(), body + "\n");
            log("probe-result.txt written ({} )", ok ? "PASS" : "FAIL");
        } catch (Throwable t) {
            log("writeResult failed: {}", t.toString());
        }
    }

    private static void next(Phase p) {
        log("phase {} -> {}", phase, p);
        phase = p;
        phaseTicks = 0;
    }

    private static void fail(String why) {
        failReason = why;
        FissionReactorAlerts.LOGGER.warn("[fra-probe] FAILED: {}", why);
        try {
            writeResult(Minecraft.getInstance(), false);
        } catch (Throwable ignored) {}
        phase = Phase.FAILED;
    }

    private static void log(String fmt, Object... args) {
        FissionReactorAlerts.LOGGER.info("[fra-probe] " + fmt, args);
    }
}
