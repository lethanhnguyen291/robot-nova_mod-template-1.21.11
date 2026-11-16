package net.teogemini.nova.entity;

import java.util.List;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.teogemini.nova.registry.NovaMaterials;

/** A vertical column: casing, body, head. Only these three blocks are consumed. */
public final class NovaAssemblyPattern {
    public record Assembly(Direction facing, BlockPos feet, List<BlockPos> consumed) {
        public Assembly { feet = feet.immutable(); consumed = List.copyOf(consumed); }
        public BlockPos chest() { return feet.below(); }
    }
    private NovaAssemblyPattern() {}

    public static Assembly find(BlockPos head, Direction facing, Function<BlockPos, BlockState> read) {
        if (!facing.getAxis().isHorizontal()
                || !is(read, head, NovaMaterials.NOVA_HEAD_UNIT)
                || !is(read, head.below(), NovaMaterials.NOVA_BODY_UNIT)
                || !is(read, head.below(2), NovaMaterials.NOVA_FRAME_CASING)) return null;
        return new Assembly(facing, head.below(), List.of(head, head.below(), head.below(2)));
    }
    private static boolean is(Function<BlockPos, BlockState> read, BlockPos pos, Block block) {
        BlockState state = read.apply(pos);
        return state != null && state.is(block);
    }
}
