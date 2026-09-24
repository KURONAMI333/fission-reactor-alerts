package com.kuronami.fissionreactoralerts.alert;

import com.kuronami.fissionreactoralerts.FissionReactorAlerts;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import mekanism.generators.common.content.fission.FissionReactorMultiblockData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

/**
 * Server-side watch loop entry point, invoked from the reactor tick mixin.
 *
 * <p>State is keyed by Mekanism's {@code inventoryID} (a UUID persisted in the
 * structure's NBT, stable across chunk reloads and restarts while the reactor
 * stays formed). Reactors whose inventoryID is missing fall back to a
 * dimension+position key so a partially initialised structure still gets one
 * coherent identity. Entries are swept after 30 minutes without a tick so a
 * dismantled reactor does not leak state — and a reactor that reforms while
 * still damaged correctly re-alerts once.
 */
public final class ReactorAlertTracker {

    private static final long STALE_TICKS = 30L * 60 * 20; // 30 min at 20 tps
    private static final long SWEEP_INTERVAL = 20L * 60;   // sweep once a minute

    private static final Map<String, Tracked> STATES = new HashMap<>();
    private static long lastSweep = Long.MIN_VALUE;
    private static WeakReference<MinecraftServer> activeServer = new WeakReference<>(null);

    /** Total alerts dispatched this session — the observable surface GameTest asserts on. */
    private static final AtomicInteger TOTAL_ALERTS = new AtomicInteger();

    private ReactorAlertTracker() {}

    /** Visible for GameTest verification. */
    public static int totalAlertsFired() {
        return TOTAL_ALERTS.get();
    }

    /** Visible for GameTest verification — per-reactor so parallel tests don't interfere. */
    public static boolean isReactorAlerted(FissionReactorMultiblockData reactor, ServerLevel level) {
        Tracked t = STATES.get(reactorKey(reactor, level));
        return t != null && t.alert.isAlerted();
    }

    private static final class Tracked {
        final DamageAlertState alert = new DamageAlertState();
        long lastSeen;
    }

    /** Called at the tail of {@code FissionReactorMultiblockData#tick}. */
    public static void onReactorTick(FissionReactorMultiblockData reactor, Level world) {
        if (world.isClientSide() || !(world instanceof ServerLevel level)) return;
        if (!reactor.isFormed()) return;

        // A new integrated/dedicated server in the same JVM must not inherit an
        // old reactor's latch or keep its server/world objects alive.
        if (activeServer.get() != level.getServer()) {
            STATES.clear();
            lastSweep = Long.MIN_VALUE;
            activeServer = new WeakReference<>(level.getServer());
        }

        long now = level.getGameTime();
        String key = reactorKey(reactor, level);
        Tracked tracked = STATES.computeIfAbsent(key, k -> new Tracked());
        tracked.lastSeen = now;

        if (tracked.alert.update(reactor.reactorDamage)) {
            TOTAL_ALERTS.incrementAndGet();
            try {
                AlertDispatcher.dispatch(level, reactor);
            } catch (Exception | LinkageError t) {
                // A delivery failure must never break the reactor's tick.
                FissionReactorAlerts.LOGGER.warn("Failed to dispatch fission reactor alert: {}", t.toString());
                FissionReactorAlerts.LOGGER.debug("Dispatch failure detail", t);
            }
        }

        if (lastSweep == Long.MIN_VALUE || now < lastSweep || now - lastSweep >= SWEEP_INTERVAL) {
            lastSweep = now;
            sweep(now);
        }
    }

    private static void sweep(long now) {
        Iterator<Map.Entry<String, Tracked>> it = STATES.entrySet().iterator();
        while (it.hasNext()) {
            long lastSeen = it.next().getValue().lastSeen;
            if (now < lastSeen || now - lastSeen > STALE_TICKS) {
                it.remove();
            }
        }
    }

    private static String reactorKey(FissionReactorMultiblockData reactor, ServerLevel level) {
        if (reactor.inventoryID != null) {
            return reactor.inventoryID.toString();
        }
        BlockPos min = reactor.getMinPos();
        return "pos:" + level.dimension().location() + ":" + min.getX() + "," + min.getY() + "," + min.getZ();
    }
}
