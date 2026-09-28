package net.teogemini.nova.entity;

import java.util.Comparator;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.teogemini.nova.registry.ModBlocks;

public final class NovaRallyManager {
    private static final int[] COLUMNS = {0, -2, 2, -4, 4};
    private NovaRallyManager() {}

    public static void register() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (hand != InteractionHand.MAIN_HAND || player.isSpectator()
                    || !player.getItemInHand(hand).is(Items.LIGHTNING_ROD)
                    || !level.getBlockState(hit.getBlockPos()).is(ModBlocks.NOVA_CHEST)) {
                return InteractionResult.PASS;
            }
            if (level instanceof ServerLevel server) {
                return triggerRally(server, hit.getBlockPos(), player);
            }
            return InteractionResult.SUCCESS;
        });
    }

    // Existing bell-on-robot shortcut uses its linked chest, never the player's position.
    public static void toggle(NovaEntity selected, Player owner) {
        if (!(selected.level() instanceof ServerLevel level) || !selected.isOwnedBy(owner)) return;
        BlockPos chest = selected.getLinkedChest();
        if (chest == null || !level.hasChunkAt(chest)
                || !level.getBlockState(chest).is(ModBlocks.NOVA_CHEST)) {
            NovaWorkSupport.message(owner, "Dùng cột thu lôi bấm vào rương NOVA để tập hợp nhé.");
            return;
        }
        triggerRally(level, chest, owner);
    }

    public static InteractionResult triggerRally(ServerLevel level, BlockPos chestPos, Player owner) {
        if (owner.isSpectator() || owner.level() != level || !level.hasChunkAt(chestPos)) {
            return InteractionResult.PASS;
        }
        var state = level.getBlockState(chestPos);
        if (!state.is(ModBlocks.NOVA_CHEST)) return InteractionResult.PASS;
        var robots = level.getEntitiesOfClass(NovaEntity.class, new AABB(chestPos).inflate(64.0),
                nova -> nova.isAlive() && nova.isOwnedBy(owner) && !nova.isNoAi());
        if (robots.isEmpty()) {
            NovaWorkSupport.message(owner, "Không có robot của bạn trong vùng 64 khối quanh rương.");
            return InteractionResult.SUCCESS;
        }
        if (robots.stream().anyMatch(NovaEntity::isRallying)) {
            robots.forEach(NovaEntity::stopRally);
            level.playSound(null, chestPos, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 1.0F, 1.0F);
            NovaWorkSupport.message(owner, "Đội hình giải tán! Trở lại chế độ trước đó.");
            return InteractionResult.SUCCESS;
        }

        robots.sort(Comparator.comparingDouble(nova -> nova.distanceToSqr(Vec3.atCenterOf(chestPos))));
        Direction facing = state.getValue(ChestBlock.FACING);
        for (int index = 0; index < robots.size(); index++) {
            robots.get(index).startRally(formationPosition(chestPos, facing, index), chestPos);
        }
        robots.getFirst().work().showRallyLeader(robots.getFirst(), robots.size());
        level.playSound(null, chestPos, SoundEvents.RAID_HORN.value(), SoundSource.BLOCKS, 5.0F, 1.0F);
        owner.displayClientMessage(Component.literal("Tập hợp đội hình!").withStyle(ChatFormatting.RED), true);
        return InteractionResult.SUCCESS;
    }

    public static BlockPos formationPosition(BlockPos chest, Direction facing, int index) {
        if (index == 0) return chest.relative(facing, 6);
        int soldier = index - 1;
        return chest.relative(facing, 9 + (soldier / COLUMNS.length) * 2)
                .relative(facing.getClockWise(), COLUMNS[soldier % COLUMNS.length]);
    }
}
