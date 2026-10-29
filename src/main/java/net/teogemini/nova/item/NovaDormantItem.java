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

public class NovaDormantItem extends Item {
    public NovaDormantItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        BlockPos clickedPos = context.getClickedPos();
        Direction direction = context.getClickedFace();
        BlockPos spawnPos = clickedPos.relative(direction);

        NovaEntity nova = ModEntities.NOVA.create(serverLevel, EntitySpawnReason.SPAWN_ITEM_USE);
        if (nova != null) {
            float yaw = context.getPlayer() != null ? context.getPlayer().getYRot() + 180.0F : 0.0F;
            nova.snapTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, yaw, 0.0F);
            nova.setYHeadRot(yaw);
            nova.setYBodyRot(yaw);
            nova.setDormant(true);
            nova.setDormantPose(0);
            nova.work().setEnergy(0);

            ItemStack stack = context.getItemInHand();
            if (stack.has(DataComponents.CUSTOM_NAME)) {
                nova.setCustomName(stack.get(DataComponents.CUSTOM_NAME));
                nova.setCustomNameVisible(true);
            }

            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            if (customData != null) {
                CompoundTag tag = customData.copyTag();
                ValueInput input = TagValueInput.create(ProblemReporter.DISCARDING, serverLevel.registryAccess(), tag);
                nova.load(input);
                // Ensure it stays dormant with 0 energy at spawn location
                nova.setDormant(true);
                nova.setDormantPose(0);
                nova.work().setEnergy(0);
                nova.snapTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, yaw, 0.0F);
                nova.setYHeadRot(yaw);
                nova.setYBodyRot(yaw);
            }

            serverLevel.addFreshEntity(nova);
            serverLevel.playSound(null, spawnPos, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.5F, 1.2F);

            if (context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
                stack.shrink(1);
            }
            return InteractionResult.CONSUME;
        }

        return InteractionResult.FAIL;
    }
}
