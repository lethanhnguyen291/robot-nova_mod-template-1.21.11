package net.teogemini.nova.client.screen.preview;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.teogemini.nova.entity.ai.build.NovaBuildGeometry;

/** Same inclusive block coordinates as the work plan; the outline encloses whole blocks. */
public final class NovaGhostGeometry {
    private NovaGhostGeometry() {}
    public static AABB bounds(BlockPos anchor,int w,int h,int d,Direction tunnelFront){
        NovaBuildGeometry.volume(w,h,d);
        BlockPos end=tunnelFront==null?anchor.offset(w-1,h-1,d-1)
                :NovaBuildGeometry.tunnel(anchor,tunnelFront,w-1,h-1,d-1);
        return new AABB(Math.min(anchor.getX(),end.getX()),anchor.getY(),Math.min(anchor.getZ(),end.getZ()),
                Math.max(anchor.getX(),end.getX())+1,anchor.getY()+h,Math.max(anchor.getZ(),end.getZ())+1);
    }
    public static double cameraYaw(Direction front){return Math.atan2(front.getStepX(),front.getStepZ());}
}
