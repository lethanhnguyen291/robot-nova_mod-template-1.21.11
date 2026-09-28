package net.teogemini.nova.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.teogemini.nova.client.renderer.NovaRenderer;
import net.teogemini.nova.registry.ModEntities;
import net.teogemini.nova.client.screen.NovaControlScreen;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.teogemini.nova.registry.ModBlockEntities;
import net.teogemini.nova.client.renderer.NovaChestRenderer;


public class ROBOTNOVA_MODClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        net.minecraft.client.gui.screens.MenuScreens.register(
                net.teogemini.nova.screen.NovaMenus.INVENTORY,
                net.teogemini.nova.client.screen.NovaInventoryScreen::new);
        BlockEntityRenderers.register(
                ModBlockEntities.NOVA_CHEST,
                NovaChestRenderer::new
        );
        NovaControlScreen.registerInteraction();
        
        EntityRenderers.register(
                ModEntities.NOVA,
                NovaRenderer::new
        );
    }
}