package net.teogemini.nova.screen;

import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.teogemini.nova.entity.NovaEntity;
import net.teogemini.nova.entity.NovaWorkSupport;

/** Five right-hand tool slots; the menu may additionally expose the actual OFFHAND. */
public final class NovaToolContainer implements Container {
    private final NovaEntity nova;
    private final boolean includeOffhand;
    public NovaToolContainer(NovaEntity nova) { this(nova, false); }
    public NovaToolContainer(NovaEntity nova, boolean includeOffhand) {
        this.nova = nova;
        this.includeOffhand = includeOffhand;
    }
    @Override public int getContainerSize() { return includeOffhand ? 6 : 5; }
    @Override public int getMaxStackSize() { return includeOffhand ? 64 : 1; }
    @Override public boolean isEmpty() {
        for (int i = 0; i < getContainerSize(); i++) if (!getItem(i).isEmpty()) return false;
        return true;
    }
    @Override public ItemStack getItem(int slot) {
        if (slot < 0 || slot >= getContainerSize()) return ItemStack.EMPTY;
        if (slot == 5) return nova.getOffhandItem();
        return slot == 0 ? nova.getMainHandItem() : nova.work().spareTools.getItem(slot - 1);
    }
    @Override public ItemStack removeItem(int slot, int amount) {
        if (amount <= 0) return ItemStack.EMPTY;
        ItemStack result = getItem(slot).split(amount);
        if (getItem(slot).isEmpty()) setItem(slot, ItemStack.EMPTY);
        setChanged();
        return result;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        ItemStack result = getItem(slot);
        setItem(slot, ItemStack.EMPTY);
        return result;
    }
    @Override public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= getContainerSize()) return;
        if (slot == 0 || slot == 5) {
            EquipmentSlot hand = slot == 0 ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
            nova.setItemSlot(hand, stack);
            nova.setGuaranteedDrop(hand);
        } else {
            nova.work().spareTools.setItem(slot - 1, stack);
        }
        setChanged();
    }
    @Override public void setChanged() { nova.work().spareTools.setChanged(); }
    @Override public boolean stillValid(Player player) { return NovaInventoryMenu.canAccess(nova, player); }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot >= 0 && slot < getContainerSize()
                && (slot == 5 ? (NovaWorkSupport.isSupply(stack) || stack.is(net.minecraft.tags.ItemTags.SWORDS)) : NovaWorkSupport.isTool(stack));
    }
    @Override public void clearContent() {
        for (int i = 0; i < getContainerSize(); i++) setItem(i, ItemStack.EMPTY);
    }
}
