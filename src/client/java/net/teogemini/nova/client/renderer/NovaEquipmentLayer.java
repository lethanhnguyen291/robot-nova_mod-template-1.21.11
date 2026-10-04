package net.teogemini.nova.client.renderer;

import java.util.List;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Either;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.teogemini.nova.entity.NovaEntity;
import net.teogemini.nova.registry.ModBlocks;
import software.bernie.geckolib.cache.model.BakedGeoModel;
import software.bernie.geckolib.cache.model.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.constant.dataticket.DataTicket;
import software.bernie.geckolib.renderer.base.BoneSnapshots;
import software.bernie.geckolib.renderer.base.GeoRenderer;
import software.bernie.geckolib.renderer.base.GeoRenderState;
import software.bernie.geckolib.renderer.base.RenderPassInfo;
import software.bernie.geckolib.renderer.layer.builtin.ItemInHandGeoLayer;

public final class NovaEquipmentLayer<R extends GeoRenderState>
        extends ItemInHandGeoLayer<NovaEntity, Void, R> {

    // =========================================================================
    // KHU VỰC CẤU HÌNH TAY VÀ CÔNG CỤ / VŨ KHÍ CỦA NOVA (DỄ DÀNG ĐIỀU CHỈNH)
    // =========================================================================

    // --- 1. CẤU HÌNH CÁNH TAY ---
    /** Góc nâng tay lên trước (Độ): -45 độ = đưa tay lên trước 45 độ */
    public static float ARM_HOLD_PITCH = 45;
    /** Góc xoay tay sang ngang (Độ): 0 độ giữ thẳng hướng phía trước */
    public static float ARM_HOLD_YAW = 0.0f;
    /** Góc nghiêng cánh tay (Độ): -18 độ giúp triệt tiêu góc nghiêng vai, tay giơ thẳng tắp ra trước (không bị vẹo vào ngực) */
    public static float ARM_HOLD_ROLL = -18.0f;

    // --- 2. CẤU HÌNH CÔNG CỤ / VŨ KHÍ TRÊN TAY ---
    /** 
     * Tỉ lệ kích thước công cụ (Scale):
     * 0.8f: Vừa vặn cân đối với bàn tay robot Nova (không bị quá bé tí hon)
     */
    public static float TOOL_SCALE = 0.68f;

    /**
     * Vị trí công cụ (Dời tọa độ X, Y, Z để chuôi cầm nằm trúng lòng bàn tay):
     * - OFFSET_X: Ngang (Âm: sang trái, Dương: sang phải)
     * - OFFSET_Y: Dọc (Âm: xuống dưới, Dương: lên trên)
     * - OFFSET_Z: Trước/Sau (Âm: ra trước, Dương: ra sau)
     */
    public static float TOOL_OFFSET_X = -0.00f;
    public static float TOOL_OFFSET_Y = -0.08f;
    public static float TOOL_OFFSET_Z = -0.05f;

    /**
     * Góc xoay công cụ theo độ (Degrees):
     * - ROT_X: Xoay ngửa / chúc đầu vũ khí (Độ)
     * - ROT_Y: Xoay quanh trục dọc (xoay mặt phẳng lưỡi kiếm)
     * - ROT_Z: Xoay dựng đứng (-90 độ để đầu vũ khí dựng thẳng đứng lên trên trời)
     */
    public static float TOOL_ROT_X = -90.0f;
    public static float TOOL_ROT_Y = 0.0f;
    public static float TOOL_ROT_Z = 0.0f;

    // =========================================================================

    private static final DataTicket<Boolean> CARRYING =
            DataTicket.create("robot-nova_mod:carrying_chest", Boolean.class);
    public static final DataTicket<Boolean> HOLDING_MAINHAND =
            DataTicket.create("robot-nova_mod:holding_mainhand", Boolean.class);
    public static final DataTicket<Boolean> HOLDING_OFFHAND =
            DataTicket.create("robot-nova_mod:holding_offhand", Boolean.class);

    public NovaEquipmentLayer(GeoRenderer<NovaEntity, Void, R> renderer) {
        super(renderer, "RightHandItem", "LeftHandItem");
    }

    @Override
    public void addRenderData(NovaEntity nova, Void related, R state, float partialTick) {
        super.addRenderData(nova, related, state, partialTick);
        state.addGeckolibData(CARRYING, nova.isCarryingChest());
        state.addGeckolibData(HOLDING_MAINHAND, !nova.getMainHandItem().isEmpty());
        state.addGeckolibData(HOLDING_OFFHAND, !nova.getOffhandItem().isEmpty());
    }

    @Override
    protected List<RenderData<R>> getRelevantBones(R state, BakedGeoModel model) {
        if (state.getOrDefaultGeckolibData(CARRYING, false)) {
            return List.of(new RenderData<>("carry_anchor", ItemDisplayContext.NONE,
                    (bone, frame) -> Either.left(new ItemStack(ModBlocks.NOVA_CHEST_ITEM))));
        }
        return super.getRelevantBones(state, model);
    }

    public static void applyCarryPose(RenderPassInfo<?> pass, BoneSnapshots bones) {
        boolean carrying = pass.getOrDefaultGeckolibData(CARRYING, false);
        for (String name : new String[]{"arm_left", "arm_right"}) {
            bones.ifPresent(name, bone -> {
                bone.skipRender(carrying);
                bone.skipChildrenRender(carrying);
            });
        }
        for (String name : new String[]{"carry_arm_left", "carry_arm_right"}) {
            bones.ifPresent(name, bone -> bone.skipRender(!carrying));
        }
    }

    public static void applyHoldingPose(RenderPassInfo<?> pass, BoneSnapshots bones) {
        // Nếu robot đang bê rương thì không áp dụng tư thế cầm vũ khí
        if (pass.getOrDefaultGeckolibData(CARRYING, false)) {
            return;
        }

        boolean holdingMain = pass.getOrDefaultGeckolibData(HOLDING_MAINHAND, false);
        if (holdingMain) {
            bones.get("arm_right").ifPresent(arm -> {
                arm.setRotX(arm.getRotX() + (float) Math.toRadians(ARM_HOLD_PITCH));
                arm.setRotY(arm.getRotY() + (float) Math.toRadians(ARM_HOLD_YAW));
                arm.setRotZ(arm.getRotZ() + (float) Math.toRadians(ARM_HOLD_ROLL));
            });
        }

        boolean holdingOff = pass.getOrDefaultGeckolibData(HOLDING_OFFHAND, false);
        if (holdingOff) {
            bones.get("arm_left").ifPresent(arm -> {
                arm.setRotX(arm.getRotX() + (float) Math.toRadians(ARM_HOLD_PITCH));
                arm.setRotY(arm.getRotY() - (float) Math.toRadians(ARM_HOLD_YAW));
                arm.setRotZ(arm.getRotZ() - (float) Math.toRadians(ARM_HOLD_ROLL));
            });
        }
    }

    @Override
    protected void submitItemStackRender(PoseStack pose, GeoBone bone, ItemStack stack,
            ItemDisplayContext context, R state, SubmitNodeCollector tasks,
            CameraRenderState camera, int light, int overlay, int color) {
        if (bone.name().equals("carry_anchor")) {
            var minecraft = Minecraft.getInstance();
            ItemStackRenderState item = new ItemStackRenderState();
            minecraft.getItemModelResolver().updateForTopItem(item, stack,
                    ItemDisplayContext.NONE, minecraft.level, null, 0);
            var bounds = item.getModelBoundingBox();
            double extent = Math.max(bounds.getXsize(), Math.max(bounds.getYsize(), bounds.getZsize()));
            if (!Double.isFinite(extent) || extent <= 0) return;
            // Match the 5x5x5 placeholder centred at (0, 5.5, -5.5) in the user's model.
            float scale = (float) ((5.0 / 16.0) / extent);
            var centre = bounds.getCenter();
            pose.pushPose();
            pose.scale(scale, scale, scale);
            pose.translate(-centre.x, -centre.y, -centre.z);
            item.submit(pose, tasks, light, overlay, 0);
            pose.popPose();
            return;
        }

        if (bone.name().equals("RightHandItem") || bone.name().equals("LeftHandItem")) {
            boolean isLeft = bone.name().equals("LeftHandItem");
            pose.pushPose();

            // 1. Dời vị trí công cụ (Offset)
            float offX = isLeft ? -TOOL_OFFSET_X : TOOL_OFFSET_X;
            pose.translate(offX, TOOL_OFFSET_Y, TOOL_OFFSET_Z);

            // 2. Góc xoay công cụ (Rotation tính bằng độ Degrees)
            if (TOOL_ROT_X != 0.0f) {
                pose.mulPose(Axis.XP.rotationDegrees(TOOL_ROT_X));
            }
            float rotY = isLeft ? -TOOL_ROT_Y : TOOL_ROT_Y;
            if (rotY != 0.0f) {
                pose.mulPose(Axis.YP.rotationDegrees(rotY));
            }
            float rotZ = isLeft ? -TOOL_ROT_Z : TOOL_ROT_Z;
            if (rotZ != 0.0f) {
                pose.mulPose(Axis.ZP.rotationDegrees(rotZ));
            }

            // 3. Kích thước công cụ (Scale)
            pose.scale(TOOL_SCALE, TOOL_SCALE, TOOL_SCALE);

            // 4. Render vật phẩm với đúng ngữ cảnh hiển thị
            var minecraft = Minecraft.getInstance();
            ItemStackRenderState item = new ItemStackRenderState();
            int seed = (int) (long) state.getOrDefaultGeckolibData(DataTickets.ANIMATABLE_INSTANCE_ID, 0L) + context.ordinal();
            minecraft.getItemModelResolver().updateForTopItem(
                    item, stack, context, minecraft.level, null, seed);
            item.submit(pose, tasks, light, overlay, 0);

            pose.popPose();
            return;
        }

        super.submitItemStackRender(pose, bone, stack, context, state, tasks,
                camera, light, overlay, color);
    }
}