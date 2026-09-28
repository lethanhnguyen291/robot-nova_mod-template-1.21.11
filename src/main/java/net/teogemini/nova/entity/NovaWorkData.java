package net.teogemini.nova.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Server-owned, persistent work data. Equipment stays in the entity's equipment slots. */
public final class NovaWorkData {
    public final SimpleContainer inventory = new SimpleContainer(27);
    // Four reserves; the fifth tool is the real MAINHAND item, never a duplicate.
    public final SimpleContainer spareTools = new SimpleContainer(4);
    public static final int MAX_ENERGY = 1000;
    public static final int REDSTONE_CHARGE = 200;
    private int energy = MAX_ENERGY;
    private int energyTicks;
    private int inventoryViewers;

    public int energy() { return energy; }
    public boolean inventoryOpen() { return inventoryViewers > 0; }
    public void openInventory() { inventoryViewers++; }
    public void closeInventory() { inventoryViewers = Math.max(0, inventoryViewers - 1); }
    public boolean recharge() {
        if (energy >= MAX_ENERGY) return false;
        energy = Math.min(MAX_ENERGY, energy + REDSTONE_CHARGE);
        return true;
    }
    // One energy unit per second of work animation, not while idle or following.
    public boolean tickEnergy(boolean working) {
        if (!working || energy == 0) return false;
        if (++energyTicks < 20) return false;
        energyTicks = 0;
        energy--;
        return energy == 0;
    }

    private GlobalPos linkedChest;
    private ItemStack carriedChest = ItemStack.EMPTY;
    private boolean enabled;
    private int experience;
    private int skillLevel = 1;
    public BlockPos rallyTarget;
    public BlockPos rallyFace;
    private Component savedRallyName;
    private Component rallyLabel;
    private boolean savedRallyVisible;

    public void showRallyLeader(NovaEntity nova, int total) {
        restoreRallyName(nova);
        savedRallyName = nova.getCustomName() == null ? null : nova.getCustomName().copy();
        savedRallyVisible = nova.isCustomNameVisible();
        rallyLabel = Component.literal("Đội 1: ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(Integer.toString(total)).withStyle(ChatFormatting.YELLOW));
        nova.setCustomName(rallyLabel);
        nova.setCustomNameVisible(true);
    }

    public void restoreRallyName(NovaEntity nova) {
        if (rallyLabel != null && rallyLabel.equals(nova.getCustomName())) {
            nova.setCustomName(savedRallyName);
            nova.setCustomNameVisible(savedRallyVisible);
        }
        savedRallyName = null;
        rallyLabel = null;
        savedRallyVisible = false;
    }

    public GlobalPos linkedChest() { return linkedChest; }
    public void link(GlobalPos pos) { linkedChest = pos; }
    public ItemStack carriedChest() { return carriedChest; }
    public void setCarriedChest(ItemStack stack) { carriedChest = stack.copy(); }
    public boolean enabled() { return enabled; }
    public void setEnabled(boolean value) { enabled = value; }
    public int experience() { return experience; }
    public int skillLevel() { return skillLevel; }

    public int nextLevelCost() { return levelCost(skillLevel); }

    private static int levelCost(int level) {
        return switch (level) {
            case 1 -> 50;
            case 2 -> 150;
            case 3 -> 300;
            default -> 500;
        };
    }

    public boolean addExperience(int amount) {
        if (amount <= 0 || skillLevel >= 5) return false;
        int oldLevel = skillLevel;
        long total = (long) experience + amount;
        while (skillLevel < 5 && total >= levelCost(skillLevel)) {
            total -= levelCost(skillLevel++);
        }
        experience = skillLevel >= 5 ? 0 : (int) total;
        return oldLevel != skillLevel;
    }

    public void save(ValueOutput output) {
        output.putInt("Energy", energy);
        output.putInt("EnergyTicks", energyTicks);
        output.store("SpareTools", ItemContainerContents.CODEC,
                ItemContainerContents.fromItems(spareTools.getItems()));
        output.storeNullable("RallySavedName", ComponentSerialization.CODEC, savedRallyName);
        output.storeNullable("RallyLabel", ComponentSerialization.CODEC, rallyLabel);
        output.putBoolean("RallySavedVisible", savedRallyVisible);
        output.store("Cargo", ItemContainerContents.CODEC,
                ItemContainerContents.fromItems(inventory.getItems()));
        output.store("CarriedChest", ItemStack.OPTIONAL_CODEC, carriedChest);
        output.storeNullable("LinkedChest", GlobalPos.CODEC, linkedChest);
        output.putBoolean("Enabled", enabled);
        output.putInt("SkillLevel", skillLevel);
        output.putInt("Experience", experience);
    }

    public void load(ValueInput input) {
        energy = Math.max(0, Math.min(MAX_ENERGY, input.getIntOr("Energy", MAX_ENERGY)));
        energyTicks = Math.max(0, Math.min(19, input.getIntOr("EnergyTicks", 0)));
        inventoryViewers = 0;
        spareTools.clearContent();
        input.read("SpareTools", ItemContainerContents.CODEC)
                .ifPresent(contents -> contents.copyInto(spareTools.getItems()));
        savedRallyName = input.read("RallySavedName", ComponentSerialization.CODEC).orElse(null);
        rallyLabel = input.read("RallyLabel", ComponentSerialization.CODEC).orElse(null);
        savedRallyVisible = input.getBooleanOr("RallySavedVisible", false);
        inventory.clearContent();
        input.read("Cargo", ItemContainerContents.CODEC)
                .ifPresent(contents -> contents.copyInto(inventory.getItems()));
        carriedChest = input.read("CarriedChest", ItemStack.OPTIONAL_CODEC)
                .orElse(ItemStack.EMPTY);
        linkedChest = input.read("LinkedChest", GlobalPos.CODEC).orElse(null);
        enabled = input.getBooleanOr("Enabled", false);
        skillLevel = Math.max(1, Math.min(5, input.getIntOr("SkillLevel", 1)));
        experience = skillLevel == 5 ? 0 : Math.max(0,
                Math.min(levelCost(skillLevel) - 1, input.getIntOr("Experience", 0)));
        rallyTarget = null;
        rallyFace = null;
    }
}
