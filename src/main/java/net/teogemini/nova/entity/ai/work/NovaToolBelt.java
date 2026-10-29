package net.teogemini.nova.entity.ai.work;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** Physical slot 0 is MAINHAND; a borrowed tool trades places with the primary tool. */
public final class NovaToolBelt {
    private final Container slots;
    private int borrowedSlot;

    public NovaToolBelt(Container slots) {
        if (slots.getContainerSize() != 5) throw new IllegalArgumentException("Five tool slots required");
        this.slots = slots;
    }

    public int borrowedSlot() { return borrowedSlot; }
    public ItemStack primary() { return slots.getItem(borrowedSlot); }
    public ItemStack logical(int slot) {
        if (slot < 0 || slot >= 5) return ItemStack.EMPTY;
        return slots.getItem(slot == 0 ? borrowedSlot : slot == borrowedSlot ? 0 : slot);
    }

    public boolean equip(int slot) {
        if (slot < 0 || slot >= 5 || logical(slot).isEmpty()) return false;
        if (borrowedSlot == slot) return true;
        restore();
        if (slot != 0) {
            swap(slot);
            borrowedSlot = slot;
        }
        return true;
    }

    private void swap(int slot) {
        // Move the same stack objects: damage, enchantments and names are retained.
        ItemStack main = slots.removeItemNoUpdate(0);
        ItemStack other = slots.removeItemNoUpdate(slot);
        slots.setItem(0, other);
        slots.setItem(slot, main);
    }

    public void restore() {
        if (borrowedSlot == 0) return;
        int previous = borrowedSlot;
        borrowedSlot = 0;
        swap(previous);
    }

    /** Called only after vanilla equipment and NovaWork spare stacks have both loaded. */
    public void restoreAfterLoad(int savedSlot) {
        borrowedSlot = savedSlot >= 1 && savedSlot <= 4 ? savedSlot : 0;
        restore();
    }
}
