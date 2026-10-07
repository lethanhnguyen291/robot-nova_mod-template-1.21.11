package net.teogemini.nova.network;

import java.util.UUID;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.teogemini.nova.ROBOTNOVA_MOD;
import net.teogemini.nova.entity.NovaEntity;
import net.teogemini.nova.entity.NovaWorkSupport;
import net.teogemini.nova.registry.ModBlocks;

public final class NovaRodControl {
    private NovaRodControl() {}

    public static boolean holdingRod(Player player) {
        return player.getMainHandItem().is(ModBlocks.NOVA_CONTROL_ROD_ITEM);
    }

    public record Open(int entityId, UUID uuid) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(ROBOTNOVA_MOD.id("open_rod_control"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Open> CODEC = StreamCodec.of(
                (buffer, value) -> { buffer.writeVarInt(value.entityId); buffer.writeUUID(value.uuid); },
                buffer -> new Open(buffer.readVarInt(), buffer.readUUID()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register() {
        PayloadTypeRegistry.playS2C().register(Open.TYPE, Open.CODEC);
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (hand != InteractionHand.MAIN_HAND || !holdingRod(player)
                    || !(entity instanceof NovaEntity nova)) return InteractionResult.PASS;
            if (player.isSpectator() || !player.isAlive()) return InteractionResult.FAIL;
            if (player instanceof ServerPlayer owner) {
                if (nova.isAlive() && nova.isTame() && nova.isOwnedBy(owner)
                        && owner.isWithinEntityInteractionRange(nova, 0) && owner.hasLineOfSight(nova)) {
                    if (owner.isShiftKeyDown()) {
                        NovaWorkSupport.recallTools(nova, owner);
                    } else {
                        ServerPlayNetworking.send(owner, new Open(nova.getId(), nova.getUUID()));
                    }
                } else {
                    NovaWorkSupport.message(owner, "Đến gần NOVA đã thuần phục của bạn để điều khiển nhé.");
                }
            }
            // SUCCESS sends the attack packet on the client, but cancels damage on BOTH sides.
            return InteractionResult.SUCCESS;
        });
    }
}
