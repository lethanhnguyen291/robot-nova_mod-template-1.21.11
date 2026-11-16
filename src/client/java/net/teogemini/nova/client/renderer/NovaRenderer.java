package net.teogemini.nova.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.teogemini.nova.client.model.NovaModel;
import net.teogemini.nova.entity.NovaEntity;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.constant.dataticket.DataTicket;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.base.BoneSnapshots;
import software.bernie.geckolib.renderer.base.GeoRenderState;
import software.bernie.geckolib.renderer.base.RenderPassInfo;

public class NovaRenderer<R extends EntityRenderState & GeoRenderState>
        extends GeoEntityRenderer<NovaEntity, R> {

    public static boolean INVERT_HEAD_PITCH = true;
    public static final DataTicket<Boolean> IS_DORMANT =
            DataTicket.create("robot-nova_mod:is_dormant", Boolean.class);
    public static final DataTicket<Integer> DORMANT_POSE =
            DataTicket.create("robot-nova_mod:dormant_pose", Integer.class);
    public static final DataTicket<Boolean> SHOULD_TRACK_OWNER =
            DataTicket.create("robot-nova_mod:should_track_owner", Boolean.class);

    public NovaRenderer(EntityRendererProvider.Context context) {
        super(context, new NovaModel());
        this.shadowRadius = 0.35F;
        withRenderLayer(new NovaFaceLayer<>(this));
        withRenderLayer(new NovaLampLayer<>(this));
        withRenderLayer(new NovaEquipmentLayer<>(this));
    }

    @Override
    public void addRenderData(NovaEntity nova, Void related, R state, float partialTick) {
        super.addRenderData(nova, related, state, partialTick);
        boolean sitting = nova.isInSittingPose() || nova.isOrderedToSit() || nova.getNovaMoveMode() == NovaEntity.MODE_WAIT;
        boolean free = nova.getNovaMoveMode() == NovaEntity.MODE_FREE;
        boolean acting = nova.isPerformingAction() || nova.getWorkAction() != NovaEntity.WORK_NONE;
        boolean shouldTrack = !sitting && !free && !acting && !nova.isDormant();
        state.addGeckolibData(SHOULD_TRACK_OWNER, shouldTrack);
        state.addGeckolibData(IS_DORMANT, nova.isDormant());
        state.addGeckolibData(DORMANT_POSE, nova.getDormantPose());
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<R> pass, BoneSnapshots bones) {
        super.adjustModelBonesForRender(pass, bones);
        NovaEquipmentLayer.applyCarryPose(pass, bones);

        boolean isDormant = pass.getOrDefaultGeckolibData(IS_DORMANT, false);
        if (isDormant) {
            int pose = pass.getOrDefaultGeckolibData(DORMANT_POSE, 0);
            applyDormantPose(bones, pose);
            return;
        }
        NovaEquipmentLayer.applyHoldingPose(pass, bones);

        double time = pass.renderState().ageInTicks / 20.0;

        float nod = (float) Math.toRadians(2.0 * Math.sin(time * 1.6));
        float turn = (float) Math.toRadians(3.0 * Math.sin(time * 1.1 + 0.8));
        float tilt = (float) Math.toRadians(2.0 * Math.sin(time * 1.3 + 1.6));

        boolean shouldTrack = pass.getOrDefaultGeckolibData(SHOULD_TRACK_OWNER, false);
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        float lookPitch = 0.0F;
        float lookYaw = 0.0F;
        boolean tracking = false;

        if (shouldTrack && player != null) {
            double novaX = pass.renderState().x;
            double novaY = pass.renderState().y + pass.renderState().eyeHeight;
            double novaZ = pass.renderState().z;

            double dx = player.getX() - novaX;
            double dy = player.getEyeY() - novaY;
            double dz = player.getZ() - novaZ;
            double distSq = dx * dx + dy * dy + dz * dz;

            if (distSq <= 32.0 * 32.0 && distSq > 0.001) {
                double distXZ = Math.sqrt(dx * dx + dz * dz);
                lookPitch = (float) (Mth.atan2(dy, distXZ) * 180.0 / Math.PI);

                float targetYawDeg = (float) (Mth.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
                float bodyYaw = pass.getOrDefaultGeckolibData(DataTickets.ENTITY_BODY_YAW, 0.0F);
                lookYaw = Mth.wrapDegrees(targetYawDeg - bodyYaw);
                tracking = true;
            }
        }

        float limit = (float) Math.toRadians(85.0);

        if (tracking) {
            float pitchRad = (float) Math.toRadians(lookPitch);
            float yawRad = (float) Math.toRadians(lookYaw);
            bones.get("head").ifPresent(head -> {
                float finalPitch = INVERT_HEAD_PITCH ? pitchRad : -pitchRad;
                head.setRotX(Mth.clamp(head.getRotX() + finalPitch + nod, -limit, limit));
                head.setRotY(Mth.clamp(head.getRotY() - yawRad + turn, -limit, limit));
                head.setRotZ(head.getRotZ() + tilt);
            });
        } else {
            bones.get("head").ifPresent(head -> {
                head.setRotX(head.getRotX() + nod);
                head.setRotY(head.getRotY() + turn);
                head.setRotZ(head.getRotZ() + tilt);
            });
        }
    }

    private void applyDormantPose(BoneSnapshots bones, int pose) {
        // Luôn ẩn 2 tay bê rương và khung rương
        bones.ifPresent("carry_arm_left", b -> { b.skipRender(true); b.skipChildrenRender(true); });
        bones.ifPresent("carry_arm_right", b -> { b.skipRender(true); b.skipChildrenRender(true); });
        bones.ifPresent("carry_anchor", b -> { b.skipRender(true); b.skipChildrenRender(true); });

        // Đảm bảo 2 tay bình thường hiển thị theo tư thế
        bones.ifPresent("arm_left", b -> { b.skipRender(false); b.skipChildrenRender(false); });
        bones.ifPresent("arm_right", b -> { b.skipRender(false); b.skipChildrenRender(false); });

        // Lưu ý hệ trục GeckoLib BoneSnapshot của Nova:
        // - Với head và body (nằm trên khớp xoay): rotX ÂM (-) là cúi về trước, DƯƠNG (+) là ngửa ra sau.
        // - Với leg và arm (nằm dưới khớp xoay): rotX DƯƠNG (+) là đưa ra phía trước, ÂM (-) là gập ra phía sau.
        // - Với arm_right / arm_left: giữ nguyên góc vai gốc (arm.getRotZ()) và cộng thêm độ mở khi cần.
        switch (pose) {
            case 0 -> { // Sentry Stand (Mặc định khi đặt ra: Đứng nghiêm thẳng tắp)
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
            case 1 -> { // Slumped / Shutdown (Cúi đầu ngủ đông về phía trước)
                bones.get("head").ifPresent(head -> {
                    head.setRotX((float) Math.toRadians(-22.0));
                    head.setRotY(0.0F);
                    head.setRotZ(0.0F);
                });
                bones.get("body").ifPresent(body -> {
                    body.setRotX((float) Math.toRadians(-5.0));
                    body.setRotY(0.0F);
                    body.setRotZ(0.0F);
                });
                bones.get("arm_right").ifPresent(arm -> {
                    arm.setRotX((float) Math.toRadians(12.0));
                    arm.setRotY(0.0F);
                    arm.setRotZ(arm.getRotZ() - (float) Math.toRadians(6.0));
                });
                bones.get("arm_left").ifPresent(arm -> {
                    arm.setRotX((float) Math.toRadians(12.0));
                    arm.setRotY(0.0F);
                    arm.setRotZ(arm.getRotZ() + (float) Math.toRadians(6.0));
                });
                bones.get("leg_right").ifPresent(leg -> { leg.setRotX(0.0F); leg.setRotY(0.0F); leg.setRotZ(0.0F); });
                bones.get("leg_left").ifPresent(leg -> { leg.setRotX(0.0F); leg.setRotY(0.0F); leg.setRotZ(0.0F); });
            }
            case 2 -> { // Sitting / Hibernation (Ngồi duỗi 2 chân về phía trước chuẩn animation.nova.sit)
                bones.get("nova_root").ifPresent(root -> root.setTranslateY(root.getTranslateY() - 1.7F));
                bones.get("head").ifPresent(head -> {
                    head.setRotX((float) Math.toRadians(-6.0));
                    head.setRotY(0.0F);
                    head.setRotZ(0.0F);
                });
                bones.get("body").ifPresent(body -> {
                    body.setRotX((float) Math.toRadians(4.0));
                    body.setRotY(0.0F);
                    body.setRotZ(0.0F);
                });
                bones.get("leg_right").ifPresent(leg -> {
                    leg.setRotX((float) Math.toRadians(90.0));
                    leg.setRotY(0.0F);
                    leg.setRotZ(0.0F);
                });
                bones.get("leg_left").ifPresent(leg -> {
                    leg.setRotX((float) Math.toRadians(90.0));
                    leg.setRotY(0.0F);
                    leg.setRotZ(0.0F);
                });
                bones.get("arm_right").ifPresent(arm -> {
                    arm.setRotX((float) Math.toRadians(15.0));
                    arm.setRotY(0.0F);
                    arm.setRotZ(arm.getRotZ() - (float) Math.toRadians(5.0));
                });
                bones.get("arm_left").ifPresent(arm -> {
                    arm.setRotX((float) Math.toRadians(15.0));
                    arm.setRotY(0.0F);
                    arm.setRotZ(arm.getRotZ() + (float) Math.toRadians(5.0));
                });
            }
            case 3 -> { // Salute / Waving (Giơ tay phải lên vẫy chào chuẩn animation.nova.wave)
                bones.get("head").ifPresent(head -> {
                    head.setRotX((float) Math.toRadians(2.0));
                    head.setRotY(0.0F);
                    head.setRotZ(0.0F);
                });
                bones.get("body").ifPresent(body -> {
                    body.setRotX(0.0F);
                    body.setRotY(0.0F);
                    body.setRotZ(0.0F);
                });
                bones.get("arm_right").ifPresent(arm -> {
                    arm.setRotX((float) Math.toRadians(20.0));
                    arm.setRotY(0.0F);
                    arm.setRotZ(arm.getRotZ() + (float) Math.toRadians(100.0));
                });
                bones.get("arm_left").ifPresent(arm -> {
                    arm.setRotX(0.0F);
                    arm.setRotY(0.0F);
                });
                bones.get("leg_right").ifPresent(leg -> { leg.setRotX(0.0F); leg.setRotY(0.0F); leg.setRotZ(0.0F); });
                bones.get("leg_left").ifPresent(leg -> { leg.setRotX(0.0F); leg.setRotY(0.0F); leg.setRotZ(0.0F); });
            }
            case 4 -> { // Proud Guard (Ngẩng đầu tự hào, 2 tay đưa ra trước chuẩn animation.nova.companion_proud)
                bones.get("head").ifPresent(head -> {
                    head.setRotX((float) Math.toRadians(7.0));
                    head.setRotY(0.0F);
                    head.setRotZ(0.0F);
                });
                bones.get("body").ifPresent(body -> {
                    body.setRotX((float) Math.toRadians(4.0));
                    body.setRotY(0.0F);
                    body.setRotZ(0.0F);
                });
                bones.get("arm_right").ifPresent(arm -> {
                    arm.setRotX((float) Math.toRadians(45.0));
                    arm.setRotY(0.0F);
                    arm.setRotZ(arm.getRotZ() + (float) Math.toRadians(15.0));
                });
                bones.get("arm_left").ifPresent(arm -> {
                    arm.setRotX((float) Math.toRadians(45.0));
                    arm.setRotY(0.0F);
                    arm.setRotZ(arm.getRotZ() - (float) Math.toRadians(15.0));
                });
                bones.get("leg_right").ifPresent(leg -> { leg.setRotX(0.0F); leg.setRotY(0.0F); leg.setRotZ(0.0F); });
                bones.get("leg_left").ifPresent(leg -> { leg.setRotX(0.0F); leg.setRotY(0.0F); leg.setRotZ(0.0F); });
            }
        }
    }
}
