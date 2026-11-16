package net.teogemini.nova.server;

import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.teogemini.nova.ROBOTNOVA_MOD;
import net.teogemini.nova.registry.ModBlocks;
import net.teogemini.nova.registry.ModItems;
import net.teogemini.nova.registry.NovaMaterials;

/** One shared bonus chest, created with the world's initial spawn rather than after a player moves. */
public final class NovaStarterKit {
    private NovaStarterKit() {}

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ServerLevel level = server.overworld();
            NovaStarterRewardsData data = level.getDataStorage().get(NovaStarterRewardsData.TYPE);
            if (data == null) return; // Existing worlds without an outstanding gift are left alone.
            data.migrateLegacy(level.getRespawnData().pos());
            if (data.worldPending()) generate(level, data, true);
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerLevel level = server.overworld();
            NovaStarterRewardsData data = level.getDataStorage().get(NovaStarterRewardsData.TYPE);
            if (data == null) return;
            if (data.worldPending()) generate(level, data, false);
            if (!data.announced() && data.worldChest().isPresent()) {
                BlockPos pos = data.worldChest().get();
                handler.getPlayer().sendSystemMessage(Component.translatable("message.robot-nova_mod.starter_kit",
                        pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.AQUA));
                data.markAnnounced();
            }
        });
    }

    /** Called at the end of vanilla setInitialSpawn, before the first player is admitted. */
    public static void onInitialSpawn(ServerLevel level, BlockPos origin) {
        if (!level.dimension().equals(Level.OVERWORLD)) return;
        NovaStarterRewardsData data = NovaStarterRewardsData.get(level.getServer());
        data.beginWorldGift(origin);
        if (data.worldPending()) generate(level, data, true);
    }

    private static void generate(ServerLevel level, NovaStarterRewardsData data, boolean generateChunks) {
        if (!data.worldPending() || data.origin().isEmpty()) return;
        Optional<BlockPos> position = NovaStarterChestPlacement.generate(level, data.origin().get(), generateChunks);
        position.ifPresent(pos -> {
            data.completeWorldGift(pos);
            ROBOTNOVA_MOD.LOGGER.info("NOVA starter chest generated near world spawn at {}", pos);
        });
    }

    public static List<ItemStack> contents() {
        return List.of(new ItemStack(NovaMaterials.NOVA_OMNI_PAD),
                new ItemStack(ModBlocks.NOVA_CONTROL_ROD_ITEM), new ItemStack(ModItems.NOVA_DORMANT),
                new ItemStack(Items.REDSTONE_BLOCK), new ItemStack(Items.REDSTONE, 20),
                new ItemStack(ModItems.NOVA_AXE), new ItemStack(ModItems.NOVA_PICKAXE),
                new ItemStack(ModItems.NOVA_SHOVEL), new ItemStack(ModItems.NOVA_HOE),
                new ItemStack(ModItems.NOVA_SWORD));
    }
}
