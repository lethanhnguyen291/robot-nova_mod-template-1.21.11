package net.teogemini.nova.entity;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class NovaExperienceManager {
    private NovaExperienceManager() {}

    // Pass the block state captured BEFORE breaking the block.
    public static void awardMiningExp(NovaEntity nova, BlockState originalState) {
        int amount = originalState.is(Blocks.ANCIENT_DEBRIS) ? 10
                : originalState.is(BlockTags.DIAMOND_ORES)
                    || originalState.is(BlockTags.EMERALD_ORES) ? 8
                : originalState.is(BlockTags.GOLD_ORES)
                    || originalState.is(BlockTags.REDSTONE_ORES)
                    || originalState.is(BlockTags.LAPIS_ORES) ? 4
                : originalState.is(BlockTags.IRON_ORES)
                    || originalState.is(BlockTags.COPPER_ORES) ? 2
                : originalState.is(BlockTags.COAL_ORES) ? 1 : 0;
        nova.addExperience(amount);
    }

    public static void awardFarmerExp(NovaEntity nova, boolean harvesting) {
        nova.addExperience(1);
    }

    public static void awardLumberjackExp(NovaEntity nova) {
        nova.addExperience(1);
    }

    public static void awardCombatExp(NovaEntity nova) {
        nova.addExperience(3);
    }
}
