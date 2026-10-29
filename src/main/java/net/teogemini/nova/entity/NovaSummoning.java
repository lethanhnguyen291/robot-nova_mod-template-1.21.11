package net.teogemini.nova.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.teogemini.nova.registry.ModEntities;

/** Iron body, redstone arms, lapis backpack and a carved pumpkin head placed last. */
public final class NovaSummoning {
    private NovaSummoning() {}

    public static boolean matchesBase(LevelReader level, BlockPos head) {
        for (Direction front : Direction.Plane.HORIZONTAL) {
            if (matchesBase(level, head, front)) return true;
        }
        return false;
    }

    private static BlockPos[] structurePositions(BlockPos head, Direction front) {
        BlockPos body = head.below();
        return new BlockPos[] {head, body, body.relative(front.getClockWise()),
                body.relative(front.getCounterClockWise()), body.relative(front.getOpposite())};
    }

    private static boolean matchesBase(LevelReader level, BlockPos head, Direction front) {
        BlockPos[] positions = structurePositions(head, front);
        for (BlockPos pos : positions) {
            if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)) return false;
        }
        return level.getBlockState(positions[1]).is(Blocks.IRON_BLOCK)
                && level.getBlockState(positions[2]).is(Blocks.REDSTONE_BLOCK)
                && level.getBlockState(positions[3]).is(Blocks.REDSTONE_BLOCK)
                && level.getBlockState(positions[4]).is(Blocks.LAPIS_BLOCK);
    }

    public static boolean matches(LevelReader level, BlockPos head) {
        if (level.isOutsideBuildHeight(head) || !level.hasChunkAt(head)) return false;
        BlockState state = level.getBlockState(head);
        return state.is(Blocks.CARVED_PUMPKIN)
                && matchesBase(level, head, state.getValue(CarvedPumpkinBlock.FACING));
    }

    public static boolean summon(ServerLevel level, BlockPos head) {
        if (!matches(level, head)) return false;

        BlockPos base = head.below();
        BlockState headState = level.getBlockState(head);
        Direction front = headState.getValue(CarvedPumpkinBlock.FACING);
        BlockPos[] positions = structurePositions(head, front);
        NovaEntity nova = ModEntities.NOVA.create(level, EntitySpawnReason.TRIGGERED);
        if (nova == null) return false;

        float yaw = front.toYRot();
        nova.snapTo(base.getX() + 0.5, base.getY() + 0.05, base.getZ() + 0.5, yaw, 0.0F);
        nova.setYHeadRot(yaw);
        nova.setYBodyRot(yaw);
        nova.finalizeSpawn(level, level.getCurrentDifficultyAt(base), EntitySpawnReason.TRIGGERED, null);
        nova.setPersistenceRequired();

        // Admission must succeed before consuming materials. No item drops are produced.
        if (!level.addFreshEntity(nova)) return false;

        BlockState[] consumed = new BlockState[positions.length];
        for (int i = 0; i < positions.length; i++) consumed[i] = level.getBlockState(positions[i]);
        for (int i = 0; i < positions.length; i++) {
            BlockPos pos = positions[i];
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            level.levelEvent(2001, pos, Block.getId(consumed[i]));
        }
        // Notify after removing the complete structure, including both redstone sources.
        for (int i = 0; i < positions.length; i++) {
            level.updateNeighborsAt(positions[i], consumed[i].getBlock());
        }
        level.sendParticles(ParticleTypes.END_ROD, base.getX() + 0.5, base.getY() + 0.8,
                base.getZ() + 0.5, 24, 0.35, 0.5, 0.35, 0.02);
        level.gameEvent(nova, GameEvent.ENTITY_PLACE, base);
        return true;
    }
}
