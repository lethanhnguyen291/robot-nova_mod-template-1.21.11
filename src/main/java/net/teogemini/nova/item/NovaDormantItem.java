package net.teogemini.nova.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.teogemini.nova.entity.NovaEntity;
import net.teogemini.nova.registry.ModEntities;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;
import java.util.function.Consumer;

public class NovaDormantItem extends Item implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public static Consumer<Consumer<GeoRenderProvider>> RENDER_PROVIDER;

    public NovaDormantItem(Properties properties) {
        super(properties.stacksTo(16));
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        if (RENDER_PROVIDER != null) {
            RENDER_PROVIDER.accept(consumer);
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level=context.getLevel();
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        if (context.getPlayer()!=null&&context.getPlayer().isSpectator()) return InteractionResult.FAIL;
        BlockPos clicked=context.getClickedPos();
        BlockPos pos=level.getBlockState(clicked).canBeReplaced()?clicked:clicked.relative(context.getClickedFace());
        if (!level.isInWorldBounds(pos)||!level.hasChunkAt(pos)) return InteractionResult.FAIL;
        NovaEntity nova=ModEntities.NOVA.create(server,EntitySpawnReason.SPAWN_ITEM_USE);
        if (nova==null) return InteractionResult.FAIL;
        UUID freshUuid=nova.getUUID();
        ItemStack stack=context.getItemInHand();
        CustomData saved=stack.get(DataComponents.CUSTOM_DATA);
        if (saved!=null) {
            CompoundTag tag=saved.copyTag();
            for (String key:new String[]{"UUID","NoAI","Pos","Motion","Rotation","Passengers","Leash"}) tag.remove(key);
            nova.load(TagValueInput.create(ProblemReporter.DISCARDING,server.registryAccess(),tag));
            nova.setUUID(freshUuid);
        }
        if (stack.has(DataComponents.CUSTOM_NAME)) {nova.setCustomName(stack.get(DataComponents.CUSTOM_NAME));nova.setCustomNameVisible(true);}
        float yaw=context.getHorizontalDirection().getOpposite().toYRot();
        nova.snapTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,yaw,0);
        nova.setYRot(yaw);nova.yRotO=yaw;nova.setYHeadRot(yaw);nova.yHeadRotO=yaw;
        nova.setYBodyRot(yaw);nova.yBodyRotO=yaw;nova.setXRot(0);nova.xRotO=0;
        nova.setNoAi(false);nova.setDormant(true);
        if (saved==null) nova.setDormantPose(0);
        nova.work().setEnergy(0);nova.setPersistenceRequired();
        if (!server.getWorldBorder().isWithinBounds(nova.getBoundingBox())||!server.noCollision(nova)||!server.addFreshEntity(nova)) {
            nova.discard();
            if (context.getPlayer()!=null) context.getPlayer().displayClientMessage(net.minecraft.network.chat.Component.translatable("message.robot-nova_mod.no_space"),true);
            return InteractionResult.FAIL;
        }
        nova.sounds().play(net.teogemini.nova.sound.NovaSounds.Cue.DORMANT_PLACE);
        server.playSound(null,pos,SoundEvents.IRON_PLACE,SoundSource.BLOCKS,.25F,1F);
        server.gameEvent(context.getPlayer(),net.minecraft.world.level.gameevent.GameEvent.BLOCK_PLACE,pos);
        if (context.getPlayer()!=null&&!context.getPlayer().getAbilities().instabuild) stack.shrink(1);
        return InteractionResult.CONSUME;
    }
}
