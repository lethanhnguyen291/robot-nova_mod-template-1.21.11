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
            if (hand != InteractionHand.MAIN_HAND || player.isSpectator() || player.isShiftKeyDown()
                    || !level.getBlockState(hit.getBlockPos()).is(ModBlocks.NOVA_CONTROL_ROD)) {
                return InteractionResult.PASS;
            }
            return level instanceof ServerLevel server ? triggerRally(server, hit.getBlockPos(), player)
                    : InteractionResult.SUCCESS;
        });
    }

    public static void toggle(NovaEntity selected, Player owner) {
        if (!(selected.level() instanceof ServerLevel level) || !selected.isOwnedBy(owner)) return;
        BlockPos anchor = selected.work().rallyFace;
        if (anchor != null && level.hasChunkAt(anchor) && level.getBlockState(anchor).is(ModBlocks.NOVA_CONTROL_ROD)
                && owner.isWithinBlockInteractionRange(anchor, 0)) {
            triggerRally(level, anchor, owner);
        } else {
            NovaWorkSupport.message(owner, "Đặt gậy điều khiển xuống đất hoặc nhấp phải gậy đã đặt để tập hợp nhé.");
        }
    }

    private static final java.util.Map<Player, Long> LAST_USE = new java.util.WeakHashMap<>();

    public static InteractionResult triggerRally(ServerLevel level, BlockPos anchor, Player owner) {
        if (owner.isSpectator() || !owner.isAlive() || owner.level() != level || !level.hasChunkAt(anchor)
                || !owner.isWithinBlockInteractionRange(anchor, 0) || !level.mayInteract(owner, anchor)) {
            return InteractionResult.PASS;
        }
        var state = level.getBlockState(anchor);
        if (!state.is(ModBlocks.NOVA_CONTROL_ROD)) return InteractionResult.PASS;
        long now = level.getGameTime();
        if (now < LAST_USE.getOrDefault(owner, Long.MIN_VALUE)) return InteractionResult.SUCCESS;
        LAST_USE.put(owner, now + 10);
        var robots = level.getEntitiesOfClass(NovaEntity.class, new AABB(anchor).inflate(64.0),
                nova -> nova.isAlive() && nova.isOwnedBy(owner) && !nova.isNoAi());
        if (robots.isEmpty()) {
            NovaWorkSupport.message(owner, "Không có robot của bạn trong vùng 64 khối quanh gậy.");
            return InteractionResult.SUCCESS;
        }
        var stationed = robots.stream().filter(nova -> nova.isRallying() && anchor.equals(nova.work().rallyFace)).toList();
        if (!stationed.isEmpty()) {
            stationed.forEach(NovaEntity::stopRally);
            level.playSound(null, anchor, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 1.0F, 1.0F);
            NovaWorkSupport.message(owner, "Đội hình giải tán! Trở lại chế độ trước đó.");
            return InteractionResult.SUCCESS;
        }
        robots.sort(Comparator.comparingDouble(nova -> nova.distanceToSqr(Vec3.atCenterOf(anchor))));
        Direction facing = state.getValue(net.teogemini.nova.block.NovaControlRodBlock.FACING);
        for (int index = 0; index < robots.size(); index++) {
            robots.get(index).startRally(formationPosition(anchor, facing, index), anchor);
        }
        robots.getFirst().work().showRallyLeader(robots.getFirst(), robots.size());
        level.playSound(null, anchor, SoundEvents.RAID_HORN.value(), SoundSource.BLOCKS, 5.0F, 1.0F);
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
