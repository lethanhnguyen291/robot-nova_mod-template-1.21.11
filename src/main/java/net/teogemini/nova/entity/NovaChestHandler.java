package net.teogemini.nova.entity;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.teogemini.nova.registry.ModBlocks;

public final class NovaChestHandler {
    private record Selection(UUID nova, long expires) {}
    private static final Map<ServerPlayer, Selection> SELECTED = new WeakHashMap<>();
    private NovaChestHandler() {}

    public static void select(NovaEntity nova, Player player) {
        if (!(player instanceof ServerPlayer owner)) return;
        SELECTED.put(owner, new Selection(nova.getUUID(), owner.level().getGameTime() + 1200));
        NovaWorkSupport.message(player, "Đã chọn mình. Dùng la bàn bấm vào rương NOVA trong 60 giây để liên kết.");
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (hand != InteractionHand.MAIN_HAND || player.isSpectator()
                    || !player.getItemInHand(hand).is(Items.COMPASS)
                    || !level.getBlockState(hit.getBlockPos()).is(ModBlocks.NOVA_CHEST)) {
                return InteractionResult.PASS;
            }
            if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer owner)) {
                return InteractionResult.SUCCESS;
            }
            Selection selection = SELECTED.get(owner);
            if (selection == null || selection.expires() < server.getGameTime()
                    || !(server.getEntity(selection.nova()) instanceof NovaEntity nova)
                    || !nova.isAlive() || !nova.isOwnedBy(owner)
                    || owner.distanceToSqr(nova) > 64.0
                    || owner.distanceToSqr(Vec3.atCenterOf(hit.getBlockPos())) > 64.0
                    || nova.isCarryingChest() || !server.mayInteract(owner, hit.getBlockPos())) {
                NovaWorkSupport.message(owner, "Đến gần, chọn mình bằng la bàn rồi chọn rương nhé.");
                return InteractionResult.SUCCESS;
            }
            nova.work().link(GlobalPos.of(server.dimension(), hit.getBlockPos().immutable()));
            nova.getNavigation().stop();
            SELECTED.remove(owner);
            NovaWorkSupport.message(owner, "Đã liên kết rương. Vùng làm việc cách rương tối đa 16 khối.");
            return InteractionResult.SUCCESS;
        });
    }

    public static Container linkedContainer(NovaEntity nova) {
        BlockPos pos = nova.getLinkedChest();
        if (pos == null || !nova.level().hasChunkAt(pos)) return null;
        var state = nova.level().getBlockState(pos);
        if (!state.is(ModBlocks.NOVA_CHEST)) return null;
        // Includes both halves of a double chest and respects a blocked lid.
        return ChestBlock.getContainer(ModBlocks.NOVA_CHEST, state, nova.level(), pos, false);
    }

    public static void toggleCarry(NovaEntity nova, Player player) {
        if (!(nova.level() instanceof ServerLevel level) || !nova.isOwnedBy(player)) return;
        if (nova.isPerformingAction()) {
            NovaWorkSupport.message(player, "Đợi mình kết thúc động tác hiện tại nhé.");
            return;
        }
        if (nova.isCarryingChest()) {
            place(nova, player, level);
            return;
        }
        BlockPos pos = nova.getLinkedChest();
        if (pos == null || !level.hasChunkAt(pos) || nova.distanceToSqr(Vec3.atCenterOf(pos)) > 9.0) {
            NovaWorkSupport.message(player, "Liên kết rương rồi đưa mình đến cách rương dưới 3 khối nhé.");
            return;
        }
        var state = level.getBlockState(pos);
        if (!state.is(ModBlocks.NOVA_CHEST) || state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            NovaWorkSupport.message(player, "Mình chỉ mang rương đơn; rương đôi vẫn dùng để cất đồ được.");
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof ChestBlockEntity chest)
                || ChestBlockEntity.getOpenCount(level, pos) > 0 || !level.mayInteract(player, pos)
                || !player.hasLineOfSight(nova)) return;
        chest.unpackLootTable(player);
        var saved = chest.saveWithFullMetadata(level.registryAccess());
        ItemStack packed = new ItemStack(ModBlocks.NOVA_CHEST_ITEM);
        packed.applyComponents(chest.collectComponents());
        packed.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(chest.getType(), saved));
        if (chest.hasCustomName()) packed.set(DataComponents.CUSTOM_NAME, chest.getCustomName());
        // Empty the world container first so vanilla does not also drop its contents.
        chest.clearContent();
        if (!level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState())) {
            chest.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
            chest.setChanged();
            return;
        }
        nova.work().setCarriedChest(packed);
        nova.setNovaCarrying(true);
        nova.stopRally();
        nova.clearWorkAnimation();
        nova.setNovaMoveMode(NovaEntity.MODE_FOLLOW);
        NovaWorkSupport.message(player, "Đã nhấc rương và giữ đồ bên trong. Dùng cột thu lôi lần nữa để đặt xuống.");
    }

    private static void place(NovaEntity nova, Player player, ServerLevel level) {
        BlockPos pos = nova.blockPosition().relative(nova.getDirection());
        if (!level.hasChunkAt(pos) || !level.isInWorldBounds(pos)
                || !level.getWorldBorder().isWithinBounds(pos) || !level.mayInteract(player, pos)
                || !level.getBlockState(pos).isAir()
                || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                || !level.noCollision(new AABB(pos).deflate(0.07))) {
            NovaWorkSupport.message(player, "Cần ô trống có nền chắc ngay phía trước mình để đặt rương.");
            return;
        }
        var state = ModBlocks.NOVA_CHEST.defaultBlockState()
                .setValue(ChestBlock.FACING, nova.getDirection())
                .setValue(ChestBlock.TYPE, ChestType.SINGLE);
        ItemStack packed = nova.work().carriedChest();
        var data = packed.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null || !level.setBlockAndUpdate(pos, state)) return;
        if (!(level.getBlockEntity(pos) instanceof ChestBlockEntity chest)) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            return;
        }
        chest.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,
                level.registryAccess(), data.copyTagWithoutId()));
        chest.applyComponentsFromItemStack(packed);
        chest.setChanged();
        level.sendBlockUpdated(pos, state, state, 3);
        nova.work().setCarriedChest(ItemStack.EMPTY);
        nova.setNovaCarrying(false);
        nova.work().link(GlobalPos.of(level.dimension(), pos.immutable()));
        NovaWorkSupport.message(player, "Đã đặt rương và liên kết lại.");
    }
}
