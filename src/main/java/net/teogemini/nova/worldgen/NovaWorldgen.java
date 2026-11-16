package net.teogemini.nova.worldgen;

import net.fabricmc.fabric.api.biome.v1.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.teogemini.nova.ROBOTNOVA_MOD;

/** Configurations and densities are data-pack JSON; all custom work is bounded to generation time. */
public final class NovaWorldgen {
    private NovaWorldgen(){}
    public static void register(){
        register("nova_ore",NovaOreFeature.Mode.NORMAL);
        register("nova_cave_ore",NovaOreFeature.Mode.CAVE);
        register("nova_ore_vein",NovaOreFeature.Mode.VEIN);
        for(String name:new String[]{"silicon_ore","energy_crystal_ore","energy_crystal_deep",
                "silicon_cave","energy_crystal_cave","silicon_vein","energy_crystal_vein"})
            BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),GenerationStep.Decoration.UNDERGROUND_ORES,
                    ResourceKey.create(Registries.PLACED_FEATURE,ROBOTNOVA_MOD.id(name)));
    }
    private static Feature<OreConfiguration> register(String name,NovaOreFeature.Mode mode){
        return Registry.register(BuiltInRegistries.FEATURE,ROBOTNOVA_MOD.id(name),new NovaOreFeature(mode));
    }
}
