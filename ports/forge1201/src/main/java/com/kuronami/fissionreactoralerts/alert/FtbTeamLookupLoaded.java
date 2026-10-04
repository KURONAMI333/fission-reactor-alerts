package com.kuronami.fissionreactoralerts.alert;

import com.kuronami.fissionreactoralerts.FissionReactorAlerts;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.api.ClaimedChunkManager;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftbteams.api.Team;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.Nullable;

/**
 * FTB Chunks → FTB Teams bridge, isolated so the rest of the mod never loads
 * an FTB class when the mods are absent.
 *
 * <p>Reactor blocks carry no owner information (Mekanism does not attach its
 * security/owner attribute to fission casings), so the reactor's owning team
 * is derived spatially: every chunk covered by the reactor's bounds is looked
 * up in the claim manager and each claimed chunk contributes its team's
 * online members. This is a lookup only — no claim or permission state is
 * modified.
 */
public final class FtbTeamLookupLoaded {

    private static boolean presenceChecked;
    private static boolean present;
    private static boolean warned;

    private FtbTeamLookupLoaded() {}

    private static boolean isAvailable() {
        if (!presenceChecked) {
            present = ModList.get().isLoaded("ftbchunks") && ModList.get().isLoaded("ftbteams");
            presenceChecked = true;
        }
        return present;
    }

    /**
     * Adds the online members of every team owning a chunk under the reactor to
     * {@code recipients}.
     *
     * @return team display names if at least one covered chunk is claimed;
     *         {@code null} when FTB Chunks is absent, the manager is not ready,
     *         or no covered chunk is claimed (caller falls back to proximity)
     */
    @Nullable
    public static List<String> collectOnlineMembers(
            ServerLevel level, BlockPos min, BlockPos max, Set<ServerPlayer> recipients) {
        if (!isAvailable()) return null;
        try {
            if (!FTBChunksAPI.api().isManagerLoaded()) return null;
            ClaimedChunkManager manager = FTBChunksAPI.api().getManager();

            Set<Team> teams = new LinkedHashSet<>();
            for (int cx = min.getX() >> 4; cx <= max.getX() >> 4; cx++) {
                for (int cz = min.getZ() >> 4; cz <= max.getZ() >> 4; cz++) {
                    ClaimedChunk chunk = manager.getChunk(new ChunkDimPos(level.dimension(), cx, cz));
                    if (chunk != null && chunk.getTeamData() != null) {
                        Team team = chunk.getTeamData().getTeam();
                        if (team != null) teams.add(team);
                    }
                }
            }
            if (teams.isEmpty()) return null;

            List<String> names = new ArrayList<>();
            for (Team team : teams) {
                names.add(team.getName().getString());
                recipients.addAll(team.getOnlineMembers());
            }
            return names;
        } catch (Exception | LinkageError t) {
            report(t);
            return null;
        }
    }

    /**
     * A lookup failure means "fall back to proximity this time", not "stop
     * working forever" — first failure logs a warning, later ones stay at
     * debug so a transient failure can't silently mute the feature.
     */
    private static void report(Throwable t) {
        if (!warned) {
            warned = true;
            FissionReactorAlerts.LOGGER.warn("FTB Chunks/Teams lookup failed: {}", t.toString());
        }
        FissionReactorAlerts.LOGGER.debug("FTB lookup failure detail", t);
    }
}
