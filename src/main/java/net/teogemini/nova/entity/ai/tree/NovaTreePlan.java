package net.teogemini.nova.entity.ai.tree;

import java.util.*;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** A bounded, immutable snapshot. Only its listed raw logs can be felled remotely. */
public record NovaTreePlan(Species species, List<BlockPos> roots, List<BlockPos> logs) {
    public static final int MAX_LOGS = 128;
    public NovaTreePlan {
        roots = roots.stream().map(BlockPos::immutable).toList();
        logs = logs.stream().map(BlockPos::immutable).toList();
        if (roots.isEmpty() || roots.size()>4 || logs.isEmpty() || logs.size()>MAX_LOGS)
            throw new IllegalArgumentException("Invalid tree size");
    }
    public BlockPos root() { return roots.getFirst(); }
    public enum Species {
        OAK(Blocks.OAK_LOG,Blocks.OAK_LEAVES,Items.OAK_SAPLING),
        BIRCH(Blocks.BIRCH_LOG,Blocks.BIRCH_LEAVES,Items.BIRCH_SAPLING),
        SPRUCE(Blocks.SPRUCE_LOG,Blocks.SPRUCE_LEAVES,Items.SPRUCE_SAPLING),
        JUNGLE(Blocks.JUNGLE_LOG,Blocks.JUNGLE_LEAVES,Items.JUNGLE_SAPLING),
        ACACIA(Blocks.ACACIA_LOG,Blocks.ACACIA_LEAVES,Items.ACACIA_SAPLING),
        DARK_OAK(Blocks.DARK_OAK_LOG,Blocks.DARK_OAK_LEAVES,Items.DARK_OAK_SAPLING),
        CHERRY(Blocks.CHERRY_LOG,Blocks.CHERRY_LEAVES,Items.CHERRY_SAPLING),
        PALE_OAK(Blocks.PALE_OAK_LOG,Blocks.PALE_OAK_LEAVES,Items.PALE_OAK_SAPLING),
        MANGROVE(Blocks.MANGROVE_LOG,Blocks.MANGROVE_LEAVES,Items.MANGROVE_PROPAGULE);
        public final Block log, leaves;
        public final Item sapling;
        Species(Block log,Block leaves,Item sapling) { this.log=log;this.leaves=leaves;this.sapling=sapling; }
        public static Species of(BlockState state) {
            if (state!=null) for (Species s:values()) if (state.is(s.log)) return s;
            return null;
        }
    }
    public static boolean soil(BlockState s) {
        return s!=null && (s.is(Blocks.DIRT) || s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.PODZOL)
                || s.is(Blocks.COARSE_DIRT) || s.is(Blocks.ROOTED_DIRT) || s.is(Blocks.MUD)
                || s.is(Blocks.MOSS_BLOCK) || s.is(Blocks.PALE_MOSS_BLOCK));
    }
    public static boolean squareRoots(List<BlockPos> roots) {
        if (roots.size()==1) return true;
        if (roots.size()!=4 || new HashSet<>(roots).size()!=4) return false;
        int minX=roots.stream().mapToInt(BlockPos::getX).min().orElseThrow();
        int minZ=roots.stream().mapToInt(BlockPos::getZ).min().orElseThrow();
        int y=roots.getFirst().getY();
        return roots.stream().allMatch(p->p.getY()==y && p.getX()>=minX && p.getX()<=minX+1
                && p.getZ()>=minZ && p.getZ()<=minZ+1);
    }
    public static boolean withinTree(BlockPos root, BlockPos pos) {
        return Math.abs(pos.getX()-root.getX())<=6 && Math.abs(pos.getZ()-root.getZ())<=6
                && pos.getY()>=root.getY() && pos.getY()<=root.getY()+24;
    }
    /** One queued position per step; callers allocate steps from a SERVER-wide budget. */
    public static final class Analyzer {
        private final BlockPos origin;
        private final Species species;
        private final ArrayDeque<BlockPos> pending=new ArrayDeque<>();
        private final Set<BlockPos> queued=new HashSet<>();
        private final Map<BlockPos,BlockState> states=new HashMap<>();
        private final List<BlockPos> logs=new ArrayList<>();
        private int leaves;
        private boolean failed,done;
        private NovaTreePlan result;
        public Analyzer(BlockPos root,Species species) { this.origin=root.immutable();this.species=species;enqueue(root); }
        private void enqueue(BlockPos pos) {
            if (queued.add(pos.immutable())) pending.add(pos.immutable());
            if (queued.size()>4096) failed=true;
        }
        public int step(Function<BlockPos,BlockState> reader,int budget) {
            int used=0;
            while (!done && !failed && used<budget && !pending.isEmpty()) {
                BlockPos pos=pending.removeFirst(); BlockState state=reader.apply(pos); used++;
                if (state==null) { failed=true;break; } // Unknown/unloaded neighbors are never traversed.
                states.put(pos,state);
                if (state.is(species.leaves) && !state.getValue(BlockStateProperties.PERSISTENT)) leaves++;
                if (!state.is(species.log)) continue;
                if (!withinTree(origin,pos) || logs.size()>=MAX_LOGS) { failed=true;break; }
                logs.add(pos);
                for (int x=-1;x<=1;x++) for (int y=-1;y<=1;y++) for (int z=-1;z<=1;z++)
                    if (x!=0 || y!=0 || z!=0) enqueue(pos.offset(x,y,z));
            }
            if (failed) { done=true;pending.clear(); }
            else if (pending.isEmpty() && !done) finish();
            return used;
        }
        private void finish() {
            done=true;
            if (leaves<6 || logs.size()<3) return;
            List<BlockPos> roots=logs.stream().filter(p->soil(states.get(p.below())))
                    .sorted(Comparator.<BlockPos>comparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ)).toList();
            if (!squareRoots(roots) || !roots.contains(origin)) return;
            if (logs.stream().anyMatch(p->!withinTree(roots.getFirst(),p))) return;
            if ((species==Species.DARK_OAK || species==Species.PALE_OAK) && roots.size()!=4) return;
            // Multiple grounded trunks that are not one 2x2 tree are rejected.
            List<BlockPos> ordered=logs.stream().sorted(Comparator.<BlockPos>comparingInt(BlockPos::getY).reversed()
                    .thenComparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ)).toList();
            result=new NovaTreePlan(species,roots,ordered);
        }
        public boolean done() { return done; }
        public NovaTreePlan result() { return result; }
    }
}
