package net.teogemini.nova.registry;

import java.util.List;
import java.util.function.Function;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.teogemini.nova.ROBOTNOVA_MOD;
import net.teogemini.nova.block.NovaUnitBlock;
import net.teogemini.nova.item.NovaOmniPadItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;

/** New materials live separately from the user's tools, chest and control rod registries. */
public final class NovaMaterials {
    public static final TagKey<Block> ORES=TagKey.create(Registries.BLOCK,ROBOTNOVA_MOD.id("ores"));
    public static final Block SILICON_ORE=ore("silicon_ore",false,false);
    public static final Block DEEPSLATE_SILICON_ORE=ore("deepslate_silicon_ore",true,false);
    public static final Block ENERGY_CRYSTAL_ORE=ore("energy_crystal_ore",false,true);
    public static final Block DEEPSLATE_ENERGY_CRYSTAL_ORE=ore("deepslate_energy_crystal_ore",true,true);
    public static final Item RAW_SILICON=item("raw_silicon");
    public static final Item SILICON_WAFER=item("silicon_wafer");
    public static final Item ENERGY_CRYSTAL=item("energy_crystal");
    public static final Item NOVA_CIRCUIT=item("nova_circuit");
    public static final Item CONTROL_CHIP=item("control_chip");
    public static final Item NOVA_AI_PROCESSOR=item("nova_ai_processor");
    public static final Item NOVA_SCREEN=item("nova_screen");
    public static final Item NOVA_ENERGY_CORE=item("nova_energy_core");
    public static final Item NOVA_BATTERY=item("nova_battery");
    public static final Item SMALL_MOTOR=item("small_motor");
    public static final Item NOVA_OMNI_PAD=registerItem("nova_omni_pad",NovaOmniPadItem::new);
    public static final Block NOVA_FRAME_CASING=block("nova_frame_casing",Block::new);
    public static final Block NOVA_HEAD_UNIT=block("nova_head_unit",NovaUnitBlock::new);
    public static final Block NOVA_BODY_UNIT=block("nova_body_unit",NovaUnitBlock::new);
    public static final List<Item> COMPONENTS=List.of(RAW_SILICON,SILICON_WAFER,ENERGY_CRYSTAL,NOVA_CIRCUIT,
            CONTROL_CHIP,NOVA_AI_PROCESSOR,NOVA_SCREEN,NOVA_ENERGY_CORE,NOVA_BATTERY,SMALL_MOTOR);
    public static final List<Block> BLOCKS=List.of(SILICON_ORE,DEEPSLATE_SILICON_ORE,ENERGY_CRYSTAL_ORE,
            DEEPSLATE_ENERGY_CRYSTAL_ORE,NOVA_FRAME_CASING,NOVA_HEAD_UNIT,NOVA_BODY_UNIT);
    private NovaMaterials(){}
    private static Item item(String name){
        return registerItem(name,Item::new);
    }
    private static Item registerItem(String name,Function<Item.Properties,Item> factory){
        var key=ResourceKey.create(Registries.ITEM,ROBOTNOVA_MOD.id(name));
        return Registry.register(BuiltInRegistries.ITEM,key,factory.apply(new Item.Properties().setId(key)));
    }
    private static Block registerBlock(String name,Function<BlockBehaviour.Properties,Block> factory,BlockBehaviour.Properties properties){
        var id=ROBOTNOVA_MOD.id(name);var key=ResourceKey.create(Registries.BLOCK,id);
        Block block=Registry.register(BuiltInRegistries.BLOCK,key,factory.apply(properties.setId(key)));
        Registry.register(BuiltInRegistries.ITEM,id,new BlockItem(block,new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM,id)).useBlockDescriptionPrefix()));
        return block;
    }
    private static Block ore(String name,boolean deep,boolean energy){
        return registerBlock(name,p->new DropExperienceBlock(UniformInt.of(energy?1:0,energy?3:1),p),
                BlockBehaviour.Properties.of().mapColor(deep?MapColor.DEEPSLATE:MapColor.STONE)
                        .strength(deep?4.5F:3F,3F).sound(deep?SoundType.DEEPSLATE:SoundType.STONE)
                        .requiresCorrectToolForDrops().lightLevel(s->energy?5:0));
    }
    private static Block block(String name,Function<BlockBehaviour.Properties,Block> factory){
        return registerBlock(name,factory,BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ)
                .strength(3F,6F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    }
    public static void register(){
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,ROBOTNOVA_MOD.id("nova"),FabricItemGroup.builder()
                .title(Component.translatable("itemGroup.robot-nova_mod.nova"))
                .icon(()->new ItemStack(NOVA_OMNI_PAD))
                .displayItems((parameters,output)->{
                    output.accept(NOVA_OMNI_PAD);
                    output.accept(ModItems.NOVA_DORMANT);
                    for(Block b:BLOCKS)output.accept(b);
                    for(Item item:COMPONENTS)output.accept(item);
                    output.accept(ModBlocks.NOVA_CHEST_ITEM);output.accept(ModBlocks.NOVA_CONTROL_ROD_ITEM);
                    output.accept(ModItems.NOVA_AXE);output.accept(ModItems.NOVA_PICKAXE);
                    output.accept(ModItems.NOVA_SHOVEL);output.accept(ModItems.NOVA_HOE);output.accept(ModItems.NOVA_SWORD);
                    output.accept(ModItems.NOVA_SPAWN_EGG);
                }).build());
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(entries -> entries.accept(NOVA_OMNI_PAD));
    }
}
