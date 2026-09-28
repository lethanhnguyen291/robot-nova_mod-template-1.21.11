package net.teogemini.nova.screen;

import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.teogemini.nova.entity.NovaEntity;
import net.teogemini.nova.entity.NovaWorkSupport;

/** Slot zero is the actual held stack. Reserves are saved once in NovaWorkData. */
public final class NovaToolContainer implements Container {
    private final NovaEntity nova;
    public NovaToolContainer(NovaEntity nova) { this.nova = nova; }
    @Override public int getContainerSize() { return 5; }
    @Override public int getMaxStackSize() { return 1; }
    @Override public boolean isEmpty() {
        for (int i = 0; i < 5; i++) if (!getItem(i).isEmpty()) return false;
        return true;
    }
    @Override public ItemStack getItem(int slot) {
        if (slot < 0 || slot >= 5) return ItemStack.EMPTY;
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
        if (slot < 0 || slot >= 5) return;
        if (slot == 0) {
            nova.setItemSlot(EquipmentSlot.MAINHAND, stack);
            nova.setGuaranteedDrop(EquipmentSlot.MAINHAND);
        } else {
            nova.work().spareTools.setItem(slot - 1, stack);
        }
        setChanged();
    }
    @Override public void setChanged() { nova.work().spareTools.setChanged(); }
    @Override public boolean stillValid(Player player) { return NovaInventoryMenu.canAccess(nova, player); }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return NovaWorkSupport.isTool(stack); }
    @Override public void clearContent() {
        for (int i = 0; i < 5; i++) setItem(i, ItemStack.EMPTY);
    }
}
