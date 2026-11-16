package net.teogemini.nova.client.screen.preview;

import java.util.*;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.fabricmc.fabric.api.client.rendering.v1.world.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.teogemini.nova.entity.NovaEntity;

/** Local positioning aid. No server packets, blocks, entities, or chunk scans. */
public final class NovaWorldGhost {
    public record Spec(BlockPos anchor,int width,int height,int depth,Direction tunnelFront){
        public Spec{anchor=anchor.immutable();NovaGhostGeometry.bounds(anchor,width,height,depth,tunnelFront);}
    }
    private record Line(double ax,double ay,double az,double bx,double by,double bz,int color){}
    private record Frame(List<Line> lines){}
    private static final RenderStateDataKey<Frame> FRAME=RenderStateDataKey.create(()->"NOVA construction outline");
    private static final RenderPipeline PIPELINE=RenderPipelines.register(RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("robot-nova_mod","pipeline/construction_ghost")).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false).withBlend(BlendFunction.TRANSLUCENT).build());
    private static final RenderType TYPE=RenderType.create("nova_construction_ghost",RenderSetup.builder(PIPELINE).createRenderSetup());
    private static ClientLevel level;
    private static UUID robot;
    private static Spec spec;
    private static Frame frame;
    private static boolean registered;
    private NovaWorldGhost(){}
    public static boolean active(UUID id){return id.equals(robot);}
    public static void enable(NovaEntity nova){level=(ClientLevel)nova.level();robot=nova.getUUID();spec=null;frame=null;}
    public static void disable(){robot=null;level=null;spec=null;frame=null;}
    public static void update(UUID id,Spec next){
        if(!active(id)||Objects.equals(spec,next))return;
        spec=next;frame=next==null?null:makeFrame(next);
    }
    public static void register(){
        if(registered)return;registered=true;
        ClientTickEvents.END_CLIENT_TICK.register(client->{if(client.level!=level||client.player==null)disable();});
        WorldRenderEvents.END_EXTRACTION.register(context->{
            if(context.worldState()==null)return;
            Frame snapshot=context.world()==level?frame:null;
            context.worldState().setData(FRAME,snapshot);
        });
        WorldRenderEvents.END_MAIN.register(NovaWorldGhost::draw);
    }
    private static Frame makeFrame(Spec s){
        AABB b=NovaGhostGeometry.bounds(s.anchor,s.width,s.height,s.depth,s.tunnelFront);
        List<Line> lines=new ArrayList<>();int cyan=0xb055ddff;
        for(double y:new double[]{b.minY,b.maxY}){
            lines.add(new Line(b.minX,y,b.minZ,b.maxX,y,b.minZ,cyan));lines.add(new Line(b.minX,y,b.maxZ,b.maxX,y,b.maxZ,cyan));
            lines.add(new Line(b.minX,y,b.minZ,b.minX,y,b.maxZ,cyan));lines.add(new Line(b.maxX,y,b.minZ,b.maxX,y,b.maxZ,cyan));
        }
        for(double x:new double[]{b.minX,b.maxX})for(double z:new double[]{b.minZ,b.maxZ})lines.add(new Line(x,b.minY,z,x,b.maxY,z,cyan));
        int step=Math.max(1,(int)Math.ceil(Math.max(b.getXsize(),b.getZsize())/16));
        for(double x=b.minX+step;x<b.maxX;x+=step)lines.add(new Line(x,b.minY+.01,b.minZ,x,b.minY+.01,b.maxZ,0x5047a9cf));
        for(double z=b.minZ+step;z<b.maxZ;z+=step)lines.add(new Line(b.minX,b.minY+.01,z,b.maxX,b.minY+.01,z,0x5047a9cf));
        // Mark the exact anchor block, including tunnels whose width/depth runs toward negative coordinates.
        double x=s.anchor.getX()+.5,y=s.anchor.getY()+.05,z=s.anchor.getZ()+.5;
        lines.add(new Line(x-.4,y,z,x+.4,y,z,0xffff6969));lines.add(new Line(x,y,z-.4,x,y,z+.4,0xff72beff));
        lines.add(new Line(x,y,z,x,y+1.3,z,0xff8aff77));
        return new Frame(List.copyOf(lines));
    }
    private static void draw(WorldRenderContext context){
        if(context.worldState()==null||context.matrices()==null||context.consumers()==null)return;
        Frame snapshot=context.worldState().getData(FRAME);if(snapshot==null)return;
        Vec3 camera=context.worldState().cameraRenderState.pos;
        var pose=context.matrices().last();VertexConsumer out=context.consumers().getBuffer(TYPE);
        for(Line line:snapshot.lines){
            double dx=line.bx-line.ax,dy=line.by-line.ay,dz=line.bz-line.az,len=Math.sqrt(dx*dx+dy*dy+dz*dz);
            if(len==0)continue;
            out.addVertex(pose,(float)(line.ax-camera.x),(float)(line.ay-camera.y),(float)(line.az-camera.z))
                    .setColor(line.color).setNormal(pose,(float)(dx/len),(float)(dy/len),(float)(dz/len)).setLineWidth(2);
            out.addVertex(pose,(float)(line.bx-camera.x),(float)(line.by-camera.y),(float)(line.bz-camera.z))
                    .setColor(line.color).setNormal(pose,(float)(dx/len),(float)(dy/len),(float)(dz/len)).setLineWidth(2);
        }
    }
}
