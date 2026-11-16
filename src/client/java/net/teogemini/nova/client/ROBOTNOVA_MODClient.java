package net.teogemini.nova.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.teogemini.nova.client.renderer.NovaRenderer;
import net.teogemini.nova.registry.ModEntities;
import net.teogemini.nova.client.screen.NovaControlScreen;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.teogemini.nova.registry.ModBlockEntities;
import net.teogemini.nova.client.renderer.NovaChestRenderer;
import net.teogemini.nova.client.renderer.NovaDormantItemRenderer;
import net.teogemini.nova.item.NovaDormantItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.renderer.GeoItemRenderer;

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
        net.teogemini.nova.client.screen.NovaConstructionScreen.register();
        net.teogemini.nova.client.screen.NovaChatScreen.register();
        net.teogemini.nova.client.screen.NovaCompanionScreen.register();
        net.teogemini.nova.client.screen.NovaOmniPadScreen.register();
        
        EntityRenderers.register(
                ModEntities.NOVA,
                NovaRenderer::new
        );

        NovaDormantItem.RENDER_PROVIDER = consumer -> consumer.accept(new GeoRenderProvider() {
            private NovaDormantItemRenderer renderer;

            @Override
            public GeoItemRenderer<?> getGeoItemRenderer() {
                if (this.renderer == null) {
                    this.renderer = new NovaDormantItemRenderer();
                }
                return this.renderer;
            }
        });
    }
}
