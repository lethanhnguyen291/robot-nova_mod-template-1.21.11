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
        boolean isDormant = pass.getOrDefaultGeckolibData(IS_DORMANT, false);
        if (isDormant) {
            int pose = pass.getOrDefaultGeckolibData(DORMANT_POSE, 0);
            applyDormantPose(bones, pose);
            return;
        }
        NovaEquipmentLayer.applyCarryPose(pass, bones);
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
        switch (pose) {
            case 0 -> { // Slumped / Shutdown (Cúi đầu ngủ đông - Tắt nguồn)
                bones.get("head").ifPresent(head -> {
                    head.setRotX((float) Math.toRadians(35.0));
                    head.setRotY(0.0F);
                    head.setRotZ(0.0F);
                });
                bones.get("body").ifPresent(body -> body.setRotX((float) Math.toRadians(8.0)));
                bones.get("arm_right").ifPresent(arm -> {
                    arm.setRotX((float) Math.toRadians(12.0));
                    arm.setRotY(0.0F);
                    arm.setRotZ((float) Math.toRadians(4.0));
                });
                bones.get("arm_left").ifPresent(arm -> {
                    arm.setRotX((float) Math.toRadians(12.0));
                    arm.setRotY(0.0F);
                    arm.setRotZ((float) Math.toRadians(-4.0));
                });
                bones.get("leg_right").ifPresent(leg -> { leg.setRotX(0.0F); leg.setRotY(0.0F); leg.setRotZ(0.0F); });
                bones.get("leg_left").ifPresent(leg -> { leg.setRotX(0.0F); leg.setRotY(0.0F); leg.setRotZ(0.0F); });
            }
            case 1 -> { // Sentry Stand (Đứng nghiêm canh gác)
                bones.get("head").ifPresent(head -> {
                    head.setRotX(0.0F);
                    head.setRotY(0.0F);
                    head.setRotZ(0.0F);
                });
                bones.get("body").ifPresent(body -> body.setRotX(0.0F));
                bones.get("arm_right").ifPresent(arm -> {
                    arm.setRotX(0.0F);
                    arm.setRotY(0.0F);
                    arm.setRotZ((float) Math.toRadians(6.0));
                });
                bones.get("arm_left").ifPresent(arm -> {
                    arm.setRotX(0.0F);
                    arm.setRotY(0.0F);
                    arm.setRotZ((float) Math.toRadians(-6.0));
                });
                bones.get("leg_right").ifPresent(leg -> leg.setRotZ((float) Math.toRadians(5.0)));
                bones.get("leg_left").ifPresent(leg -> leg.setRotZ((float) Math.toRadians(-5.0)));
            }
            case 2 -> { // Sitting / Hibernation (Ngồi gập gối chờ lệnh)
                bones.get("nova_root").ifPresent(root -> root.setTranslateY(root.getTranslateY() - 6.0F));
                bones.get("head").ifPresent(head -> {
                    head.setRotX((float) Math.toRadians(10.0));
                    head.setRotY(0.0F);
                    head.setRotZ((float) Math.toRadians(12.0));
                });
                bones.get("body").ifPresent(body -> body.setRotX((float) Math.toRadians(5.0)));
                bones.get("leg_right").ifPresent(leg -> leg.setRotX((float) Math.toRadians(-75.0)));
                bones.get("leg_left").ifPresent(leg -> leg.setRotX((float) Math.toRadians(-75.0)));
                bones.get("arm_right").ifPresent(arm -> {
                    arm.setRotX((float) Math.toRadians(-60.0));
                    arm.setRotY((float) Math.toRadians(15.0));
                });
                bones.get("arm_left").ifPresent(arm -> {
                    arm.setRotX((float) Math.toRadians(-60.0));
                    arm.setRotY((float) Math.toRadians(-15.0));
                });
            }
            case 3 -> { // Salute / Waving (Giơ tay chào)
                bones.get("head").ifPresent(head -> {
                    head.setRotX((float) Math.toRadians(-5.0));
                    head.setRotY((float) Math.toRadians(15.0));
                    head.setRotZ((float) Math.toRadians(-6.0));
                });
                bones.get("arm_right").ifPresent(arm -> {
                    arm.setRotX((float) Math.toRadians(-115.0));
                    arm.setRotY((float) Math.toRadians(-15.0));
                    arm.setRotZ((float) Math.toRadians(25.0));
                });
                bones.get("arm_left").ifPresent(arm -> {
                    arm.setRotX((float) Math.toRadians(-12.0));
                    arm.setRotZ((float) Math.toRadians(-12.0));
                });
                bones.get("leg_right").ifPresent(leg -> leg.setRotZ((float) Math.toRadians(3.0)));
                bones.get("leg_left").ifPresent(leg -> leg.setRotZ((float) Math.toRadians(-3.0)));
            }
            case 4 -> { // Hero / Combat Guard (Chiến binh thủ thế)
                bones.get("head").ifPresent(head -> {
                    head.setRotX((float) Math.toRadians(-8.0));
                    head.setRotY((float) Math.toRadians(-10.0));
                });
                bones.get("body").ifPresent(body -> {
                    body.setRotX((float) Math.toRadians(-5.0));
                    body.setRotY((float) Math.toRadians(10.0));
                });
                bones.get("arm_right").ifPresent(arm -> {
                    arm.setRotX((float) Math.toRadians(-45.0));
                    arm.setRotY((float) Math.toRadians(25.0));
                    arm.setRotZ((float) Math.toRadians(15.0));
                });
                bones.get("arm_left").ifPresent(arm -> {
                    arm.setRotX((float) Math.toRadians(-55.0));
                    arm.setRotY((float) Math.toRadians(-25.0));
                    arm.setRotZ((float) Math.toRadians(-15.0));
                });
                bones.get("leg_right").ifPresent(leg -> leg.setRotX((float) Math.toRadians(18.0)));
                bones.get("leg_left").ifPresent(leg -> leg.setRotX((float) Math.toRadians(-18.0)));
            }
        }
    }
}
