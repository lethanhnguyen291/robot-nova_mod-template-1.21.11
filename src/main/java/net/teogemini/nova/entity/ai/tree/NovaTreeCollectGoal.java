package net.teogemini.nova.entity.ai.tree;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.teogemini.nova.entity.NovaEntity;
import net.teogemini.nova.entity.NovaWorkSupport;

/**
 * Periodically (every 5-10 minutes) searches for dropped saplings, sticks, and apples
 * left from chopped trees and leaf decay, and actively walks around to pick them up into inventory.
 */
public final class NovaTreeCollectGoal extends Goal {
    private final NovaEntity nova;
    private final double speed;
    private ItemEntity target;
    private final Set<UUID> unreachable = new HashSet<>();
    private int ticks;
    private int stuckTicks;
    private double lastDistance = Double.MAX_VALUE;

    public NovaTreeCollectGoal(NovaEntity nova, double speed) {
        this.nova = nova;
        this.speed = speed;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public static boolean isTreeDebris(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.is(ItemTags.SAPLINGS) || stack.is(Items.STICK) || stack.is(Items.APPLE);
    }

    private boolean hasInventoryRoom() {
        for (int i = 0; i < nova.getInventory().getContainerSize(); i++) {
            ItemStack slot = nova.getInventory().getItem(i);
            if (slot.isEmpty() || (isTreeDebris(slot) && slot.getCount() < slot.getMaxStackSize())) {
                return true;
            }
        }
        return false;
    }

    private List<ItemEntity> findNearbyDebris() {
        AABB searchBox = nova.getBoundingBox().inflate(24.0, 8.0, 24.0);
        return nova.level().getEntitiesOfClass(ItemEntity.class, searchBox, item ->
                item.isAlive()
                && !item.hasPickUpDelay()
                && !unreachable.contains(item.getUUID())
                && isTreeDebris(item.getItem())
                && NovaTreeService.inside(nova, item.blockPosition())
        );
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public boolean canUse() {
        if (!nova.canStartNovaJob() || nova.work().energy() <= 100) return false;
        if (!nova.getPrimaryTool().is(ItemTags.AXES) || !nova.companion().usableTool(nova.getPrimaryTool())) return false;
        if (!hasInventoryRoom()) return false;

        long now = nova.level().getGameTime();
        if (now < nova.novaNextTreeCollectTime) return false;

        unreachable.clear();
        List<ItemEntity> items = findNearbyDebris();
        if (items.isEmpty()) {
            nova.novaNextTreeCollectTime = now + 1200;
            return false;
        }

        items.sort(Comparator.comparingDouble(nova::distanceToSqr));
        for (ItemEntity candidate : items) {
            var path = nova.getNavigation().createPath(candidate, 0);
            if (path != null && path.canReach()) {
                target = candidate;
                return true;
            }
            unreachable.add(candidate.getUUID());
        }

        nova.novaNextTreeCollectTime = now + 1200;
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return nova.canStartNovaJob()
                && nova.work().energy() > 100
                && ticks < 600
                && hasInventoryRoom()
                && (target != null && target.isAlive() || !findNearbyDebris().isEmpty());
    }

    @Override
    public void start() {
        ticks = 0;
        stuckTicks = 0;
        lastDistance = Double.MAX_VALUE;
        nova.clearWorkAnimation();
        if (target != null) {
            nova.getNavigation().moveTo(target, speed);
        }
    }

    @Override
    public void tick() {
        ticks++;
        if (!canContinueToUse()) {
            stop();
            return;
        }

        if (target == null || !target.isAlive() || unreachable.contains(target.getUUID())) {
            List<ItemEntity> remaining = findNearbyDebris();
            if (remaining.isEmpty()) {
                stop();
                return;
            }
            remaining.sort(Comparator.comparingDouble(nova::distanceToSqr));
            target = null;
            for (ItemEntity candidate : remaining) {
                var path = nova.getNavigation().createPath(candidate, 0);
                if (path != null && path.canReach()) {
                    target = candidate;
                    nova.getNavigation().moveTo(target, speed);
                    stuckTicks = 0;
                    lastDistance = Double.MAX_VALUE;
                    break;
                }
                unreachable.add(candidate.getUUID());
            }
            if (target == null) {
                stop();
                return;
            }
        }

        nova.getLookControl().setLookAt(target.getX(), target.getY() + 0.2, target.getZ(), 30.0F, 30.0F);

        double distSq = nova.distanceToSqr(target);
        if (distSq <= 2.56) {
            collect(target);
            vacuumSurrounding();
            target = null;
            return;
        }

        if (ticks % 10 == 0) {
            if (distSq >= lastDistance - 0.1) {
                if (++stuckTicks >= 6) {
                    unreachable.add(target.getUUID());
                    target = null;
                    stuckTicks = 0;
                    lastDistance = Double.MAX_VALUE;
                    return;
                }
            } else {
                stuckTicks = 0;
                lastDistance = distSq;
            }
            nova.getNavigation().moveTo(target, speed);
        }
    }

    private void collect(ItemEntity item) {
        if (!item.isAlive() || item.hasPickUpDelay()) return;
        ItemStack stack = item.getItem().copy();

        if (stack.is(ItemTags.SAPLINGS)) {
            ItemStack offhand = nova.getOffhandItem();
            if (offhand.isEmpty()) {
                int take = Math.min(stack.getCount(), stack.getMaxStackSize());
                nova.setItemSlot(EquipmentSlot.OFFHAND, stack.split(take));
                nova.setGuaranteedDrop(EquipmentSlot.OFFHAND);
            } else if (offhand.is(stack.getItem()) && offhand.getCount() < offhand.getMaxStackSize()) {
                int take = Math.min(offhand.getMaxStackSize() - offhand.getCount(), stack.getCount());
                offhand.grow(take);
                stack.shrink(take);
            }
        }

        if (!stack.isEmpty()) {
            ItemStack remainder = nova.getInventory().addItem(stack);
            if (remainder.isEmpty()) {
                item.discard();
            } else {
                item.setItem(remainder);
            }
        } else {
            item.discard();
        }
        nova.swing(InteractionHand.MAIN_HAND);
        nova.level().playSound(null, nova.getX(), nova.getY(), nova.getZ(),
                SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.4F, 1.2F);
    }

    private void vacuumSurrounding() {
        AABB vacuumBox = nova.getBoundingBox().inflate(2.5, 1.5, 2.5);
        List<ItemEntity> nearby = nova.level().getEntitiesOfClass(ItemEntity.class, vacuumBox, e ->
                e.isAlive() && !e.hasPickUpDelay() && isTreeDebris(e.getItem()));
        for (ItemEntity e : nearby) {
            collect(e);
        }
    }

    @Override
    public void stop() {
        nova.getNavigation().stop();
        target = null;
        unreachable.clear();
        nova.novaNextTreeCollectTime = nova.level().getGameTime() + 6000 + nova.getRandom().nextInt(6001);
    }
}
