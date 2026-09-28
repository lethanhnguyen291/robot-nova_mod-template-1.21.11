package net.teogemini.nova.entity.ai;

import java.util.EnumSet;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.teogemini.nova.entity.NovaEntity;

public final class NovaRallyGoal extends Goal {
    private final NovaEntity nova;
    private int pathTimer;

    public NovaRallyGoal(NovaEntity nova) {
        this.nova = nova;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public boolean canUse() { return nova.isAlive() && nova.isRallying(); }
    @Override public boolean canContinueToUse() { return canUse(); }
    @Override public void start() { pathTimer = 0; }

    @Override public void tick() {
        var target = nova.work().rallyTarget;
        if (target == null) return;
        double x = target.getX() + .5;
        double y = target.getY();
        double z = target.getZ() + .5;
        double horizontalDistance = nova.distanceToSqr(x, nova.getY(), z);

        if (horizontalDistance < .5) {
            nova.getNavigation().stop();
            var centred = nova.getBoundingBox().move(x - nova.getX(), 0, z - nova.getZ());
            if (nova.level().noCollision(nova, centred)) nova.setPos(x, nova.getY(), z);
            faceChest();
        } else if (horizontalDistance < 9.0 && directApproachClear(x, z)) {
            nova.getNavigation().stop();
            nova.getMoveControl().setWantedPosition(x, y, z, 1.2);
        } else if (--pathTimer <= 0) {
            pathTimer = 10;
            nova.getNavigation().moveTo(x, y, z, 1.2);
        }
    }

    private boolean directApproachClear(double x, double z) {
        Vec3 destination = new Vec3(x, nova.getEyeY(), z);
        return nova.level().clip(new ClipContext(nova.getEyePosition(), destination,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, nova)).getType() == HitResult.Type.MISS;
    }

    private void faceChest() {
        var chest = nova.work().rallyFace;
        if (chest == null) return;
        float targetYaw = (float) (Math.atan2(chest.getZ() + .5 - nova.getZ(),
                chest.getX() + .5 - nova.getX()) * 180.0 / Math.PI) - 90;
        if (nova.level().hasChunkAt(chest)) {
            var state = nova.level().getBlockState(chest);
            if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                targetYaw = state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite().toYRot();
            }
        }
        float yaw = Mth.approachDegrees(nova.getYRot(), targetYaw, 15);
        nova.setYRot(yaw);
        nova.setYBodyRot(yaw);
        nova.setYHeadRot(yaw);
        nova.setXRot(Mth.approachDegrees(nova.getXRot(), 0, 10));
        // Keep LookControl from turning the flanking soldiers toward the chest's centre.
        double radians = Math.toRadians(yaw);
        nova.getLookControl().setLookAt(nova.getX() - Math.sin(radians) * 4,
                nova.getEyeY(), nova.getZ() + Math.cos(radians) * 4, 15, 10);
    }

    @Override public void stop() {
        nova.getNavigation().stop();
        nova.stopRally();
    }
}
