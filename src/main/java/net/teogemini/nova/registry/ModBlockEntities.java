package net.teogemini.nova.registry;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.teogemini.nova.ROBOTNOVA_MOD;
import net.teogemini.nova.block.entity.NovaChestBlockEntity;

public final class ModBlockEntities {
    public static final BlockEntityType<NovaChestBlockEntity> NOVA_CHEST =
            Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    ROBOTNOVA_MOD.id("nova_chest"),
                    FabricBlockEntityTypeBuilder.create(
                            NovaChestBlockEntity::new, ModBlocks.NOVA_CHEST).build());

    public static void register() {
    }

    private ModBlockEntities() {
    }
}
