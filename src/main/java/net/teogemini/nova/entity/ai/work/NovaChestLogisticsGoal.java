package net.teogemini.nova.entity.ai.work;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.teogemini.nova.entity.NovaChestHandler;
import net.teogemini.nova.entity.NovaEntity;

public final class NovaChestLogisticsGoal extends Goal {
    private final NovaEntity nova;
    private BlockPos target;
    private int cooldown, ticks, actionTicks;
    private boolean depositing;

    public NovaChestLogisticsGoal(NovaEntity nova) {
        this.nova = nova;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }
    // Compatibility helpers: data now belongs to the saved entity, not static maps.
    public static SimpleContainer getInventory(NovaEntity nova) { return nova.getInventory(); }
    public static BlockPos getLinkedChest(NovaEntity nova) { return nova.getLinkedChest(); }
    public static void setLinkedChest(NovaEntity nova, BlockPos pos) { nova.setLinkedChest(pos); }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override public boolean canUse() {
        if (!nova.canNovaWork() || --cooldown > 0) return false;
        cooldown = 60;
        target = nova.getLinkedChest();
        if (target == null) return false;
        Container chest = NovaChestHandler.linkedContainer(nova);
        if (chest == null) return false;
        int filled = 0;
        for (int i = 0; i < nova.getInventory().getContainerSize(); i++)
            if (!nova.getInventory().getItem(i).isEmpty()) filled++;
        depositing = filled >= nova.getInventory().getContainerSize() - 2;
        if (depositing) {
            for (int i = 0; i < nova.getInventory().getContainerSize(); i++) {
                ItemStack stack = nova.getInventory().getItem(i);
                if (roomFor(chest, stack)) return true;
            }
            return false;
        }
        return nova.getMainHandItem().is(ItemTags.HOES) && nova.getOffhandItem().isEmpty()
                && seedSlot(chest) >= 0;
    }

    @Override public boolean canContinueToUse() {
        return nova.canNovaWork() && target != null && target.equals(nova.getLinkedChest())
                && ticks < 400 && NovaChestHandler.linkedContainer(nova) != null;
    }
    @Override public void start() { ticks = 0; actionTicks = 0; }
    @Override public void tick() {
        ticks++;
        if (!canContinueToUse()) { stop(); return; }
        nova.getLookControl().setLookAt(target.getX() + .5, target.getY() + .5, target.getZ() + .5);
        if (nova.distanceToSqr(Vec3.atCenterOf(target)) > 6.25) {
            nova.clearWorkAnimation();
            if (ticks % 10 == 1) nova.getNavigation().moveTo(target.getX() + .5, target.getY(), target.getZ() + .5, 1.0);
            return;
        }
        nova.getNavigation().stop();
        if (!net.teogemini.nova.entity.NovaWorkSupport.canReach(nova, target)) { stop(); return; }
        net.teogemini.nova.entity.NovaWorkSupport.faceTarget(nova, target);
        nova.playWorkAnimation(NovaEntity.WORK_CHEST);
        if (++actionTicks < 40) return;
        Container chest = NovaChestHandler.linkedContainer(nova);
        if (chest != null) {
            if (depositing) {
                for (int i = 0; i < nova.getInventory().getContainerSize(); i++)
                    transfer(chest, nova.getInventory().getItem(i));
                nova.getInventory().setChanged();
            } else if (nova.getOffhandItem().isEmpty()) {
                int slot = seedSlot(chest);
                if (slot >= 0) {
                    nova.setItemSlot(EquipmentSlot.OFFHAND, chest.removeItem(slot, 32));
                    nova.setGuaranteedDrop(EquipmentSlot.OFFHAND);
                }
            }
            chest.setChanged();
        }
        stop();
    }
    @Override public void stop() {
        target = null;
        cooldown = 80;
        nova.getNavigation().stop();
        nova.clearWorkAnimation();
    }

    private static int seedSlot(Container chest) {
        for (int i = 0; i < chest.getContainerSize(); i++) {
            ItemStack stack = chest.getItem(i);
            if (stack.is(Items.WHEAT_SEEDS) || stack.is(Items.BEETROOT_SEEDS)
                    || stack.is(Items.CARROT) || stack.is(Items.POTATO)) return i;
        }
        return -1;
    }
    private static boolean roomFor(Container chest, ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (int i = 0; i < chest.getContainerSize(); i++) {
            if (!chest.canPlaceItem(i, stack)) continue;
            ItemStack current = chest.getItem(i);
            if (current.isEmpty() || ItemStack.isSameItemSameComponents(current, stack)
                    && current.getCount() < Math.min(current.getMaxStackSize(), chest.getMaxStackSize())) return true;
        }
        return false;
    }
    private static void transfer(Container chest, ItemStack stack) {
        for (int pass = 0; pass < 2 && !stack.isEmpty(); pass++) {
            for (int i = 0; i < chest.getContainerSize() && !stack.isEmpty(); i++) {
                if (!chest.canPlaceItem(i, stack)) continue;
                ItemStack current = chest.getItem(i);
                int limit = Math.min(stack.getMaxStackSize(), chest.getMaxStackSize());
                if (pass == 0 && !current.isEmpty() && ItemStack.isSameItemSameComponents(current, stack)) {
                    int count = Math.min(stack.getCount(), Math.max(0, limit - current.getCount()));
                    current.grow(count);
                    stack.shrink(count);
                } else if (pass == 1 && current.isEmpty()) {
                    chest.setItem(i, stack.split(Math.min(limit, stack.getCount())));
                }
            }
        }
    }
}
