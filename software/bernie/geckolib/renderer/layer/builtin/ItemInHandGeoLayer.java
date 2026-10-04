package software.bernie.geckolib.renderer.layer.builtin;

import com.mojang.datafixers.util.Either;
import org.jspecify.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.model.BakedGeoModel;
import software.bernie.geckolib.cache.model.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.constant.dataticket.DataTicket;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.base.GeoRenderState;
import software.bernie.geckolib.renderer.base.GeoRenderer;

import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.class_11659;
import net.minecraft.class_12075;
import net.minecraft.class_1304;
import net.minecraft.class_1306;
import net.minecraft.class_1309;
import net.minecraft.class_1799;
import net.minecraft.class_1819;
import net.minecraft.class_4587;
import net.minecraft.class_7833;
import net.minecraft.class_811;

/**
 * Built-in GeoLayer for rendering the item in a {@link class_1309}'s hands.
 * <p>
 * Handles all the boilerplate for basic handheld item rendering.
 * <p>
 * Assumes the {@link GeoModel} has bones for both hands called {@code RightHandItem} and {@code LeftHandItem}.
 * If you have different names, use the {@link ItemInHandGeoLayer#ItemInHandGeoLayer(GeoRenderer, String, String)} constructor.
 *
 * @param <T> Entity animatable class type. Inherited from the renderer this layer is attached to
 * @param <O> Associated object class type, or {@link Void} if none. Inherited from the renderer this layer is attached to
 * @param <R> RenderState class type. Inherited from the renderer this layer is attached to
 */
public class ItemInHandGeoLayer<T extends class_1309 & GeoAnimatable, O, R extends GeoRenderState> extends BlockAndItemGeoLayer<T, O, R> {
    protected final String rightHandBone;
    protected final String leftHandBone;

    public ItemInHandGeoLayer(GeoRenderer<T, O, R> renderer) {
        this(renderer, "RightHandItem", "LeftHandItem");
    }

    public ItemInHandGeoLayer(GeoRenderer<T, O, R> renderer, String rightHandBoneName, String leftHandBoneName) {
        super(renderer);

        this.rightHandBone = rightHandBoneName;
        this.leftHandBone = leftHandBoneName;
    }

    /**
     * Return a list of the bone names that this layer will render for.
     * <p>
     * Ideally, you would cache this list in a class-field if you don't need any data from the input renderState or model
     */
    @Override
    protected List<RenderData<R>> getRelevantBones(R renderState, BakedGeoModel model) {
        boolean isLeftHanded = renderState.getOrDefaultGeckolibData(DataTickets.IS_LEFT_HANDED, false);

        return List.of(
                renderDataForHand(this.rightHandBone, class_1306.field_6183, isLeftHanded, renderState),
                renderDataForHand(this.leftHandBone, class_1306.field_6182, isLeftHanded, renderState));
    }

    /**
     * Helper method for creating {@link RenderData} for a given hand
     */
    protected static <R extends GeoRenderState> RenderData<R> renderDataForHand(String boneName, R renderState) {
        return renderDataForHand(boneName, class_1306.field_6183, false, renderState);
    }

    /**
     * Helper method for creating {@link RenderData} for a given hand
     */
    protected static <R extends GeoRenderState> RenderData<R> renderDataForHand(String boneName, class_1306 arm, boolean isLeftHanded, R renderState) {
        class_1306 mainHandArm = isLeftHanded ? class_1306.field_6182 : class_1306.field_6183;
        class_1304 slot = arm == mainHandArm ? class_1304.field_6173 : class_1304.field_6171;

        class_811 context = switch (slot) {
            case field_6173 -> mainHandArm == class_1306.field_6183 ? class_811.field_4320 : class_811.field_4323;
            case field_6171 -> mainHandArm == class_1306.field_6183 ? class_811.field_4323 : class_811.field_4322;
            default -> class_811.field_4315;
        };

        return new RenderData<>(boneName, context, (bone, renderState2) -> Either.left((class_1799)renderState2.getGeckolibData(DataTickets.EQUIPMENT_BY_SLOT).get(slot)));
    }

    /**
     * Override to add any custom {@link DataTicket}s you need to capture for rendering.
     * <p>
     * The animatable is discarded from the rendering context after this, so any data needed
     * for rendering should be captured in the renderState provided
     *
     * @param animatable The animatable instance being rendered
     * @param relatedObject An object related to the render pass or null if not applicable.
     *                         (E.G., ItemStack for GeoItemRenderer, entity instance for GeoReplacedEntityRenderer).
     * @param renderState The GeckoLib RenderState to add data to, will be passed through the rest of rendering
     * @param partialTick The fraction of a tick that has elapsed as of the current render pass
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void addRenderData(T animatable, @Nullable O relatedObject, R renderState, float partialTick) {
        EnumMap<class_1304, class_1799> equipment = renderState.getOrDefaultGeckolibData(DataTickets.EQUIPMENT_BY_SLOT, (Supplier<EnumMap>)() -> new EnumMap<>(class_1304.class));

        //noinspection DataFlowIssue
        equipment.put(class_1304.field_6173, animatable.method_6047());
        equipment.put(class_1304.field_6171, animatable.method_6079());

        renderState.addGeckolibData(DataTickets.EQUIPMENT_BY_SLOT, equipment);
        renderState.addGeckolibData(DataTickets.IS_LEFT_HANDED, animatable.method_6068() == class_1306.field_6182);
    }

    /**
     * Render the given {@link class_1799} for the provided {@link GeoBone}.
     */
    @Override
    protected void submitItemStackRender(class_4587 poseStack, GeoBone bone, class_1799 stack, class_811 displayContext, R renderState, class_11659 renderTasks,
                                         class_12075 cameraState, int packedLight, int packedOverlay, int renderColor) {
        poseStack.method_22903();

        if (displayContext == class_811.field_4320) {
            poseStack.method_22907(class_7833.field_40713.rotationDegrees(90f));
            poseStack.method_46416(0, 0.125f, -0.0625f);

            if (stack.method_7909() instanceof class_1819)
                poseStack.method_22904(0, 0.125, -0.25);
        }
        else if (displayContext == class_811.field_4323) {
            poseStack.method_22907(class_7833.field_40713.rotationDegrees(90f));
            poseStack.method_46416(0, 0.125f, -0.0625f);

            if (stack.method_7909() instanceof class_1819) {
                poseStack.method_22904(0, 0.125, 0.25);
                poseStack.method_22907(class_7833.field_40716.rotationDegrees(180));
            }
        }

        super.submitItemStackRender(poseStack, bone, stack, displayContext, renderState, renderTasks, cameraState, packedLight, packedOverlay, renderColor);
        poseStack.method_22909();
    }
}
