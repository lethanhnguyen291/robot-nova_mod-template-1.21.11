package net.teogemini.nova.entity.ai.tree;

import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.teogemini.nova.entity.NovaEntity;
import net.teogemini.nova.entity.NovaChestHandler;
import net.teogemini.nova.entity.NovaWorkSupport;
import net.teogemini.nova.entity.companion.NovaCompanionPaths;
import net.teogemini.nova.registry.ModBlocks;

/** All mutable state is server-thread-only. No chunk loading and no entity references in caches. */
public final class NovaTreeService {
    public static final int RADIUS=32, SCAN_BUDGET=768, PATH_BUDGET=2, CACHE_LIMIT=2048;
    private static final Map<MinecraftServer,Manager> SERVERS=new IdentityHashMap<>();
    private NovaTreeService() {}
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server->{ var m=SERVERS.get(server);if(m!=null)m.tick(server); });
        ServerLifecycleEvents.SERVER_STOPPED.register(SERVERS::remove);
        UseBlockCallback.EVENT.register((player,world,hand,hit)->{
            if (hand!=InteractionHand.MAIN_HAND || player.isSpectator() || !player.isShiftKeyDown()
                    || !player.getMainHandItem().is(ModBlocks.NOVA_CONTROL_ROD_ITEM)
                    || NovaTreePlan.Species.of(world.getBlockState(hit.getBlockPos()))==null) return InteractionResult.PASS;
            if (player instanceof ServerPlayer owner) {
                NovaEntity nova=NovaChestHandler.selected(owner);
                if (nova==null || !owner.hasLineOfSight(nova) || !world.mayInteract(owner,hit.getBlockPos())) {
                    NovaWorkSupport.message(owner,"Chọn NOVA bằng gậy trước, rồi Shift + bấm khúc gỗ để đánh dấu cây cần giữ.");
                } else {
                    int result=nova.trees().protect(GlobalPos.of(world.dimension(),hit.getBlockPos().immutable()));
                    if(result==1 && nova.trees().active()!=null && nova.trees().active().logs().contains(hit.getBlockPos())) {
                        release(nova,nova.trees().active());nova.trees().cancel();
                    }
                    NovaWorkSupport.message(owner,result<0 ? "Đã đủ 32 dấu bảo vệ cây."
                            : result==1 ? "Đã đánh dấu giữ cây này cho mình." : "Đã bỏ dấu giữ cây.");
                }
            }
            return InteractionResult.SUCCESS;
        });
    }
    public static BlockState read(ServerLevel level,BlockPos pos) {
        return level.isInWorldBounds(pos) && level.hasChunkAt(pos) ? level.getBlockState(pos) : null;
    }
    public static boolean inside(NovaEntity nova,BlockPos pos) {
        return area(nova,pos) && nova.level().hasChunkAt(pos) && nova.level().getWorldBorder().isWithinBounds(pos);
    }
    public static boolean area(NovaEntity nova,BlockPos pos) {
        BlockPos home=nova.getLinkedChest();
        if (home==null) return false;
        long dx=(long)pos.getX()-home.getX(),dz=(long)pos.getZ()-home.getZ();
        return dx*dx+dz*dz<=RADIUS*RADIUS && pos.getY()>=home.getY()-16 && pos.getY()<=home.getY()+40;
    }
    private static Manager manager(ServerLevel level) { return SERVERS.computeIfAbsent(level.getServer(),s->new Manager()); }
    public static void request(NovaEntity nova) {
        if (!(nova.level() instanceof ServerLevel level) || nova.getLinkedChest()==null) return;
        var m=manager(level);var key=GlobalPos.of(level.dimension(),nova.getLinkedChest());
        Scan present=m.scans.get(key);
        if (present!=null) { present.expires=level.getGameTime()+200;return; }
        if (m.scans.size()>=32 || level.getGameTime()<m.nextScan.getOrDefault(key,0L)) return;
        Scan scan=new Scan(level,key,m.cache(level));m.scans.put(key,scan);m.queue.addLast(scan);
    }
    public static List<NovaTreePlan> candidates(NovaEntity nova) {
        if (!(nova.level() instanceof ServerLevel level)) return List.of();
        return manager(level).cache(level).near(nova.blockPosition(),level.getGameTime()).stream()
                .filter(p->inside(nova,p.root()))
                .filter(p->!nova.trees().protectedTree(level.dimension(),p))
                .filter(p->!nova.companion().retry.blocked(p.root(),level.getGameTime()))
                .sorted(Comparator.comparingDouble(p->p.root().distSqr(nova.blockPosition())
                        +Math.abs(p.root().getY()-nova.getY())*8+p.logs().size()*.2))
                .limit(8).toList();
    }
    public static boolean claim(NovaEntity nova,NovaTreePlan plan) {
        ServerLevel level=(ServerLevel)nova.level();
        return manager(level).leases.claim(GlobalPos.of(level.dimension(),plan.root()),nova.getUUID(),level.getGameTime());
    }
    public static void release(NovaEntity nova,NovaTreePlan plan) {
        if (plan!=null && nova.level() instanceof ServerLevel level)
            manager(level).leases.release(GlobalPos.of(level.dimension(),plan.root()),nova.getUUID());
    }
    public static void forget(ServerLevel level,NovaTreePlan plan) { manager(level).cache(level).remove(plan.root()); }
    public record Route(boolean attempted,Path path) {}
    public static Route route(NovaEntity nova,BlockPos stand) {
        var m=manager((ServerLevel)nova.level());
        if (!m.paths.take(((ServerLevel)nova.level()).getServer().getTickCount())) return new Route(false,null);
        if (!NovaCompanionPaths.safe(nova,stand)) return new Route(true,null);
        // This overload sets the search range for this request without changing FOLLOW_RANGE.
        Path path=nova.getNavigation().createPath(stand,0,48);
        if (path==null || !path.canReach() || path.getNodeCount()>96) return new Route(true,null);
        for (int i=0;i<path.getNodeCount();i++) {
            var n=path.getNode(i);
            if (!NovaCompanionPaths.safe(nova,new BlockPos(n.x,n.y,n.z))) return new Route(true,null);
            if (i>0 && Math.abs(n.y-path.getNode(i-1).y)>1) return new Route(true,null);
        }
        return new Route(true,path);
    }
    public static List<BlockPos> stands(NovaEntity nova,BlockPos root) {
        List<BlockPos> options=new ArrayList<>();
        for (var direction:net.minecraft.core.Direction.Plane.HORIZONTAL)
            for (int y=0;y>=-1;y--) options.add(root.relative(direction,2).above(y));
        options.sort(Comparator.comparingDouble(p->p.distSqr(nova.blockPosition())));
        return options;
    }
    public static final class TickBudget {
        private final int maximum; private int used,last=Integer.MIN_VALUE;
        public TickBudget(int maximum) { this.maximum=maximum; }
        public boolean take(int tick) { if(tick!=last){used=0;last=tick;}if(used>=maximum)return false;used++;return true; }
    }
    public static final class Reservations {
        private record Lease(UUID robot,long until) {}
        private final Map<GlobalPos,Lease> entries=new HashMap<>();
        public boolean claim(GlobalPos root,UUID robot,long now) {
            Lease old=entries.get(root);
            if(old!=null && old.until>now && !old.robot.equals(robot))return false;
            if(old==null && entries.size()>=2048)return false;
            entries.put(root,new Lease(robot,now+200));return true;
        }
        public void release(GlobalPos root,UUID robot) { var l=entries.get(root);if(l!=null && l.robot.equals(robot))entries.remove(root); }
        void expire(MinecraftServer server) { entries.entrySet().removeIf(e->{var l=server.getLevel(e.getKey().dimension());return l==null || e.getValue().until<=l.getGameTime();}); }
    }
    private static final class Cache {
        private final LinkedHashMap<BlockPos,NovaTreePlan> plans=new LinkedHashMap<>();
        private final Map<Long,Set<BlockPos>> chunks=new HashMap<>();
        private final Map<BlockPos,Long> expires=new HashMap<>();
        private final LinkedHashMap<BlockPos,Long> checked=new LinkedHashMap<>();
        void remove(BlockPos root) {
            plans.remove(root);expires.remove(root);long key=ChunkPos.asLong(root);var set=chunks.get(key);
            if(set!=null){set.remove(root);if(set.isEmpty())chunks.remove(key);}
        }
        void put(NovaTreePlan plan,long now) {
            remove(plan.root());plans.put(plan.root(),plan);
            expires.put(plan.root(),now+600);
            chunks.computeIfAbsent(ChunkPos.asLong(plan.root()),k->new HashSet<>()).add(plan.root());
            while(plans.size()>CACHE_LIMIT)remove(plans.keySet().iterator().next());
        }
        void checked(BlockPos pos,long now) { checked.put(pos.immutable(),now+600);while(checked.size()>4096)checked.remove(checked.keySet().iterator().next()); }
        List<NovaTreePlan> near(BlockPos pos,long now) {
            List<NovaTreePlan> out=new ArrayList<>();int cx=pos.getX()>>4,cz=pos.getZ()>>4;
            for(int x=cx-2;x<=cx+2;x++)for(int z=cz-2;z<=cz+2;z++)
                for(BlockPos root:chunks.getOrDefault(ChunkPos.asLong(x,z),Set.of()))
                    if(now<expires.getOrDefault(root,0L))out.add(plans.get(root));
            return out;
        }
    }
    private static final class Scan {
        final ServerLevel level;final GlobalPos key;final Cache cache;final Iterator<BlockPos> cursor;
        long expires;NovaTreePlan.Analyzer analyzer;BlockPos candidate;
        Scan(ServerLevel level,GlobalPos key,Cache cache) {
            this.level=level;this.key=key;this.cache=cache;expires=level.getGameTime()+200;
            cursor=BlockPos.withinManhattan(key.pos(),RADIUS,16,RADIUS).iterator();
        }
        int step(int limit) {
            int used=0;
            while(used<limit) {
                if(analyzer!=null) {
                    used+=analyzer.step(pos->read(level,pos),limit-used);
                    if(!analyzer.done())break;
                    var plan=analyzer.result();cache.checked(candidate,level.getGameTime());
                    if(plan!=null){cache.put(plan,level.getGameTime());for(var root:plan.roots())cache.checked(root,level.getGameTime());}
                    analyzer=null;continue;
                }
                if(!cursor.hasNext())break;
                BlockPos pos=cursor.next().immutable();used++;
                long dx=(long)pos.getX()-key.pos().getX(),dz=(long)pos.getZ()-key.pos().getZ();
                if(dx*dx+dz*dz>RADIUS*RADIUS || level.getGameTime()<cache.checked.getOrDefault(pos,0L))continue;
                var state=read(level,pos);var species=NovaTreePlan.Species.of(state);
                if(species!=null && NovaTreePlan.soil(read(level,pos.below()))) {
                    candidate=pos;analyzer=new NovaTreePlan.Analyzer(pos,species);
                }
            }
            return used;
        }
        boolean finished(){return analyzer==null && !cursor.hasNext();}
    }
    private static final class Manager {
        final Map<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>,Cache> worlds=new HashMap<>();
        final Map<GlobalPos,Scan> scans=new HashMap<>();final ArrayDeque<Scan> queue=new ArrayDeque<>();
        final LinkedHashMap<GlobalPos,Long> nextScan=new LinkedHashMap<>();
        final Reservations leases=new Reservations();final TickBudget paths=new TickBudget(PATH_BUDGET);
        Cache cache(ServerLevel level){return worlds.computeIfAbsent(level.dimension(),k->new Cache());}
        void tick(MinecraftServer server) {
            int remaining=SCAN_BUDGET;
            while(remaining>0 && !queue.isEmpty()) {
                Scan scan=queue.removeFirst();
                if(server.getLevel(scan.key.dimension())!=scan.level || scan.expires<scan.level.getGameTime()) {scans.remove(scan.key);continue;}
                int used=scan.step(Math.min(64,remaining));remaining-=Math.max(1,used);
                if(scan.finished()){scans.remove(scan.key);nextScan.put(scan.key,scan.level.getGameTime()+600);}
                else queue.addLast(scan);
            }
            while(nextScan.size()>256)nextScan.remove(nextScan.keySet().iterator().next());
            if(server.getTickCount()%20==0)leases.expire(server);
        }
    }
}
