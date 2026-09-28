package net.teogemini.nova.client.renderer;

import java.util.List;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Either;
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
import software.bernie.geckolib.constant.dataticket.DataTicket;
import software.bernie.geckolib.renderer.base.BoneSnapshots;
import software.bernie.geckolib.renderer.base.GeoRenderer;
import software.bernie.geckolib.renderer.base.GeoRenderState;
import software.bernie.geckolib.renderer.base.RenderPassInfo;
import software.bernie.geckolib.renderer.layer.builtin.ItemInHandGeoLayer;

public final class NovaEquipmentLayer<R extends GeoRenderState>
        extends ItemInHandGeoLayer<NovaEntity, Void, R> {
    private static final DataTicket<Boolean> CARRYING =
            DataTicket.create("robot-nova_mod:carrying_chest", Boolean.class);

    public NovaEquipmentLayer(GeoRenderer<NovaEntity, Void, R> renderer) {
        super(renderer, "RightHandItem", "LeftHandItem");
    }
    // =========================================================================
    // KHU VỰC CẤU HÌNH TAY VÀ CÔNG CỤ / VŨ KHÍ CỦA NOVA (DỄ DÀNG ĐIỀU CHỈNH)
    // =========================================================================

    // --- 1. CẤU HÌNH GÓC CÁNH TAY KHI CẦM VŨ KHÍ ---
    /** Góc nâng tay lên trước (Độ): -45 độ = đưa tay lên trước 45 độ (Âm: đưa ra trước, Dương: đưa ra sau) */
    public static float ARM_HOLD_PITCH = -45.0f;
    /** Góc xoay tay sang ngang (Độ): Dương: xoay vào trong người, Âm: xoay mở ra ngoài */
    public static float ARM_HOLD_YAW = 0.0f;
    /** Góc nghiêng cánh tay (Độ) */
    public static float ARM_HOLD_ROLL = 0.0f;

    // --- 2. CẤU HÌNH CÔNG CỤ / VŨ KHÍ TRÊN TAY ---
    /** 
     * Tỉ lệ kích thước công cụ (Scale):
     * 1.0f = 100% kích thước gốc
     * 0.6f = giảm 40% kích thước (còn 60%)
     */
    public static float TOOL_SCALE = 0.6f;

    /**
     * Vị trí công cụ (Dời tọa độ X, Y, Z so với bàn tay):
     * - OFFSET_X: Ngang (Âm: sang trái, Dương: sang phải)
     * - OFFSET_Y: Dọc (Âm: xuống dưới, Dương: lên trên)
     * - OFFSET_Z: Trước/Sau (Âm: ra trước, Dương: ra sau)
     */
    public static float TOOL_OFFSET_X = 0.0f;
    public static float TOOL_OFFSET_Y = 0.0f;
    public static float TOOL_OFFSET_Z = 0.0f;

    /**
     * Góc xoay công cụ theo độ (Degrees):
     * - ROT_X: Xoay ngửa / chúc đầu vũ khí (Mặc định 0.0f đầu vũ khí sẽ hướng lên trên)
     * - ROT_Y: Xoay quanh trục dọc (xoay hướng lưỡi kiếm / mặt công cụ)
     * - ROT_Z: Xoay nghiêng công cụ sang trái / phải
     */
    public static float TOOL_ROT_X = 0.0f;
    public static float TOOL_ROT_Y = 0.0f;
    public static float TOOL_ROT_Z = 0.0f;
    
    
    @Override
    public void addRenderData(NovaEntity nova, Void related, R state, float partialTick) {
        super.addRenderData(nova, related, state, partialTick);
        state.addGeckolibData(CARRYING, nova.isCarryingChest());
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

    @Override
    protected void submitItemStackRender(PoseStack pose, GeoBone bone, ItemStack stack,
            ItemDisplayContext context, R state, SubmitNodeCollector tasks,
            CameraRenderState camera, int light, int overlay, int color) {
        if (!bone.name().equals("carry_anchor")) {
            super.submitItemStackRender(pose, bone, stack, context, state, tasks,
                    camera, light, overlay, color);
            return;
        }
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
    }
}
