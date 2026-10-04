package net.teogemini.nova.entity.companion;

import java.util.LinkedHashMap;
import net.minecraft.core.BlockPos;

/** A failed target expires independently; ten distinct chests are never required. */
public final class NovaRetryMemory {
    private final LinkedHashMap<BlockPos, Long> failures = new LinkedHashMap<>();
    public void fail(BlockPos pos, long now) {
        failures.remove(pos);
        failures.put(pos.immutable(), now + 600);
        while (failures.size() > 9) failures.remove(failures.keySet().iterator().next());
    }
    public boolean blocked(BlockPos pos, long now) {
        failures.entrySet().removeIf(entry -> entry.getValue() <= now);
        return failures.containsKey(pos);
    }
    public void success(BlockPos pos) { failures.remove(pos); }
    public void clear() { failures.clear(); }
    public int size() { return failures.size(); }
}
