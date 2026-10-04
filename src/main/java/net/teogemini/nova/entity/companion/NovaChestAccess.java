package net.teogemini.nova.entity.companion;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.teogemini.nova.entity.NovaEntity;
import net.teogemini.nova.registry.ModBlocks;

/** Only the linked chest and explicitly permitted deposits are eligible. No world-wide scans. */
public final class NovaChestAccess {
    private NovaChestAccess() {}
    public static boolean chestBlock(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(ModBlocks.NOVA_CHEST) || state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST);
    }
    public static boolean sameChest(NovaEntity nova, BlockPos first, BlockPos other) {
        if (first.equals(other)) return true;
        if (!nova.level().hasChunkAt(first)) return false;
        var state=nova.level().getBlockState(first);
        return chestBlock(state) && state.getValue(ChestBlock.TYPE)!=ChestType.SINGLE
                && first.relative(ChestBlock.getConnectedDirection(state)).equals(other);
    }
    public static boolean authorized(NovaEntity nova, BlockPos pos) {
        if (pos.equals(nova.getLinkedChest())) return true;
        return nova.companion().memories().deposits().contains(net.minecraft.core.GlobalPos.of(nova.level().dimension(),pos));
    }
    public static Container container(NovaEntity nova, BlockPos pos) {
        if (pos==null || !authorized(nova,pos) || !nova.level().hasChunkAt(pos)
                || !nova.level().getWorldBorder().isWithinBounds(pos)) return null;
        var state=nova.level().getBlockState(pos);
        if (!chestBlock(state)) return null;
        if (!(nova.level().getBlockEntity(pos) instanceof ChestBlockEntity first) || first.isLocked()) return null;
        if (state.getValue(ChestBlock.TYPE)!=ChestType.SINGLE
                && !nova.level().hasChunkAt(pos.relative(ChestBlock.getConnectedDirection(state)))) return null;
        if (state.getValue(ChestBlock.TYPE)!=ChestType.SINGLE
                && (!(nova.level().getBlockEntity(pos.relative(ChestBlock.getConnectedDirection(state))) instanceof ChestBlockEntity second)
                    || second.isLocked())) return null;
        return ChestBlock.getContainer((ChestBlock)state.getBlock(),state,nova.level(),pos,false);
    }
    public static boolean busy(NovaEntity nova, BlockPos pos) {
        int own=nova.companion().hasOpenChest(pos) ? 1 : 0;
        if (ChestBlockEntity.getOpenCount(nova.level(),pos)>own) return true;
        var state=nova.level().getBlockState(pos);
        return chestBlock(state) && state.getValue(ChestBlock.TYPE)!=ChestType.SINGLE
                && ChestBlockEntity.getOpenCount(nova.level(),pos.relative(ChestBlock.getConnectedDirection(state)))>own;
    }
    public static BlockPos depositTarget(NovaEntity nova) {
        List<BlockPos> candidates=new ArrayList<>();
        if (nova.getLinkedChest()!=null) candidates.add(nova.getLinkedChest());
        for (var saved:nova.companion().memories().deposits())
            if (saved.dimension().equals(nova.level().dimension()) && !candidates.contains(saved.pos())) candidates.add(saved.pos());
        candidates.sort(java.util.Comparator.comparingDouble(p -> p.distSqr(nova.blockPosition())));
        for (BlockPos pos:candidates) {
            if (Math.abs(pos.getX()-nova.getBlockX())>32 || Math.abs(pos.getZ()-nova.getBlockZ())>32
                    || Math.abs(pos.getY()-nova.getBlockY())>8 || !nova.level().hasChunkAt(pos)
                    || nova.companion().retry.blocked(pos,nova.level().getGameTime())) continue;
            Container chest=container(nova,pos);
            if (chest==null || busy(nova,pos)) continue;
            for (int i=0;i<nova.getInventory().getContainerSize();i++)
                if (roomFor(chest,nova.getInventory().getItem(i))) return pos;
            nova.companion().retry.fail(pos,nova.level().getGameTime());
        }
        return null;
    }
    public static boolean roomFor(Container chest, ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (int i=0;i<chest.getContainerSize();i++) {
            if (!chest.canPlaceItem(i,stack)) continue;
            var current=chest.getItem(i);
            if (current.isEmpty() || ItemStack.isSameItemSameComponents(current,stack)
                    && current.getCount()<Math.min(stack.getMaxStackSize(),chest.getMaxStackSize())) return true;
        }
        return false;
    }
    public static int transfer(Container chest, ItemStack stack) {
        int before=stack.getCount();
        for (int pass=0;pass<2 && !stack.isEmpty();pass++)
            for (int i=0;i<chest.getContainerSize() && !stack.isEmpty();i++) {
                if (!chest.canPlaceItem(i,stack)) continue;
                var current=chest.getItem(i);
                int limit=Math.min(stack.getMaxStackSize(),chest.getMaxStackSize());
                if (pass==0 && !current.isEmpty() && ItemStack.isSameItemSameComponents(current,stack)) {
                    int count=Math.min(stack.getCount(),Math.max(0,limit-current.getCount()));
                    current.grow(count); stack.shrink(count);
                } else if (pass==1 && current.isEmpty()) chest.setItem(i,stack.split(Math.min(limit,stack.getCount())));
            }
        chest.setChanged();
        return before-stack.getCount();
    }
}
