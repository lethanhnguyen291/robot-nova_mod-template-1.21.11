package net.teogemini.nova.entity.companion;

import java.util.EnumMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.teogemini.nova.entity.NovaEntity;

/** Coordinates intentions; individual work Goals continue to own their jobs. */
public final class NovaCompanion {
    public static final int NORMAL=0, RETURNING=1, RESTING=2, RAIN=3, SOCIAL=4, HELP=5, PANIC=6;
    public enum Notice { BATTERY, TOOL, FULL, PATH, CHEST, RAIN, COMPLETE, THANKS }
    private final NovaEntity nova;
    private final NovaMemories memories = new NovaMemories();
    public final NovaRetryMemory retry = new NovaRetryMemory();
    private final EnumMap<Notice, Long> notices = new EnumMap<>(Notice.class);
    private int panicTicks, ownerAbsentTicks, stuckTicks, quietTicks, finishedBlocks;
    private long pauseWorkUntil, helpUntil, nextBond, nextCommand, nextRoutine, nextSocial, gestureUntil;
    private Vec3 lastProgress, trappedAt, danger;
    private BlockPos openChest;
    private int chestTicks;
    private boolean toolBlocked;
    private boolean ownerHelped;

    public NovaCompanion(NovaEntity nova) { this.nova=nova; }
    public NovaMemories memories() { return memories; }
    public long now() { return nova.level().getGameTime(); }
    public boolean panicking() { return panicTicks > 0; }
    public Vec3 danger() { return danger == null ? nova.position() : danger; }
    public boolean workAllowed() {
        return !panicking() && now() >= pauseWorkUntil
                && (nova.work().energy()>100 || nova.getWorkAction()!=NovaEntity.WORK_NONE)
                && (nova.getCompanionState() == NORMAL || nova.getCompanionState() == SOCIAL);
    }
    public boolean controlsLook() {
        return nova.getCompanionState() != NORMAL || nova.work().inventoryOpen();
    }
    public boolean idleAllowed() {
        return nova.isTame() && nova.isAlive() && !nova.isNoAi() && !panicking()
                && !nova.work().inventoryOpen() && !nova.isOrderedToSit()
                && !nova.isPerformingAction() && !nova.isRallying()
                && !nova.isCarryingChest() && nova.getNovaMoveMode() != NovaEntity.MODE_WAIT;
    }
    public boolean socialReady() { return now() >= nextSocial; }
    public void postponeSocial() {
        nextSocial = now() + memories.personality().socialDelay + nova.getRandom().nextInt(401);
    }
    public boolean routineReady() { return now() >= nextRoutine; }
    public void postponeRoutine() { nextRoutine = now() + 200; }
    public boolean ownerMissing() { return ownerAbsentTicks >= 600; }
    public boolean wantsHome() {
        if (!idleAllowed() || nova.getLinkedChest() == null) return false;
        if (nova.getNovaMoveMode() == NovaEntity.MODE_FOLLOW) return ownerMissing();
        long day = Math.floorMod(nova.level().getDayTime(), 24000);
        return nova.work().energy() <= 100 || nova.level().dimensionType().hasSkyLight()
                && day >= 12500 && day < 23000;
    }
    public boolean raining() { return nova.level().isRainingAt(nova.blockPosition()); }
    public void state(int value) { nova.setCompanionState(value); }
    public void hurt(Vec3 source) {
        panicTicks=160; danger=source; state(PANIC);
        nova.interruptCompanionAction();
        nova.clearWorkAnimation();
        nova.getNavigation().stop();
    }
    public void ownerCommand() {
        // Explicit commands cancel voluntary routines, never erase memories or work settings.
        if (!panicking()) state(NORMAL);
        pauseWorkUntil=0; helpUntil=0; retry.clear();
        postponeRoutine(); postponeSocial();
        cancelGesture();
        nova.setInSittingPose(false);
    }
    public boolean acceptCommand() {
        if (now() < nextCommand) return false;
        nextCommand=now()+10; return true;
    }
    public void gesture(String name) {
        if (nova.isCarryingChest() || nova.isPerformingAction() || panicking()
                || nova.getWorkAction()!=NovaEntity.WORK_NONE || now()<gestureUntil) return;
        nova.triggerAnim("companion_gesture", name);
        gestureUntil=now()+60;
    }
    public void cancelGesture() {
        if (gestureUntil>now()) nova.stopTriggeredAnim("companion_gesture",null);
        gestureUntil=0;
    }
    public void notifyOwner(Notice type, String message) {
        if (now() < notices.getOrDefault(type, Long.MIN_VALUE)) return;
        if (nova.getOwner() instanceof ServerPlayer player && player.level() == nova.level()
                && player.distanceToSqr(nova) <= 64 * 64) {
            player.displayClientMessage(Component.literal(nova.getName().getString()+": "+message), true);
            notices.put(type, now()+1200);
            if (type==Notice.BATTERY || type==Notice.FULL || type==Notice.TOOL) gesture("point");
        }
    }
    public void stuck(BlockPos target) {
        if (target != null) retry.fail(target, now());
        pauseWorkUntil=now()+200;
        if (nova.getNovaMoveMode() == NovaEntity.MODE_FREE && !panicking()) {
            state(HELP); helpUntil=now()+60;
        }
        trappedAt=nova.position();
        ownerHelped=false;
        nova.clearWorkAnimation(); nova.getNavigation().stop();
        gesture("point");
        notifyOwner(Notice.PATH, "Mình chưa tới được chỗ đó. Bạn giúp mình mở đường nhé.");
    }
    public void storageBlocked() {
        pauseWorkUntil=now()+200;
        gesture("point");
        notifyOwner(Notice.CHEST,"Các rương được phép đều chưa cất được đồ. Mình giữ đồ và chờ bạn nhé.");
    }
    public void ownerClearedPath(BlockPos pos) {
        if (trappedAt!=null && Vec3.atCenterOf(pos).distanceToSqr(trappedAt)<36) ownerHelped=true;
    }
    public void recharged(int before) {
        if (before == 0) memories.chargedFromEmpty();
        if (now() >= nextBond) { memories.growBond(1); nextBond=now()+1200; }
        if (nova.getCompanionState() == RESTING || nova.getCompanionState() == HELP) state(NORMAL);
        gesture("nod"); nova.showNovaFace(0, 40);
        notifyOwner(Notice.THANKS, "Cảm ơn bạn, mình thấy khỏe hơn rồi!");
    }
    public void worked(BlockState block) {
        if (nova.work().energy()<=100) pauseWorkUntil=now()+200;
        finishedBlocks=Math.min(999,finishedBlocks+1); quietTicks=0;
        if (block.is(BlockTags.LOGS)) memories.log();
        else if (block.is(BlockTags.DIAMOND_ORES) || block.is(BlockTags.COAL_ORES)
                || block.is(BlockTags.IRON_ORES) || block.is(BlockTags.COPPER_ORES)
                || block.is(BlockTags.GOLD_ORES) || block.is(BlockTags.EMERALD_ORES)
                || block.is(BlockTags.REDSTONE_ORES) || block.is(BlockTags.LAPIS_ORES)) memories.mined();
        else if (block.is(BlockTags.CROPS)) memories.harvested();
        if (block.is(BlockTags.DIAMOND_ORES)) memories.diamond();
    }
    public void planted() {
        if (nova.work().energy()<=100) pauseWorkUntil=now()+200;
        memories.planted(); finishedBlocks=Math.min(999,finishedBlocks+1); quietTicks=0;
    }
    public void delivered(int count) {
        if (count <= 0) return;
        memories.delivered(count); memories.remember("Đã cất an toàn "+count+" vật phẩm vào rương.");
        gesture("nod");
    }
    public boolean completionPending() { return finishedBlocks>0 && quietTicks>=60; }
    public void reportCompletion() {
        if (!completionPending()) return;
        notifyOwner(Notice.COMPLETE, "Mình vừa xử lý xong "+finishedBlocks+" khối/trồng cây.");
        if (now() >= nextBond) { memories.growBond(1); nextBond=now()+1200; }
        finishedBlocks=0; quietTicks=0;
    }
    public void openChest(BlockPos pos) { openChest=pos.immutable(); chestTicks=0; }
    public void closeChest() { openChest=null; chestTicks=0; }
    public boolean hasOpenChest(BlockPos pos) {
        return openChest != null && nova.isAlive() && !nova.isRemoved()
                && NovaChestAccess.sameChest(nova, openChest, pos);
    }
    public int contextFace() {
        if (panicking()) return 32;
        return switch (nova.getCompanionState()) {
            case RESTING -> 4;
            case RAIN, HELP -> 2;
            default -> openChest != null ? (chestTicks<40 ? 39 : 1)
                    : nova.work().inventoryOpen() ? 2 : nova.work().energy()<=100 ? 6 : -1;
        };
    }
    public boolean usableTool(ItemStack stack) {
        return !stack.isDamageableItem() || stack.getMaxDamage()-stack.getDamageValue()>5;
    }
    private static boolean sameTool(ItemStack a, ItemStack b) {
        return a.is(ItemTags.AXES)&&b.is(ItemTags.AXES) || a.is(ItemTags.PICKAXES)&&b.is(ItemTags.PICKAXES)
                || a.is(ItemTags.HOES)&&b.is(ItemTags.HOES) || a.is(ItemTags.SHOVELS)&&b.is(ItemTags.SHOVELS)
                || a.is(ItemTags.SWORDS)&&b.is(ItemTags.SWORDS);
    }
    private void checkTool() {
        ItemStack held=nova.getMainHandItem();
        toolBlocked=!usableTool(held);
        if (!toolBlocked || !nova.work().enabled() || nova.work().inventoryOpen() || nova.isCarryingChest()) return;
        if (memories.autoTools()) {
            for (int i=0;i<4;i++) {
                ItemStack spare=nova.work().spareTools.getItem(i);
                if (!spare.isEmpty() && sameTool(held,spare) && usableTool(spare)) {
                    nova.work().spareTools.setItem(i, ItemStack.EMPTY);
                    nova.setItemSlot(EquipmentSlot.MAINHAND, spare);
                    nova.setGuaranteedDrop(EquipmentSlot.MAINHAND);
                    nova.work().spareTools.setItem(i,held);
                    toolBlocked=false; gesture("nod"); return;
                }
            }
        }
        notifyOwner(Notice.TOOL,"Công cụ gần hỏng rồi. Bạn đổi giúp mình nhé.");
    }
    public void tick() {
        if (nova.level().isClientSide() || !nova.isAlive() || !nova.isTame() || nova.isNoAi()) return;
        memories.initialize(nova.getUUID().hashCode());
        if (panicTicks>0 && --panicTicks==0) { state(NORMAL); nova.setNovaRunning(false); }
        if (nova.getCompanionState()==HELP && now()>=helpUntil) state(NORMAL);
        if (openChest!=null) chestTicks++;
        if (nova.getWorkAction()==NovaEntity.WORK_NONE) quietTicks=Math.min(60,quietTicks+1); else quietTicks=0;
        var owner=nova.getOwner();
        if (owner==null || !owner.isAlive() || owner.level()!=nova.level()) ownerAbsentTicks=Math.min(600,ownerAbsentTicks+1);
        else {
            ownerAbsentTicks=0; memories.meet(owner.getName().getString());
            if (ownerHelped && trappedAt!=null && nova.position().distanceToSqr(trappedAt)>4
                    && nova.distanceToSqr(owner)<36 && now()>=pauseWorkUntil) {
                memories.rescued(); trappedAt=null; ownerHelped=false; gesture("nod");
            }
        }
        if (nova.tickCount%20 != Math.floorMod(nova.getId(),20)) return;
        checkTool();
        if (nova.work().energy()<=100) notifyOwner(Notice.BATTERY,"Pin mình còn "+nova.work().energy()/10+"%. Mình cần redstone.");
        boolean full=true;
        for (int i=0;i<nova.getInventory().getContainerSize();i++)
            if (nova.getInventory().getItem(i).isEmpty()) { full=false; break; }
        if (full && nova.work().enabled()) notifyOwner(Notice.FULL,"Ba lô mình đầy rồi, mình cần chỗ cất đồ.");
        if (!panicking() && !nova.work().inventoryOpen() && nova.getNavigation().isInProgress()) {
            if (lastProgress!=null && nova.position().distanceToSqr(lastProgress)<0.04) stuckTicks+=20;
            else stuckTicks=0;
            if (stuckTicks>=100) { stuck(nova.getNavigation().getTargetPos()); stuckTicks=0; }
        } else stuckTicks=0;
        lastProgress=nova.position();
    }
}
