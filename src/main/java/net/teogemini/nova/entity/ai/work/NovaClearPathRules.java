package net.teogemini.nova.entity.ai.work;

import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Deliberately excludes containers, crops, logs/buildings and the floor below NOVA. */
public final class NovaClearPathRules {
    public enum Material { NONE, LEAVES, EARTH, ROCK, WEB }
    private NovaClearPathRules() {}

    public static Material material(BlockState state) {
        if (state.hasBlockEntity() || !state.getFluidState().isEmpty()) return Material.NONE;
        if (state.getBlock() instanceof LeavesBlock) return Material.LEAVES;
        if (state.is(Blocks.COBWEB)) return Material.WEB;
        if (state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.ROOTED_DIRT) || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.SAND) || state.is(Blocks.RED_SAND) || state.is(Blocks.GRAVEL)
                || state.is(Blocks.CLAY) || state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK)) return Material.EARTH;
        if (state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE) || state.is(Blocks.GRANITE)
                || state.is(Blocks.DIORITE) || state.is(Blocks.ANDESITE) || state.is(Blocks.TUFF)
                || state.is(Blocks.CALCITE) || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.COBBLED_DEEPSLATE) || state.is(Blocks.NETHERRACK)
                || state.is(Blocks.BASALT) || state.is(Blocks.BLACKSTONE) || state.is(Blocks.END_STONE)
                || state.is(BlockTags.COAL_ORES) || state.is(BlockTags.IRON_ORES)
                || state.is(BlockTags.COPPER_ORES) || state.is(BlockTags.GOLD_ORES)
                || state.is(BlockTags.REDSTONE_ORES) || state.is(BlockTags.LAPIS_ORES)
                || state.is(BlockTags.DIAMOND_ORES) || state.is(BlockTags.EMERALD_ORES)
                || state.is(Blocks.NETHER_QUARTZ_ORE)) return Material.ROCK;
        return Material.NONE;
    }

    public static int score(ItemStack tool, BlockState state) {
        if (tool.isEmpty() || tool.isDamageableItem() && tool.getMaxDamage() - tool.getDamageValue() <= 5
                || state.requiresCorrectToolForDrops() && !tool.isCorrectToolForDrops(state)) return 0;
        return switch (material(state)) {
            case LEAVES -> tool.is(ItemTags.AXES) ? 30 : tool.is(ItemTags.HOES) ? 20 : tool.is(ItemTags.SWORDS) ? 10 : 0;
            case EARTH -> tool.is(ItemTags.SHOVELS) ? 30 : 0;
            case ROCK -> tool.is(ItemTags.PICKAXES) ? 30 : 0;
            case WEB -> tool.is(ItemTags.SWORDS) ? 30 : 0;
            default -> 0;
        };
    }

    public static int chooseTool(NovaToolBelt belt, BlockState state) {
        int best = -1, bestScore = 0;
        for (int i = 0; i < 5; i++) {
            int score = score(belt.logical(i), state);
            if (score > bestScore) { best = i; bestScore = score; }
        }
        return best;
    }

    public static boolean hazardous(BlockState state) {
        return !state.getFluidState().isEmpty() || state.is(Blocks.MAGMA_BLOCK)
                || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE) || state.is(Blocks.CACTUS)
                || state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)
                || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.POWDER_SNOW);
    }

    /** A null read means an unloaded or out-of-bounds cell; never infer that it is air. */
    public static boolean safeNeighborhood(BlockPos pos, int feet,
            Function<BlockPos, BlockState> read, BiPredicate<BlockPos, BlockState> sturdy) {
        if (pos.getY() < feet || pos.getY() > feet + 2) return false;
        BlockPos floor = new BlockPos(pos.getX(), feet - 1, pos.getZ());
        BlockState support = read.apply(floor);
        if (support == null || hazardous(support) || !sturdy.test(floor, support)) return false;
        for (Direction direction : Direction.values()) {
            BlockState neighbor = read.apply(pos.relative(direction));
            if (neighbor == null || hazardous(neighbor)
                    || direction == Direction.UP && neighbor.getBlock() instanceof FallingBlock) return false;
        }
        return true;
    }

    public static BlockPos firstBodyObstacle(Vec3 from, float width, float height, BlockPos destination,
            Function<BlockPos, Boolean> collision, Predicate<BlockPos> reachable) {
        Vec3 delta = Vec3.atCenterOf(destination).subtract(from).multiply(1, 0, 1);
        double length = delta.length();
        if (length <= .1) return null;
        Vec3 direction = delta.scale(1 / length);
        double half = Math.max(.1, Math.min(width, 1) * .5 - .02);
        int foot = Mth.floor(from.y + .05), head = Mth.floor(from.y + Math.min(height, 2) - .01);
        for (double step = .25; step <= Math.min(2.5, length); step += .25) {
            Vec3 center = from.add(direction.scale(step));
            for (int y = head; y >= foot; y--)
                for (int x = Mth.floor(center.x - half); x <= Mth.floor(center.x + half); x++)
                    for (int z = Mth.floor(center.z - half); z <= Mth.floor(center.z + half); z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        Boolean solid = collision.apply(pos);
                        if (solid == null) return null;
                        if (solid && reachable.test(pos)) return pos;
                    }
        }
        return null;
    }
}
