package net.teogemini.nova.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class NovaWorkSupport {
    private NovaWorkSupport() {}

    public static void faceTarget(NovaEntity nova, BlockPos pos) {
        float target = (float) (Math.atan2(pos.getZ() + .5 - nova.getZ(),
                pos.getX() + .5 - nova.getX()) * 180.0 / Math.PI) - 90;
        float yaw = net.minecraft.util.Mth.approachDegrees(nova.getYRot(), target, 8);
        nova.setYRot(yaw);
        nova.setYBodyRot(yaw);
    }

    public static boolean canReach(NovaEntity nova, BlockPos pos) {
        if (nova.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) > 12.25) return false;
        BlockHitResult hit = nova.level().clip(new ClipContext(nova.getEyePosition(), Vec3.atCenterOf(pos),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, nova));
        return hit.getType() != HitResult.Type.BLOCK || hit.getBlockPos().equals(pos);
    }

    public static boolean isTool(ItemStack stack) {
        return stack.is(ItemTags.AXES) || stack.is(ItemTags.PICKAXES)
                || stack.is(ItemTags.HOES) || stack.is(ItemTags.SHOVELS)
                || stack.is(ItemTags.SWORDS);
    }

    public static boolean isSupply(ItemStack stack) {
        return stack.is(ItemTags.SAPLINGS) || stack.is(Items.WHEAT_SEEDS)
                || stack.is(Items.BEETROOT_SEEDS) || stack.is(Items.CARROT)
                || stack.is(Items.POTATO) || stack.is(Items.TORCH);
    }

    public static void message(Player player, String text) {
        player.displayClientMessage(Component.literal("NOVA: " + text), true);
    }

    public static InteractionResult interact(NovaEntity nova, Player player, InteractionHand hand) {
        if (!nova.isTame() || !nova.isOwnedBy(player) || player.isSpectator()
                || hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        // Keep the existing Shift + redstone celebration shortcut.
        if (held.is(Items.REDSTONE) && player.isShiftKeyDown()) return InteractionResult.PASS;
        boolean tool = isTool(held), supply = isSupply(held);
        if (!(tool || supply || held.is(Items.COMPASS) || held.is(Items.LIGHTNING_ROD)
                || held.is(Items.EMERALD) || held.is(Items.BELL) || held.is(Items.IRON_NUGGET)
                || held.is(Items.REDSTONE))) {
            return InteractionResult.PASS;
        }
        if (!(nova.level() instanceof ServerLevel)) return InteractionResult.SUCCESS;
        if (held.is(Items.REDSTONE)) {
            if (nova.work().recharge()) {
                if (!player.getAbilities().instabuild) held.shrink(1);
                message(player, "Đã sạc pin: " + (nova.work().energy() / 10) + "%.");
            } else {
                message(player, "Pin đã đầy.");
            }
        } else if (held.is(Items.COMPASS)) {
            NovaChestHandler.select(nova, player);
        } else if (held.is(Items.LIGHTNING_ROD)) {
            NovaChestHandler.toggleCarry(nova, player);
        } else if (held.is(Items.EMERALD)) {
            nova.work().setEnabled(!nova.work().enabled());
            nova.clearWorkAnimation();
            nova.getNavigation().stop();
            if (nova.work().enabled()) nova.setNovaMoveMode(NovaEntity.MODE_FREE);
            message(player, nova.work().enabled() ? "Bật làm việc trong vùng rương đã liên kết."
                    : "Đã tắt làm việc.");
        } else if (held.is(Items.BELL)) {
            NovaRallyManager.toggle(nova, player);
        } else if (held.is(Items.IRON_NUGGET)) {
            if (player.isShiftKeyDown()) {
                returnEquipment(nova, player, EquipmentSlot.MAINHAND);
                returnEquipment(nova, player, EquipmentSlot.OFFHAND);
                nova.clearWorkAnimation();
            } else {
                net.teogemini.nova.screen.NovaMenus.open(player, nova);
            }
        } else {
            if (nova.isCarryingChest()) {
                message(player, "Đặt rương xuống trước khi đổi công cụ nhé.");
                return InteractionResult.SUCCESS;
            }
            EquipmentSlot slot = tool ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
            ItemStack replacement = held.copyWithCount(tool ? 1 : held.getCount());
            if (!player.getAbilities().instabuild) held.shrink(replacement.getCount());
            returnEquipment(nova, player, slot);
            nova.setItemSlot(slot, replacement);
            nova.setGuaranteedDrop(slot);
            nova.clearWorkAnimation();
        }
        return InteractionResult.SUCCESS;
    }

    private static void returnEquipment(NovaEntity nova, Player player, EquipmentSlot slot) {
        ItemStack previous = nova.getItemBySlot(slot).copy();
        nova.setItemSlot(slot, ItemStack.EMPTY);
        if (!previous.isEmpty() && !player.getInventory().add(previous)) player.drop(previous, false);
    }

    public static void collectNearby(NovaEntity nova) {
        if (!(nova.level() instanceof ServerLevel) || !nova.isTame()) return;
        for (ItemEntity drop : nova.level().getEntitiesOfClass(ItemEntity.class,
                nova.getBoundingBox().inflate(1.25), item -> item.isAlive() && !item.hasPickUpDelay())) {
            ItemStack remainder = nova.work().inventory.addItem(drop.getItem().copy());
            if (remainder.isEmpty()) drop.discard();
            else drop.setItem(remainder);
        }
    }

    public static boolean breakBlock(NovaEntity nova, BlockPos pos) {
        if (!(nova.level() instanceof ServerLevel level) || !nova.canNovaWork()
                || !nova.isWithinWorkspace(pos) || !level.hasChunkAt(pos)
                || !level.getGameRules().get(GameRules.MOB_GRIEFING)) return false;
        BlockState original = level.getBlockState(pos);
        ItemStack tool = nova.getMainHandItem();
        if (original.isAir() || original.getDestroySpeed(level, pos) < 0
                || original.hasBlockEntity()
                || (original.requiresCorrectToolForDrops() && !tool.isCorrectToolForDrops(original))
                || nova.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) > 12.25) return false;
        BlockHitResult hit = level.clip(new ClipContext(nova.getEyePosition(), Vec3.atCenterOf(pos),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, nova));
        if (hit.getType() == HitResult.Type.BLOCK && !hit.getBlockPos().equals(pos)) return false;
        var drops = Block.getDrops(original, level, pos, null, nova, tool);
        if (!level.destroyBlock(pos, false, nova)) return false;
        for (ItemStack drop : drops) {
            ItemStack remainder = nova.work().inventory.addItem(drop.copy());
            if (!remainder.isEmpty()) Block.popResource(level, pos, remainder);
        }
        if (!tool.isEmpty()) tool.hurtAndBreak(1, nova, EquipmentSlot.MAINHAND);
        return true;
    }

    public static void dropCargo(NovaEntity nova, ServerLevel level) {
        for (int slot = 0; slot < nova.work().inventory.getContainerSize(); slot++) {
            ItemStack stack = nova.work().inventory.removeItemNoUpdate(slot);
            if (!stack.isEmpty()) nova.spawnAtLocation(level, stack);
        }
        for (int slot = 0; slot < nova.work().spareTools.getContainerSize(); slot++) {
            ItemStack stack = nova.work().spareTools.removeItemNoUpdate(slot);
            if (!stack.isEmpty()) nova.spawnAtLocation(level, stack);
        }
        ItemStack chest = nova.work().carriedChest().copy();
        nova.work().setCarriedChest(ItemStack.EMPTY);
        if (!chest.isEmpty()) nova.spawnAtLocation(level, chest);
    }
}
