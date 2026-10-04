package net.teogemini.nova.network;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.teogemini.nova.ROBOTNOVA_MOD;
import net.teogemini.nova.entity.NovaChestHandler;
import net.teogemini.nova.entity.NovaEntity;
import net.teogemini.nova.entity.NovaWorkSupport;
import net.teogemini.nova.entity.companion.NovaChestAccess;

public final class NovaCompanionCommands {
    private NovaCompanionCommands() {}
    public record Request(int entityId, UUID uuid, int action) implements CustomPacketPayload {
        public static final Type<Request> TYPE=new Type<>(ROBOTNOVA_MOD.id("companion_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Request> CODEC=StreamCodec.of(
                (b,v)->{ b.writeVarInt(v.entityId); b.writeUUID(v.uuid); b.writeVarInt(v.action); },
                b->new Request(b.readVarInt(),b.readUUID(),b.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record Page(int entityId, UUID uuid, String name, String personality, int bond,
                       boolean autoTools, String totals, List<String> entries, int deposits, int buttons)
            implements CustomPacketPayload {
        public Page { entries=List.copyOf(entries); }
        public static final Type<Page> TYPE=new Type<>(ROBOTNOVA_MOD.id("companion_page"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Page> CODEC=StreamCodec.of((b,v)->{
            b.writeVarInt(v.entityId); b.writeUUID(v.uuid); b.writeUtf(v.name,128); b.writeUtf(v.personality,32);
            b.writeVarInt(v.bond); b.writeBoolean(v.autoTools); b.writeUtf(v.totals,256);
            b.writeVarInt(v.entries.size()); for (String line:v.entries) b.writeUtf(line,180);
            b.writeVarInt(v.deposits); b.writeVarInt(v.buttons);
        },b->{
            int id=b.readVarInt(); UUID uuid=b.readUUID(); String name=b.readUtf(128), personality=b.readUtf(32);
            int bond=b.readVarInt(); boolean auto=b.readBoolean(); String totals=b.readUtf(256);
            int count=b.readVarInt(); if (count<0 || count>8) throw new IllegalArgumentException("Invalid journal length");
            List<String> entries=new ArrayList<>(); for (int i=0;i<count;i++) entries.add(b.readUtf(180));
            return new Page(id,uuid,name,personality,bond,auto,totals,entries,b.readVarInt(),b.readVarInt());
        });
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public static void register() {
        net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.AFTER.register((world,player,pos,state,entity)->{
            if (world.isClientSide()) return;
            for (NovaEntity nova:world.getEntitiesOfClass(NovaEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(8),
                    robot -> robot.isAlive() && robot.isTame() && robot.isOwnedBy(player)))
                nova.companion().ownerClearedPath(pos);
        });
        PayloadTypeRegistry.playC2S().register(Request.TYPE,Request.CODEC);
        PayloadTypeRegistry.playS2C().register(Page.TYPE,Page.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Request.TYPE,(request,context)->{
            var player=context.player();
            if (request.action<0 || request.action>2 || player.isSpectator() || !player.isAlive()
                    || !NovaRodControl.holdingRod(player)
                    || !(player.level().getEntity(request.entityId) instanceof NovaEntity nova)
                    || !nova.isAlive() || !nova.isTame() || !nova.getUUID().equals(request.uuid)
                    || !nova.isOwnedBy(player) || player.distanceToSqr(nova)>64 || !player.hasLineOfSight(nova)
                    || !nova.companion().acceptCommand()) return;
            var memories=nova.companion().memories();
            memories.initialize(nova.getUUID().hashCode());
            if (request.action==1) memories.cyclePersonality();
            if (request.action==2) memories.toggleAutoTools();
            String name=nova.getName().getString();
            ServerPlayNetworking.send(player,new Page(nova.getId(),nova.getUUID(),name.substring(0,Math.min(128,name.length())),
                    memories.personality().label,memories.bond(),memories.autoTools(),memories.totals(),memories.journal(),
                    memories.deposits().size(),memories.buttons().size()));
        });
    }
    public static InteractionResult markBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
        if (hand!=InteractionHand.MAIN_HAND || player.isSpectator() || !player.isShiftKeyDown()
                || !player.getItemInHand(hand).is(net.teogemini.nova.registry.ModBlocks.NOVA_CONTROL_ROD_ITEM)) return InteractionResult.PASS;
        var pos=hit.getBlockPos(); var state=level.getBlockState(pos);
        boolean button=state.getBlock() instanceof ButtonBlock;
        if (!button && !NovaChestAccess.chestBlock(state)) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer owner)) return InteractionResult.SUCCESS;
        NovaEntity nova=NovaChestHandler.selected(owner);
        if (nova==null || nova.isCarryingChest() || !nova.isTame() || !owner.hasLineOfSight(nova)
                || owner.distanceToSqr(Vec3.atCenterOf(pos))>64 || !server.mayInteract(owner,pos)) {
            NovaWorkSupport.message(player,"Dùng gậy điều khiển chọn NOVA ở gần trước, rồi Shift + bấm vào rương/nút cần cho phép.");
            return InteractionResult.SUCCESS;
        }
        GlobalPos global=GlobalPos.of(server.dimension(),pos.immutable());
        int result=button ? nova.companion().memories().toggleButton(global) : nova.companion().memories().toggleDeposit(global);
        nova.companion().retry.clear();
        NovaWorkSupport.message(player,result<0 ? "Đã đủ 9 vị trí; bỏ một vị trí trước nhé."
                : (result==1 ? "Đã cho phép " : "Đã bỏ quyền ")+(button ? "bấm nút này." : "cất đồ vào rương này."));
        return InteractionResult.SUCCESS;
    }
}
