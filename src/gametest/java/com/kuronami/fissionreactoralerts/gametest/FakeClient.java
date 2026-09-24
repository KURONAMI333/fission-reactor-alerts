package com.kuronami.fissionreactoralerts.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/**
 * A real {@link ServerPlayer} put through the full login path
 * ({@code PlayerList#placeNewPlayer}) on an {@link EmbeddedChannel}.
 *
 * <p>Going through {@code placeNewPlayer} (rather than a bare constructor)
 * registers the player in the player list and level, adds them to
 * {@code level.players()}, and fires {@code PlayerLoggedInEvent} — which is
 * what FTB Teams listens to when it auto-creates a player team. Outbound
 * packets stay queued on the embedded channel, so tests can assert what the
 * player would actually have received.
 */
record FakeClient(ServerPlayer player, EmbeddedChannel channel) {

    static FakeClient login(ServerLevel level, String name) {
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(
                new GameProfile(UUID.randomUUID(), name), false);
        ServerPlayer player = new ServerPlayer(
                level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        EmbeddedChannel channel = new EmbeddedChannel(connection);
        // Pretend a fully negotiated NeoForge client, or the login path throws
        // "Payload may not be sent to the client" when mods sync their data.
        NetworkRegistry.configureMockConnection(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        return new FakeClient(player, channel);
    }

    /**
     * Drains the channel's outbound queue and returns the content of every
     * system-chat packet sent to this player. With a bare EmbeddedChannel
     * there is no outbound encoder, so {@code readOutbound} yields the raw
     * {@code Packet} objects — no serialization is involved.
     */
    List<Component> drainSystemChat() {
        List<Component> out = new ArrayList<>();
        Object msg;
        while ((msg = channel.readOutbound()) != null) {
            if (msg instanceof ClientboundSystemChatPacket packet) {
                out.add(packet.content());
            }
        }
        return out;
    }
}
