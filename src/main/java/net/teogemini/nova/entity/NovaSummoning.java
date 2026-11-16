package net.teogemini.nova.entity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.teogemini.nova.block.NovaUnitBlock;
import net.teogemini.nova.block.entity.NovaChestBlockEntity;
import net.teogemini.nova.registry.ModBlocks;
import net.teogemini.nova.registry.ModEntities;
import net.teogemini.nova.registry.NovaMaterials;

/** The three-block column becomes a linked, empty NOVA chest with a dormant robot on top. */
public final class NovaSummoning {
    private NovaSummoning() {}

    public static NovaAssemblyPattern.Assembly find(LevelReader level, BlockPos head) {
        if (level.isOutsideBuildHeight(head) || !level.hasChunkAt(head)) return null;
        BlockState state = level.getBlockState(head);
        if (!state.is(NovaMaterials.NOVA_HEAD_UNIT)) return null;
        return NovaAssemblyPattern.find(head, state.getValue(NovaUnitBlock.FACING), pos ->
                level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)
                || !level.getWorldBorder().isWithinBounds(new AABB(pos)) ? null : level.getBlockState(pos));
    }
    // Source compatibility for the old, unregistered pumpkin mixin.
    public static boolean matchesBase(LevelReader level, BlockPos head) { return find(level, head) != null; }
    public static boolean matches(LevelReader level, BlockPos head) { return find(level, head) != null; }

    public static boolean summon(ServerLevel level, BlockPos head) {
        var assembly = find(level, head);
        if (assembly == null) return false;
        BlockPos chestPos = assembly.chest();
        BlockState chestState = ModBlocks.NOVA_CHEST.defaultBlockState()
                .setValue(ChestBlock.FACING, assembly.facing()).setValue(ChestBlock.TYPE, ChestType.SINGLE);
        double chestTop = chestState.getCollisionShape(level, chestPos).max(Direction.Axis.Y);
        NovaEntity nova = ModEntities.NOVA.create(level, EntitySpawnReason.TRIGGERED);
        if (nova == null) return false;
        float yaw = assembly.facing().toYRot();
        nova.snapTo(chestPos.getX() + .5, chestPos.getY() + chestTop, chestPos.getZ() + .5, yaw, 0);
        nova.setYRot(yaw); nova.yRotO = yaw; nova.setYHeadRot(yaw); nova.yHeadRotO = yaw;
        nova.setYBodyRot(yaw); nova.yBodyRotO = yaw;
        nova.finalizeSpawn(level, level.getCurrentDifficultyAt(chestPos), EntitySpawnReason.TRIGGERED, null);
        nova.setNoAi(false); nova.setDormant(true); nova.setDormantPose(0);
        nova.work().setEnergy(0); nova.setPersistenceRequired();
        nova.setLinkedChest(chestPos);
        if (!fits(level, nova, new HashSet<>(assembly.consumed()))) { nova.discard(); return false; }

        List<BlockPos> positions = assembly.consumed();
        BlockState[] previous = positions.stream().map(level::getBlockState).toArray(BlockState[]::new);
        if (!level.addFreshEntity(nova)) { nova.discard(); return false; }
        for (int i = 0; i < previous.length; i++) if (!level.getBlockState(positions.get(i)).equals(previous[i])) {
            nova.discard(); return false;
        }

        // Install the actual chest first. Failed conversion restores only the three assembly blocks.
        if (!replace(level, chestPos, chestState)
                || !(level.getBlockEntity(chestPos) instanceof NovaChestBlockEntity)
                || !replace(level, assembly.feet(), Blocks.AIR.defaultBlockState())
                || !replace(level, head, Blocks.AIR.defaultBlockState())) {
            nova.discard();
            restore(level, positions, previous);
            return false;
        }
        nova.setDeltaMovement(0, 0, 0);
        nova.setOnGround(true);
        for (int i = 0; i < previous.length; i++) {
            BlockPos pos = positions.get(i);
            level.levelEvent(2001, pos, Block.getId(previous[i]));
            level.updateNeighborsAt(pos, previous[i].getBlock());
        }
        level.gameEvent(nova, GameEvent.BLOCK_PLACE, chestPos);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, nova.getX(), nova.getY() + .7, nova.getZ(), 24, .35, .5, .35, .02);
        nova.sounds().play(net.teogemini.nova.sound.NovaSounds.Cue.DORMANT_PLACE);
        level.gameEvent(nova, GameEvent.ENTITY_PLACE, nova.blockPosition());
        return true;
    }

    private static boolean replace(ServerLevel level, BlockPos pos, BlockState state) {
        return level.setBlock(pos, state, Block.UPDATE_CLIENTS) && level.getBlockState(pos).equals(state);
    }
    private static void restore(ServerLevel level, List<BlockPos> positions, BlockState[] previous) {
        // Bottom first, head last, retaining the original facing and block states.
        for (int i = positions.size() - 1; i >= 0; i--) {
            BlockPos pos = positions.get(i);
            if (!level.getBlockState(pos).equals(previous[i])) level.setBlock(pos, previous[i], Block.UPDATE_CLIENTS);
        }
        for (int i = 0; i < previous.length; i++) level.updateNeighborsAt(positions.get(i), previous[i].getBlock());
    }
    private static boolean fits(ServerLevel level, NovaEntity nova, Set<BlockPos> consumed) {
        AABB box = nova.getBoundingBox();
        if (!level.getWorldBorder().isWithinBounds(box) || !level.isUnobstructed(nova)) return false;
        for (BlockPos pos : BlockPos.betweenClosed(Mth.floor(box.minX), Mth.floor(box.minY), Mth.floor(box.minZ),
                Mth.floor(box.maxX - 1e-7), Mth.floor(box.maxY - 1e-7), Mth.floor(box.maxZ - 1e-7))) {
            if (consumed.contains(pos)) continue;
            if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)) return false;
            var shape = level.getBlockState(pos).getCollisionShape(level, pos, CollisionContext.of(nova));
            if (Shapes.joinIsNotEmpty(Shapes.create(box), shape.move(pos.getX(), pos.getY(), pos.getZ()), BooleanOp.AND)) return false;
        }
        return true;
    }
}
