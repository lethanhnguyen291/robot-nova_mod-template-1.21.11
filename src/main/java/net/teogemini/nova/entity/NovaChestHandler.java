package net.teogemini.nova.entity;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.teogemini.nova.registry.ModBlocks;

public final class NovaChestHandler {
    private record Selection(UUID nova, long expires) {}
    private static final Map<ServerPlayer, Selection> SELECTED = new WeakHashMap<>();
    private NovaChestHandler() {}

    public static void select(NovaEntity nova, Player player) {
        if (!(player instanceof ServerPlayer owner)) return;
        if (nova.isSwordNova()) {
            NovaWorkSupport.message(nova, player, "Mình là chiến binh dùng kiếm, luôn đi theo và bảo vệ bạn!");
            return;
        }
        if (nova.isCarryingChest()) {
            NovaWorkSupport.message(nova, player, "Mình đang mang rương trên lưng, hãy đặt xuống trước.");
            return;
        }
        BlockPos linked = nova.getLinkedChest();
        if (linked != null) {
            // Kiểm tra rương cũ có còn tồn tại trong thế giới không
            if (nova.level().hasChunkAt(linked) && !nova.level().getBlockState(linked).is(ModBlocks.NOVA_CHEST)) {
                nova.setLinkedChest(null);
            } else {
                SELECTED.remove(owner);
                NovaWorkSupport.message(nova, player, "Mình đã liên kết với rương rồi!");
                return;
            }
        }
        SELECTED.put(owner, new Selection(nova.getUUID(), owner.level().getGameTime() + 1200));
        NovaWorkSupport.message(nova, player, "Đã chọn mình. Dùng gậy điều khiển bấm vào rương NOVA trong 60 giây để liên kết.");
    }

    public static NovaEntity selected(ServerPlayer owner) {
        Selection selected = SELECTED.get(owner);
        if (selected == null || selected.expires() < owner.level().getGameTime()) return null;
        return owner.level().getEntity(selected.nova()) instanceof NovaEntity nova && nova.isAlive()
                && nova.isOwnedBy(owner) && owner.distanceToSqr(nova) <= 4096.0 ? nova : null;
    }

    public static List<NovaEntity> getNovasLinkedTo(ServerLevel level, BlockPos pos) {
        return level.getEntitiesOfClass(NovaEntity.class, new AABB(pos).inflate(128.0),
                nova -> nova.isAlive() && pos.equals(nova.getLinkedChest()));
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            var marked = net.teogemini.nova.network.NovaCompanionCommands.markBlock(player, level, hand, hit);
            if (marked != InteractionResult.PASS) return marked;
            if (hand != InteractionHand.MAIN_HAND || player.isSpectator()
                    || !player.getItemInHand(hand).is(ModBlocks.NOVA_CONTROL_ROD_ITEM)
                    || !level.getBlockState(hit.getBlockPos()).is(ModBlocks.NOVA_CHEST)) {
                return InteractionResult.PASS;
            }
            if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer owner)) {
                return InteractionResult.SUCCESS;
            }

            BlockPos chestPos = hit.getBlockPos().immutable();
            String chestLabel = "Rương";
            if (server.getBlockEntity(chestPos) instanceof ChestBlockEntity cbe && cbe.hasCustomName()) {
                chestLabel = "Rương \"" + cbe.getCustomName().getString() + "\"";
            }

            // SHIFT-CLICK: Nhấc rương từ xa hoặc trực tiếp
            if (player.isShiftKeyDown()) {
                List<NovaEntity> linked = getNovasLinkedTo(server, chestPos);
                NovaEntity myNova = null;
                for (NovaEntity n : linked) {
                    if (n.isOwnedBy(owner)) {
                        myNova = n;
                        break;
                    }
                }
                if (myNova == null) {
                    NovaWorkSupport.message(owner, "Không có robot NOVA nào của bạn liên kết với " + chestLabel + " để nhấc.");
                    return InteractionResult.SUCCESS;
                }
                if (myNova.isCarryingChest()) {
                    NovaWorkSupport.message(myNova, owner, "Mình đang mang một rương khác rồi!");
                    return InteractionResult.SUCCESS;
                }
                if (myNova.distanceToSqr(Vec3.atCenterOf(chestPos)) <= 9.0) {
                    if (pickUp(myNova, server, chestPos)) {
                        NovaWorkSupport.message(myNova, owner, "Đã nhấc " + chestLabel + " lên! Mình sẽ đi theo bạn.");
                    } else {
                        NovaWorkSupport.message(myNova, owner, "Không thể nhấc " + chestLabel + " (rương có thể đang mở hoặc là rương đôi).");
                    }
                } else {
                    myNova.startFetchingChest(chestPos);
                    NovaWorkSupport.message(myNova, owner, "Đang đến " + chestLabel + " của mình để nhấc lên nhé!");
                }
                return InteractionResult.SUCCESS;
            }

            // CLICK THƯỜNG:
            Selection selection = SELECTED.get(owner);
            // 1. Không có robot nào đang được chọn: Xem thông tin các robot liên kết với rương này
            if (selection == null || selection.expires() < server.getGameTime()) {
                List<NovaEntity> linked = getNovasLinkedTo(server, chestPos);
                if (linked.isEmpty()) {
                    NovaWorkSupport.message(owner, chestLabel + " chưa liên kết với robot NOVA nào.");
                } else {
                    String names = linked.stream()
                            .map(NovaEntity::getNovaDisplayName)
                            .collect(Collectors.joining(", "));
                    NovaWorkSupport.message(owner, chestLabel + " đang liên kết với: " + names);
                }
                return InteractionResult.SUCCESS;
            }

            // 2. Đang có robot được chọn: Tiến hành liên kết
            if (!(server.getEntity(selection.nova()) instanceof NovaEntity nova)
                    || !nova.isAlive() || !nova.isOwnedBy(owner)) {
                SELECTED.remove(owner);
                NovaWorkSupport.message(owner, "Robot NOVA đã chọn không còn khả dụng.");
                return InteractionResult.SUCCESS;
            }

            if (nova.isSwordNova()) {
                SELECTED.remove(owner);
                NovaWorkSupport.message(nova, owner, "Mình là chiến binh dùng kiếm, không liên kết với rương đâu nhé!");
                return InteractionResult.SUCCESS;
            }
            if (nova.isCarryingChest()) {
                NovaWorkSupport.message(nova, owner, "Mình đang mang rương trên lưng, hãy đặt xuống trước khi liên kết rương mới.");
                return InteractionResult.SUCCESS;
            }

            BlockPos previousChest = nova.getLinkedChest();
            boolean wasSame = chestPos.equals(previousChest);

            List<NovaEntity> otherNovas = getNovasLinkedTo(server, chestPos).stream()
                    .filter(n -> !n.getUUID().equals(nova.getUUID()))
                    .toList();

            // CHẶN: Nếu rương đã có Nova khác liên kết -> Không cho liên kết thêm robot khác!
            if (!otherNovas.isEmpty()) {
                String otherNames = otherNovas.stream()
                        .map(NovaEntity::getNovaDisplayName)
                        .collect(Collectors.joining(", "));
                SELECTED.remove(owner);
                NovaWorkSupport.message(owner, chestLabel + " đã liên kết với " + otherNames + " rồi, không thể liên kết thêm robot khác!");
                return InteractionResult.SUCCESS;
            }

            if (wasSame) {
                SELECTED.remove(owner);
                NovaWorkSupport.message(nova, owner, nova.getNovaDisplayName() + " đã liên kết với " + chestLabel + " từ trước rồi!");
                return InteractionResult.SUCCESS;
            }

            // Nếu con Nova này đã có rương khác trong thế giới
            if (previousChest != null && server.hasChunkAt(previousChest)
                    && server.getBlockState(previousChest).is(ModBlocks.NOVA_CHEST)) {
                SELECTED.remove(owner);
                NovaWorkSupport.message(nova, owner, nova.getNovaDisplayName() + " đã có rương liên kết rồi!");
                return InteractionResult.SUCCESS;
            }

            nova.companion().ownerCommand();
            nova.work().link(GlobalPos.of(server.dimension(), chestPos));
            nova.sounds().play(net.teogemini.nova.sound.NovaSounds.Cue.LINK);
            nova.companion().memories().remember("Chủ nhân đã chọn một chiếc rương làm nhà cho mình.");
            nova.getNavigation().stop();
            SELECTED.remove(owner);

            NovaWorkSupport.message(nova, owner, "Đã liên kết " + nova.getNovaDisplayName() + " với " + chestLabel + ". Vùng làm việc tối đa 16 khối.");
            return InteractionResult.SUCCESS;
        });
    }

    public static Container linkedContainer(NovaEntity nova) {
        BlockPos pos = nova.getLinkedChest();
        if (pos == null || !nova.level().hasChunkAt(pos)) return null;
        var state = nova.level().getBlockState(pos);
        if (!state.is(ModBlocks.NOVA_CHEST)) return null;
        return ChestBlock.getContainer(ModBlocks.NOVA_CHEST, state, nova.level(), pos, false);
    }

    public static boolean pickUp(NovaEntity nova, ServerLevel level, BlockPos pos) {
        if (pos == null || !level.hasChunkAt(pos)) return false;
        var state = level.getBlockState(pos);
        if (!state.is(ModBlocks.NOVA_CHEST) || state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            return false;
        }
        if (!(level.getBlockEntity(pos) instanceof ChestBlockEntity chest)
                || ChestBlockEntity.getOpenCount(level, pos) > 0) {
            return false;
        }
        var owner = nova.getOwner();
        if (owner instanceof Player player) {
            chest.unpackLootTable(player);
        }
        var saved = chest.saveWithFullMetadata(level.registryAccess());
        ItemStack packed = new ItemStack(ModBlocks.NOVA_CHEST_ITEM);
        packed.applyComponents(chest.collectComponents());
        packed.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(chest.getType(), saved));
        if (chest.hasCustomName()) packed.set(DataComponents.CUSTOM_NAME, chest.getCustomName());
        chest.clearContent();
        if (!level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState())) {
            chest.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
            chest.setChanged();
            return false;
        }
        nova.work().setCarriedChest(packed);
        nova.setNovaCarrying(true);
        nova.sounds().play(net.teogemini.nova.sound.NovaSounds.Cue.CHEST_LIFT);
        nova.stopRally();
        nova.clearWorkAnimation();
        nova.setNovaMoveMode(NovaEntity.MODE_FOLLOW);
        return true;
    }

    public static void toggleCarry(NovaEntity nova, Player player) {
        if (!(nova.level() instanceof ServerLevel level) || !nova.isOwnedBy(player)) return;
        if (nova.isSwordNova()) {
            NovaWorkSupport.message(nova, player, "Mình đang cầm kiếm chiến đấu, không mang rương đâu nhé!");
            return;
        }
        if (nova.isPerformingAction()) {
            NovaWorkSupport.message(nova, player, "Đợi mình kết thúc động tác hiện tại nhé.");
            return;
        }
        if (nova.isCarryingChest()) {
            place(nova, player, level);
            return;
        }
        BlockPos pos = nova.getLinkedChest();
        if (pos == null || !level.hasChunkAt(pos)) {
            NovaWorkSupport.message(nova, player, "Mình chưa được liên kết với rương nào.");
            return;
        }
        var state = level.getBlockState(pos);
        if (!state.is(ModBlocks.NOVA_CHEST) || state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            NovaWorkSupport.message(nova, player, "Mình chỉ mang rương đơn; rương đôi vẫn dùng để cất đồ được.");
            return;
        }
        if (nova.distanceToSqr(Vec3.atCenterOf(pos)) > 9.0) {
            nova.startFetchingChest(pos);
            NovaWorkSupport.message(nova, player, "Đang đến rương của mình để nhấc lên nhé!");
            return;
        }
        if (pickUp(nova, level, pos)) {
            NovaWorkSupport.message(nova, player, "Đã nhấc rương và giữ đồ bên trong. Dùng gậy điều khiển lần nữa để đặt xuống.");
        } else {
            NovaWorkSupport.message(nova, player, "Không thể nhấc rương (rương có thể đang mở hoặc có vật cản).");
        }
    }

    private static void place(NovaEntity nova, Player player, ServerLevel level) {
        BlockPos pos = nova.blockPosition().relative(nova.getDirection());
        if (!level.hasChunkAt(pos) || !level.isInWorldBounds(pos)
                || !level.getWorldBorder().isWithinBounds(pos) || !level.mayInteract(player, pos)
                || !level.getBlockState(pos).isAir()
                || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                || !level.noCollision(new AABB(pos).deflate(0.07))) {
            NovaWorkSupport.message(nova, player, "Cần ô trống có nền chắc ngay phía trước mình để đặt rương.");
            return;
        }
        var state = ModBlocks.NOVA_CHEST.defaultBlockState()
                .setValue(ChestBlock.FACING, nova.getDirection())
                .setValue(ChestBlock.TYPE, ChestType.SINGLE);
        ItemStack packed = nova.work().carriedChest();
        var data = packed.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null || !level.setBlockAndUpdate(pos, state)) return;
        if (!(level.getBlockEntity(pos) instanceof ChestBlockEntity chest)) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            return;
        }
        chest.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,
                level.registryAccess(), data.copyTagWithoutId()));
        chest.applyComponentsFromItemStack(packed);
        chest.setChanged();
        level.sendBlockUpdated(pos, state, state, 3);
        nova.work().setCarriedChest(ItemStack.EMPTY);
        nova.setNovaCarrying(false);
        nova.sounds().play(net.teogemini.nova.sound.NovaSounds.Cue.CHEST_DROP);
        nova.work().link(GlobalPos.of(level.dimension(), pos.immutable()));
        NovaWorkSupport.message(nova, player, "Đã đặt rương và liên kết lại.");
    }
}
