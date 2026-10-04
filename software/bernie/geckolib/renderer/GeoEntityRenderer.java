package software.bernie.geckolib.renderer;

import net.minecraft.class_10017;
import net.minecraft.class_10042;
import net.minecraft.class_10442;
import net.minecraft.class_11659;
import net.minecraft.class_12075;
import net.minecraft.class_12249;
import net.minecraft.class_1297;
import net.minecraft.class_1299;
import net.minecraft.class_1304;
import net.minecraft.class_1308;
import net.minecraft.class_1309;
import net.minecraft.class_1628;
import net.minecraft.class_1657;
import net.minecraft.class_1747;
import net.minecraft.class_1799;
import net.minecraft.class_1921;
import net.minecraft.class_2190;
import net.minecraft.class_2350;
import net.minecraft.class_2561;
import net.minecraft.class_270;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_4050;
import net.minecraft.class_4587;
import net.minecraft.class_4608;
import net.minecraft.class_5617;
import net.minecraft.class_7833;
import net.minecraft.class_7923;
import net.minecraft.class_811;
import net.minecraft.class_897;
import net.minecraft.class_922;
import net.minecraft.class_9334;
import net.minecraft.class_970;
import net.minecraft.class_9848;
import net.minecraft.world.entity.*;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;
import software.bernie.geckolib.GeckoLibClientServices;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.base.GeoRenderState;
import software.bernie.geckolib.renderer.base.GeoRenderer;
import software.bernie.geckolib.renderer.base.RenderPassInfo;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayersContainer;
import software.bernie.geckolib.util.ClientUtil;
import software.bernie.geckolib.util.MiscUtil;

import java.util.List;
import java.util.function.Function;

/**
 * Base {@link GeoRenderer} class for rendering {@link class_1297 Entities} specifically
 * <p>
 * All entities added to be rendered by GeckoLib should use an instance of this class
 * <p>
 * This also includes {@link net.minecraft.class_1676 Projectiles}
 *
 * @param <T> Entity animatable class type
 * @param <R> RenderState class type
 */
public class GeoEntityRenderer<T extends class_1297 & GeoAnimatable, R extends class_10017 & GeoRenderState> extends class_897<T, R> implements GeoRenderer<T, Void, R> {
	protected final GeoRenderLayersContainer<T, Void, R> renderLayers = new GeoRenderLayersContainer<>(this);
	protected final GeoModel<T> model;
	protected final class_10442 itemModelResolver;

	protected float scaleWidth = 1;
	protected float scaleHeight = 1;

    /**
     * Creates a new defaulted renderer instance, using the entity's registered id as the file name for its assets
     */
    public GeoEntityRenderer(class_5617.class_5618 context, class_1299<? extends T> entityType) {
        this(context, new DefaultedEntityGeoModel<>(class_7923.field_41177.method_10221(entityType)));
    }

	public GeoEntityRenderer(class_5617.class_5618 context, GeoModel<T> model) {
		super(context);

		this.model = model;
		this.itemModelResolver = context.method_65566();
	}

	/**
	 * Get the maximum distance (in blocks) that an entity's nameplate should be visible when it is sneaking
	 * <p>
	 * This is only a short-circuit predicate, and other conditions after this check must be also passed in order for the name to render
	 * <p>
	 * This is hard-capped at a maximum of 256 blocks regardless of what this method returns
	 */
	public double getNameRenderCutoffDistance(T animatable) {
		return 32d;
	}

    /**
     * Returns the max rotation value for dying entities
     * <p>
     * You might want to modify this for different aesthetics, such as a {@link class_1628} flipping upside down on death
     * <p>
     * Functionally equivalent to {@code LivingEntityRenderer#getFlipDegrees}
     */
    protected float getDeathMaxRotation(GeoRenderState renderState) {
        return 90f;
    }

    /**
     * Makes a covariant variable of the given {@link GeoRenderState} and {@link class_10042}
     * (Essentially a variable that is <b>both</b> types) for ease of use
     * <p>
     * Because of the lack of extensibility in covariant return types, a new version of this method needs to be made
     * for any other covariant combination
     *
     * @param renderState The base GeoRenderState to cast
     * @return The GeoRenderState cast <i>additively</i> as a LivingEntityRenderState
     */
    @SuppressWarnings("unchecked")
    protected <S extends class_10042 & GeoRenderState> S convertRenderStateToLiving(R renderState) {
        return (S)renderState;
    }

    //<editor-fold defaultstate="collapsed" desc="<Internal Methods>">
    /**
     * Gets the model instance for this renderer
     */
    @Override
    public GeoModel<T> getGeoModel() {
        return this.model;
    }

    /**
     * Returns the list of registered {@link GeoRenderLayer GeoRenderLayers} for this renderer
     */
    @Override
    public List<GeoRenderLayer<T, Void, R>> getRenderLayers() {
        return this.renderLayers.getRenderLayers();
    }

    /**
     * Adds a {@link GeoRenderLayer} to this renderer, to be called after the main model is rendered each frame
     */
    @SuppressWarnings("UnusedReturnValue")
    public GeoEntityRenderer<T, R> withRenderLayer(Function<? super GeoEntityRenderer<T, R>, GeoRenderLayer<T, Void, R>> renderLayer) {
        return withRenderLayer(renderLayer.apply(this));
    }

    /**
     * Adds a {@link GeoRenderLayer} to this renderer, to be called after the main model is rendered each frame
     */
    public GeoEntityRenderer<T, R> withRenderLayer(GeoRenderLayer<T, Void, R> renderLayer) {
        this.renderLayers.addLayer(renderLayer);

        return this;
    }

    /**
     * Sets a scale override for this renderer, telling GeckoLib to pre-scale the model
     */
    public GeoEntityRenderer<T, R> withScale(float scale) {
        return withScale(scale, scale);
    }

    /**
     * Sets a scale override for this renderer, telling GeckoLib to pre-scale the model
     */
    public GeoEntityRenderer<T, R> withScale(float scaleWidth, float scaleHeight) {
        this.scaleWidth = scaleWidth;
        this.scaleHeight = scaleHeight;

        return this;
    }

    /**
     * Gets the id that represents the current animatable's instance for animation purposes.
     *
     * @param animatable The Animatable instance being renderer
     */
    @ApiStatus.OverrideOnly
    @Override
    public long getInstanceId(T animatable, @Nullable Void ignored) {
        return animatable.method_5628();
    }

    /**
     * Gets a tint-applying color to render the given animatable with
     * <p>
     * Returns opaque white by default, modified for invisibility in spectator
     */
    @Override
    public int getRenderColor(T animatable, @Nullable Void relatedObject, float partialTick) {
        int color = GeoRenderer.super.getRenderColor(animatable, relatedObject, partialTick);
        class_1657 player = ClientUtil.getClientPlayer();

        if (animatable.method_5767() && player != null && !animatable.method_5756(player))
            color = class_9848.method_61330(class_3532.method_15386(class_9848.method_61320(color) * 38 / 255f), color);

        return color;
    }

    /**
     * Gets a packed overlay coordinate pair for rendering
     * <p>
     * Mostly just used for the red tint when an entity is hurt,
     * but can be used for other things like the {@link net.minecraft.class_1548}
     * white tint when exploding.
     */
    @Override
    public int getPackedOverlay(T animatable, @Nullable Void relatedObject, float u, float partialTick) {
        if (!(animatable instanceof class_1309 entity))
            return class_4608.field_21444;

        return class_4608.method_23625(class_4608.method_23210(u),
                                   class_4608.method_23212(entity.field_6235 > 0 || entity.field_6213 > 0));
    }

    /**
     * Whether the entity's nametag should be rendered or not
     * <p>
     * Used to determine nametag attachment in {@link class_897#method_62354(class_1297, class_10017, float)}
     */
    @Override
    public boolean method_3921(T animatable, double distToCameraSq) {
        if (!(animatable instanceof class_1309))
            return super.method_3921(animatable, distToCameraSq);

        if (animatable.method_21751()) {
            double nameRenderCutoff = getNameRenderCutoffDistance(animatable);

            if (distToCameraSq >= nameRenderCutoff * nameRenderCutoff)
                return false;
        }

        if (animatable instanceof class_1308 && (!animatable.method_5733() && (!animatable.method_16914() || animatable != this.field_4676.field_4678)))
            return false;

        final class_310 minecraft = class_310.method_1551();
        final class_1657 player = ClientUtil.getClientPlayer();
        boolean visibleToClient = player != null && !animatable.method_5756(player);
        class_270 entityTeam = animatable.method_5781();

        if (player == null || entityTeam == null)
            return class_310.method_1498() && animatable != minecraft.method_1560() && visibleToClient && !animatable.method_5782();

        class_270 playerTeam = ClientUtil.getClientPlayer().method_5781();

        return switch (entityTeam.method_1201()) {
            case field_1442 -> visibleToClient;
            case field_1443 -> false;
            case field_1444 -> playerTeam == null ? visibleToClient : entityTeam.method_1206(playerTeam) && (entityTeam.method_1199() || visibleToClient);
            case field_1446 -> playerTeam == null ? visibleToClient : !entityTeam.method_1206(playerTeam) && visibleToClient;
        };
    }

    /**
     * Calculate the yaw of the given animatable.
     * <p>
     * Normally only called for non-{@link class_1309 LivingEntities}, and shouldn't be considered a safe place to modify rotation<br>
     * Do that in {@link software.bernie.geckolib.renderer.base.GeoRendererInternals#addRenderData(GeoAnimatable, Object, GeoRenderState, float)} instead
     */
    protected float calculateYRot(T animatable, float yHeadRot, float partialTick) {
        if (!(animatable.method_5854() instanceof class_1309 vehicle))
            return animatable instanceof class_1309 livingEntity ? class_3532.method_17821(partialTick, livingEntity.field_6220, livingEntity.field_6283) : animatable.method_73188();

        float vehicleRotation = class_3532.method_17821(partialTick, vehicle.field_6220, vehicle.field_6283);
        float clampedVehicleRotation = class_3532.method_15363(class_3532.method_15393(-vehicleRotation), -85, 85);
        vehicleRotation = yHeadRot - vehicleRotation;

        if (Math.abs(clampedVehicleRotation) > 50)
            vehicleRotation += clampedVehicleRotation * 0.2f;

        return vehicleRotation;
    }

    /**
     * Gets the {@link class_1921} to render the current render pass with
     * <p>
     * Uses the {@link class_12249#method_75994} {@code RenderType} by default
     * <p>
     * Override this to change the way a model will render (such as translucent models, etc.)
     *
     * @return Return the RenderType to use, or null to prevent the model rendering. Returning null will not prevent animation functions from taking place
     */
    @Override
    public @Nullable class_1921 getRenderType(R renderState, class_2960 texture) {
        if (renderState.field_53333 && !renderState.getOrDefaultGeckolibData(DataTickets.INVISIBLE_TO_PLAYER, false))
            return class_12249.method_75998(texture);

        if (!renderState.field_53333)
            return GeoRenderer.super.getRenderType(renderState, texture);

        return renderState.method_72997() ? class_12249.method_76018(texture) : null;
    }

    /**
     * Internal method for capturing the common RenderState data for all animatable objects
     */
    @ApiStatus.Internal
    @Override
    public void captureDefaultRenderState(T animatable, @Nullable Void relatedObject, R renderState, float partialTick) {
        GeoRenderer.super.captureDefaultRenderState(animatable, relatedObject, renderState, partialTick);

        renderState.addGeckolibData(DataTickets.VELOCITY, animatable.method_18798());
        renderState.addGeckolibData(DataTickets.BLOCKPOS, animatable.method_24515());
        renderState.addGeckolibData(DataTickets.SPRINTING, animatable.method_5624());
        renderState.addGeckolibData(DataTickets.IS_CROUCHING, animatable.method_18276());
        renderState.addGeckolibData(DataTickets.POSITION, animatable.method_73189());
        renderState.addGeckolibData(DataTickets.IS_MOVING, (animatable instanceof class_1309 livingEntity ? livingEntity.field_42108.method_48566() : animatable.method_18798().method_1027()) >= getMotionAnimThreshold(animatable));

        if (animatable instanceof class_1309 livingEntity) {
            renderState.addGeckolibData(DataTickets.SWINGING_ARM, livingEntity.field_6252);
            renderState.addGeckolibData(DataTickets.IS_DEAD_OR_DYING, livingEntity.method_29504());
        }

        if (!(renderState instanceof class_10042)) {
            renderState.addGeckolibData(DataTickets.INVISIBLE_TO_PLAYER, animatable.method_5767() && (ClientUtil.getClientPlayer() == null || animatable.method_5756(ClientUtil.getClientPlayer())));
            renderState.addGeckolibData(DataTickets.IS_SHAKING, animatable.method_32314());
            renderState.addGeckolibData(DataTickets.ENTITY_POSE, animatable.method_18376());
            renderState.addGeckolibData(DataTickets.ENTITY_PITCH, animatable.method_61414(partialTick));
            renderState.addGeckolibData(DataTickets.ENTITY_YAW, calculateYRot(animatable, 0, partialTick));
            renderState.addGeckolibData(DataTickets.ENTITY_BODY_YAW, renderState.getOrDefaultGeckolibData(DataTickets.ENTITY_YAW, 0f));
        }
    }

    /**
     * Scales the {@link class_4587} in preparation for rendering the model, excluding when re-rendering the model as part of a {@link GeoRenderLayer} or external render call
     * <p>
     * Override and call {@code super} with modified scale values as needed to further modify the scale of the model
     */
    @Override
    public void scaleModelForRender(RenderPassInfo<R> renderPassInfo, float widthScale, float heightScale) {
        float nativeScale = renderPassInfo.renderState() instanceof class_10042 livingRenderState ? livingRenderState.field_53453 : 1;

        GeoRenderer.super.scaleModelForRender(renderPassInfo, widthScale * this.scaleWidth * nativeScale, heightScale * this.scaleHeight * nativeScale);
    }

    /**
     * Transform the {@link class_4587} in preparation for rendering the model.
     * <p>
     * This is called after {@link #scaleModelForRender}, and so any transformations here will be scaled appropriately.
     * If you need to do pre-scale translations, use {@link #preRenderPass}
     * <p>
     * PoseStack translations made here are kept until the end of the render process
     */
    @Override
    public void adjustRenderPose(RenderPassInfo<R> renderPassInfo) {
        final R renderState = renderPassInfo.renderState();
        final class_10042 livingRenderState = renderState instanceof class_10042 state ? state : null;
        final class_4587 poseStack = renderPassInfo.poseStack();

        if (livingRenderState != null && renderState.getGeckolibData(DataTickets.ENTITY_POSE) == class_4050.field_18078) {
            class_2350 bedDirection = livingRenderState.field_53463;

            if (bedDirection != null) {
                float eyePosOffset = livingRenderState.field_53331 - 0.1F;

                poseStack.method_46416(-bedDirection.method_10148() * eyePosOffset, 0, -bedDirection.method_10165() * eyePosOffset);
            }
        }

        applyRotations(renderPassInfo, poseStack, livingRenderState != null ? livingRenderState.field_53453 : 1);
        poseStack.method_46416(0, 0.01f, 0);
    }

    /**
     * Initial access point for vanilla's {@link GeoEntityRenderer} class<br>
     * Immediately defers to {@link GeoRenderer#performRenderPass(GeoRenderState, class_4587, class_11659, class_12075)}
     */
    @ApiStatus.Internal
    @Override
    public void method_3936(R renderState, class_4587 poseStack, class_11659 renderTasks, class_12075 cameraState) {
        GeoRenderer.super.performRenderPass(renderState, poseStack, renderTasks, cameraState);
    }

    /**
     * Called after the rest of the render pass has completed, including discarding the PoseStack's pose.
     * <p>
     * The actual rendering of the object has not yet taken place, as that is done in a deferred {@link #performRenderPass submission}
     */
    @Override
    public void postRenderPass(RenderPassInfo<R> renderPassInfo, class_11659 renderTasks) {
        super.method_3936(renderPassInfo.renderState(), renderPassInfo.poseStack(), renderTasks, renderPassInfo.cameraState());
    }

    /**
     * Applies rotation transformations to the renderer prior to render time to account for various entity states
     */
    protected void applyRotations(RenderPassInfo<R> renderPassInfo, class_4587 poseStack, float nativeScale) {
        final R renderState = renderPassInfo.renderState();
        float rotationYaw = renderState.getOrDefaultGeckolibData(DataTickets.ENTITY_BODY_YAW, 0f);

        if (renderState.getOrDefaultGeckolibData(DataTickets.IS_SHAKING, false))
            rotationYaw += (float)(Math.cos(renderState.field_53328 * 3.25d) * Math.PI * 0.4d);

        boolean sleeping = renderState.getGeckolibData(DataTickets.ENTITY_POSE) == class_4050.field_18078;

        if (!sleeping)
            poseStack.method_22907(class_7833.field_40716.rotationDegrees(180f - rotationYaw));

        if (renderState instanceof class_10042 livingRenderState) {
            if (livingRenderState.field_53449 > 0) {
                poseStack.method_22907(class_7833.field_40718.rotationDegrees(Math.min(class_3532.method_15355((livingRenderState.field_53449 - 1f) / 20f * 1.6f), 1) * getDeathMaxRotation(renderState)));
            }
            else if (livingRenderState.field_53459) {
                poseStack.method_22907(class_7833.field_40714.rotationDegrees(-90f - livingRenderState.field_53448));
                poseStack.method_22907(class_7833.field_40716.rotationDegrees(renderState.field_53328 * -75f));
            }
            else if (sleeping) {
                class_2350 bedOrientation = livingRenderState.field_53463;

                poseStack.method_22907(class_7833.field_40716.rotationDegrees(bedOrientation != null ? MiscUtil.getDirectionAngle(bedOrientation) : rotationYaw));
                poseStack.method_22907(class_7833.field_40718.rotationDegrees(getDeathMaxRotation(renderState)));
                poseStack.method_22907(class_7833.field_40716.rotationDegrees(270f));
            }
            else if (livingRenderState.field_53455) {
                poseStack.method_46416(0, (livingRenderState.field_53330 + 0.1f) / nativeScale, 0);
                poseStack.method_22907(class_7833.field_40718.rotationDegrees(180f));
            }
        }
    }

    /**
     * Create the base (blank) {@link R renderState} instance for this renderer.
     * <p>
     * By default, it is an {@link class_10017}, or a {@link class_10042} if the entity is an instance of {@link class_1309}<br>
     * All EntityRenderStates of any kind are automatically {@link GeoRenderState}s
     * <p>
     * Override this if you want to utilize a different subclass of EntityRenderState
     */
    @SuppressWarnings("unchecked")
    @Override
    public R createRenderState(T animatable, @Nullable Void relatedObject) {
        return (R)(animatable instanceof class_1309 ? new class_10042() : new class_10017());
    }

    /**
     * Create the contextually relevant {@link class_10017} for the current render pass
     * <p>
     * GeckoLib also uses this to dynamically handle the default EntityRenderState setup
     * <p>
     * If overriding this for a custom RenderState, ensure you call {@code super} first
     */
    @ApiStatus.Internal
    @Override
    public final R method_62425(T entity, float partialTick) {
        R renderState = createRenderState(entity, null);

        method_62354(entity, renderState, partialTick);
        method_73154(entity, renderState);

        return renderState;
    }

    /**
     * Fill the EntityRenderState for the current render pass.
     * <p>
     * You should only be overriding this if you have extended the {@link R renderState} type.<br>
     * If you're just adding GeckoLib rendering data, you should be using {@link software.bernie.geckolib.renderer.base.GeoRendererInternals#addRenderData(GeoAnimatable, Object, GeoRenderState, float)} instead
     */
    @ApiStatus.OverrideOnly
    @Override
    public void method_62354(T entity, R entityRenderState, float partialTick) {
        super.method_62354(entity, entityRenderState, partialTick);

        if (entityRenderState instanceof class_10042 livingEntityRenderState)
            extractLivingEntityRenderState((class_1309)entity, livingEntityRenderState, partialTick, this.itemModelResolver);

        fillRenderState(entity, null, entityRenderState, partialTick);
    }

    /**
     * Replica of {@link class_922#method_62355(class_1309, class_10042, float)}.
     * <p>
     * This is only called if the entity for this renderer is a {@link class_1309}
     */
    protected void extractLivingEntityRenderState(class_1309 entity, class_10042 renderState, float partialTick, class_10442 itemModelResolver) {
        final class_310 minecraft = class_310.method_1551();
        final float lerpHeadYRot = class_3532.method_17821(partialTick, entity.field_6259, entity.field_6241);
        final class_2561 customName = entity.method_5797();
        final class_1799 helmetStack = entity.method_6118(class_1304.field_6169);

        renderState.field_53446 = class_922.method_62482(entity, lerpHeadYRot, partialTick);
        renderState.field_53447 = class_3532.method_15393(lerpHeadYRot - renderState.field_53446);
        renderState.field_53448 = entity.method_61414(partialTick);
        renderState.field_53455 = customName != null && class_922.method_74932(customName.getString());

        if (renderState.field_53455) {
            renderState.field_53448 *= -1;
            renderState.field_53447 *= -1;
        }

        if (!entity.method_5765() && entity.method_5805()) {
            renderState.field_53450 = entity.field_42108.method_48572(partialTick);
            renderState.field_53451 = entity.field_42108.method_48570(partialTick);
        }
        else {
            renderState.field_53450 = 0;
            renderState.field_53451 = 0;
        }

        if (entity.method_5854() instanceof class_1309 vehicle) {
            renderState.field_53452 = vehicle.field_42108.method_48572(partialTick);
        }
        else {
            renderState.field_53452 = renderState.field_53450;
        }

        renderState.field_53453 = entity.method_55693();
        renderState.field_53454 = entity.method_17825();
        renderState.field_53465 = entity.method_18376();
        renderState.field_53463 = entity.method_18401();

        if (renderState.field_53463 != null)
            renderState.field_53331 = entity.method_18381(class_4050.field_18076);

        renderState.field_53456 = entity.method_32314();
        renderState.field_53457 = entity.method_6109();
        renderState.field_53458 = entity.method_5799();
        renderState.field_53459 = entity.method_6123();
        renderState.field_53460 = entity.field_6235 > 0 || entity.field_6213 > 0;

        if (helmetStack.method_7909() instanceof class_1747 blockItem && blockItem.method_7711() instanceof class_2190 skullBlock) {
            renderState.field_55315 = skullBlock.method_9327();
            renderState.field_55316 = helmetStack.method_58694(class_9334.field_49617);
            renderState.field_53467.method_65605();
        }
        else {
            renderState.field_55315 = null;
            renderState.field_55316 = null;

            if (!class_970.method_64081(helmetStack, class_1304.field_6169)) {
                this.itemModelResolver.method_65597(renderState.field_53467, helmetStack, class_811.field_4316, entity);
            }
            else {
                renderState.field_53467.method_65605();
            }
        }

        renderState.field_53449 = entity.field_6213 > 0 ? (float)entity.field_6213 + partialTick : 0;
        renderState.field_53461 = renderState.field_53333 && minecraft.field_1724 != null && entity.method_5756(minecraft.field_1724);
    }

    /**
     * Create and fire the relevant {@code CompileLayers} event hook for this renderer
     */
    @Override
    public void fireCompileRenderLayersEvent() {
        GeckoLibClientServices.EVENTS.fireCompileEntityRenderLayers(this);
    }

    /**
     * Create and fire the relevant {@code CompileRenderState} event hook for this renderer
     */
    @Override
    public void fireCompileRenderStateEvent(T animatable, @Nullable Void relatedObject, R renderState, float partialTick) {
        GeckoLibClientServices.EVENTS.fireCompileEntityRenderState(this, renderState, animatable);
    }

    /**
     * Create and fire the relevant {@code Pre-Render} event hook for this renderer
     *
     * @return Whether the renderer should proceed based on the cancellation state of the event
     */
    @Override
    public boolean firePreRenderEvent(RenderPassInfo<R> renderPassInfo, class_11659 renderTasks) {
        return GeckoLibClientServices.EVENTS.fireEntityPreRender(renderPassInfo, renderTasks);
    }

    /**
     * @deprecated GeckoLib defers creation of this to allow for dynamic handling in {@link #method_62354(class_1297, class_10017, float)}
     */
    @Deprecated
    @ApiStatus.Internal
    @Override
    public final @Nullable R method_55269() {
        return null;
    }
    //</editor-fold>
}
