package net.teogemini.nova.registry;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.teogemini.nova.ROBOTNOVA_MOD;
import net.teogemini.nova.block.NovaChestBlock;

public final class ModBlocks {
    private static final ResourceKey<Block> CHEST_KEY =
            ResourceKey.create(Registries.BLOCK, ROBOTNOVA_MOD.id("nova_chest"));

    public static final NovaChestBlock NOVA_CHEST = Registry.register(
            BuiltInRegistries.BLOCK, CHEST_KEY,
            new NovaChestBlock(BlockBehaviour.Properties.of()
                    .setId(CHEST_KEY)
                    .mapColor(MapColor.QUARTZ)
                    .strength(2.5F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final Item NOVA_CHEST_ITEM = Registry.register(
            BuiltInRegistries.ITEM, ROBOTNOVA_MOD.id("nova_chest"),
            new BlockItem(NOVA_CHEST, new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM,
                            ROBOTNOVA_MOD.id("nova_chest")))
                    .useBlockDescriptionPrefix()));

    public static void register() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
                .register(entries -> entries.accept(NOVA_CHEST_ITEM));
    }

    private ModBlocks() {
    }
}
