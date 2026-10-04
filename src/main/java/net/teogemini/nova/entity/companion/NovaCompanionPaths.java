package net.teogemini.nova.entity.companion;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.teogemini.nova.entity.NovaEntity;

public final class NovaCompanionPaths {
    private NovaCompanionPaths() {}
    public static boolean safe(NovaEntity nova, BlockPos pos) {
        var level = nova.level();
        if (!level.hasChunkAt(pos) || !level.hasChunkAt(pos.below()) || !level.isInWorldBounds(pos)
                || !level.getWorldBorder().isWithinBounds(pos)) return false;
        for (int y = -1; y <= 1; y++) {
            var p = pos.above(y);
            var state = level.getBlockState(p);
            if (!level.getFluidState(p).isEmpty() || state.is(BlockTags.FIRE)
                    || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.CACTUS)
                    || state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)
                    || state.is(Blocks.SWEET_BERRY_BUSH)) return false;
        }
        if (level.getBlockState(pos.below()).getCollisionShape(level, pos.below()).isEmpty()) return false;
        Vec3 target = Vec3.atBottomCenterOf(pos);
        return level.noCollision(nova, nova.getBoundingBox().move(target.subtract(nova.position())));
    }
    public static Path path(NovaEntity nova, BlockPos pos) {
        if (!safe(nova, pos)) return null;
        Path path = nova.getNavigation().createPath(pos, 0);
        if (path == null || !path.canReach()) return null;
        for (int i = 0; i < path.getNodeCount(); i++) {
            var node = path.getNode(i);
            if (!safe(nova, new BlockPos(node.x, node.y, node.z))) return null;
            if (i > 0 && Math.abs(node.y - path.getNode(i - 1).y) > 1) return null;
        }
        return path;
    }
    public static BlockPos beside(NovaEntity nova, BlockPos anchor, boolean shelter) {
        // At most eight route searches per call; all destinations stay in loaded chunks.
        int[][] offsets = {{1,0},{-1,0},{0,1},{0,-1},{2,0},{-2,0},{0,2},{0,-2}};
        for (int[] offset : offsets) {
            BlockPos pos = anchor.offset(offset[0], 0, offset[1]);
            if ((!shelter || !nova.level().canSeeSky(pos)) && path(nova, pos) != null) return pos;
        }
        return null;
    }
}
