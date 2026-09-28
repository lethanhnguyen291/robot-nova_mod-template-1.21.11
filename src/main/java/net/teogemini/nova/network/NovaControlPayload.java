package net.teogemini.nova.network;

import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.teogemini.nova.ROBOTNOVA_MOD;
import net.teogemini.nova.entity.NovaEntity;

public record NovaControlPayload(int entityId, UUID entityUuid, int action)
        implements CustomPacketPayload {
    public static final int SIT = 3;
    public static final int STAND = 4;
    public static final Type<NovaControlPayload> TYPE =
            new Type<>(ROBOTNOVA_MOD.id("nova_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NovaControlPayload> CODEC =
            StreamCodec.of((buffer, value) -> {
                buffer.writeVarInt(value.entityId());
                buffer.writeUUID(value.entityUuid());
                buffer.writeVarInt(value.action());
            }, buffer -> new NovaControlPayload(
                    buffer.readVarInt(), buffer.readUUID(), buffer.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(TYPE, CODEC);
        ServerPlayNetworking.registerGlobalReceiver(TYPE, (payload, context) -> {
            var player = context.player();
            if (player.isSpectator() || !player.isAlive()
                    || !(player.level().getEntity(payload.entityId()) instanceof NovaEntity nova)
                    || !nova.getUUID().equals(payload.entityUuid()) || !nova.isAlive()
                    || !nova.isTame() || !nova.isOwnedBy(player)
                    || player.distanceToSqr(nova) > 64.0 || !player.hasLineOfSight(nova)) {
                return;
            }

            int action = payload.action();
            if (action >= NovaEntity.MODE_FOLLOW && action <= NovaEntity.MODE_WAIT) {
                nova.setNovaMoveMode(action);
            } else if (action == SIT || action == STAND) {
                nova.setNovaSitRequested(action == SIT);
            } else {
                return;
            }

            String message = switch (action) {
                case NovaEntity.MODE_FOLLOW -> "Mình sẽ đi theo bạn.";
                case NovaEntity.MODE_FREE -> "Mình sẽ tự đi lại quanh đây.";
                case NovaEntity.MODE_WAIT -> "Mình sẽ đứng chờ ở đây.";
                case SIT -> "Mình ngồi nghỉ nhé.";
                default -> "Mình đứng dậy rồi, tiếp tục chế độ trước đó.";
            };
            player.displayClientMessage(Component.literal("NOVA: " + message), true);
        });
    }
}
