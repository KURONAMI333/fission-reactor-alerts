package com.kuronami.fissionreactoralerts.alert;

import com.kuronami.fissionreactoralerts.FissionReactorAlerts;
import dev.ftb.mods.ftbchunks.data.ClaimedChunk;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkManager;
import dev.ftb.mods.ftbchunks.data.FTBChunksAPI;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftbteams.data.Team;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

/** FTB 1.19.2 bridge, isolated so it is never loaded without both optional mods. */
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
     * Adds online members of every FTB team claiming a chunk beneath the reactor.
     * @return team names when any covered chunk is claimed; {@code null} when
     *         FTB is unavailable, not ready, or the site is unclaimed.
     */
    public static List<String> collectOnlineMembers(
            ServerLevel level, BlockPos min, BlockPos max, Set<ServerPlayer> recipients) {
        if (!isAvailable()) return null;
        try {
            if (!FTBChunksAPI.isManagerLoaded()) return null;
            ClaimedChunkManager manager = FTBChunksAPI.getManager();
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

    private static void report(Throwable t) {
        if (!warned) {
            warned = true;
            FissionReactorAlerts.LOGGER.warn("FTB Chunks/Teams lookup failed: {}", t.toString());
        }
        FissionReactorAlerts.LOGGER.debug("FTB lookup failure detail", t);
    }
}
