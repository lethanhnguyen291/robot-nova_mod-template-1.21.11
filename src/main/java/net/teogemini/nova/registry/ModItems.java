package net.teogemini.nova.registry;

import java.util.function.Function;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.ToolMaterial;
import net.teogemini.nova.ROBOTNOVA_MOD;

public final class ModItems {
    private static final ToolMaterial NOVA_MATERIAL = ToolMaterial.COPPER;

    public static final Item NOVA_AXE = register("nova_axe",
            properties -> new AxeItem(NOVA_MATERIAL, 7.0F, -3.2F, properties));

    public static final Item NOVA_HOE = register("nova_hoe",
            properties -> new HoeItem(NOVA_MATERIAL, -1.0F, -2.0F, properties));

    public static final Item NOVA_PICKAXE = register("nova_pickaxe",
            properties -> new Item(
                    properties.pickaxe(NOVA_MATERIAL, 1.0F, -2.8F)));

    public static final Item NOVA_SHOVEL = register("nova_shovel",
            properties -> new ShovelItem(NOVA_MATERIAL, 1.5F, -3.0F, properties));

    public static final Item NOVA_SWORD = register("nova_sword",
            properties -> new Item(
                    properties.sword(NOVA_MATERIAL, 3.0F, -2.4F)));

    private static Item register(String name, Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(
                Registries.ITEM,
                ROBOTNOVA_MOD.id(name)
        );

        Item item = factory.apply(new Item.Properties().setId(key));

        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    public static void register() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(entries -> {
                    entries.accept(NOVA_AXE);
                    entries.accept(NOVA_HOE);
                    entries.accept(NOVA_PICKAXE);
                    entries.accept(NOVA_SHOVEL);
                });

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.COMBAT)
                .register(entries -> {
                    entries.accept(NOVA_SWORD);
                    entries.accept(NOVA_AXE);
                });
    }

    private ModItems() {
    }
}