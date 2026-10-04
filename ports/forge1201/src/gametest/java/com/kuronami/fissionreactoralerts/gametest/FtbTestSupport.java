package com.kuronami.fissionreactoralerts.gametest;

import dev.ftb.mods.ftbchunks.api.ClaimResult;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

/**
 * FTB-dependent test helpers. Every FTB-typed signature lives here so the
 * class is only loaded when FTB Chunks/Teams are actually present — the
 * caller guards with {@code ModList} before touching it.
 */
final class FtbTestSupport {

    private FtbTestSupport() {}

    /** The player's effective team, created by FTB Teams on login. */
    static Team requirePlayerTeam(ServerPlayer player) {
        return FTBTeamsAPI.api().getManager().getTeamForPlayer(player)
                .orElseThrow(() -> new IllegalStateException(
                        "FTB Teams did not create a team for fake player " + player.getGameProfile().getName()));
    }

    /**
     * Claims every chunk covered by [min, max] as the given player.
     *
     * @throws IllegalStateException when any claim did not succeed
     */
    static void claimAllChunks(ServerPlayer player, ServerLevel level, BlockPos min, BlockPos max) {
        List<ClaimResult> failures = new ArrayList<>();
        for (int cx = min.getX() >> 4; cx <= max.getX() >> 4; cx++) {
            for (int cz = min.getZ() >> 4; cz <= max.getZ() >> 4; cz++) {
                ClaimResult r = FTBChunksAPI.api()
                        .claimAsPlayer(player, level.dimension(), new ChunkPos(cx, cz), false);
                if (!r.isSuccess()) failures.add(r);
            }
        }
        if (!failures.isEmpty()) {
            throw new IllegalStateException("claimAsPlayer failed for " + failures.size()
                    + " chunk(s): " + failures.get(0).getResultId());
        }
    }
}
