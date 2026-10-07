package net.teogemini.nova.entity.ai.tree;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Per-robot saved work. Runtime world caches and reservations never retain the entity. */
public final class NovaTreeMemory {
    public record Planting(GlobalPos root,NovaTreePlan.Species species,List<BlockPos> positions) {}
    private NovaTreePlan active;
    private GlobalPos activeRoot;
    private final ArrayDeque<Planting> planting=new ArrayDeque<>();
    private final LinkedHashSet<GlobalPos> protectedLogs=new LinkedHashSet<>();
    public NovaTreePlan active() { return active; }
    public GlobalPos activeRoot() { return activeRoot; }
    public void begin(GlobalPos root,NovaTreePlan plan) { activeRoot=root;active=plan; }
    public void cancel() { active=null;activeRoot=null; }
    public void complete() {
        if (active!=null && planting.size()<8) {
            planting.addLast(new Planting(activeRoot,active.species(),active.roots()));
            cancel();
        }
    }
    public boolean full() { return planting.size()>=8; }
    public List<Planting> planting() { return List.copyOf(planting); }
    public void planted(Planting job) { planting.remove(job); }
    public int protect(GlobalPos pos) {
        if (protectedLogs.remove(pos)) return 0;
        if (protectedLogs.size()>=32) return -1;
        protectedLogs.add(pos); return 1;
    }
    public boolean protectedTree(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,NovaTreePlan plan) {
        return protectedLogs.stream().anyMatch(p->p.dimension().equals(dimension) && plan.logs().contains(p.pos()));
    }
    private static void writePlan(ValueOutput out,NovaTreePlan plan) {
        out.putString("Species",plan.species().name());
        out.store("Roots",BlockPos.CODEC.listOf(),plan.roots());
        out.store("Logs",BlockPos.CODEC.listOf(),plan.logs());
    }
    private static NovaTreePlan readPlan(ValueInput in) {
        try {
            var species=NovaTreePlan.Species.valueOf(in.getStringOr("Species",""));
            var roots=in.read("Roots",BlockPos.CODEC.listOf()).orElse(List.of());
            var logs=in.read("Logs",BlockPos.CODEC.listOf()).orElse(List.of());
            if (!NovaTreePlan.squareRoots(roots) || logs.size()>128 || new HashSet<>(logs).size()!=logs.size()) return null;
            if (!logs.containsAll(roots)) return null;
            if (roots.isEmpty() || logs.stream().anyMatch(p->!NovaTreePlan.withinTree(roots.getFirst(),p))) return null;
            return new NovaTreePlan(species,roots,logs);
        } catch (IllegalArgumentException ex) { return null; }
    }
    public void save(ValueOutput out) {
        if (active!=null) { var a=out.child("Active"); a.store("Location",GlobalPos.CODEC,activeRoot);writePlan(a,active); }
        int i=0;
        for (Planting job:planting) {
            var p=out.child("Plant"+i++);p.store("Location",GlobalPos.CODEC,job.root);
            p.putString("Species",job.species.name());p.store("Positions",BlockPos.CODEC.listOf(),job.positions);
        }
        out.putInt("PlantCount",i);
        out.store("Protected",GlobalPos.CODEC.listOf(),List.copyOf(protectedLogs));
    }
    public void load(ValueInput in) {
        cancel();planting.clear();protectedLogs.clear();
        var a=in.childOrEmpty("Active");var plan=readPlan(a);var location=a.read("Location",GlobalPos.CODEC).orElse(null);
        if (plan!=null && location!=null && location.pos().equals(plan.root())) begin(location,plan);
        for (int i=0;i<Math.min(8,Math.max(0,in.getIntOr("PlantCount",0)));i++) {
            var p=in.childOrEmpty("Plant"+i);
            try {
                var root=p.read("Location",GlobalPos.CODEC).orElse(null);
                var species=NovaTreePlan.Species.valueOf(p.getStringOr("Species",""));
                var positions=p.read("Positions",BlockPos.CODEC.listOf()).orElse(List.of());
                if (root!=null && NovaTreePlan.squareRoots(positions) && positions.contains(root.pos()))
                    planting.add(new Planting(root,species,List.copyOf(positions)));
            } catch (IllegalArgumentException ignored) {}
        }
        in.read("Protected",GlobalPos.CODEC.listOf()).orElse(List.of()).stream().limit(32).forEach(protectedLogs::add);
    }
}
