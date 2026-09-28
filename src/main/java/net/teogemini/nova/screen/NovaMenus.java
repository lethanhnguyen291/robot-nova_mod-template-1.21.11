package net.teogemini.nova.screen;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.teogemini.nova.ROBOTNOVA_MOD;
import net.teogemini.nova.entity.NovaEntity;

public final class NovaMenus {
    public static final ExtendedScreenHandlerType<NovaInventoryMenu, Integer> INVENTORY =
            Registry.register(BuiltInRegistries.MENU, ROBOTNOVA_MOD.id("nova_inventory"),
                    new ExtendedScreenHandlerType<>(NovaInventoryMenu::new, ByteBufCodecs.VAR_INT));

    private NovaMenus() {}
    public static void register() {}

    public static void open(Player player, NovaEntity nova) {
        if (!(player instanceof ServerPlayer) || !NovaInventoryMenu.canAccess(nova, player)) return;
        player.openMenu(new ExtendedScreenHandlerFactory<Integer>() {
            @Override public Integer getScreenOpeningData(ServerPlayer viewer) { return nova.getId(); }
            @Override public Component getDisplayName() { return Component.literal("NOVA"); }
            @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player viewer) {
                return new NovaInventoryMenu(id, inventory, nova);
            }
        });
    }
}
