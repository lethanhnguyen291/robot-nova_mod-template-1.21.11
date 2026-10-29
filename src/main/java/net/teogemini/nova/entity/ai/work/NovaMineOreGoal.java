package net.teogemini.nova.entity.ai.work;

import java.util.EnumSet;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.teogemini.nova.entity.NovaEntity;
import net.teogemini.nova.entity.NovaExperienceManager;
import net.teogemini.nova.entity.NovaWorkSupport;

public class NovaMineOreGoal extends Goal {
    private enum MineMode {
        MINE_ORE,
        SEEK_CAVE,
        EXPAND_SHAFT
    }

    private final NovaEntity nova;
    private final double speed;
    private int novaTaskTicks;
    private int miningTick;
    private int searchDelay;

    private MineMode mode = MineMode.MINE_ORE;
    private BlockPos targetOrePos;
    private BlockPos targetPos;

    public NovaMineOreGoal(NovaEntity nova, double speed) {
        this.nova = nova;
        this.speed = speed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean requiresUpdateEveryTick() { return true; }

    @Override
    public boolean canUse() {
        if (!nova.canStartNovaJob()) return false;
        if (!nova.companion().usableTool(nova.getPrimaryTool())) {
            NovaWorkSupport.swapToolFromBody(nova);
        }
        if (!nova.companion().usableTool(nova.getPrimaryTool())) return false;
        if (!this.nova.getPrimaryTool().is(ItemTags.PICKAXES)) return false;

        if (this.searchDelay > 0) {
            this.searchDelay--;
            return false;
        }
        this.searchDelay = 30; // Quét mỗi 1.5 giây

        // 1. Tìm quặng lộ thiên gần nhất
        this.targetOrePos = findNearestExposedOre();
        if (this.targetOrePos != null) {
            this.mode = MineMode.MINE_ORE;
            this.targetPos = this.targetOrePos;
            return true;
        }

        // 2. Không thấy quặng và đang ở trên mặt đất: Tìm cửa hang/vị trí dưới lòng đất trong vùng làm việc
        Level level = nova.level();
        if (level.canSeeSky(nova.blockPosition())) {
            BlockPos cave = findNearbyCaveFloor();
            if (cave != null) {
                this.mode = MineMode.SEEK_CAVE;
                this.targetPos = cave;
                return true;
            }
            // Ở mặt đất mà không có hang: Không đào bừa bãi làm hỏng địa hình!
            return false;
        }

        // 3. Đang ở dưới hang nhưng chưa thấy quặng lộ: Đến điểm sâu nhất của hang
        BlockPos deepest = findDeepestCaveFloor();
        if (deepest != null && nova.blockPosition().distSqr(deepest) > 6.0) {
            this.mode = MineMode.SEEK_CAVE;
            this.targetPos = deepest;
            return true;
        }

        // 4. Đã ở cuối hang / điểm sâu nhất: Mở rộng đường hầm bậc thang xuống dưới
        BlockPos expandBlock = findNextExpansionBlock();
        if (expandBlock != null) {
            this.mode = MineMode.EXPAND_SHAFT;
            this.targetPos = expandBlock;
            return true;
        }

        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (!nova.canStartNovaJob()) return false;
        if (!nova.companion().usableTool(nova.getPrimaryTool())) {
            NovaWorkSupport.swapToolFromBody(nova);
        }
        if (!nova.companion().usableTool(nova.getPrimaryTool())) return false;
        if (!this.nova.getPrimaryTool().is(ItemTags.PICKAXES)) return false;
        return this.targetPos != null;
    }

    @Override
    public void start() {
        novaTaskTicks = 0;
        this.miningTick = 0;
        NovaWorkSupport.ensureOffhandTorch(nova);
    }

    @Override
    public void tick() {
        if (!nova.canStartNovaJob()) { stop(); return; }
        if (++novaTaskTicks > 240) {
            if (targetPos != null) nova.companion().stuck(targetPos);
            stop();
            return;
        }
        if (novaTaskTicks % 20 == 0) NovaWorkSupport.ensureOffhandTorch(nova);
        nova.clearWorkAnimation();

        if (this.targetPos == null) {
            stop();
            return;
        }

        Level level = nova.level();

        // CHẾ ĐỘ 1: KHAI THÁC QUẶNG
        if (this.mode == MineMode.MINE_ORE) {
            if (this.targetOrePos == null || !isOre(level.getBlockState(this.targetOrePos))) {
                // Quặng đã vỡ hoặc mất: Tìm quặng tiếp theo
                BlockPos nextOre = findNearestExposedOre();
                if (nextOre != null) {
                    this.targetOrePos = nextOre;
                    this.targetPos = nextOre;
                } else {
                    stop();
                    return;
                }
            }

            nova.getLookControl().setLookAt(
                targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5,
                10.0F, nova.getMaxHeadXRot()
            );

            double distSqr = nova.blockPosition().distSqr(targetPos);
            if (distSqr <= 5.0) {
                nova.getNavigation().stop();
                NovaWorkSupport.faceTarget(nova, targetPos);
                nova.playWorkAnimation(2);

                if (miningTick % 10 == 0) {
                    nova.swing(InteractionHand.MAIN_HAND);
                }
                miningTick++;

                if (miningTick >= 20) {
                    BlockState brokenState = level.getBlockState(targetPos);
                    if (!NovaWorkSupport.breakBlock(nova, targetPos)) {
                        nova.companion().stuck(targetPos);
                        stop();
                        return;
                    }
                    NovaExperienceManager.awardMiningExp(nova, brokenState);
                    novaTaskTicks = 0;
                    miningTick = 0;

                    // Quét mạch quặng liền kề
                    BlockPos adjacent = findAdjacentOre(targetPos);
                    if (adjacent != null) {
                        targetOrePos = adjacent;
                        targetPos = adjacent;
                    } else {
                        // Mạch đã hết, tìm quặng lộ thiên gần nhất
                        BlockPos next = findNearestExposedOre();
                        if (next != null) {
                            targetOrePos = next;
                            targetPos = next;
                        } else {
                            targetOrePos = null;
                            targetPos = null;
                            stop();
                        }
                    }
                }
            } else {
                if (novaTaskTicks % 20 == 1) {
                    NovaWorkSupport.moveToWork(nova, targetPos, speed);
                }
            }
            return;
        }

        // CHẾ ĐỘ 2: TÌM HANG / XUỐNG ĐIỂM SÂU NHẤT
        if (this.mode == MineMode.SEEK_CAVE) {
            // Kiểm tra dọc đường đi xem có quặng lộ thiên nào không
            if (novaTaskTicks % 20 == 0) {
                BlockPos exposed = findNearestExposedOre();
                if (exposed != null) {
                    this.mode = MineMode.MINE_ORE;
                    this.targetOrePos = exposed;
                    this.targetPos = exposed;
                    return;
                }
            }

            double distSqr = nova.blockPosition().distSqr(targetPos);
            if (distSqr <= 4.0) {
                nova.getNavigation().stop();
                // Đã đến vị trí hang, kiểm tra quặng hoặc chuẩn bị mở rộng đường
                BlockPos exposed = findNearestExposedOre();
                if (exposed != null) {
                    this.mode = MineMode.MINE_ORE;
                    this.targetOrePos = exposed;
                    this.targetPos = exposed;
                } else {
                    BlockPos expandBlock = findNextExpansionBlock();
                    if (expandBlock != null) {
                        this.mode = MineMode.EXPAND_SHAFT;
                        this.targetPos = expandBlock;
                    } else {
                        stop();
                    }
                }
            } else {
                if (novaTaskTicks % 20 == 1) {
                    NovaWorkSupport.moveToWork(nova, targetPos, speed);
                }
            }
            return;
        }

        // CHẾ ĐỘ 3: MỞ RỘNG ĐƯỜNG HẦM (ĐÀO BẬC THANG XUỐNG DƯỚI)
        if (this.mode == MineMode.EXPAND_SHAFT) {
            // Kiểm tra nếu khối mục tiêu đã vỡ
            if (level.getBlockState(targetPos).isAir()) {
                // Kiểm tra quặng vừa lộ ra
                BlockPos exposed = findNearestExposedOre();
                if (exposed != null) {
                    this.mode = MineMode.MINE_ORE;
                    this.targetOrePos = exposed;
                    this.targetPos = exposed;
                    return;
                }
                BlockPos nextExpand = findNextExpansionBlock();
                if (nextExpand == null) { stop(); return; }
                this.targetPos = nextExpand;
            }

            nova.getLookControl().setLookAt(
                targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5,
                10.0F, nova.getMaxHeadXRot()
            );

            double distSqr = nova.blockPosition().distSqr(targetPos);
            if (distSqr <= 5.0) {
                nova.getNavigation().stop();
                NovaWorkSupport.faceTarget(nova, targetPos);
                nova.playWorkAnimation(2);

                if (miningTick % 10 == 0) {
                    nova.swing(InteractionHand.MAIN_HAND);
                }
                miningTick++;

                if (miningTick >= 25) { // 25 ticks cho đá/deepslate
                    BlockState broken = level.getBlockState(targetPos);
                    if (!NovaWorkSupport.breakBlock(nova, targetPos)) {
                        nova.companion().stuck(targetPos);
                        stop();
                        return;
                    }
                    NovaExperienceManager.awardMiningExp(nova, broken);
                    novaTaskTicks = 0;
                    miningTick = 0;

                    // Nếu khối vừa đào trúng quặng hoặc làm lộ quặng
                    BlockPos exposed = findNearestExposedOre();
                    if (exposed != null) {
                        this.mode = MineMode.MINE_ORE;
                        this.targetOrePos = exposed;
                        this.targetPos = exposed;
                    } else {
                        this.targetPos = findNextExpansionBlock();
                    }
                }
            } else {
                if (novaTaskTicks % 20 == 1) {
                    NovaWorkSupport.moveToWork(nova, targetPos, speed);
                }
            }
        }
    }

    // TÌM QUẶNG LỘ THIÊN GẦN NHẤT
    private BlockPos findNearestExposedOre() {
        Optional<BlockPos> closestOre = BlockPos.findClosestMatch(
            this.nova.blockPosition(),
            16,
            9,
            pos -> nova.isWithinWorkspace(pos)
                && !nova.companion().retry.blocked(pos, nova.level().getGameTime())
                && isOre(nova.level().getBlockState(pos))
                && isExposedOre(nova.level(), pos)
        );
        return closestOre.orElse(null);
    }

    // KIỂM TRA QUẶNG CÓ MẶT NÀO TIẾP XÚC VỚI KHÔNG KHÍ HAY KHÔNG
    private boolean isExposedOre(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            BlockState nState = level.getBlockState(neighbor);
            if (nState.isAir() || !nState.isSolid() || !nState.canOcclude()) {
                return true;
            }
        }
        return false;
    }

    // TÌM MẠCH QUẶNG LIỀN KỀ (MẠNG LƯỚI QUANH CỤC VỪA ĐẬP)
    private BlockPos findAdjacentOre(BlockPos center) {
        Level level = nova.level();
        for (Direction dir : Direction.values()) {
            BlockPos checkPos = center.relative(dir);
            if (!nova.isWithinWorkspace(checkPos)) continue;
            if (isOre(level.getBlockState(checkPos))) {
                return checkPos;
            }
        }
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    BlockPos checkPos = center.offset(x, y, z);
                    if (!nova.isWithinWorkspace(checkPos)) continue;
                    if (isOre(level.getBlockState(checkPos))) {
                        return checkPos;
                    }
                }
            }
        }
        return null;
    }

    // TÌM SÀN HANG AN TOÀN TRONG VÙNG LÀM VIỆC (DƯỚI LÒNG ĐẤT, KHÔNG THẤY TRỜI)
    private BlockPos findNearbyCaveFloor() {
        Level level = nova.level();
        BlockPos center = nova.blockPosition();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        int r = 16;
        for (int x = -r; x <= r; x += 2) {
            for (int z = -r; z <= r; z += 2) {
                for (int y = -16; y <= 2; y++) {
                    BlockPos p = center.offset(x, y, z);
                    if (!nova.isWithinWorkspace(p)) continue;
                    if (level.canSeeSky(p)) continue;
                    if (isWalkableSafeCave(level, p)) {
                        double d = center.distSqr(p);
                        if (d < bestDist) {
                            bestDist = d;
                            best = p;
                        }
                    }
                }
            }
        }
        return best;
    }

    // TÌM ĐIỂM SÂU NHẤT TRONG HANG TRONG VÙNG LÀM VIỆC
    private BlockPos findDeepestCaveFloor() {
        Level level = nova.level();
        BlockPos center = nova.blockPosition();
        BlockPos lowest = null;
        int lowestY = center.getY();

        int r = 14;
        for (int x = -r; x <= r; x += 2) {
            for (int z = -r; z <= r; z += 2) {
                for (int y = -14; y <= 4; y++) {
                    BlockPos p = center.offset(x, y, z);
                    if (!nova.isWithinWorkspace(p)) continue;
                    if (level.canSeeSky(p)) continue;
                    if (isWalkableSafeCave(level, p)) {
                        if (p.getY() < lowestY) {
                            lowestY = p.getY();
                            lowest = p;
                        }
                    }
                }
            }
        }
        return lowest;
    }

    private boolean isWalkableSafeCave(Level level, BlockPos pos) {
        if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) return false;
        BlockPos below = pos.below();
        BlockState floor = level.getBlockState(below);
        if (!floor.isFaceSturdy(level, below, Direction.UP)) return false;
        if (floor.is(Blocks.LAVA) || floor.is(Blocks.MAGMA_BLOCK) || floor.is(Blocks.FIRE)) return false;
        return true;
    }

    // TÌM KHỐI TIẾP THEO ĐỂ MỞ RỘNG ĐƯỜNG HẦM BẬC THANG XUỐNG DƯỚI
    private BlockPos findNextExpansionBlock() {
        Level level = nova.level();
        BlockPos stand = nova.blockPosition();
        Direction dir = getBestExpandDirection(stand);
        if (dir == null) return null;

        BlockPos head = stand.relative(dir).above();
        BlockPos body = stand.relative(dir);
        BlockPos step = stand.relative(dir).below();

        // Ưu tiên đào đầu để tránh ngạt
        if (!level.getBlockState(head).isAir() && isSafeToDig(level, head)) {
            return head;
        }
        // Tiếp theo đào thân
        if (!level.getBlockState(body).isAir() && isSafeToDig(level, body)) {
            return body;
        }
        // Cuối cùng đào bước chân xuống
        if (!level.getBlockState(step).isAir() && isSafeToDig(level, step)) {
            // Bảo hiểm: Bên dưới bước chân không được là dung nham
            if (!level.getBlockState(step.below()).is(Blocks.LAVA)) {
                return step;
            }
        }

        return null;
    }

    private Direction getBestExpandDirection(BlockPos stand) {
        Direction dir = nova.getDirection();
        if (dir.getAxis().isVertical()) dir = Direction.NORTH;

        Direction[] candidates = { dir, dir.getClockWise(), dir.getCounterClockWise(), dir.getOpposite() };
        int minY = getMinMiningY();

        for (Direction d : candidates) {
            BlockPos targetStep = stand.relative(d).below();
            if (stand.getY() - 1 >= minY && nova.isWithinWorkspace(targetStep)) {
                return d;
            }
        }
        return null;
    }

    private int getMinMiningY() {
        BlockPos chest = nova.getLinkedChest();
        int chestY = chest != null ? chest.getY() : nova.blockPosition().getY();
        return Math.max(nova.level().getMinY() + 5, chestY - 24);
    }

    private boolean isSafeToDig(Level level, BlockPos pos) {
        if (!level.isInWorldBounds(pos)) return false;
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return true;
        if (state.getDestroySpeed(level, pos) < 0) return false; // Không đào bedrock
        if (!level.getFluidState(pos).isEmpty()) return false; // Tránh đào vào khối chất lỏng
        if (!level.getFluidState(pos.above()).isEmpty()) return false; // Tránh chất lỏng phía trên đổ xuống
        return true;
    }

    // NHẬN DIỆN TẤT CẢ CÁC LOẠI QUẶNG (BAO GỒM DEEPSLATE VÀ THẺ MINECRAFT)
    public static boolean isOre(BlockState state) {
        if (state.is(BlockTags.COAL_ORES) || state.is(BlockTags.IRON_ORES)
                || state.is(BlockTags.COPPER_ORES) || state.is(BlockTags.GOLD_ORES)
                || state.is(BlockTags.REDSTONE_ORES) || state.is(BlockTags.LAPIS_ORES)
                || state.is(BlockTags.DIAMOND_ORES) || state.is(BlockTags.EMERALD_ORES)) {
            return true;
        }
        Block b = state.getBlock();
        return b == Blocks.COAL_ORE || b == Blocks.DEEPSLATE_COAL_ORE
            || b == Blocks.IRON_ORE || b == Blocks.DEEPSLATE_IRON_ORE
            || b == Blocks.COPPER_ORE || b == Blocks.DEEPSLATE_COPPER_ORE
            || b == Blocks.GOLD_ORE || b == Blocks.DEEPSLATE_GOLD_ORE
            || b == Blocks.REDSTONE_ORE || b == Blocks.DEEPSLATE_REDSTONE_ORE
            || b == Blocks.LAPIS_ORE || b == Blocks.DEEPSLATE_LAPIS_ORE
            || b == Blocks.DIAMOND_ORE || b == Blocks.DEEPSLATE_DIAMOND_ORE
            || b == Blocks.EMERALD_ORE || b == Blocks.DEEPSLATE_EMERALD_ORE
            || b == Blocks.NETHER_QUARTZ_ORE || b == Blocks.NETHER_GOLD_ORE
            || b == Blocks.ANCIENT_DEBRIS;
    }

    @Override
    public void stop() {
        nova.pathClearing().track(null);
        targetOrePos = null;
        targetPos = null;
        miningTick = 0;
        nova.clearWorkAnimation();
        nova.getNavigation().stop();
    }
}
