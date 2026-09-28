package net.teogemini.nova.client.light;

import dev.lambdaurora.lambdynlights.api.DynamicLightsContext;
import dev.lambdaurora.lambdynlights.api.DynamicLightsInitializer;
import dev.lambdaurora.lambdynlights.api.behavior.DynamicLightBehavior;
import dev.lambdaurora.lambdynlights.api.behavior.DynamicLightBehaviorManager;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.teogemini.nova.entity.NovaEntity;

public final class NovaLampLighting implements DynamicLightsInitializer {
    private final Map<Integer, Lamp> lamps = new HashMap<>();
    private ClientLevel world;

    @Override
    public void onInitializeDynamicLights(DynamicLightsContext context) {
        DynamicLightBehaviorManager manager = context.dynamicLightBehaviorManager();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (world != client.level) {
                lamps.values().forEach(manager::remove);
                lamps.clear();
                world = client.level;
            }

            if (world == null) {
                return;
            }

            lamps.values().removeIf(lamp -> {
                if (lamp.isRemoved()) {
                    lamp.nova.setNovaLampOn(false);
                    manager.remove(lamp);
                    return true;
                }
                return false;
            });

            for (Entity entity : world.entitiesForRendering()) {
                if (entity instanceof NovaEntity nova && nova.isAlive()) {
                    Lamp lamp = lamps.get(nova.getId());

                    if (lamp == null) {
                        lamp = new Lamp(nova);
                        lamp.update();
                        lamps.put(nova.getId(), lamp);
                        manager.add(lamp);
                    } else {
                        lamp.update();
                    }
                }
            }
        });
    }

    private static final class Lamp implements DynamicLightBehavior {
        private final NovaEntity nova;
        private volatile Sample sample = new Sample(0, 0, 0, 0);
        private Sample lastSample;

        private Lamp(NovaEntity nova) {
            this.nova = nova;
        }

        private void update() {
            double yaw = Math.toRadians(nova.yBodyRot);
            double y = nova.getY() + nova.getNovaLampHeight();
            BlockPos sensor = BlockPos.containing(nova.getX(), y, nova.getZ());

            // Không tính ánh sáng động của chính NOVA vào cảm biến.
            boolean on = nova.level().getMaxLocalRawBrightness(sensor) < 7;
            nova.setNovaLampOn(on);

            sample = new Sample(
                    nova.getX() - Math.sin(yaw) * 0.5,
                    y,
                    nova.getZ() + Math.cos(yaw) * 0.5,
                    on ? 10 : 0);
        }

        @Override
        public double lightAtPos(BlockPos pos, double falloffRatio) {
            Sample s = sample;
            double dx = pos.getX() + 0.5 - s.x();
            double dy = pos.getY() + 0.5 - s.y();
            double dz = pos.getZ() + 0.5 - s.z();

            return Math.max(
                    0,
                    s.level() - Math.sqrt(dx * dx + dy * dy + dz * dz) * falloffRatio);
        }

        @Override
        public BoundingBox getBoundingBox() {
            Sample s = sample;
            int x = Mth.floor(s.x());
            int y = Mth.floor(s.y());
            int z = Mth.floor(s.z());

            return new BoundingBox(x, y, z, x + 1, y + 1, z + 1);
        }

        @Override
        public boolean hasChanged() {
            Sample s = sample;
            boolean changed = !s.equals(lastSample);
            lastSample = s;
            return changed;
        }

        @Override
        public boolean isRemoved() {
            return nova.isRemoved() || !nova.isAlive();
        }
    }

    private record Sample(double x, double y, double z, int level) {
    }
}