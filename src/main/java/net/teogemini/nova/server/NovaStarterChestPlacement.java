package net.teogemini.nova.server;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.teogemini.nova.block.entity.NovaChestBlockEntity;
import net.teogemini.nova.registry.ModBlocks;

/** Uses vanilla bonus-chest heightmap/column search, with a bounded neighboring-chunk fallback. */
public final class NovaStarterChestPlacement {
    private NovaStarterChestPlacement() {}

    public static Optional<BlockPos> generate(ServerLevel level, BlockPos origin, boolean generateChunks) {
        RandomSource random = RandomSource.create(level.getSeed() ^ origin.asLong() ^ 0x4e4f56414cL);
        ChunkPos center = new ChunkPos(origin);
        for (int ring = 0; ring <= 1; ring++) for (int dx = -ring; dx <= ring; dx++) for (int dz = -ring; dz <= ring; dz++) {
            if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
            ChunkPos chunk = new ChunkPos(center.x + dx, center.z + dz);
            if (!level.hasChunk(chunk.x, chunk.z)) {
                if (!generateChunks) continue;
                level.getChunk(chunk.x, chunk.z);
            }
            int[] xs = shuffledColumns(chunk.getMinBlockX(), random);
            int[] zs = shuffledColumns(chunk.getMinBlockZ(), random);
            for (int x : xs) for (int z : zs) {
                BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
                if (safeLocation(level, pos) && place(level, pos, random)) return Optional.of(pos);
            }
        }
        return Optional.empty();
    }

    private static int[] shuffledColumns(int start, RandomSource random) {
        int[] values = new int[16];
        for (int i = 0; i < values.length; i++) values[i] = start + i;
        for (int i = values.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1), value = values[i]; values[i] = values[j]; values[j] = value;
        }
        return values;
    }
    private static boolean clearForPlacement(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getFluidState().isEmpty() && !state.hasBlockEntity()
                && (state.isAir() || state.canBeReplaced() && state.getCollisionShape(level, pos).isEmpty());
    }
    private static boolean safeLocation(ServerLevel level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos.below()) || level.isOutsideBuildHeight(pos.above())
                || !level.getWorldBorder().isWithinBounds(new AABB(pos))) return false;
        if (!clearForPlacement(level, pos) || !clearForPlacement(level, pos.above())) return false;
        BlockState floor = level.getBlockState(pos.below());
        if (!floor.getFluidState().isEmpty() || !floor.isFaceSturdy(level, pos.below(), Direction.UP)
                || floor.is(BlockTags.LOGS) || floor.is(BlockTags.LEAVES)
                || floor.is(Blocks.MAGMA_BLOCK) || floor.is(Blocks.CACTUS)) return false;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos neighbor = pos.relative(side);
            if (!level.hasChunkAt(neighbor)) return false;
            if (level.getBlockState(neighbor).getBlock() instanceof ChestBlock) return false;
        }
        return level.getEntities(null, new AABB(pos).expandTowards(0, 1, 0)).isEmpty();
    }
    private static boolean place(ServerLevel level, BlockPos pos, RandomSource random) {
        BlockState old = level.getBlockState(pos);
        BlockState chestState = ModBlocks.NOVA_CHEST.defaultBlockState()
                .setValue(ChestBlock.TYPE, ChestType.SINGLE)
                .setValue(ChestBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(random));
        if (!level.setBlock(pos, chestState, Block.UPDATE_ALL)) return false;
        if (!(level.getBlockEntity(pos) instanceof NovaChestBlockEntity chest)) {
            if (level.getBlockState(pos).is(ModBlocks.NOVA_CHEST)) level.setBlock(pos, old, Block.UPDATE_ALL);
            return false;
        }
        ItemStack name = new ItemStack(ModBlocks.NOVA_CHEST_ITEM);
        name.set(DataComponents.CUSTOM_NAME, Component.translatable("container.robot-nova_mod.world_starter_kit"));
        chest.applyComponentsFromItemStack(name);
        List<ItemStack> items = NovaStarterKit.contents();
        int[] slots = {10, 11, 12, 13, 14, 19, 20, 21, 22, 23};
        for (int i = 0; i < items.size(); i++) chest.setItem(slots[i], items.get(i));
        chest.setChanged();
        BlockState torch = Blocks.TORCH.defaultBlockState();
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos target = pos.relative(side);
            if (level.hasChunkAt(target) && level.getWorldBorder().isWithinBounds(target)
                    && clearForPlacement(level, target) && torch.canSurvive(level, target)) {
                level.setBlock(target, torch, Block.UPDATE_ALL);
            }
        }
        return true;
    }
}
