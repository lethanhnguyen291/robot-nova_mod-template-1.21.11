package net.teogemini.nova.entity.ai.tree;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.teogemini.nova.entity.NovaEntity;
import net.teogemini.nova.entity.NovaExperienceManager;
import net.teogemini.nova.entity.companion.NovaCompanion;

/** A narrow exception for a reserved tree; ordinary mining reach remains unchanged. */
public final class NovaTreeHarvest {
    public enum Result { BROKEN, GONE, STOP }
    private NovaTreeHarvest() {}
    public static boolean fits(List<ItemStack> inventory,List<ItemStack> incoming) {
        List<ItemStack> test=inventory.stream().map(ItemStack::copy).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        for (ItemStack stack:incoming) {
            int remaining=stack.getCount();
            for (ItemStack stored:test) {
                if (ItemStack.isSameItemSameComponents(stored,stack)) {
                    int added=Math.min(remaining,Math.max(0,Math.min(64,stored.getMaxStackSize())-stored.getCount()));
                    stored.grow(added);remaining-=added;
                }
            }
            for(int i=0;i<test.size() && remaining>0;i++) if(test.get(i).isEmpty()) {
                int added=Math.min(remaining,Math.min(64,stack.getMaxStackSize()));test.set(i,stack.copyWithCount(added));remaining-=added;
            }
            if(remaining>0)return false;
        }
        return true;
    }
    public static Result fell(NovaEntity nova,NovaTreePlan plan,BlockPos pos) {
        if (!(nova.level() instanceof ServerLevel level) || !nova.canNovaWork() || nova.work().energy()<=100
                || !nova.getMainHandItem().is(ItemTags.AXES) || !nova.companion().usableTool(nova.getMainHandItem())
                || !plan.logs().contains(pos) || !NovaTreeService.inside(nova,pos)
                || nova.trees().protectedTree(level.dimension(),plan)
                || nova.getEyePosition().distanceToSqr(Vec3.atCenterOf(plan.root()))>16
                || !NovaTreeService.claim(nova,plan)) return Result.STOP;
        var state=NovaTreeService.read(level,pos);
        if(state==null)return Result.STOP;
        if(!state.is(plan.species().log))return Result.GONE;
        if(state.hasBlockEntity() || state.getDestroySpeed(level,pos)<0)return Result.STOP;
        var tool=nova.getMainHandItem();
        var drops=Block.getDrops(state,level,pos,null,nova,tool);
        if(!fits(nova.getInventory().getItems(),drops)) {
            nova.companion().notifyOwner(NovaCompanion.Notice.FULL,"Ba lô không đủ chỗ. Mình giữ lại cây để làm tiếp sau khi cất đồ.");
            return Result.STOP;
        }
        if(!level.destroyBlock(pos,false,nova))return Result.STOP;
        for(var drop:drops) {
            var remainder=nova.getInventory().addItem(drop.copy());
            if(!remainder.isEmpty())Block.popResource(level,pos,remainder);
        }
        tool.hurtAndBreak(1,nova,EquipmentSlot.MAINHAND);
        nova.work().spendTreeEnergy();
        nova.companion().worked(state);
        NovaExperienceManager.awardLumberjackExp(nova);
        return Result.BROKEN;
    }
}
