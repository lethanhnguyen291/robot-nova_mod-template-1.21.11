package net.teogemini.nova.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.teogemini.nova.block.entity.NovaChestBlockEntity;
import net.teogemini.nova.registry.ModBlockEntities;

public final class NovaChestBlock extends ChestBlock {
    public static final MapCodec<NovaChestBlock> CODEC = simpleCodec(NovaChestBlock::new);

    public NovaChestBlock(BlockBehaviour.Properties properties) {
        super(() -> ModBlockEntities.NOVA_CHEST,
                SoundEvents.IRON_TRAPDOOR_OPEN,
                SoundEvents.IRON_TRAPDOOR_CLOSE,
                properties);
    }

    @Override
    public MapCodec<? extends ChestBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NovaChestBlockEntity(pos, state);
    }
}
