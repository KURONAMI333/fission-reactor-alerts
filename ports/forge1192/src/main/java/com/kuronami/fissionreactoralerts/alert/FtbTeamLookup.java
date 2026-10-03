package com.kuronami.fissionreactoralerts.alert;

import com.kuronami.fissionreactoralerts.FissionReactorAlerts;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

/**
 * Optional FTB bridge loader. The FTB-typed implementation lives in a
 * separate class and is loaded only after both optional mods are confirmed,
 * so a server without FTB can still use nearby-player delivery.
 */
public final class FtbTeamLookup {
    private static final String BRIDGE_CLASS =
            "com.kuronami.fissionreactoralerts.alert.FtbTeamLookupLoaded";
    private static boolean presenceChecked;
    private static boolean present;
    private static boolean warned;

    private FtbTeamLookup() {}

    private static boolean isAvailable() {
        if (!presenceChecked) {
            present = ModList.get().isLoaded("ftbchunks") && ModList.get().isLoaded("ftbteams");
            presenceChecked = true;
        }
        return present;
    }

    /**
     * Adds the online members of teams that claim a reactor chunk.
     * @return team names when claims exist; otherwise {@code null}, which asks
     *         the caller to use proximity fallback.
     */
    @SuppressWarnings("unchecked")
    public static List<String> collectOnlineMembers(
            ServerLevel level, BlockPos min, BlockPos max, Set<ServerPlayer> recipients) {
        if (!isAvailable()) return null;
        try {
            Class<?> bridge = Class.forName(BRIDGE_CLASS, true, FtbTeamLookup.class.getClassLoader());
            Method method = bridge.getMethod(
                    "collectOnlineMembers", ServerLevel.class, BlockPos.class, BlockPos.class, Set.class);
            return (List<String>) method.invoke(null, level, min, max, recipients);
        } catch (InvocationTargetException e) {
            report(e.getCause() == null ? e : e.getCause());
        } catch (ReflectiveOperationException | LinkageError e) {
            report(e);
        }
        return null;
    }

    private static void report(Throwable t) {
        if (!warned) {
            warned = true;
            FissionReactorAlerts.LOGGER.warn("FTB Chunks/Teams lookup failed; using nearby-player fallback: {}", t.toString());
        }
        FissionReactorAlerts.LOGGER.debug("FTB lookup failure detail", t);
    }
}
