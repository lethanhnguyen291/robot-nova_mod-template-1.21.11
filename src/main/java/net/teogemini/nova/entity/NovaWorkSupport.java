package net.teogemini.nova.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.item.Item;
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

    public static void moveToWork(NovaEntity nova, BlockPos target, double speed) {
        if (!nova.canStartNovaJob()) return;
        nova.pathClearing().track(target);
        boolean moved = nova.getNavigation().moveTo(target.getX() + .5, target.getY(), target.getZ() + .5, speed);
        var path = nova.getNavigation().getPath();
        var end = path == null ? null : path.getEndNode();
        if (!moved || path != null && !path.canReach() && (nova.getNavigation().isDone()
                || end != null && nova.distanceToSqr(end.x + .5, end.y, end.z + .5) < 1))
            nova.companion().stuck(target);
    }

    public static boolean isTool(ItemStack stack) {
        return stack.is(ItemTags.AXES) || stack.is(ItemTags.PICKAXES)
                || stack.is(ItemTags.HOES) || stack.is(ItemTags.SHOVELS)
                || stack.is(ItemTags.SWORDS);
    }

    public static boolean isSupply(ItemStack stack) {
        return stack.is(ItemTags.SAPLINGS) || isSeedSupply(stack) || stack.is(Items.TORCH);
    }

    public static boolean isSeedSupply(ItemStack stack) {
        return stack.is(Items.WHEAT_SEEDS) || stack.is(Items.BEETROOT_SEEDS)
                || stack.is(Items.CARROT) || stack.is(Items.POTATO);
    }

    public static void message(NovaEntity nova, Player player, String text) {
        player.displayClientMessage(Component.literal(nova.getNovaDisplayName() + ": " + text), true);
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
        if (!(tool || supply || held.is(net.teogemini.nova.registry.ModBlocks.NOVA_CONTROL_ROD_ITEM) || held.is(Items.LIGHTNING_ROD)
                || held.is(Items.EMERALD) || held.is(Items.BELL) || held.is(Items.IRON_NUGGET)
                || held.is(Items.REDSTONE) || held.is(Items.REDSTONE_BLOCK) || held.is(Items.IRON_INGOT) || held.is(Items.COPPER_INGOT))) {
            return InteractionResult.PASS;
        }
        if (!(nova.level() instanceof ServerLevel)) return InteractionResult.SUCCESS;
        nova.pathClearing().cancel();
        boolean isRecharge = held.is(Items.REDSTONE) || held.is(Items.REDSTONE_BLOCK);
        boolean isRepair = held.is(Items.IRON_INGOT) || held.is(Items.COPPER_INGOT);
        if (isRecharge) {
            boolean isBlock = held.is(Items.REDSTONE_BLOCK);
            int energyToAdd = isBlock ? NovaWorkData.MAX_ENERGY : NovaWorkData.REDSTONE_CHARGE;
            float healAmount = isBlock ? 6.0F : 2.0F;

            int previousEnergy = nova.work().energy();
            boolean recharged = nova.work().recharge(energyToAdd);
            boolean healed = false;
            if (nova.getHealth() < nova.getMaxHealth()) {
                nova.heal(healAmount);
                healed = true;
            }

            if (recharged || healed) {
                nova.companion().recharged(previousEnergy);
                if (!player.getAbilities().instabuild) held.shrink(1);
                nova.sounds().play(net.teogemini.nova.sound.NovaSounds.Cue.RECHARGE);

                if (nova.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, nova.getX(), nova.getY() + 0.8, nova.getZ(),
                            isBlock ? 20 : 10, 0.35, 0.45, 0.35, 0.08);
                    serverLevel.sendParticles(ParticleTypes.HEART, nova.getX(), nova.getY() + 1.1, nova.getZ(),
                            isBlock ? 6 : 3, 0.3, 0.3, 0.3, 0.02);
                    serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, nova.getX(), nova.getY() + 0.9, nova.getZ(),
                            isBlock ? 10 : 5, 0.3, 0.3, 0.3, 0.02);
                }

                nova.showNovaFace(1, 40);
                nova.companion().gesture("nod");

                String msg = "Đã sạc pin: " + (nova.work().energy() / 10) + "%";
                if (healed) msg += " & phục hồi: " + (int)nova.getHealth() + "/" + (int)nova.getMaxHealth() + " HP.";
                else msg += ".";
                message(nova, player, msg);
            } else {
                message(nova, player, "Pin và thân máy đều đang ở trạng thái tối ưu.");
            }
        } else if (isRepair) {
            boolean isIron = held.is(Items.IRON_INGOT);
            float healAmount = isIron ? 5.0F : 3.0F;

            if (nova.getHealth() < nova.getMaxHealth()) {
                nova.heal(healAmount);
                if (!player.getAbilities().instabuild) held.shrink(1);
                nova.sounds().play(net.teogemini.nova.sound.NovaSounds.Cue.REPAIR);

                if (nova.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.WAX_OFF, nova.getX(), nova.getY() + 0.7, nova.getZ(),
                            12, 0.3, 0.4, 0.3, 0.05);
                    serverLevel.sendParticles(ParticleTypes.HEART, nova.getX(), nova.getY() + 1.1, nova.getZ(),
                            4, 0.25, 0.25, 0.25, 0.02);
                }

                nova.showNovaFace(0, 40);
                nova.companion().gesture("nod");
                message(nova, player, "Đã sửa chữa thân vỏ hợp kim: " + (int)nova.getHealth() + "/" + (int)nova.getMaxHealth() + " HP.");
            } else {
                message(nova, player, "Thân vỏ không có hư hại, không cần sửa chữa.");
            }
        } else if (held.is(net.teogemini.nova.registry.ModBlocks.NOVA_CONTROL_ROD_ITEM)) {
            if (player.isShiftKeyDown()) {
                NovaChestHandler.toggleCarry(nova, player);
            } else {
                NovaChestHandler.select(nova, player);
            }
        } else if (held.is(Items.LIGHTNING_ROD)) {
            NovaChestHandler.toggleCarry(nova, player);
        } else if (held.is(Items.EMERALD)) {
            nova.companion().ownerCommand();
            boolean newState = !nova.isNovaWorkEnabled();
            nova.setNovaWorkEnabled(newState);
            nova.clearWorkAnimation();
            nova.getNavigation().stop();
            if (newState) {
                nova.setNovaMoveMode(NovaEntity.MODE_FREE);
                checkWorkConditionsAndNotify(nova, player);
            } else {
                message(nova, player, "Đã tắt làm việc.");
            }
        } else if (held.is(Items.BELL)) {
            NovaRallyManager.toggle(nova, player);
        } else if (held.is(Items.IRON_NUGGET)) {
            if (player.isShiftKeyDown()) {
                recallTools(nova, player);
            } else {
                net.teogemini.nova.screen.NovaMenus.open(player, nova);
            }
        } else {
            if (nova.isCarryingChest()) {
                message(player, "Đặt rương xuống trước khi đổi công cụ nhé.");
                return InteractionResult.SUCCESS;
            }
            EquipmentSlot slot = tool ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
            if (held.is(ItemTags.SWORDS) && nova.getMainHandItem().is(ItemTags.SWORDS) && !nova.getOffhandItem().is(ItemTags.SWORDS)) {
                slot = EquipmentSlot.OFFHAND;
            }
            ItemStack replacement = held.copyWithCount(tool ? 1 : held.getCount());
            if (!player.getAbilities().instabuild) held.shrink(replacement.getCount());
            returnEquipment(nova, player, slot);
            nova.setItemSlot(slot, replacement);
            nova.setGuaranteedDrop(slot);
            nova.sounds().play(net.teogemini.nova.sound.NovaSounds.Cue.EQUIP);
            nova.clearWorkAnimation();
            if (slot == EquipmentSlot.OFFHAND && replacement.is(ItemTags.SWORDS)) {
                message(nova, player, "Đã trang bị song kiếm (cầm 2 kiếm trên 2 tay)!");
            }
        }
        return InteractionResult.SUCCESS;
    }

    public static void recallTools(NovaEntity nova, Player player) {
        nova.pathClearing().cancel();
        boolean hadMain = !nova.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty();
        boolean hadOff = !nova.getItemBySlot(EquipmentSlot.OFFHAND).isEmpty();
        if (!hadMain && !hadOff) {
            message(player, "Mình không cầm công cụ nào cả.");
            return;
        }
        returnEquipment(nova, player, EquipmentSlot.MAINHAND);
        returnEquipment(nova, player, EquipmentSlot.OFFHAND);
        nova.clearWorkAnimation();
        nova.sounds().play(net.teogemini.nova.sound.NovaSounds.Cue.EQUIP);
        message(player, "Đã thu hồi công cụ về túi đồ của bạn.");
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
        if (!nova.companion().usableTool(tool)) return false;
        if (!level.destroyBlock(pos, false, nova)) return false;
        nova.companion().worked(original);
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

    public static void checkWorkConditionsAndNotify(NovaEntity nova, Player player) {
        String name = nova.getNovaDisplayName();

        if (nova.isSwordNova()) {
            if (nova.work().energy() <= 100) {
                player.displayClientMessage(Component.literal(name + ": Pin mình dưới 10% rồi, bạn sạc giúp mình trước nhé."), true);
                return;
            }
            if (!hasAnyUsableSword(nova)) {
                player.displayClientMessage(Component.literal(name + ": Vũ khí của mình đã hỏng hết rồi, cấp vũ khí cho mình nhé!"), true);
                return;
            }
            ensureOffhandSword(nova);
            player.displayClientMessage(Component.literal(name + ": Bật chế độ chiến binh! Mình sẽ đi tuần tra, săn bắt và bảo vệ bạn."), true);
            return;
        }

        BlockPos chest = nova.getLinkedChest();
        if (chest == null || !nova.level().getBlockState(chest).is(net.teogemini.nova.registry.ModBlocks.NOVA_CHEST)) {
            player.displayClientMessage(Component.literal(name + ": Bạn chưa liên kết rương cho mình, hãy dùng gậy chọn rương trước nhé."), true);
            return;
        }

        if (nova.work().energy() <= 100) {
            player.displayClientMessage(Component.literal(name + ": Pin mình dưới 10% rồi, bạn sạc giúp mình trước nhé."), true);
            return;
        }

        if (isTool(nova.getMainHandItem()) && !nova.companion().usableTool(nova.getMainHandItem())) {
            swapToolFromBody(nova);
        }
        boolean hasValidTool = isTool(nova.getMainHandItem()) && nova.companion().usableTool(nova.getMainHandItem());
        if (!hasValidTool) {
            for (int i = 0; i < 4; i++) {
                ItemStack spare = nova.work().spareTools.getItem(i);
                if (!spare.isEmpty() && isTool(spare) && nova.companion().usableTool(spare)) {
                    hasValidTool = true;
                    break;
                }
            }
        }
        if (!hasValidTool && isTool(nova.getMainHandItem()) && hasUsableToolOnBody(nova, nova.getMainHandItem())) {
            hasValidTool = true;
        }
        if (!hasValidTool) {
            player.displayClientMessage(Component.literal(name + ": Mình chưa có công cụ phù hợp (hoặc công cụ đã hết độ bền)."), true);
            return;
        }

        player.displayClientMessage(Component.literal(name + ": Bật làm việc! Mình bắt đầu làm việc đây."), true);
    }

    public static boolean ensureOffhandSapling(NovaEntity nova, Item requiredSapling) {
        if (requiredSapling == null) return false;
        ItemStack offhand = nova.getOffhandItem();

        // 1. Nếu tay trái đã đang cầm đúng loại mầm cây này
        if (offhand.is(requiredSapling)) {
            // Nạp thêm từ ba lô cho đầy stack nếu ba lô còn
            if (offhand.getCount() < offhand.getMaxStackSize()) {
                int needed = offhand.getMaxStackSize() - offhand.getCount();
                for (int i = 0; i < nova.getInventory().getContainerSize() && needed > 0; i++) {
                    ItemStack slot = nova.getInventory().getItem(i);
                    if (slot.is(requiredSapling)) {
                        int take = Math.min(needed, slot.getCount());
                        slot.shrink(take);
                        offhand.grow(take);
                        needed -= take;
                        if (slot.isEmpty()) nova.getInventory().setItem(i, ItemStack.EMPTY);
                    }
                }
                nova.getInventory().setChanged();
            }
            return true;
        }

        // 2. Tay trái trống hoặc cầm đồ khác. Kiểm tra xem ba lô có mầm cây cần tìm không.
        int totalAvailable = 0;
        for (int i = 0; i < nova.getInventory().getContainerSize(); i++) {
            ItemStack slot = nova.getInventory().getItem(i);
            if (slot.is(requiredSapling)) {
                totalAvailable += slot.getCount();
            }
        }
        if (totalAvailable == 0) return false;

        // 3. Nếu tay trái đang cầm đồ khác, cất vào ba lô hoặc swap
        if (!offhand.isEmpty()) {
            ItemStack oldOffhand = offhand.copy();
            ItemStack remainder = nova.getInventory().addItem(oldOffhand);
            if (!remainder.isEmpty()) {
                // Ba lô đầy, tìm slot mầm cây để đổi chỗ trực tiếp
                int swapSlot = -1;
                for (int i = 0; i < nova.getInventory().getContainerSize(); i++) {
                    if (nova.getInventory().getItem(i).is(requiredSapling)) {
                        swapSlot = i;
                        break;
                    }
                }
                if (swapSlot < 0) return false;
                ItemStack saplingStack = nova.getInventory().removeItemNoUpdate(swapSlot);
                nova.getInventory().setItem(swapSlot, oldOffhand);
                nova.setItemSlot(EquipmentSlot.OFFHAND, saplingStack);
                nova.setGuaranteedDrop(EquipmentSlot.OFFHAND);
                nova.getInventory().setChanged();
                return true;
            }
            nova.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        }

        // 4. Lấy mầm cây từ ba lô vào tay trái (tối đa 64)
        ItemStack gathered = ItemStack.EMPTY;
        for (int i = 0; i < nova.getInventory().getContainerSize(); i++) {
            ItemStack slot = nova.getInventory().getItem(i);
            if (slot.is(requiredSapling)) {
                if (gathered.isEmpty()) {
                    gathered = slot.copy();
                    nova.getInventory().setItem(i, ItemStack.EMPTY);
                } else if (gathered.getCount() < gathered.getMaxStackSize()) {
                    int take = Math.min(gathered.getMaxStackSize() - gathered.getCount(), slot.getCount());
                    gathered.grow(take);
                    slot.shrink(take);
                    if (slot.isEmpty()) nova.getInventory().setItem(i, ItemStack.EMPTY);
                }
            }
        }
        nova.setItemSlot(EquipmentSlot.OFFHAND, gathered);
        nova.setGuaranteedDrop(EquipmentSlot.OFFHAND);
        nova.getInventory().setChanged();
        return true;
    }

    public static boolean autoSelectOffhandSapling(NovaEntity nova) {
        if (!nova.trees().planting().isEmpty()) {
            return ensureOffhandSapling(nova, nova.trees().planting().getFirst().species().sapling);
        }
        if (nova.trees().active() != null) {
            return ensureOffhandSapling(nova, nova.trees().active().species().sapling);
        }
        if (nova.getOffhandItem().is(ItemTags.SAPLINGS)) {
            return ensureOffhandSapling(nova, nova.getOffhandItem().getItem());
        }
        if (nova.getOffhandItem().isEmpty()) {
            for (int i = 0; i < nova.getInventory().getContainerSize(); i++) {
                ItemStack slot = nova.getInventory().getItem(i);
                if (slot.is(ItemTags.SAPLINGS)) {
                    return ensureOffhandSapling(nova, slot.getItem());
                }
            }
        }
        return false;
    }

    public static boolean isSameToolType(ItemStack a, ItemStack b) {
        if (a.isEmpty() || b.isEmpty()) return false;
        return a.is(ItemTags.AXES) && b.is(ItemTags.AXES)
                || a.is(ItemTags.PICKAXES) && b.is(ItemTags.PICKAXES)
                || a.is(ItemTags.HOES) && b.is(ItemTags.HOES)
                || a.is(ItemTags.SHOVELS) && b.is(ItemTags.SHOVELS)
                || a.is(ItemTags.SWORDS) && b.is(ItemTags.SWORDS);
    }

    public static String getToolTypeName(ItemStack tool) {
        if (tool.is(ItemTags.PICKAXES)) return "cúp";
        if (tool.is(ItemTags.AXES)) return "rìu";
        if (tool.is(ItemTags.SHOVELS)) return "xẻng";
        if (tool.is(ItemTags.HOES)) return "cuốc";
        if (tool.is(ItemTags.SWORDS)) return "kiếm";
        return "công cụ";
    }

    public static boolean hasUsableToolOnBody(NovaEntity nova, ItemStack tool) {
        if (!isTool(tool)) return false;
        for (int i = 0; i < 4; i++) {
            ItemStack spare = nova.work().spareTools.getItem(i);
            if (!spare.isEmpty() && isSameToolType(tool, spare) && nova.companion().usableTool(spare)) {
                return true;
            }
        }
        for (int i = 0; i < nova.getInventory().getContainerSize(); i++) {
            ItemStack invItem = nova.getInventory().getItem(i);
            if (!invItem.isEmpty() && isSameToolType(tool, invItem) && nova.companion().usableTool(invItem)) {
                return true;
            }
        }
        return false;
    }

    public static boolean swapToolFromBody(NovaEntity nova) {
        if (nova.toolBelt().borrowedSlot() != 0) {
            nova.toolBelt().restore();
        }
        ItemStack held = nova.getMainHandItem();
        if (!isTool(held) || nova.companion().usableTool(held)) return false;

        // 1. Tìm trong đai phụ (spareTools, 4 ô)
        for (int i = 0; i < 4; i++) {
            ItemStack spare = nova.work().spareTools.getItem(i);
            if (!spare.isEmpty() && isSameToolType(held, spare) && nova.companion().usableTool(spare)) {
                nova.work().spareTools.setItem(i, ItemStack.EMPTY);
                nova.setItemSlot(EquipmentSlot.MAINHAND, spare);
                nova.setGuaranteedDrop(EquipmentSlot.MAINHAND);
                nova.work().spareTools.setItem(i, held);
                nova.work().spareTools.setChanged();
                return true;
            }
        }

        // 2. Tìm trong ba lô (inventory, 27 ô)
        for (int i = 0; i < nova.getInventory().getContainerSize(); i++) {
            ItemStack invItem = nova.getInventory().getItem(i);
            if (!invItem.isEmpty() && isSameToolType(held, invItem) && nova.companion().usableTool(invItem)) {
                nova.getInventory().setItem(i, ItemStack.EMPTY);
                nova.setItemSlot(EquipmentSlot.MAINHAND, invItem);
                nova.setGuaranteedDrop(EquipmentSlot.MAINHAND);
                nova.getInventory().setItem(i, held);
                nova.getInventory().setChanged();
                return true;
            }
        }

        return false;
    }

    public static boolean ensureOffhandSeed(NovaEntity nova) {
        if (!nova.getPrimaryTool().is(ItemTags.HOES)) return false;
        ItemStack offhand = nova.getOffhandItem();

        // 1. Nếu tay trái đang cầm hạt giống/củ quả trồng được -> nạp thêm từ ba lô cho đầy stack
        if (isSeedSupply(offhand)) {
            if (offhand.getCount() < offhand.getMaxStackSize()) {
                int needed = offhand.getMaxStackSize() - offhand.getCount();
                for (int i = 0; i < nova.getInventory().getContainerSize() && needed > 0; i++) {
                    ItemStack slot = nova.getInventory().getItem(i);
                    if (ItemStack.isSameItemSameComponents(slot, offhand)) {
                        int take = Math.min(needed, slot.getCount());
                        slot.shrink(take);
                        offhand.grow(take);
                        needed -= take;
                        if (slot.isEmpty()) nova.getInventory().setItem(i, ItemStack.EMPTY);
                    }
                }
                nova.getInventory().setChanged();
            }
            return true;
        }

        // 2. Nếu tay trái trống, tìm hạt giống trong ba lô để đưa lên tay trái
        if (!offhand.isEmpty()) return false;
        for (int i = 0; i < nova.getInventory().getContainerSize(); i++) {
            ItemStack slot = nova.getInventory().getItem(i);
            if (isSeedSupply(slot)) {
                Item seedItem = slot.getItem();
                ItemStack gathered = slot.copy();
                nova.getInventory().setItem(i, ItemStack.EMPTY);
                for (int j = i + 1; j < nova.getInventory().getContainerSize() && gathered.getCount() < gathered.getMaxStackSize(); j++) {
                    ItemStack other = nova.getInventory().getItem(j);
                    if (other.is(seedItem) && ItemStack.isSameItemSameComponents(gathered, other)) {
                        int take = Math.min(gathered.getMaxStackSize() - gathered.getCount(), other.getCount());
                        gathered.grow(take);
                        other.shrink(take);
                        if (other.isEmpty()) nova.getInventory().setItem(j, ItemStack.EMPTY);
                    }
                }
                nova.setItemSlot(EquipmentSlot.OFFHAND, gathered);
                nova.setGuaranteedDrop(EquipmentSlot.OFFHAND);
                nova.getInventory().setChanged();
                return true;
            }
        }
        return false;
    }

    public static void ensureOffhandSword(NovaEntity nova) {
        if (nova.getMainHandItem().isEmpty() && nova.getOffhandItem().is(ItemTags.SWORDS)) {
            nova.setItemSlot(EquipmentSlot.MAINHAND, nova.getOffhandItem().copy());
            nova.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            nova.setGuaranteedDrop(EquipmentSlot.MAINHAND);
            nova.setGuaranteedDrop(EquipmentSlot.OFFHAND);
        }
        if (!nova.isSwordNova()) return;
        ItemStack offhand = nova.getOffhandItem();
        if (!offhand.isEmpty()) {
            if (offhand.is(ItemTags.SWORDS) && nova.companion().usableTool(offhand)) {
                return;
            }
            ItemStack oldOffhand = offhand.copy();
            ItemStack remainder = nova.getInventory().addItem(oldOffhand);
            if (remainder.isEmpty()) {
                nova.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            } else {
                return;
            }
        }
        // 1. Tìm trong đai phụ (spareTools, 4 ô)
        for (int i = 0; i < 4; i++) {
            ItemStack spare = nova.work().spareTools.getItem(i);
            if (!spare.isEmpty() && spare.is(ItemTags.SWORDS) && nova.companion().usableTool(spare)) {
                ItemStack swordToEquip = nova.work().spareTools.removeItem(i, 1);
                nova.setItemSlot(EquipmentSlot.OFFHAND, swordToEquip);
                nova.setGuaranteedDrop(EquipmentSlot.OFFHAND);
                nova.work().spareTools.setChanged();
                return;
            }
        }
        // 2. Tìm trong ba lô (inventory, 27 ô)
        for (int i = 0; i < nova.getInventory().getContainerSize(); i++) {
            ItemStack invItem = nova.getInventory().getItem(i);
            if (!invItem.isEmpty() && invItem.is(ItemTags.SWORDS) && nova.companion().usableTool(invItem)) {
                ItemStack swordToEquip = nova.getInventory().removeItem(i, 1);
                nova.setItemSlot(EquipmentSlot.OFFHAND, swordToEquip);
                nova.setGuaranteedDrop(EquipmentSlot.OFFHAND);
                nova.getInventory().setChanged();
                return;
            }
        }
    }

    public static boolean hasAnyUsableSword(NovaEntity nova) {
        if (nova.getMainHandItem().is(ItemTags.SWORDS) && nova.companion().usableTool(nova.getMainHandItem())) {
            return true;
        }
        if (nova.getOffhandItem().is(ItemTags.SWORDS) && nova.companion().usableTool(nova.getOffhandItem())) {
            return true;
        }
        for (int i = 0; i < 4; i++) {
            ItemStack spare = nova.work().spareTools.getItem(i);
            if (!spare.isEmpty() && spare.is(ItemTags.SWORDS) && nova.companion().usableTool(spare)) {
                return true;
            }
        }
        for (int i = 0; i < nova.getInventory().getContainerSize(); i++) {
            ItemStack invItem = nova.getInventory().getItem(i);
            if (!invItem.isEmpty() && invItem.is(ItemTags.SWORDS) && nova.companion().usableTool(invItem)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasTorchesOnBody(NovaEntity nova) {
        if (nova.getOffhandItem().is(Items.TORCH)) return true;
        for (int i = 0; i < nova.getInventory().getContainerSize(); i++) {
            if (nova.getInventory().getItem(i).is(Items.TORCH)) return true;
        }
        return false;
    }

    public static boolean needsTorches(NovaEntity nova) {
        if (!nova.canNovaWork()) return false;
        boolean isMiner = nova.getMainHandItem().is(ItemTags.PICKAXES);
        return isMiner && !hasTorchesOnBody(nova);
    }

    public static boolean ensureOffhandTorch(NovaEntity nova) {
        ItemStack offhand = nova.getOffhandItem();

        // 1. Nếu tay trái đã đang cầm đuốc
        if (offhand.is(Items.TORCH)) {
            if (offhand.getCount() < offhand.getMaxStackSize()) {
                int needed = offhand.getMaxStackSize() - offhand.getCount();
                for (int i = 0; i < nova.getInventory().getContainerSize() && needed > 0; i++) {
                    ItemStack slot = nova.getInventory().getItem(i);
                    if (slot.is(Items.TORCH)) {
                        int take = Math.min(needed, slot.getCount());
                        slot.shrink(take);
                        offhand.grow(take);
                        needed -= take;
                        if (slot.isEmpty()) nova.getInventory().setItem(i, ItemStack.EMPTY);
                    }
                }
                nova.getInventory().setChanged();
            }
            return true;
        }

        // 2. Tay trái trống hoặc cầm đồ khác. Kiểm tra xem ba lô có đuốc không.
        int totalAvailable = 0;
        for (int i = 0; i < nova.getInventory().getContainerSize(); i++) {
            ItemStack slot = nova.getInventory().getItem(i);
            if (slot.is(Items.TORCH)) {
                totalAvailable += slot.getCount();
            }
        }
        if (totalAvailable == 0) return false;

        // 3. Nếu tay trái đang cầm đồ khác, cất vào ba lô hoặc swap
        if (!offhand.isEmpty()) {
            ItemStack oldOffhand = offhand.copy();
            ItemStack remainder = nova.getInventory().addItem(oldOffhand);
            if (!remainder.isEmpty()) {
                int swapSlot = -1;
                for (int i = 0; i < nova.getInventory().getContainerSize(); i++) {
                    if (nova.getInventory().getItem(i).is(Items.TORCH)) {
                        swapSlot = i;
                        break;
                    }
                }
                if (swapSlot < 0) return false;
                ItemStack torchStack = nova.getInventory().removeItemNoUpdate(swapSlot);
                nova.getInventory().setItem(swapSlot, oldOffhand);
                nova.setItemSlot(EquipmentSlot.OFFHAND, torchStack);
                nova.setGuaranteedDrop(EquipmentSlot.OFFHAND);
                nova.getInventory().setChanged();
                return true;
            }
            nova.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        }

        // 4. Lấy đuốc từ ba lô vào tay trái (tối đa 64 cây)
        ItemStack gathered = ItemStack.EMPTY;
        for (int i = 0; i < nova.getInventory().getContainerSize(); i++) {
            ItemStack slot = nova.getInventory().getItem(i);
            if (slot.is(Items.TORCH)) {
                if (gathered.isEmpty()) {
                    gathered = slot.copy();
                    nova.getInventory().setItem(i, ItemStack.EMPTY);
                } else if (gathered.getCount() < gathered.getMaxStackSize()) {
                    int take = Math.min(gathered.getMaxStackSize() - gathered.getCount(), slot.getCount());
                    gathered.grow(take);
                    slot.shrink(take);
                    if (slot.isEmpty()) nova.getInventory().setItem(i, ItemStack.EMPTY);
                }
            }
        }
        nova.setItemSlot(EquipmentSlot.OFFHAND, gathered);
        nova.setGuaranteedDrop(EquipmentSlot.OFFHAND);
        nova.getInventory().setChanged();
        return true;
    }
}
