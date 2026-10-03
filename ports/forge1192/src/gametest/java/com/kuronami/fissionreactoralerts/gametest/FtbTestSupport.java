package com.kuronami.fissionreactoralerts.gametest;

import dev.ftb.mods.ftbchunks.data.ClaimResult;
import dev.ftb.mods.ftbchunks.data.FTBChunksAPI;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.data.Team;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

/** Keep optional FTB references isolated from GameTests for FTB-free runs. */
final class FtbTestSupport {

    private FtbTestSupport() {}

    static Team requirePlayerTeam(ServerPlayer player) {
        Team team = FTBTeamsAPI.getPlayerTeam(player);
        if (team == null) {
            throw new IllegalStateException(
                    "FTB Teams did not create a team for fake player " + player.getGameProfile().getName());
        }
        return team;
    }

    static void claimAllChunks(ServerPlayer player, ServerLevel level, BlockPos min, BlockPos max) {
        List<ClaimResult> failures = new ArrayList<>();
        for (int cx = min.getX() >> 4; cx <= max.getX() >> 4; cx++) {
            for (int cz = min.getZ() >> 4; cz <= max.getZ() >> 4; cz++) {
                ClaimResult result = FTBChunksAPI.claimAsPlayer(
                        player, level.dimension(), new ChunkPos(cx, cz), false);
                if (!result.isSuccess()) failures.add(result);
            }
        }
        if (!failures.isEmpty()) {
            throw new IllegalStateException("claimAsPlayer failed: " + failures.get(0).claimResultName());
        }
    }
}
