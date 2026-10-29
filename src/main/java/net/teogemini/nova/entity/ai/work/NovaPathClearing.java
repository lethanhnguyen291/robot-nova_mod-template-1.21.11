package net.teogemini.nova.entity.ai.work;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.teogemini.nova.entity.NovaEntity;
import net.teogemini.nova.entity.NovaWorkSupport;
import net.teogemini.nova.entity.ai.tree.NovaTreeHarvest;
import net.teogemini.nova.entity.ai.tree.NovaTreeService;
import net.teogemini.nova.entity.companion.NovaCompanionPaths;

/** Bounded, server-only rescue of an existing work destination; never selects a new job. */
public final class NovaPathClearing {
    public static final int MAX_BLOCKS = 8, MAX_TICKS = 600;
    private static final Map<ServerLevel, NovaTreeService.TickBudget> PATH_BUDGETS = new WeakHashMap<>();
    private final NovaEntity nova;
    private BlockPos destination, obstacle, jobDestination;
    private BlockState expected;
    private Vec3 lastProgress;
    private long requestedAt, lastUsefulAt, nextRequest, nextDecision;
    private double bestDistance;
    private int blocks, actionTicks, breakTicks, noProgress;
    private boolean running, needPath;

    public NovaPathClearing(NovaEntity nova) { this.nova = nova; }
    public boolean busy() { return destination != null; }
    public void track(BlockPos target) { jobDestination = target == null ? null : target.immutable(); }
    public BlockPos jobDestination() { return jobDestination; }
    public boolean allowed() {
        return nova.canNovaWork() && nova.work().energy() > 100
                && NovaWorkSupport.isTool(nova.getPrimaryTool())
                && nova.companion().usableTool(nova.getPrimaryTool());
    }

    private boolean area(BlockPos pos) {
        // Lumberjacks already have a 32-block workspace; other professions keep 16.
        return nova.getPrimaryTool().is(ItemTags.AXES)
                ? NovaTreeService.inside(nova, pos) : nova.isWithinWorkspace(pos);
    }

    public boolean request(BlockPos target) {
        if (busy()) return true;
        long now = nova.level().getGameTime();
        if (target == null || !allowed() || now < nextRequest || !area(target)) return false;
        destination = target.immutable();
        requestedAt = now;
        lastUsefulAt = now;
        bestDistance = nova.distanceToSqr(Vec3.atCenterOf(target));
        blocks = actionTicks = noProgress = 0;
        running = false;
        return true;
    }

    /** Cancels pending work too, even if a higher-priority Goal prevented start(). */
    public void maintain() {
        if (nova.level().isClientSide() || !busy()) return;
        if (!allowed() || !running && nova.level().getGameTime() - requestedAt > 60) cancel();
    }

    public void start() {
        running = true;
        needPath = true;
        nextDecision = 0;
        lastProgress = nova.position();
        nova.getNavigation().stop();
        nova.clearWorkAnimation();
    }

    public void cancel() {
        boolean wasBusy = busy();
        clearCracks();
        destination = obstacle = null;
        expected = null;
        running = false;
        nova.toolBelt().restore();
        if (wasBusy) {
            jobDestination = null;
            nextRequest = nova.level().getGameTime() + 20;
            nova.getNavigation().stop();
            nova.clearWorkAnimation();
        }
    }

    private void fail(String reason) {
        BlockPos target = destination;
        cancel();
        nextRequest = nova.level().getGameTime() + 200;
        nova.companion().needsPathHelp(target, reason);
    }

    private void finish() {
        nova.companion().retry.success(destination);
        cancel();
    }

    public void tick() {
        if (!busy() || !allowed()) { cancel(); return; }
        long now = nova.level().getGameTime();
        if (now - lastUsefulAt >= MAX_TICKS) {
            fail("Mình đã thử mở một đoạn đường nhưng chưa tới nơi. Bạn kiểm tra lối đi giúp mình nhé.");
            return;
        }
        if (obstacle != null) { chop(); return; }
        if (now < nextDecision) return;
        nextDecision = now + 10;
        double distance = nova.distanceToSqr(Vec3.atCenterOf(destination));
        if (distance < bestDistance - .25) { bestDistance = distance; lastUsefulAt = now; }
        if (nova.position().distanceToSqr(lastProgress) < .04) noProgress += 10;
        else noProgress = 0;
        lastProgress = nova.position();
        if (NovaWorkSupport.canReach(nova, destination)
                && nova.blockPosition().distSqr(destination) <= 4) { finish(); return; }
        if (nova.getNavigation().isInProgress() && noProgress < 40) return;
        nova.getNavigation().stop();
        Path path = null;
        if (needPath) {
            ServerLevel level = (ServerLevel) nova.level();
            if (!PATH_BUDGETS.computeIfAbsent(level, l -> new NovaTreeService.TickBudget(2))
                    .take(level.getServer().getTickCount())) return;
            // One bounded search; incomplete paths can approach the near side of a wall.
            path = nova.getNavigation().createPath(destination, 1, 32);
            needPath = false;
            if (usablePath(path) && path.canReach()) { walk(path); return; }
        }
        BlockPos found = firstObstacle();
        if (found != null) {
            BlockState state = nova.level().getBlockState(found);
            int slot = NovaClearPathRules.chooseTool(nova.toolBelt(), state);
            if (!safeToBreak(found, state) || slot < 0) {
                fail(slot < 0 && NovaClearPathRules.material(state) != NovaClearPathRules.Material.NONE
                        ? "Mình thiếu công cụ phù hợp còn tốt để phá vật cản này. Bạn kiểm tra 5 ô công cụ nhé."
                        : "Vật cản này chưa thể phá an toàn. Bạn giúp mình mở lối khác nhé.");
                return;
            }
            if (blocks >= MAX_BLOCKS) {
                // Yield briefly between batches; successful progress never requires owner help.
                blocks = 0;
                nextDecision = now + 40;
                needPath = true;
                return;
            }
            obstacle = found;
            expected = state;
            nova.toolBelt().equip(slot);
            actionTicks = 0;
            float speed = Math.max(1, nova.getMainHandItem().getDestroySpeed(state));
            breakTicks = Mth.clamp((int) Math.ceil(state.getDestroySpeed(nova.level(), found) * 30 / speed), 20, 80);
            nova.clearWorkAnimation();
            return;
        }
        if (usablePath(path)) { walk(path); return; }
        fail("Mình chưa tìm được vật cản gần có thể dọn an toàn. Bạn kiểm tra độ cao hoặc lối đi giúp mình nhé.");
    }

    private boolean usablePath(Path path) {
        if (path == null || path.getNodeCount() < 2 || path.getNodeCount() > 64) return false;
        var end = path.getEndNode();
        if (end == null || nova.distanceToSqr(end.x + .5, end.y, end.z + .5) < .5) return false;
        double endDistance = destination.distSqr(new BlockPos(end.x, end.y, end.z));
        if (!path.canReach() && endDistance >= destination.distSqr(nova.blockPosition()) - 1) return false;
        for (int i = 0; i < path.getNodeCount(); i++) {
            var n = path.getNode(i);
            if (!area(new BlockPos(n.x, n.y, n.z)) || !NovaCompanionPaths.safe(nova, new BlockPos(n.x, n.y, n.z))) return false;
            if (i > 0 && Math.abs(n.y - path.getNode(i - 1).y) > 1) return false;
        }
        return true;
    }

    private void walk(Path path) {
        nova.toolBelt().restore();
        noProgress = 0;
        nova.getNavigation().moveTo(path, 1.1);
    }

    private boolean loaded(BlockPos pos) {
        return nova.level().isInWorldBounds(pos) && nova.level().hasChunkAt(pos)
                && nova.level().getWorldBorder().isWithinBounds(pos);
    }

    private boolean collides(BlockPos pos) {
        if (!loaded(pos)) return true;
        BlockState state = nova.level().getBlockState(pos);
        return state.is(Blocks.COBWEB) || !state.getCollisionShape(nova.level(), pos).isEmpty();
    }

    /** Checks a short body-width corridor, top first; never scans the whole workspace. */
    private BlockPos firstObstacle() {
        BlockPos body = NovaClearPathRules.firstBodyObstacle(nova.position(), nova.getBbWidth(),
                nova.getBbHeight(), destination, p -> loaded(p) ? collides(p) : null,
                p -> NovaWorkSupport.canReach(nova, p));
        if (body != null) return body;
        Vec3 ray = Vec3.atCenterOf(destination).subtract(nova.getEyePosition());
        Vec3 end = nova.getEyePosition().add(ray.scale(Math.min(1, 3.5 / Math.max(.01, ray.length()))));
        Vec3 shortRay = end.subtract(nova.getEyePosition());
        for (int i = 0; i <= 16; i++)
            if (!loaded(BlockPos.containing(nova.getEyePosition().add(shortRay.scale(i / 16.0))))) return null;
        var hit = nova.level().clip(new ClipContext(nova.getEyePosition(), end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, nova));
        if (hit.getType() == HitResult.Type.BLOCK && !hit.getBlockPos().equals(destination)
                && loaded(hit.getBlockPos()) && NovaWorkSupport.canReach(nova, hit.getBlockPos())) return hit.getBlockPos();
        return null;
    }

    private boolean safeToBreak(BlockPos pos, BlockState state) {
        int feet = Mth.floor(nova.getY() + .05);
        if (!allowed() || !area(pos) || !loaded(pos) || pos.getY() < feet || pos.getY() > feet + 2
                || NovaClearPathRules.material(state) == NovaClearPathRules.Material.NONE
                || state.getDestroySpeed(nova.level(), pos) < 0 || !NovaWorkSupport.canReach(nova, pos)) return false;
        return NovaClearPathRules.safeNeighborhood(pos, feet,
                p -> loaded(p) ? nova.level().getBlockState(p) : null,
                (p, s) -> s.isFaceSturdy(nova.level(), p, Direction.UP));
    }

    private void chop() {
        if (!loaded(obstacle)) { cancel(); return; }
        BlockState current = nova.level().getBlockState(obstacle);
        if (!current.equals(expected)) { resetObstacle(); return; }
        if (!safeToBreak(obstacle, current) || NovaClearPathRules.score(nova.getMainHandItem(), current) == 0) {
            fail("Vật cản hoặc công cụ đã thay đổi. Mình dừng dọn đường để tránh làm hỏng đồ.");
            return;
        }
        nova.getNavigation().stop();
        NovaWorkSupport.faceTarget(nova, obstacle);
        nova.getLookControl().setLookAt(obstacle.getX() + .5, obstacle.getY() + .5, obstacle.getZ() + .5);
        nova.playWorkAnimation(switch (NovaClearPathRules.material(current)) {
            case LEAVES, WEB -> NovaEntity.WORK_CHOP;
            case EARTH -> NovaEntity.WORK_DIG;
            default -> NovaEntity.WORK_MINE;
        });
        if (++actionTicks % 10 == 0) nova.swing(InteractionHand.MAIN_HAND);
        ServerLevel level = (ServerLevel) nova.level();
        level.destroyBlockProgress(nova.getId(), obstacle, Math.min(9, actionTicks * 10 / breakTicks));
        if (actionTicks < breakTicks) return;
        var tool = nova.getMainHandItem();
        var drops = Block.getDrops(current, level, obstacle, null, nova, tool);
        if (!NovaTreeHarvest.fits(nova.getInventory().getItems(), drops)) {
            fail("Ba lô không đủ chỗ nhận đồ từ vật cản. Bạn giúp mình cất bớt đồ nhé.");
            return;
        }
        if (!level.destroyBlock(obstacle, false, nova)) { fail("Mình chưa phá được vật cản này."); return; }
        for (var drop : drops) {
            var remainder = nova.getInventory().addItem(drop.copy());
            if (!remainder.isEmpty()) Block.popResource(level, obstacle, remainder);
        }
        tool.hurtAndBreak(1, nova, EquipmentSlot.MAINHAND);
        nova.work().spendTreeEnergy();
        blocks++;
        lastUsefulAt = nova.level().getGameTime();
        resetObstacle();
    }

    private void clearCracks() {
        if (obstacle != null && nova.level() instanceof ServerLevel level)
            level.destroyBlockProgress(nova.getId(), obstacle, -1);
    }

    private void resetObstacle() {
        clearCracks();
        obstacle = null;
        expected = null;
        nova.toolBelt().restore();
        nova.clearWorkAnimation();
        needPath = true;
        noProgress = 0;
        nextDecision = nova.level().getGameTime() + 10;
    }
}
