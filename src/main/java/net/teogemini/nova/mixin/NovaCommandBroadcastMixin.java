package net.teogemini.nova.mixin;

import net.minecraft.network.chat.LastSeenMessages;
import net.minecraft.network.protocol.game.ServerboundChatCommandSignedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.teogemini.nova.ai.dialogue.NovaMainChatListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class NovaCommandBroadcastMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(method = "performUnsignedChatCommand", at = @At("HEAD"), cancellable = true)
    private void nova$onPerformUnsignedChatCommand(String command, CallbackInfo ci) {
        if (NovaMainChatListener.tryHandleCommandPacket(this.player, command)) {
            ci.cancel();
        }
    }

    @Inject(method = "performSignedChatCommand", at = @At("HEAD"), cancellable = true)
    private void nova$onPerformSignedChatCommand(ServerboundChatCommandSignedPacket packet, LastSeenMessages lastSeenMessages, CallbackInfo ci) {
        if (packet != null && NovaMainChatListener.tryHandleCommandPacket(this.player, packet.command())) {
            ci.cancel();
        }
    }
}
