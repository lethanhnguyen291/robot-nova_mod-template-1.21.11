package net.teogemini.nova.server;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;

public final class NovaOwnerSessions {
    private static final Map<UUID, String> SESSIONS = new HashMap<>();

    private NovaOwnerSessions() {
    }

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                SESSIONS.put(handler.getPlayer().getUUID(), UUID.randomUUID().toString()));

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                SESSIONS.remove(handler.getPlayer().getUUID()));

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> SESSIONS.clear());
    }

    public static String get(ServerPlayer player) {
        return SESSIONS.getOrDefault(player.getUUID(), "");
    }
}