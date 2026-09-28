package net.teogemini.nova.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.teogemini.nova.ROBOTNOVA_MOD;
import net.teogemini.nova.entity.NovaEntity;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.model.GeoBone;
import software.bernie.geckolib.cache.model.cuboid.CuboidGeoBone;
import software.bernie.geckolib.constant.dataticket.DataTicket;
import software.bernie.geckolib.renderer.base.GeoRenderer;
import software.bernie.geckolib.renderer.base.GeoRenderState;
import software.bernie.geckolib.renderer.base.RenderPassInfo;
import software.bernie.geckolib.renderer.layer.builtin.CustomBoneTextureGeoLayer;
import software.bernie.geckolib.util.RenderUtil;

public final class NovaLampLayer<R extends GeoRenderState>
        extends CustomBoneTextureGeoLayer<NovaEntity, Void, R> {
    private static final DataTicket<Integer> ENTITY_ID =
            DataTicket.create("robot-nova_mod:lamp_entity", Integer.class);

    private static final DataTicket<Boolean> ON =
            DataTicket.create("robot-nova_mod:lamp_on", Boolean.class);

    public NovaLampLayer(GeoRenderer<NovaEntity, Void, R> renderer) {
        super(renderer, "amber", ROBOTNOVA_MOD.id("textures/entity/nova.png"));
    }

    @Override
    public void addRenderData(NovaEntity nova, Void relatedObject,
                              R state, float partialTick) {
        state.addGeckolibData(ENTITY_ID, nova.getId());
        state.addGeckolibData(ON, nova.isNovaLampOn());
    }

    @Override
    protected void renderBone(RenderPassInfo<R> pass, GeoBone bone,
                              SubmitNodeCollector tasks) {
        if (!(bone instanceof CuboidGeoBone cuboid)) {
            return;
        }

        R state = pass.renderState();
        PoseStack lampPose = new PoseStack();
        lampPose.last().set(pass.poseStack().last());
        bone.translateAwayFromPivotPoint(lampPose);

        Matrix4f localPose = RenderUtil.extractPoseFromRoot(
                lampPose.last().pose(), pass.getPreRenderMatrixState());

        Vector3f lens = localPose.transformPosition(
                new Vector3f(-3.405F / 16F, 17.07F / 16F, -2.46F / 16F));

        var level = Minecraft.getInstance().level;
        int id = state.getOrDefaultGeckolibData(ENTITY_ID, -1);

        if (level != null && level.getEntity(id) instanceof NovaEntity nova) {
            nova.updateNovaLampHeight(lens.y());
        }

        RenderType type = getRenderType(state, getTextureResource(state));
        if (type == null) {
            return;
        }

        boolean on = state.getOrDefaultGeckolibData(ON, false);
        int normalLight = pass.packedLight();
        int overlay = pass.packedOverlay();
        int color = pass.renderColor();

        tasks.submitCustomGeometry(pass.poseStack(), type, (pose, buffer) -> {
            PoseStack matrices = new PoseStack();
            matrices.last().set(pose);
            bone.translateAwayFromPivotPoint(matrices);

            for (int i = 0; i < cuboid.cubes.length; i++) {
                matrices.pushPose();

                int light = on && (i == 1 || i == 2)
                        ? LightTexture.FULL_BRIGHT : normalLight;

                renderCube(cuboid.cubes[i], matrices, buffer,
                        light, overlay, color, 1F, 1F);

                matrices.popPose();
            }
        });
    }
}