package net.teogemini.nova.ai;

import net.minecraft.server.level.ServerPlayer;
import net.teogemini.nova.entity.NovaEntity;
import net.teogemini.nova.network.NovaDialogueNetwork;

/** The existing control-menu entry point now opens the offline dialogue screen. */
public final class NovaChatHandler {
    private NovaChatHandler() {}

    public static void handleChat(NovaEntity nova, ServerPlayer player) {
        NovaDialogueNetwork.open(nova, player);
    }
}
