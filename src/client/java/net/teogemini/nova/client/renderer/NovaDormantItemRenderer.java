package net.teogemini.nova.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.teogemini.nova.client.model.NovaDormantItemModel;
import net.teogemini.nova.item.NovaDormantItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.base.BoneSnapshots;
import software.bernie.geckolib.renderer.base.GeoRenderState;
import software.bernie.geckolib.renderer.base.RenderPassInfo;

public class NovaDormantItemRenderer extends GeoItemRenderer<NovaDormantItem> {
    public NovaDormantItemRenderer() {
        super(new NovaDormantItemModel());
        useAlternateGuiLighting();
        withRenderLayer(new NovaFaceLayer<>(this));
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<GeoRenderState> pass, BoneSnapshots bones) {
        super.adjustModelBonesForRender(pass, bones);

        // Luôn ẩn 2 tay bê rương và khung rương
        bones.ifPresent("carry_arm_left", b -> { b.skipRender(true); b.skipChildrenRender(true); });
        bones.ifPresent("carry_arm_right", b -> { b.skipRender(true); b.skipChildrenRender(true); });
        bones.ifPresent("carry_anchor", b -> { b.skipRender(true); b.skipChildrenRender(true); });

        // Hiện 2 tay thường
        bones.ifPresent("arm_left", b -> { b.skipRender(false); b.skipChildrenRender(false); });
        bones.ifPresent("arm_right", b -> { b.skipRender(false); b.skipChildrenRender(false); });

        // Tư thế nghiêm đứng thẳng trên tay và trong ô đồ
        bones.get("head").ifPresent(head -> {
            head.setRotX(0.0F);
            head.setRotY(0.0F);
            head.setRotZ(0.0F);
        });
        bones.get("body").ifPresent(body -> {
            body.setRotX(0.0F);
            body.setRotY(0.0F);
            body.setRotZ(0.0F);
        });
        bones.get("arm_right").ifPresent(arm -> {
            arm.setRotX(0.0F);
            arm.setRotY(0.0F);
            arm.setRotZ(arm.getRotZ() - (float) Math.toRadians(10.0));
        });
        bones.get("arm_left").ifPresent(arm -> {
            arm.setRotX(0.0F);
            arm.setRotY(0.0F);
            arm.setRotZ(arm.getRotZ() + (float) Math.toRadians(10.0));
        });
        bones.get("leg_right").ifPresent(leg -> { leg.setRotX(0.0F); leg.setRotY(0.0F); leg.setRotZ(0.0F); });
        bones.get("leg_left").ifPresent(leg -> { leg.setRotX(0.0F); leg.setRotY(0.0F); leg.setRotZ(0.0F); });
    }

    @Override
    public void adjustRenderPose(RenderPassInfo<GeoRenderState> pass) {
        PoseStack poseStack = pass.poseStack();
        poseStack.translate(0.5F, 0.0F, 0.5F);
    }
}
