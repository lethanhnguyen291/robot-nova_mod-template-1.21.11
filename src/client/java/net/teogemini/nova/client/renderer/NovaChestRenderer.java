package net.teogemini.nova.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.teogemini.nova.ROBOTNOVA_MOD;
import net.teogemini.nova.block.entity.NovaChestBlockEntity;

public final class NovaChestRenderer extends ChestRenderer<NovaChestBlockEntity> {
    private static final Material SINGLE =
            Sheets.CHEST_MAPPER.apply(ROBOTNOVA_MOD.id("nova_chest"));
    private static final Material LEFT =
            Sheets.CHEST_MAPPER.apply(ROBOTNOVA_MOD.id("nova_chest_left"));
    private static final Material RIGHT =
            Sheets.CHEST_MAPPER.apply(ROBOTNOVA_MOD.id("nova_chest_right"));

    private final MaterialSet novaMaterials;
    private final ChestModel single;
    private final ChestModel left;
    private final ChestModel right;

    public NovaChestRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
        novaMaterials = context.materials();
        single = new ChestModel(context.bakeLayer(ModelLayers.CHEST));
        left = new ChestModel(context.bakeLayer(ModelLayers.DOUBLE_CHEST_LEFT));
        right = new ChestModel(context.bakeLayer(ModelLayers.DOUBLE_CHEST_RIGHT));
    }

    @Override
    public void submit(ChestRenderState state, PoseStack poses,
                       SubmitNodeCollector collector, CameraRenderState camera) {
        ChestModel model = switch (state.type) {
            case LEFT -> left;
            case RIGHT -> right;
            default -> single;
        };
        Material material = switch (state.type) {
            case LEFT -> LEFT;
            case RIGHT -> RIGHT;
            default -> SINGLE;
        };

        float closed = 1.0F - state.open;
        float openness = 1.0F - closed * closed * closed;

        poses.pushPose();
        poses.translate(0.5F, 0.5F, 0.5F);
        poses.mulPose(Axis.YP.rotationDegrees(-state.angle));
        poses.translate(-0.5F, -0.5F, -0.5F);

        collector.submitModel(model, openness, poses,
                material.renderType(RenderTypes::entityCutout),
                state.lightCoords, OverlayTexture.NO_OVERLAY, -1,
                novaMaterials.get(material), 0, state.breakProgress);

        poses.popPose();
    }
}
