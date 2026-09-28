package net.teogemini.nova.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.teogemini.nova.ROBOTNOVA_MOD;
import net.teogemini.nova.entity.NovaEntity;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.model.GeoBone;
import software.bernie.geckolib.cache.model.GeoQuad;
import software.bernie.geckolib.cache.model.GeoVertex;
import software.bernie.geckolib.cache.model.cuboid.CuboidGeoBone;
import software.bernie.geckolib.cache.model.cuboid.GeoCube;
import software.bernie.geckolib.constant.dataticket.DataTicket;
import software.bernie.geckolib.renderer.base.GeoRenderer;
import software.bernie.geckolib.renderer.base.GeoRenderState;
import software.bernie.geckolib.renderer.base.RenderPassInfo;
import software.bernie.geckolib.renderer.layer.builtin.CustomBoneTextureGeoLayer;
import software.bernie.geckolib.util.RenderUtil;

public class NovaFaceLayer<R extends GeoRenderState>
        extends CustomBoneTextureGeoLayer<NovaEntity, Void, R> {

    private static final DataTicket<Integer> FACE =
            DataTicket.create("robot-nova_mod:face_frame", Integer.class);

    private static final DataTicket<Float> SPIN =
            DataTicket.create("robot-nova_mod:face_spin", Float.class);

    private static final Identifier ORIGINAL =
            ROBOTNOVA_MOD.id("textures/entity/nova.png");

    private static final Identifier SHEET_A =
            ROBOTNOVA_MOD.id("textures/entity/nova_faces_a.png");

    private static final Identifier SHEET_B =
            ROBOTNOVA_MOD.id("textures/entity/nova_faces_b.png");

    // Mỗi mục: x, y, rộng, cao. Thứ tự tương ứng ID 0-39.
    private static final int[][] RECTS = {
            // 0-7: nova.png
            {5, 5, 303, 303}, {318, 5, 303, 303},
            {631, 5, 304, 303}, {945, 5, 304, 303},
            {5, 318, 303, 302}, {318, 318, 303, 302},
            {631, 318, 304, 302}, {945, 318, 304, 302},

            // 8-23: nova_faces_a.png
            {3, 18, 303, 303}, {318, 20, 303, 303},
            {631, 12, 303, 303}, {946, 17, 303, 303},
            {4, 304, 303, 303}, {317, 304, 303, 303},
            {632, 320, 303, 303}, {944, 315, 303, 303},
            {6, 613, 303, 303}, {316, 606, 303, 303},
            {630, 602, 303, 303}, {946, 616, 303, 303},
            {4, 912, 303, 303}, {318, 919, 303, 303},
            {630, 902, 303, 303}, {945, 906, 303, 303},

            // 24-39: nova_faces_b.png
            {4, 20, 303, 303}, {320, 18, 303, 303},
            {634, 15, 303, 303}, {950, 16, 303, 303},
            {4, 318, 303, 303}, {320, 303, 303, 303},
            {632, 314, 303, 303}, {950, 322, 303, 303},
            {6, 600, 303, 303}, {318, 602, 303, 303},
            {636, 595, 303, 303}, {946, 620, 303, 303},
            {6, 934, 303, 303}, {314, 908, 303, 303},
            {633, 925, 303, 303}, {946, 924, 303, 303}
    };

    public NovaFaceLayer(GeoRenderer<NovaEntity, Void, R> renderer) {
        super(renderer, "face", ORIGINAL);
    }

    @Override
    public void addRenderData(NovaEntity nova, Void relatedObject,
                              R state, float partialTick) {
        int frame = Math.max(0, Math.min(nova.getNovaFace(), 39));
        int phase = Math.floorMod(nova.tickCount + nova.getId() * 13, 90);

        // Mặt bình thường tự chớp mắt.
        if (frame == 0 && phase < 3) {
            frame = 7;
        }

        state.addGeckolibData(FACE, frame);
        state.addGeckolibData(SPIN,
                (nova.tickCount % 40 + partialTick) * (float) Math.PI / 20F);
    }

    @Override
    protected Identifier getTextureResource(R state) {
        int frame = state.getOrDefaultGeckolibData(FACE, 0);
        return frame < 8 ? ORIGINAL : frame < 24 ? SHEET_A : SHEET_B;
    }

    @Override
    protected void renderBone(RenderPassInfo<R> pass, GeoBone bone,
                              SubmitNodeCollector tasks) {
        R state = pass.renderState();
        RenderType type = getRenderType(state, getTextureResource(state));

        if (type == null || !(bone instanceof CuboidGeoBone cuboid)) {
            return;
        }

        int frame = state.getOrDefaultGeckolibData(FACE, 0);
        int[] rect = RECTS[frame];
        float angle = state.getOrDefaultGeckolibData(SPIN, 0F);
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        int light = pass.packedLight();
        int overlay = pass.packedOverlay();
        int color = pass.renderColor();

        tasks.submitCustomGeometry(pass.poseStack(), type, (pose, buffer) -> {
            PoseStack matrices = new PoseStack();
            matrices.last().set(pose);
            bone.translateAwayFromPivotPoint(matrices);

            for (GeoCube cube : cuboid.cubes) {
                matrices.pushPose();
                cube.translateToPivotPoint(matrices);
                cube.rotate(matrices);
                cube.translateAwayFromPivotPoint(matrices);

                Matrix4f matrix = matrices.last().pose();

                for (GeoQuad quad : cube.quads()) {
                    if (quad == null) {
                        continue;
                    }

                    Vector3f normal = matrices.last().normal().transform(quad.normalVec());
                    RenderUtil.fixInvertedFlatCube(cube, normal);

                    for (GeoVertex vertex : quad.vertices()) {
                        // Các cạnh mỏng dùng một điểm nền xanh đậm.
                        float u = (rect[0] + 1) / 1254F;
                        float v = (rect[1] + 1) / 1254F;

                        if (quad.direction() == Direction.NORTH) {
                            float x = (vertex.texU() * 1254F - 5F) / 303F;
                            float y = (vertex.texV() * 1254F - 5F) / 303F;

                            if (frame == 39) {
                                // Bù tỉ lệ màn hình để vòng loading tròn.
                                float dx = (x - 0.5F) * 0.60F * (8.3F / 6.4F);
                                float dy = (y - 0.5F) * 0.60F;

                                x = 0.5F + dx * cos - dy * sin;
                                y = 0.5F + dx * sin + dy * cos;
                            }

                            u = (rect[0] + x * rect[2]) / 1254F;
                            v = (rect[1] + y * rect[3]) / 1254F;
                        }

                        Vector4f point = matrix.transform(new Vector4f(
                                vertex.posX(), vertex.posY(), vertex.posZ(), 1));

                        buffer.addVertex(
                                point.x(), point.y(), point.z(), color,
                                u, v, overlay, light,
                                normal.x(), normal.y(), normal.z()
                        );
                    }
                }

                matrices.popPose();
            }
        });
    }
}