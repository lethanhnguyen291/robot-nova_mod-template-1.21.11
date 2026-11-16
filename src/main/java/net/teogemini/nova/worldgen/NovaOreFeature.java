package net.teogemini.nova.worldgen;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;

/** Regular ore, cave-wall clusters and occasional connected veins; never scans loaded chunks per tick. */
public final class NovaOreFeature extends Feature<OreConfiguration> {
    public enum Mode{NORMAL,CAVE,VEIN}
    private final Mode mode;
    public NovaOreFeature(Mode mode){super(OreConfiguration.CODEC);this.mode=mode;}
    @Override public boolean place(FeaturePlaceContext<OreConfiguration> context){
        return switch(mode){case NORMAL->normal(context);case CAVE->cave(context);case VEIN->vein(context);};
    }
    private boolean normal(FeaturePlaceContext<OreConfiguration> c){
        int max=c.config().size,min=Math.max(1,max-(max>=10?4:3));
        var config=new OreConfiguration(c.config().targetStates,min+c.random().nextInt(max-min+1),0);
        return Feature.ORE.place(config,c.level(),c.chunkGenerator(),c.random(),c.origin());
    }
    private static boolean adjacentAir(WorldGenLevel level,BlockPos p){
        for(Direction d:Direction.values()){
            BlockPos n=p.relative(d);if(level.ensureCanWrite(n)&&level.isEmptyBlock(n))return true;
        }
        return false;
    }
    private static BlockState target(WorldGenLevel level,BlockPos p,OreConfiguration config,RandomSource random){
        if(level.isOutsideBuildHeight(p)||!level.ensureCanWrite(p))return null;
        BlockState existing=level.getBlockState(p);
        if(!existing.getFluidState().isEmpty())return null;
        for(var target:config.targetStates)if(target.target.test(existing,random))return target.state;
        return null;
    }
    private boolean cave(FeaturePlaceContext<OreConfiguration> c){
        var level=c.level();var random=c.random();BlockPos origin=c.origin();
        int minX=origin.getX()&~15,minZ=origin.getZ()&~15;
        BlockPos seed=null;
        // Fixed 32 probes, at the height chosen by placement, in this one chunk.
        for(int i=0;i<32;i++){
            BlockPos p=new BlockPos(minX+random.nextInt(16),origin.getY(),minZ+random.nextInt(16));
            if(target(level,p,c.config(),random)==null)continue;
            if(level.getHeight(Heightmap.Types.WORLD_SURFACE_WG,p.getX(),p.getZ())<p.getY()+8)continue;
            if(adjacentAir(level,p)){seed=p;break;}
        }
        if(seed==null)return false;
        int limit=Math.min(12,c.config().size),placed=0,probes=0;
        var queue=new ArrayDeque<BlockPos>();var seen=new HashSet<Long>();queue.add(seed);
        while(!queue.isEmpty()&&placed<limit&&probes++<96){
            BlockPos p=queue.removeFirst();
            if(p.getX()<minX||p.getX()>minX+15||p.getZ()<minZ||p.getZ()>minZ+15||!seen.add(p.asLong()))continue;
            BlockState state=target(level,p,c.config(),random);if(state==null)continue;
            level.setBlock(p,state,Block.UPDATE_CLIENTS);placed++;
            var directions=new ArrayList<>(List.of(Direction.values()));Collections.rotate(directions,random.nextInt(6));
            for(Direction d:directions){BlockPos n=p.relative(d);if(adjacentAir(level,n))queue.addFirst(n);else queue.addLast(n);}
        }
        return placed>0;
    }
    private boolean vein(FeaturePlaceContext<OreConfiguration> c){
        var level=c.level();var random=c.random();var config=c.config();BlockPos origin=c.origin();
        int minX=origin.getX()&~15,minZ=origin.getZ()&~15,max=Math.min(40,config.size);
        if(target(level,origin,config,random)==null)return false;
        int length=max>=40?12+random.nextInt(13):10+random.nextInt(11),placed=0;
        Direction front=Direction.Plane.HORIZONTAL.getRandomDirection(random);BlockPos p=origin;
        var branches=new ArrayList<BlockPos>();var visited=new HashSet<Long>();
        for(int i=0;i<length&&placed<max;i++){
            if(i==length/3||i==length*2/3)branches.add(p);
            placed+=put(level,p,config,random,visited);
            if(random.nextBoolean()&&placed<max)placed+=put(level,p.above(),config,random,visited);
            BlockPos next=p.relative(front);
            if(next.getX()<minX||next.getX()>minX+15||next.getZ()<minZ||next.getZ()>minZ+15){front=front.getOpposite();next=p.relative(front);}
            p=next.offset(0,random.nextInt(3)-1,0);
            if(random.nextInt(5)==0)front=random.nextBoolean()?front.getClockWise():front.getCounterClockWise();
        }
        for(BlockPos start:branches){
            p=start;Direction side=random.nextBoolean()?front.getClockWise():front.getCounterClockWise();
            for(int i=0;i<4&&placed<max;i++){
                p=p.relative(side);if(p.getX()<minX||p.getX()>minX+15||p.getZ()<minZ||p.getZ()>minZ+15)break;
                placed+=put(level,p,config,random,visited);
            }
        }
        return placed>0;
    }
    private static int put(WorldGenLevel level,BlockPos p,OreConfiguration config,RandomSource random,Set<Long> visited){
        if(!visited.add(p.asLong()))return 0;BlockState target=target(level,p,config,random);if(target==null)return 0;
        level.setBlock(p,target,Block.UPDATE_CLIENTS);return 1;
    }
}
