package com.kuronami.fissionreactoralerts.alert;

import com.kuronami.fissionreactoralerts.FissionReactorAlerts;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import mekanism.generators.common.content.fission.FissionReactorMultiblockData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Delivers one reactor alert to the right players.
 *
 * <p>Recipient rule:
 * <ul>
 *   <li>Any chunk of the reactor claimed via FTB Chunks → the online members of
 *       every owning team (usually exactly one). Nearby outsiders are NOT
 *       notified — the claim defines who is responsible for the site.</li>
 *   <li>Completely unclaimed (or FTB Chunks absent) → players within
 *       {@value #NEARBY_RADIUS} blocks of the reactor's centre.</li>
 * </ul>
 * The server log always gets a line so an admin can audit even if nobody was
 * online to receive the message.
 */
public final class AlertDispatcher {

    private static final double NEARBY_RADIUS = 64.0;

    private AlertDispatcher() {}

    public static void dispatch(ServerLevel level, FissionReactorMultiblockData reactor) {
        BlockPos min = reactor.getMinPos();
        BlockPos max = reactor.getMaxPos();
        BlockPos center = new BlockPos(
                (min.getX() + max.getX()) / 2,
                (min.getY() + max.getY()) / 2,
                (min.getZ() + max.getZ()) / 2);
        String posText = center.getX() + ", " + center.getY() + ", " + center.getZ();
        String dimText = level.dimension().location().toString();
        long damagePct = reactor.getDamagePercent();
        // getTemperature() is the kelvin value Mekanism's damage logic uses;
        // getHeat() is stored thermal energy (capacity × temperature).
        long tempK = Math.round(reactor.heatCapacitor.getTemperature());

        Set<ServerPlayer> recipients = new LinkedHashSet<>();
        List<String> teamNames = FtbTeamLookup.collectOnlineMembers(level, min, max, recipients);
        if (teamNames == null) {
            // FTB Chunks absent or site unclaimed: fall back to proximity.
            double r2 = NEARBY_RADIUS * NEARBY_RADIUS;
            for (ServerPlayer p : level.players()) {
                if (p.blockPosition().distSqr(center) <= r2) {
                    recipients.add(p);
                }
            }
        }

        Component message = teamNames == null || teamNames.isEmpty()
                ? Component.translatable("message.fissionreactoralerts.damage", posText, dimText, damagePct, tempK)
                : Component.translatable("message.fissionreactoralerts.damage_team",
                        posText, dimText, damagePct, tempK, String.join(", ", teamNames));

        for (ServerPlayer p : recipients) {
            p.sendSystemMessage(message);
        }
        FissionReactorAlerts.LOGGER.warn(
                "Fission reactor taking damage at {} in {} (damage {}%, temp {} K) — alerted {} player(s){}",
                posText, dimText, damagePct, tempK, recipients.size(),
                teamNames != null && !teamNames.isEmpty() ? " of team(s) " + teamNames : "");
    }
}
