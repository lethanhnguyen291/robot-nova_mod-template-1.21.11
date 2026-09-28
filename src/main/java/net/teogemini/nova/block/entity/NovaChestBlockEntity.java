package net.teogemini.nova.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.teogemini.nova.registry.ModBlockEntities;

public final class NovaChestBlockEntity extends ChestBlockEntity {
    public NovaChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NOVA_CHEST, pos, state);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.robot-nova_mod.nova_chest");
    }
}
