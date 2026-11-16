package net.teogemini.nova.network;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.teogemini.nova.ROBOTNOVA_MOD;
import net.teogemini.nova.entity.NovaEntity;
import net.teogemini.nova.entity.NovaWorkData;
import net.teogemini.nova.entity.NovaWorkSupport;
import net.teogemini.nova.item.NovaOmniPadItem;
import net.teogemini.nova.registry.NovaMaterials;
import net.teogemini.nova.screen.NovaMenus;
import net.teogemini.nova.sound.NovaSounds;

public final class NovaOmniPadNetwork {
    public static final int ACTION_REFRESH = 0;
    public static final int ACTION_RENAME = 1;
    public static final int ACTION_TELEPORT_ONE = 2;
    public static final int ACTION_PING_ONE = 3;
    public static final int ACTION_OPEN_INVENTORY = 4;
    public static final int ACTION_CHARGE_ONE = 5;
    public static final int ACTION_LOAD_PAD_ENERGY = 6;

    public static final int MASS_WORK_ON = 10;
    public static final int MASS_WORK_CHOP = 11;
    public static final int MASS_WORK_MINE = 12;
    public static final int MASS_WORK_FARM = 13;
    public static final int MASS_WORK_DIG = 14;
    public static final int MASS_WORK_OFF = 15;
    public static final int MASS_FOLLOW = 16;
    public static final int MASS_WAIT = 17;
    public static final int MASS_FREE = 18;
    public static final int MASS_RALLY = 19;
    public static final int MASS_RETURN_CHEST = 20;
    public static final int MASS_TELEPORT = 21;
    public static final int MASS_PING = 22;
    public static final int MASS_SMART_ASSIGN = 23;
    public static final int MASS_WIRELESS_CHARGE = 24;
    public static final int MASS_LOCKDOWN_TOGGLE = 25;

    private static final Map<UUID, Boolean> LOCKDOWN_STATE = new ConcurrentHashMap<>();

    private NovaOmniPadNetwork() {}

    public record RobotInfo(
            int entityId,
            UUID uuid,
            String customName,
            String displayName,
            int energyPercent,
            int health,
            int maxHealth,
            int skillLevel,
            int exp,
            int maxExp,
            int x,
            int y,
            int z,
            int distanceMeters,
            String statusLabel,
            boolean working,
            boolean lowBattery,
            boolean alert,
            boolean sitting,
            boolean dormant,
            int moveMode,
            String toolItemId,
            int toolDamage,
            int toolMaxDamage,
            int faceIndex
    ) {
        public static void write(RegistryFriendlyByteBuf buf, RobotInfo info) {
            buf.writeVarInt(info.entityId);
            buf.writeUUID(info.uuid);
            buf.writeUtf(info.customName, 64);
            buf.writeUtf(info.displayName, 64);
            buf.writeVarInt(info.energyPercent);
            buf.writeVarInt(info.health);
            buf.writeVarInt(info.maxHealth);
            buf.writeVarInt(info.skillLevel);
            buf.writeVarInt(info.exp);
            buf.writeVarInt(info.maxExp);
            buf.writeVarInt(info.x);
            buf.writeVarInt(info.y);
            buf.writeVarInt(info.z);
            buf.writeVarInt(info.distanceMeters);
            buf.writeUtf(info.statusLabel, 96);
            buf.writeBoolean(info.working);
            buf.writeBoolean(info.lowBattery);
            buf.writeBoolean(info.alert);
            buf.writeBoolean(info.sitting);
            buf.writeBoolean(info.dormant);
            buf.writeVarInt(info.moveMode);
            buf.writeUtf(info.toolItemId, 64);
            buf.writeVarInt(info.toolDamage);
            buf.writeVarInt(info.toolMaxDamage);
            buf.writeVarInt(info.faceIndex);
        }

        public static RobotInfo read(RegistryFriendlyByteBuf buf) {
            return new RobotInfo(
                    buf.readVarInt(),
                    buf.readUUID(),
                    buf.readUtf(64),
                    buf.readUtf(64),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readUtf(96),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readVarInt(),
                    buf.readUtf(64),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt()
            );
        }
    }

    public record SyncPayload(
            boolean openScreen,
            int reserveEnergyPercent,
            int inventoryRedstonePercent,
            boolean lockdownActive,
            String feedback,
            List<RobotInfo> robots
    ) implements CustomPacketPayload {
        public static final Type<SyncPayload> TYPE = new Type<>(ROBOTNOVA_MOD.id("omni_pad_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SyncPayload> CODEC = StreamCodec.of(
                (buf, val) -> {
                    buf.writeBoolean(val.openScreen);
                    buf.writeVarInt(val.reserveEnergyPercent);
                    buf.writeVarInt(val.inventoryRedstonePercent);
                    buf.writeBoolean(val.lockdownActive);
                    buf.writeUtf(val.feedback, 180);
                    buf.writeVarInt(val.robots.size());
                    for (RobotInfo r : val.robots) {
                        RobotInfo.write(buf, r);
                    }
                },
                buf -> {
                    boolean open = buf.readBoolean();
                    int reserve = buf.readVarInt();
                    int invRs = buf.readVarInt();
                    boolean lockdown = buf.readBoolean();
                    String feedback = buf.readUtf(180);
                    int count = Math.min(256, Math.max(0, buf.readVarInt()));
                    List<RobotInfo> list = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) {
                        list.add(RobotInfo.read(buf));
                    }
                    return new SyncPayload(open, reserve, invRs, lockdown, feedback, list);
                }
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ActionPayload(int action, List<UUID> targetUuids, String textParam) implements CustomPacketPayload {
        public static final Type<ActionPayload> TYPE = new Type<>(ROBOTNOVA_MOD.id("omni_pad_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ActionPayload> CODEC = StreamCodec.of(
                (buf, val) -> {
                    buf.writeVarInt(val.action);
                    buf.writeVarInt(val.targetUuids.size());
                    for (UUID id : val.targetUuids) {
                        buf.writeUUID(id);
                    }
                    buf.writeUtf(val.textParam == null ? "" : val.textParam, 64);
                },
                buf -> {
                    int action = buf.readVarInt();
                    int size = Math.min(256, Math.max(0, buf.readVarInt()));
                    List<UUID> ids = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) {
                        ids.add(buf.readUUID());
                    }
                    String text = buf.readUtf(64);
                    return new ActionPayload(action, ids, text);
                }
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void register() {
        PayloadTypeRegistry.playS2C().register(SyncPayload.TYPE, SyncPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ActionPayload.TYPE, ActionPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ActionPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            if (player == null || !player.isAlive() || player.isSpectator() || !hasOmniPad(player)) {
                return;
            }
            handleAction(player, payload);
        });
    }

    public static boolean hasOmniPad(Player player) {
        if (player == null) return false;
        if (player.getMainHandItem().is(NovaMaterials.NOVA_OMNI_PAD)
                || player.getOffhandItem().is(NovaMaterials.NOVA_OMNI_PAD)) {
            return true;
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(NovaMaterials.NOVA_OMNI_PAD)) {
                return true;
            }
        }
        return false;
    }

    private static ItemStack findOmniPadStack(Player player) {
        if (player.getMainHandItem().is(NovaMaterials.NOVA_OMNI_PAD)) return player.getMainHandItem();
        if (player.getOffhandItem().is(NovaMaterials.NOVA_OMNI_PAD)) return player.getOffhandItem();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(NovaMaterials.NOVA_OMNI_PAD)) return s;
        }
        return ItemStack.EMPTY;
    }

    public static List<NovaEntity> collectOwnedNovas(ServerPlayer player) {
        List<NovaEntity> result = new ArrayList<>();
        var server = player.level().getServer();
        if (server == null) return result;
        for (ServerLevel level : server.getAllLevels()) {
            for (var entity : level.getAllEntities()) {
                if (entity instanceof NovaEntity nova && nova.isAlive() && nova.isTame() && nova.isOwnedBy(player)) {
                    result.add(nova);
                }
            }
        }
        result.sort(Comparator
                .comparing(NovaEntity::isDormant)
                .thenComparingDouble(n -> n.level() == player.level() ? n.distanceToSqr(player) : 1_000_000.0));
        return result;
    }

    private static String describeStatus(NovaEntity nova, boolean lockdown) {
        if (nova.isDormant()) {
            return "Chưa kích hoạt (Cạn pin)";
        }
        if (lockdown && nova.isInSittingPose()) {
            return "Khóa an toàn (Phòng thủ)";
        }
        if (nova.construction().unfinished() && nova.construction().allowed()) {
            return "Đang xây dựng / Đào hầm";
        }
        int workAct = nova.getWorkAction();
        if (workAct != NovaEntity.WORK_NONE) {
            return switch (workAct) {
                case NovaEntity.WORK_CHOP -> "Đang chặt cây";
                case NovaEntity.WORK_MINE -> "Đang đào mỏ";
                case NovaEntity.WORK_FARM -> "Đang làm ruộng";
                case NovaEntity.WORK_DIG -> "Đang đào đất / Dọn đường";
                case NovaEntity.WORK_PLANT -> "Đang trồng lại cây";
                case NovaEntity.WORK_CHEST -> "Đang cất đồ vào rương";
                default -> "Đang làm việc";
            };
        }
        if (nova.isRallying()) {
            return "Đang tập hợp đội hình";
        }
        if (nova.isCarryingChest() || nova.isFetchingChest()) {
            return "Đang vận chuyển rương";
        }
        if (nova.isNovaSitRequested() || nova.isInSittingPose()) {
            return "Đang ngồi nghỉ";
        }
        ItemStack primary = nova.getPrimaryTool().isEmpty() ? nova.getMainHandItem() : nova.getPrimaryTool();
        if (nova.isSwordNova() && (nova.isNovaWorkEnabled() || nova.getNovaMoveMode() == NovaEntity.MODE_FOLLOW)) {
            return "Vệ sĩ bảo vệ chủ nhân";
        }
        if (nova.isNovaWorkEnabled()) {
            if (primary.is(ItemTags.AXES)) return "Tiều phu (Chặt cây)";
            if (primary.is(ItemTags.PICKAXES)) return "Thợ mỏ (Đào quặng)";
            if (primary.is(ItemTags.HOES)) return "Nông dân (Làm ruộng)";
            if (primary.is(ItemTags.SHOVELS)) return "Công binh (Xây / Đào đất)";
            return "Đang bật Giao việc";
        }
        return switch (nova.getNovaMoveMode()) {
            case NovaEntity.MODE_FOLLOW -> "Đi theo chủ nhân";
            case NovaEntity.MODE_FREE -> "Rảnh rỗi (Tự do)";
            default -> "Đứng chờ tại chỗ";
        };
    }

    public static void sendSync(ServerPlayer player, boolean openScreen) {
        sendSync(player, openScreen, "");
    }

    public static void sendSync(ServerPlayer player, boolean openScreen, String feedback) {
        List<NovaEntity> novas = collectOwnedNovas(player);
        boolean lockdown = LOCKDOWN_STATE.getOrDefault(player.getUUID(), false);
        List<RobotInfo> infos = new ArrayList<>(novas.size());
        for (NovaEntity nova : novas) {
            String customName = nova.hasCustomName() ? nova.getCustomName().getString() : "";
            int energyPct = Mth.clamp(Math.round(nova.work().energy() * 100f / NovaWorkData.MAX_ENERGY), 0, 100);
            int hp = Math.max(0, Math.round(nova.getHealth()));
            int maxHp = Math.max(1, Math.round(nova.getMaxHealth()));
            int dist = nova.level() == player.level() ? Mth.floor(Math.sqrt(nova.distanceToSqr(player))) : 9999;
            boolean working = !nova.isDormant() && !nova.isNovaSitRequested()
                    && (nova.isNovaWorkEnabled() || nova.getWorkAction() != NovaEntity.WORK_NONE
                    || (nova.construction().unfinished() && nova.construction().allowed()));
            boolean lowBat = energyPct < 15 || nova.isDormant();
            boolean alert = lowBat || nova.getHealth() < nova.getMaxHealth() * 0.35F;

            ItemStack tool = nova.getPrimaryTool().isEmpty() ? nova.getMainHandItem() : nova.getPrimaryTool();
            String toolId = tool.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(tool.getItem()).toString();
            int toolDmg = tool.isEmpty() ? 0 : tool.getDamageValue();
            int toolMax = tool.isEmpty() ? 0 : tool.getMaxDamage();

            infos.add(new RobotInfo(
                    nova.getId(),
                    nova.getUUID(),
                    customName,
                    nova.getNovaDisplayName(),
                    energyPct,
                    hp,
                    maxHp,
                    nova.work().skillLevel(),
                    nova.work().experience(),
                    Math.max(1, nova.work().nextLevelCost()),
                    nova.getBlockX(),
                    nova.getBlockY(),
                    nova.getBlockZ(),
                    dist,
                    describeStatus(nova, lockdown),
                    working,
                    lowBat,
                    alert,
                    nova.isNovaSitRequested() || nova.isInSittingPose(),
                    nova.isDormant(),
                    nova.getNovaMoveMode(),
                    toolId,
                    toolDmg,
                    toolMax,
                    nova.getNovaFace()
            ));
        }

        ItemStack pad = findOmniPadStack(player);
        int reservePct = NovaOmniPadItem.getReserveEnergy(pad) / 10;
        int invRedstonePct = countInventoryRedstonePercent(player);
        ServerPlayNetworking.send(player, new SyncPayload(openScreen, reservePct, invRedstonePct, lockdown, feedback, infos));
    }

    private static int countInventoryRedstonePercent(ServerPlayer player) {
        if (player.getAbilities().instabuild) return 9990;
        int pct = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(Items.REDSTONE)) pct += s.getCount() * 20;
            else if (s.is(Items.REDSTONE_BLOCK)) pct += s.getCount() * 100;
        }
        return pct;
    }

    private static void applyMasterOverride(NovaEntity nova) {
        nova.toolBelt().restore();
        nova.pathClearing().cancel();
        nova.stopRally();
        nova.stopFetchingChest();
        nova.interruptCompanionAction();
        nova.companion().ownerCommand();
        nova.companion().retry.clear();
        nova.clearWorkAnimation();
        nova.getNavigation().stop();
        nova.removeEffect(MobEffects.RESISTANCE);
    }

    private static boolean equipToolByTag(NovaEntity nova, TagKey<Item> tag) {
        ItemStack main = nova.getMainHandItem();
        if (!main.isEmpty() && main.is(tag) && nova.companion().usableTool(main)) {
            return true;
        }
        // Search spareTools (4 slots)
        for (int i = 0; i < 4; i++) {
            ItemStack spare = nova.work().spareTools.getItem(i);
            if (!spare.isEmpty() && spare.is(tag) && nova.companion().usableTool(spare)) {
                nova.work().spareTools.setItem(i, main);
                nova.setItemSlot(EquipmentSlot.MAINHAND, spare);
                nova.setGuaranteedDrop(EquipmentSlot.MAINHAND);
                nova.work().spareTools.setChanged();
                return true;
            }
        }
        // Search backpack (27 slots)
        for (int i = 0; i < nova.getInventory().getContainerSize(); i++) {
            ItemStack inv = nova.getInventory().getItem(i);
            if (!inv.isEmpty() && inv.is(tag) && nova.companion().usableTool(inv)) {
                nova.getInventory().setItem(i, main);
                nova.setItemSlot(EquipmentSlot.MAINHAND, inv);
                nova.setGuaranteedDrop(EquipmentSlot.MAINHAND);
                nova.getInventory().setChanged();
                return true;
            }
        }
        return false;
    }

    private static List<NovaEntity> resolveTargets(ServerPlayer player, List<UUID> requestedUuids) {
        List<NovaEntity> all = collectOwnedNovas(player);
        if (requestedUuids == null || requestedUuids.isEmpty()) {
            return all;
        }
        Set<UUID> set = new HashSet<>(requestedUuids);
        List<NovaEntity> filtered = new ArrayList<>();
        for (NovaEntity n : all) {
            if (set.contains(n.getUUID())) {
                filtered.add(n);
            }
        }
        return filtered;
    }

    private static void handleAction(ServerPlayer player, ActionPayload payload) {
        int action = payload.action();
        if (action == ACTION_REFRESH) {
            sendSync(player, false);
            return;
        }

        List<NovaEntity> targets = resolveTargets(player, payload.targetUuids());
        String feedback = "";

        switch (action) {
            case ACTION_RENAME -> {
                if (!targets.isEmpty()) {
                    NovaEntity nova = targets.getFirst();
                    String newName = payload.textParam() == null ? "" : payload.textParam().strip();
                    if (newName.isEmpty()) {
                        nova.setCustomName(null);
                        nova.setCustomNameVisible(false);
                        feedback = "Đã xóa tên định danh riêng của NOVA.";
                    } else {
                        nova.setCustomName(Component.literal(newName));
                        nova.setCustomNameVisible(true);
                        feedback = "Đã định danh thành công: [" + newName + "]!";
                    }
                    nova.sounds().play(NovaSounds.Cue.ACK);
                    notifyActionbar(player, feedback, ChatFormatting.AQUA);
                }
            }
            case ACTION_OPEN_INVENTORY -> {
                if (!targets.isEmpty()) {
                    NovaEntity nova = targets.getFirst();
                    if (nova.level() == player.level()) {
                        nova.toolBelt().restore();
                        NovaMenus.open(player, nova);
                        return;
                    } else {
                        feedback = "NOVA đang ở chiều không gian khác, hãy Dịch Chuyển về trước!";
                        notifyActionbar(player, feedback, ChatFormatting.RED);
                    }
                }
            }
            case ACTION_TELEPORT_ONE, MASS_TELEPORT -> {
                if (!targets.isEmpty()) {
                    int count = teleportNovasToPlayer(player, targets);
                    feedback = "Dịch Chuyển Lượng Tử: Đã triệu hồi " + count + " NOVA về vị trí của bạn!";
                    notifyActionbar(player, feedback, ChatFormatting.AQUA);
                }
            }
            case ACTION_PING_ONE, MASS_PING -> {
                if (!targets.isEmpty()) {
                    for (NovaEntity nova : targets) {
                        nova.addEffect(new MobEffectInstance(MobEffects.GLOWING, 300, 0, false, false, true));
                        if (nova.level() instanceof ServerLevel sl) {
                            sl.sendParticles(ParticleTypes.END_ROD, nova.getX(), nova.getY() + 1.2, nova.getZ(), 18, 0.25, 0.6, 0.25, 0.03);
                            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, nova.getX(), nova.getY() + 0.8, nova.getZ(), 15, 0.3, 0.4, 0.3, 0.06);
                        }
                        nova.sounds().play(NovaSounds.Cue.ACK);
                    }
                    player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.5F, 1.5F);
                    if (targets.size() == 1) {
                        NovaEntity n = targets.getFirst();
                        feedback = "Radar Ping: [" + n.getNovaDisplayName() + "] phát sáng xuyên tường 15s tại ("
                                + n.getBlockX() + ", " + n.getBlockY() + ", " + n.getBlockZ() + ")!";
                    } else {
                        feedback = "Radar Ping: Đã quét phát sáng xuyên tường 15s cho " + targets.size() + " NOVA!";
                    }
                    notifyActionbar(player, feedback, ChatFormatting.GREEN);
                }
            }
            case ACTION_LOAD_PAD_ENERGY -> {
                feedback = loadReserveEnergyFromInventory(player);
                notifyActionbar(player, feedback, ChatFormatting.AQUA);
            }
            case ACTION_CHARGE_ONE, MASS_WIRELESS_CHARGE -> {
                List<NovaEntity> toCharge = targets;
                if (action == MASS_WIRELESS_CHARGE && payload.targetUuids().isEmpty()) {
                    toCharge = collectOwnedNovas(player);
                }
                feedback = wirelessChargeNovas(player, toCharge);
                notifyActionbar(player, feedback, ChatFormatting.GREEN);
            }
            case MASS_WORK_ON, MASS_WORK_CHOP, MASS_WORK_MINE, MASS_WORK_FARM, MASS_WORK_DIG -> {
                int count = 0;
                for (NovaEntity nova : targets) {
                    if (nova.isDormant()) continue;
                    applyMasterOverride(nova);
                    nova.setNovaSitRequested(false);
                    if (action == MASS_WORK_CHOP) equipToolByTag(nova, ItemTags.AXES);
                    else if (action == MASS_WORK_MINE) equipToolByTag(nova, ItemTags.PICKAXES);
                    else if (action == MASS_WORK_FARM) equipToolByTag(nova, ItemTags.HOES);
                    else if (action == MASS_WORK_DIG) equipToolByTag(nova, ItemTags.SHOVELS);
                    nova.setNovaMoveMode(NovaEntity.MODE_FREE);
                    nova.setNovaWorkEnabled(true);
                    if (nova.isSwordNova()) {
                        NovaWorkSupport.ensureOffhandSword(nova);
                    }
                    nova.sounds().play(NovaSounds.Cue.ACK);
                    count++;
                }
                LOCKDOWN_STATE.put(player.getUUID(), false);
                String jobName = switch (action) {
                    case MASS_WORK_CHOP -> "Chặt cây";
                    case MASS_WORK_MINE -> "Đào mỏ";
                    case MASS_WORK_FARM -> "Làm ruộng";
                    case MASS_WORK_DIG -> "Dọn đường / Đào đất";
                    default -> "Làm việc";
                };
                feedback = "Ghi Đè Tối Thượng: Đã phát lệnh [" + jobName + "] cho " + count + " NOVA!";
                notifyActionbar(player, feedback, ChatFormatting.AQUA);
            }
            case MASS_WORK_OFF -> {
                int count = 0;
                for (NovaEntity nova : targets) {
                    if (nova.isDormant()) continue;
                    applyMasterOverride(nova);
                    nova.setNovaWorkEnabled(false);
                    nova.sounds().play(NovaSounds.Cue.ACK);
                    count++;
                }
                feedback = "Đã tắt chế độ làm việc của " + count + " NOVA.";
                notifyActionbar(player, feedback, ChatFormatting.YELLOW);
            }
            case MASS_FOLLOW -> {
                int count = 0;
                for (NovaEntity nova : targets) {
                    if (nova.isDormant()) continue;
                    applyMasterOverride(nova);
                    nova.setNovaSitRequested(false);
                    nova.setNovaMoveMode(NovaEntity.MODE_FOLLOW);
                    if (!nova.isSwordNova()) nova.setNovaWorkEnabled(false);
                    nova.sounds().play(NovaSounds.Cue.ACK);
                    count++;
                }
                LOCKDOWN_STATE.put(player.getUUID(), false);
                feedback = "Đội hình [" + count + " NOVA] đã chuyển sang chế độ: Đi theo bảo vệ chủ nhân!";
                notifyActionbar(player, feedback, ChatFormatting.AQUA);
            }
            case MASS_WAIT -> {
                int count = 0;
                for (NovaEntity nova : targets) {
                    if (nova.isDormant()) continue;
                    applyMasterOverride(nova);
                    nova.setNovaWorkEnabled(false);
                    nova.setNovaSitRequested(false);
                    nova.setNovaMoveMode(NovaEntity.MODE_WAIT);
                    nova.sounds().play(NovaSounds.Cue.ACK);
                    count++;
                }
                feedback = "Đội hình [" + count + " NOVA] đã chuyển sang chế độ: Đứng yên tại chỗ!";
                notifyActionbar(player, feedback, ChatFormatting.YELLOW);
            }
            case MASS_FREE -> {
                int count = 0;
                for (NovaEntity nova : targets) {
                    if (nova.isDormant()) continue;
                    applyMasterOverride(nova);
                    nova.setNovaSitRequested(false);
                    nova.setNovaMoveMode(NovaEntity.MODE_FREE);
                    nova.sounds().play(NovaSounds.Cue.ACK);
                    count++;
                }
                LOCKDOWN_STATE.put(player.getUUID(), false);
                feedback = "Đội hình [" + count + " NOVA] đã chuyển sang chế độ: Hoạt động tự do!";
                notifyActionbar(player, feedback, ChatFormatting.AQUA);
            }
            case MASS_RALLY -> {
                int count = 0;
                Direction facing = player.getDirection();
                BlockPos base = player.blockPosition();
                int[] cols = {0, -2, 2, -4, 4};
                for (int i = 0; i < targets.size(); i++) {
                    NovaEntity nova = targets.get(i);
                    if (nova.isDormant()) continue;
                    applyMasterOverride(nova);
                    nova.setNovaWorkEnabled(false);
                    nova.setNovaSitRequested(false);
                    nova.setNovaMoveMode(NovaEntity.MODE_FOLLOW);
                    BlockPos formPos = base.relative(facing, 2 + (i / cols.length) * 2)
                            .relative(facing.getClockWise(), cols[i % cols.length]);
                    nova.startRally(formPos, base);
                    count++;
                }
                if (count > 0) {
                    targets.getFirst().sounds().play(NovaSounds.Cue.RALLY);
                    player.level().playSound(null, base, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6F, 1.4F);
                }
                LOCKDOWN_STATE.put(player.getUUID(), false);
                feedback = "Tập Hợp Đội Hình: " + count + " NOVA đang tiến về vị trí của bạn!";
                notifyActionbar(player, feedback, ChatFormatting.AQUA);
            }
            case MASS_RETURN_CHEST -> {
                int count = 0;
                for (NovaEntity nova : targets) {
                    if (nova.isDormant()) continue;
                    applyMasterOverride(nova);
                    nova.setNovaWorkEnabled(false);
                    nova.setNovaSitRequested(false);
                    BlockPos chest = nova.getLinkedChest();
                    if (chest != null && nova.level() == player.level()) {
                        nova.setNovaMoveMode(NovaEntity.MODE_WAIT);
                        nova.startRally(chest.above(), chest);
                        count++;
                    } else {
                        nova.setNovaMoveMode(NovaEntity.MODE_WAIT);
                        nova.setNovaSitRequested(true);
                        count++;
                    }
                }
                feedback = "Đã lệnh cho " + count + " NOVA quay về rương liên kết / nghỉ ngơi!";
                notifyActionbar(player, feedback, ChatFormatting.GREEN);
            }
            case MASS_SMART_ASSIGN -> {
                feedback = smartAutoAssign(player, targets.isEmpty() ? collectOwnedNovas(player) : targets);
                LOCKDOWN_STATE.put(player.getUUID(), false);
                notifyActionbar(player, feedback, ChatFormatting.GREEN);
            }
            case MASS_LOCKDOWN_TOGGLE -> {
                boolean newLockdown = !LOCKDOWN_STATE.getOrDefault(player.getUUID(), false);
                LOCKDOWN_STATE.put(player.getUUID(), newLockdown);
                List<NovaEntity> all = collectOwnedNovas(player);
                int count = 0;
                for (NovaEntity nova : all) {
                    if (nova.isDormant()) continue;
                    applyMasterOverride(nova);
                    if (newLockdown) {
                        nova.setNovaWorkEnabled(false);
                        nova.setNovaMoveMode(NovaEntity.MODE_WAIT);
                        nova.setNovaSitRequested(true);
                        nova.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 2400, 2, false, false, true));
                        nova.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 0, false, false, true));
                        nova.showNovaFace(39, 100);
                    } else {
                        nova.setNovaSitRequested(false);
                        nova.removeEffect(MobEffects.RESISTANCE);
                        nova.setNovaMoveMode(NovaEntity.MODE_FOLLOW);
                        nova.showNovaFace(0, 60);
                    }
                    count++;
                }
                player.level().playSound(null, player.blockPosition(),
                        newLockdown ? SoundEvents.BEACON_DEACTIVATE : SoundEvents.BEACON_ACTIVATE,
                        SoundSource.PLAYERS, 0.7F, newLockdown ? 0.85F : 1.35F);
                feedback = newLockdown
                        ? "KHÓA AN TOÀN TOÀN CỤC: " + count + " NOVA đã ngừng việc & bật giáp phòng thủ!"
                        : "ĐÃ MỞ KHÓA AN TOÀN: " + count + " NOVA đã thức dậy và sẵn sàng nhận lệnh!";
                notifyActionbar(player, feedback, newLockdown ? ChatFormatting.GOLD : ChatFormatting.GREEN);
            }
            default -> {}
        }

        sendSync(player, false, feedback);
    }

    private static int teleportNovasToPlayer(ServerPlayer player, List<NovaEntity> targets) {
        if (!(player.level() instanceof ServerLevel targetLevel)) return 0;
        Direction facing = player.getDirection();
        BlockPos origin = player.blockPosition();
        int[] offsets = {0, -1, 1, -2, 2, -3, 3};
        int count = 0;
        for (int i = 0; i < targets.size(); i++) {
            NovaEntity nova = targets.get(i);
            if (!nova.isAlive()) continue;
            applyMasterOverride(nova);
            if (!nova.isDormant()) {
                nova.setNovaSitRequested(false);
            }
            int row = i / offsets.length;
            int col = offsets[i % offsets.length];
            BlockPos dest = origin.relative(facing, 2 + row * 2).relative(facing.getClockWise(), col);
            double tx = dest.getX() + 0.5;
            double ty = player.getY();
            double tz = dest.getZ() + 0.5;
            float yaw = facing.getOpposite().toYRot();

            if (nova.level() == targetLevel) {
                nova.teleportTo(tx, ty, tz);
                nova.setYRot(yaw);
                nova.setYHeadRot(yaw);
                nova.setYBodyRot(yaw);
            } else {
                nova.teleportTo(targetLevel, tx, ty, tz, Set.of(), yaw, 0.0F, true);
            }
            nova.setDeltaMovement(0, 0, 0);
            targetLevel.sendParticles(ParticleTypes.PORTAL, tx, ty + 0.6, tz, 24, 0.3, 0.5, 0.3, 0.15);
            targetLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, tx, ty + 0.8, tz, 16, 0.3, 0.4, 0.3, 0.08);
            nova.sounds().play(NovaSounds.Cue.ACK);
            count++;
        }
        if (count > 0) {
            targetLevel.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.75F, 1.25F);
        }
        return count;
    }

    private static String loadReserveEnergyFromInventory(ServerPlayer player) {
        ItemStack pad = findOmniPadStack(player);
        if (pad.isEmpty()) return "Không tìm thấy Omni-Pad trong túi đồ.";
        int current = NovaOmniPadItem.getReserveEnergy(pad);
        if (current >= NovaOmniPadItem.MAX_RESERVE_ENERGY) {
            return "Khe Năng Lượng Dự Phòng của Omni-Pad đã đầy tối đa (1000%)!";
        }
        if (player.getAbilities().instabuild) {
            NovaOmniPadItem.setReserveEnergy(pad, NovaOmniPadItem.MAX_RESERVE_ENERGY);
            player.level().playSound(null, player.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 0.5F, 1.4F);
            return "Đã nạp đầy 1000% năng lượng dự phòng cho Omni-Pad (Sáng tạo)!";
        }
        int added = 0;
        for (int i = 0; i < player.getInventory().getContainerSize() && current + added < NovaOmniPadItem.MAX_RESERVE_ENERGY; i++) {
            ItemStack s = player.getInventory().getItem(i);
            while (!s.isEmpty() && s.is(Items.REDSTONE_BLOCK) && current + added + 1000 <= NovaOmniPadItem.MAX_RESERVE_ENERGY) {
                s.shrink(1);
                added += 1000;
            }
            while (!s.isEmpty() && s.is(Items.REDSTONE) && current + added + 200 <= NovaOmniPadItem.MAX_RESERVE_ENERGY) {
                s.shrink(1);
                added += 200;
            }
        }
        if (added == 0) {
            return "Cần Bụi Đá Đỏ (+20%) hoặc Khối Đá Đỏ (+100%) trong túi đồ để nạp vào Omni-Pad!";
        }
        NovaOmniPadItem.setReserveEnergy(pad, current + added);
        player.level().playSound(null, player.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 0.55F, 1.35F);
        return "Đã nạp +" + (added / 10) + "% vào Omni-Pad (Dự trữ hiện tại: " + ((current + added) / 10) + "%)!";
    }

    private static String wirelessChargeNovas(ServerPlayer player, List<NovaEntity> targets) {
        if (targets.isEmpty()) return "Chưa có NOVA nào để truyền điện.";
        ItemStack pad = findOmniPadStack(player);
        int reserve = NovaOmniPadItem.getReserveEnergy(pad);
        boolean creative = player.getAbilities().instabuild;

        int chargedCount = 0;
        for (NovaEntity nova : targets) {
            int needed = NovaWorkData.MAX_ENERGY - nova.work().energy();
            boolean needHeal = nova.getHealth() < nova.getMaxHealth();
            if (needed <= 0 && !needHeal && !nova.isDormant()) continue;

            int cost = Math.max(200, needed);
            if (!creative) {
                // Pull from inventory into reserve if needed
                while (reserve < cost) {
                    int pulled = pullOneRedstoneFromInventory(player);
                    if (pulled <= 0) break;
                    reserve += pulled;
                }
                if (reserve <= 0) break;
                int actualGive = Math.min(needed, reserve);
                if (nova.isDormant()) {
                    if (reserve < NovaWorkData.MAX_ENERGY) continue;
                    reserve -= NovaWorkData.MAX_ENERGY;
                    nova.work().setEnergy(NovaWorkData.MAX_ENERGY);
                    nova.setDormant(false);
                    nova.setNoAi(false);
                } else {
                    nova.work().recharge(actualGive > 0 ? actualGive : 200);
                    reserve = Math.max(0, reserve - Math.max(200, actualGive));
                }
            } else {
                nova.work().setEnergy(NovaWorkData.MAX_ENERGY);
                if (nova.isDormant()) {
                    nova.setDormant(false);
                    nova.setNoAi(false);
                }
            }

            if (nova.getHealth() < nova.getMaxHealth()) {
                nova.heal(6.0F);
            }
            nova.showNovaFace(1, 50);
            nova.sounds().play(NovaSounds.Cue.RECHARGE_FULL);
            if (nova.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, nova.getX(), nova.getY() + 0.8, nova.getZ(), 22, 0.35, 0.45, 0.35, 0.08);
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, nova.getX(), nova.getY() + 1.0, nova.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
            }
            chargedCount++;
        }

        if (!pad.isEmpty() && !creative) {
            NovaOmniPadItem.setReserveEnergy(pad, reserve);
        }
        if (chargedCount == 0) {
            return "Các NOVA đã đầy 100% pin (hoặc bạn đã hết Đá Đỏ dự phòng)!";
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.6F, 1.6F);
        return "Truyền Điện Không Dây: Đã sạc pin & hồi phục từ xa cho " + chargedCount + " NOVA!";
    }

    private static int pullOneRedstoneFromInventory(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(Items.REDSTONE_BLOCK)) {
                s.shrink(1);
                return 1000;
            }
            if (s.is(Items.REDSTONE)) {
                s.shrink(1);
                return 200;
            }
        }
        return 0;
    }

    private static String smartAutoAssign(ServerPlayer player, List<NovaEntity> targets) {
        int chop = 0, mine = 0, guard = 0, farm = 0, dig = 0, unassigned = 0;
        for (NovaEntity nova : targets) {
            if (nova.isDormant()) continue;
            applyMasterOverride(nova);
            nova.setNovaSitRequested(false);

            // Check current mainhand or find best usable tool in spareTools / backpack
            ItemStack main = nova.getMainHandItem();
            if (main.isEmpty() || !NovaWorkSupport.isTool(main) || !nova.companion().usableTool(main)) {
                if (!equipToolByTag(nova, ItemTags.AXES)
                        && !equipToolByTag(nova, ItemTags.PICKAXES)
                        && !equipToolByTag(nova, ItemTags.SWORDS)
                        && !equipToolByTag(nova, ItemTags.HOES)
                        && !equipToolByTag(nova, ItemTags.SHOVELS)) {
                    unassigned++;
                    continue;
                }
                main = nova.getMainHandItem();
            }

            if (main.is(ItemTags.SWORDS)) {
                NovaWorkSupport.ensureOffhandSword(nova);
                nova.setNovaMoveMode(NovaEntity.MODE_FOLLOW);
                nova.setNovaWorkEnabled(true);
                guard++;
            } else if (main.is(ItemTags.AXES)) {
                nova.setNovaMoveMode(NovaEntity.MODE_FREE);
                nova.setNovaWorkEnabled(true);
                chop++;
            } else if (main.is(ItemTags.PICKAXES)) {
                nova.setNovaMoveMode(NovaEntity.MODE_FREE);
                nova.setNovaWorkEnabled(true);
                mine++;
            } else if (main.is(ItemTags.HOES)) {
                nova.setNovaMoveMode(NovaEntity.MODE_FREE);
                nova.setNovaWorkEnabled(true);
                farm++;
            } else if (main.is(ItemTags.SHOVELS)) {
                nova.setNovaMoveMode(NovaEntity.MODE_FREE);
                nova.setNovaWorkEnabled(true);
                dig++;
            } else {
                unassigned++;
            }
            nova.sounds().play(NovaSounds.Cue.ACK);
        }

        List<String> parts = new ArrayList<>();
        if (chop > 0) parts.add(chop + " Tiều phu");
        if (mine > 0) parts.add(mine + " Thợ mỏ");
        if (guard > 0) parts.add(guard + " Vệ sĩ");
        if (farm > 0) parts.add(farm + " Nông dân");
        if (dig > 0) parts.add(dig + " Công binh");
        if (parts.isEmpty()) {
            return "Chưa có NOVA nào cầm công cụ trong tay/ba lô để tự động phân công!";
        }
        return "Tự Động Phân Công: " + String.join(", ", parts)
                + (unassigned > 0 ? " (" + unassigned + " thiếu công cụ)" : "") + "!";
    }

    private static void notifyActionbar(ServerPlayer player, String text, ChatFormatting color) {
        if (text != null && !text.isEmpty()) {
            player.displayClientMessage(Component.literal("[OMNI-PAD] " + text).withStyle(color), true);
        }
    }
}
