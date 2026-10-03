package com.kuronami.fissionreactoralerts.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.Connection;
import net.minecraft.world.entity.player.ProfilePublicKey;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** A server player registered through the real player-list login path. */
record FakeClient(ServerPlayer player, EmbeddedChannel channel) {

    static FakeClient login(ServerLevel level, String name) {
        ServerPlayer player = new ServerPlayer(
                level.getServer(), level, new GameProfile(UUID.randomUUID(), name), (ProfilePublicKey) null);
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        EmbeddedChannel channel = new EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player);
        return new FakeClient(player, channel);
    }

    List<Component> drainSystemChat() {
        List<Component> out = new ArrayList<>();
        Object msg;
        while ((msg = channel.readOutbound()) != null) {
            if (msg instanceof ClientboundSystemChatPacket packet) out.add(packet.content());
        }
        return out;
    }
}
