package net.teogemini.nova.entity;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Local repair timer; no world searches and no healing while the entity is unloaded. */
public final class NovaSelfRepair {
    public static final int DAMAGE_DELAY = 200, REPAIR_INTERVAL = 100;
    public static final int ENERGY_COST = NovaWorkData.MAX_ENERGY / 100;
    public static final int ENERGY_RESERVE = NovaWorkData.MAX_ENERGY / 10;
    public static final float HEALTH_PER_REPAIR = 1.0F;
    private int remainingTicks = DAMAGE_DELAY;

    public void damaged() { remainingTicks = DAMAGE_DELAY; }

    /** Returns one repair pulse at most; missed time never creates a burst of repairs. */
    public boolean advance(boolean damaged, int energy) {
        if (remainingTicks > 0) remainingTicks--;
        if (!damaged || remainingTicks > 0 || energy < ENERGY_RESERVE + ENERGY_COST) return false;
        remainingTicks = REPAIR_INTERVAL;
        return true;
    }

    public void tick(NovaEntity nova) {
        if (nova.level().isClientSide() || !nova.isAlive() || nova.isRemoved() || nova.isNoAi()) return;
        boolean needsRepair = nova.getHealth() > 0 && nova.getHealth() < nova.getMaxHealth();
        if (advance(needsRepair, nova.work().energy())
                && nova.work().trySpendEnergy(ENERGY_COST, ENERGY_RESERVE)) {
            nova.heal(HEALTH_PER_REPAIR);
            nova.sounds().play(net.teogemini.nova.sound.NovaSounds.Cue.REPAIR);
            if (nova.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.WAX_OFF,
                        nova.getX(), nova.getY() + 0.7, nova.getZ(),
                        4, 0.25, 0.35, 0.25, 0.04
                );
                serverLevel.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.HEART,
                        nova.getX(), nova.getY() + 1.1, nova.getZ(),
                        1, 0.2, 0.2, 0.2, 0.02
                );
            }
        }
    }

    public void save(ValueOutput output) { output.putInt("RemainingTicks", remainingTicks); }
    public void load(ValueInput input) {
        remainingTicks = Math.max(0, Math.min(DAMAGE_DELAY, input.getIntOr("RemainingTicks", DAMAGE_DELAY)));
    }
}
