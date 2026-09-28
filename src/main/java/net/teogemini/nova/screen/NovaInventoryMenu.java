package net.teogemini.nova.screen;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.teogemini.nova.entity.NovaEntity;
import net.teogemini.nova.entity.NovaWorkData;
import net.teogemini.nova.entity.NovaWorkSupport;

public final class NovaInventoryMenu extends AbstractContainerMenu {
    public static final int CARGO_END = 27, TOOLS_END = 32, PLAYER_END = 68;
    private final NovaEntity nova;
    private final int entityId;
    private final Container tools;
    private final ContainerData data;
    private final boolean serverMenu;
    private int toolScroll;
    private boolean released;

    // Client uses independent containers populated by vanilla slot-sync packets.
    public NovaInventoryMenu(int id, Inventory playerInventory, Integer entityId) {
        this(id, playerInventory, null, entityId, new SimpleContainer(27),
                new SimpleContainer(5), new SimpleContainerData(6));
    }
    public NovaInventoryMenu(int id, Inventory playerInventory, NovaEntity nova) {
        this(id, playerInventory, nova, nova.getId(), nova.getInventory(),
                new NovaToolContainer(nova), serverData(nova));
        nova.work().openInventory();
        nova.getNavigation().stop();
        nova.clearWorkAnimation();
    }
    private NovaInventoryMenu(int id, Inventory playerInventory, NovaEntity nova, int entityId,
                              Container cargo, Container tools, ContainerData data) {
        super(NovaMenus.INVENTORY, id);
        this.nova = nova;
        this.entityId = entityId;
        this.tools = tools;
        this.data = data;
        this.serverMenu = nova != null;
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(cargo, row * 9 + col, 13 + col * 18, 109 + row * 18));
        for (int i = 0; i < 5; i++) addSlot(toolSlot(i));
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(playerInventory, 9 + row * 9 + col, 13 + col * 18, 177 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(playerInventory, col, 13 + col * 18, 235));
        addDataSlots(data);
    }
    private static ContainerData serverData(NovaEntity nova) {
        return new ContainerData() {
            @Override public int get(int index) {
                return switch (index) {
                    case 0 -> nova.work().experience();
                    case 1 -> nova.work().nextLevelCost();
                    case 2 -> nova.work().skillLevel();
                    case 3 -> Math.min(32767, Math.max(0, Math.round(nova.getHealth() * 10)));
                    case 4 -> Math.min(32767, Math.max(1, Math.round(nova.getMaxHealth() * 10)));
                    case 5 -> nova.work().energy();
                    default -> 0;
                };
            }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return 6; }
        };
    }
    private Slot toolSlot(int toolIndex) {
        int row = toolIndex - toolScroll;
        return new Slot(tools, toolIndex, 206, row >= 0 && row < 4 ? 55 + row * 44 : -10000) {
            @Override public boolean mayPlace(ItemStack stack) {
                return NovaWorkSupport.isTool(stack) && (nova == null || !nova.isCarryingChest());
            }
            @Override public boolean mayPickup(Player player) {
                return nova == null || !nova.isCarryingChest();
            }
            @Override public int getMaxStackSize() { return 1; }
            @Override public boolean isActive() {
                return serverMenu || toolIndex >= toolScroll && toolIndex < toolScroll + 4;
            }
        };
    }
    // Layout changes only: packet slot IDs and backing container indices never change.
    public void setToolScroll(int offset) {
        if (serverMenu) return;
        toolScroll = Math.max(0, Math.min(1, offset));
        for (int i = 0; i < 5; i++) {
            Slot replacement = toolSlot(i);
            replacement.index = CARGO_END + i;
            slots.set(CARGO_END + i, replacement);
        }
    }
    public int toolScroll() { return toolScroll; }
    public int entityId() { return entityId; }
    public int experience() { return data.get(0); }
    public int maxExperience() { return Math.max(1, data.get(1)); }
    public int skillLevel() { return Math.max(1, data.get(2)); }
    public float health() { return data.get(3) / 10f; }
    public float maxHealth() { return Math.max(1, data.get(4)) / 10f; }
    public int energy() { return data.get(5); }
    public float energyFraction() { return (float) energy() / NovaWorkData.MAX_ENERGY; }

    public static boolean canAccess(NovaEntity nova, Player player) {
        return nova.isAlive() && !nova.isRemoved() && nova.isTame() && nova.isOwnedBy(player)
                && player.isAlive() && !player.isSpectator() && player.level() == nova.level()
                && player.distanceToSqr(nova) <= 64;
    }
    @Override public boolean stillValid(Player player) { return !serverMenu || canAccess(nova, player); }
    @Override public void clicked(int slot, int button, ClickType type, Player player) {
        if (stillValid(player)) super.clicked(slot, button, type, player);
    }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (!serverMenu || !stillValid(player) || nova.isCarryingChest()
                || button < 1 || button > 4 || !getCarried().isEmpty()) return false;
        ItemStack next = tools.getItem(button);
        ItemStack previous = tools.getItem(0);
        if (next.isEmpty() || !NovaWorkSupport.isTool(next)
                || !previous.isEmpty() && !NovaWorkSupport.isTool(previous)) return false;
        tools.setItem(button, ItemStack.EMPTY);
        tools.setItem(0, next);
        tools.setItem(button, previous);
        nova.clearWorkAnimation();
        broadcastChanges();
        return true;
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack original = slot.getItem();
        ItemStack result = original.copy();
        if (index < TOOLS_END) {
            if (!moveItemStackTo(original, TOOLS_END, PLAYER_END, true)) return ItemStack.EMPTY;
        } else {
            if (NovaWorkSupport.isTool(original)) moveItemStackTo(original, CARGO_END, TOOLS_END, false);
            if (!original.isEmpty()) moveItemStackTo(original, 0, CARGO_END, false);
            if (original.getCount() == result.getCount()) return ItemStack.EMPTY;
        }
        if (original.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, original);
        return result;
    }
    @Override public void removed(Player player) {
        super.removed(player);
        if (serverMenu && !released) {
            nova.work().closeInventory();
            released = true;
        }
    }
}
