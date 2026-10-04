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
        boolean shouldTrack = !sitting && !free && !acting;
        state.addGeckolibData(SHOULD_TRACK_OWNER, shouldTrack);
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<R> pass, BoneSnapshots bones) {
        super.adjustModelBonesForRender(pass, bones);
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
}