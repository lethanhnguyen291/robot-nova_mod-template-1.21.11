package net.teogemini.nova.network;

import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.teogemini.nova.ROBOTNOVA_MOD;
import net.teogemini.nova.ai.NovaChatHandler;
import net.teogemini.nova.entity.NovaEntity;

public record NovaControlPayload(int entityId, UUID entityUuid, int action)
        implements CustomPacketPayload {
    public static final int SIT = 3;
    public static final int STAND = 4;
    public static final int TOGGLE_WORK = 5;
    public static final int FACE_NEXT = 6;
    public static final int FACE_PREV = 7;
    public static final int FACE_RESET = 8;
    public static final int ACTION_WAVE = 10;
    public static final int ACTION_CELEBRATE = 11;
    public static final int ACTION_APOLOGIZE = 12;
    public static final int ACTION_NOD = 13;
    public static final int ACTION_PROUD = 14;
    public static final int CHAT = 20;

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
            if (player.isSpectator() || !player.isAlive() || !NovaRodControl.holdingRod(player)
                    || !(player.level().getEntity(payload.entityId()) instanceof NovaEntity nova)
                    || !nova.getUUID().equals(payload.entityUuid()) || !nova.isAlive()
                    || !nova.isTame() || !nova.isOwnedBy(player)
                    || player.distanceToSqr(nova) > 64.0 || !player.hasLineOfSight(nova)) {
                return;
            }

            int action = payload.action();
            if (action >= NovaEntity.MODE_FOLLOW && action <= NovaEntity.MODE_WAIT) {
                nova.setNovaMoveMode(action);
                String message = switch (action) {
                    case NovaEntity.MODE_FOLLOW -> "Mình sẽ đi theo bạn.";
                    case NovaEntity.MODE_FREE -> "Mình sẽ tự đi lại quanh đây.";
                    default -> "Mình sẽ đứng chờ ở đây.";
                };
                player.displayClientMessage(Component.literal("NOVA: " + message), true);
            } else if (action == SIT || action == STAND) {
                nova.setNovaSitRequested(action == SIT);
                String message = action == SIT ? "Mình ngồi nghỉ nhé." : "Mình đứng dậy rồi, tiếp tục chế độ trước đó.";
                player.displayClientMessage(Component.literal("NOVA: " + message), true);
            } else if (action == TOGGLE_WORK) {
                nova.companion().ownerCommand();
                nova.work().setEnabled(!nova.work().enabled());
                nova.clearWorkAnimation();
                nova.getNavigation().stop();
                if (nova.work().enabled()) nova.setNovaMoveMode(NovaEntity.MODE_FREE);
                player.displayClientMessage(Component.literal("NOVA: " + (nova.work().enabled()
                        ? "Bật làm việc trong vùng rương đã liên kết." : "Đã tắt làm việc.")), true);
            } else if (action == FACE_NEXT) {
                int face = nova.cyclePreviewFace(1);
                player.displayClientMessage(Component.literal("NOVA: Biểu cảm " + face + "/39"), true);
            } else if (action == FACE_PREV) {
                int face = nova.cyclePreviewFace(-1);
                player.displayClientMessage(Component.literal("NOVA: Biểu cảm " + face + "/39"), true);
            } else if (action == FACE_RESET) {
                nova.resetPreviewFace();
                player.displayClientMessage(Component.literal("NOVA: Đã trở về biểu cảm tự động."), true);
            } else if (action == ACTION_WAVE) {
                if (nova.isPerformingAction()) {
                    player.displayClientMessage(Component.literal("NOVA: Mình đang làm động tác rồi."), true);
                } else {
                    nova.startNovaAction(NovaEntity.ACTION_WAVE);
                    player.displayClientMessage(Component.literal("NOVA: Chào chủ nhân!"), true);
                }
            } else if (action == ACTION_CELEBRATE) {
                if (nova.isPerformingAction()) {
                    player.displayClientMessage(Component.literal("NOVA: Mình đang làm động tác rồi."), true);
                } else {
                    nova.startNovaAction(NovaEntity.ACTION_CELEBRATE);
                    player.displayClientMessage(Component.literal("NOVA: Ăn mừng nào!"), true);
                }
            } else if (action == ACTION_APOLOGIZE) {
                nova.startApologizeAction();
                player.displayClientMessage(Component.literal("NOVA: Mình xin lỗi ạ..."), true);
            } else if (action == ACTION_NOD) {
                nova.companion().gesture("nod");
                nova.showNovaFace(0, 40);
                player.displayClientMessage(Component.literal("NOVA: *Gật đầu đồng ý*"), true);
            } else if (action == ACTION_PROUD) {
                nova.companion().gesture("proud");
                player.displayClientMessage(Component.literal("NOVA: *Tự hào*"), true);
            } else if (action == CHAT) {
                NovaChatHandler.handleChat(nova, player);
            }
        });
    }
}

