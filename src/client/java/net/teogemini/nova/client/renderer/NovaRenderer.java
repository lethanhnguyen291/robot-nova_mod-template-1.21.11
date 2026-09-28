package net.teogemini.nova.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.teogemini.nova.client.model.NovaModel;
import net.teogemini.nova.entity.NovaEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.base.GeoRenderState;
import software.bernie.geckolib.renderer.base.BoneSnapshots;
import software.bernie.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;

public class NovaRenderer<R extends EntityRenderState & GeoRenderState>
        extends GeoEntityRenderer<NovaEntity, R> {

    public NovaRenderer(EntityRendererProvider.Context context) {
        super(context, new NovaModel());
        this.shadowRadius = 0.35F;
              withRenderLayer(new NovaFaceLayer<>(this));
              withRenderLayer(new NovaLampLayer<>(this));
              withRenderLayer(new NovaEquipmentLayer<>(this));
    }
    @Override
    public void adjustModelBonesForRender(RenderPassInfo<R> pass, BoneSnapshots bones) {
        super.adjustModelBonesForRender(pass, bones);
        NovaEquipmentLayer.applyCarryPose(pass, bones);

        double time = pass.renderState().ageInTicks / 20.0;

        // Ngước lên và cúi xuống.
        float nod = (float) Math.toRadians(
                2.0 * Math.sin(time * 1.6));

        // Quay đầu nhẹ sang trái và phải.
        float turn = (float) Math.toRadians(
                3.0 * Math.sin(time * 1.1 + 0.8));

        // Nghiêng đầu qua hai bên.
        float tilt = (float) Math.toRadians(
                2.0 * Math.sin(time * 1.3 + 1.6));
        // Lấy góc nhìn hiện tại của Nova từ dữ liệu GeckoLib
        float lookPitch = pass.getOrDefaultGeckolibData(
                DataTickets.ENTITY_PITCH, 0.0F);
        // Lấy góc nhìn hiện tại của Nova từ dữ liệu GeckoLib
        float lookYaw = pass.getOrDefaultGeckolibData(
                DataTickets.ENTITY_YAW, 0.0F);

        // Đổi góc nhìn từ Độ sang Radian chuẩn cho xương GeckoLib
        float pitchRad = (float) Math.toRadians(lookPitch);
        float yawRad = (float) Math.toRadians(lookYaw);

        float limit = (float) Math.toRadians(90.0);

        bones.get("head").ifPresent(head -> {
            head.setRotX(Mth.clamp(
                    head.getRotX() - pitchRad + nod, -limit, limit));

            head.setRotY(Mth.clamp(
                    head.getRotY() - yawRad + turn, -limit, limit));

            head.setRotZ(head.getRotZ() + tilt);
        });
    }
}