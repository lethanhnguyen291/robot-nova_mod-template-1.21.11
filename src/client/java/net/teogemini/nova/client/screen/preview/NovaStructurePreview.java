package net.teogemini.nova.client.screen.preview;

import java.util.*;
import org.joml.Matrix3x2f;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.teogemini.nova.entity.ai.build.*;

/** Native block model faces + the active block atlas. Camera-only changes never contact the server. */
public final class NovaStructurePreview {
    private static final int MAX_FACES=60000;
    private record Quad(float[] xyz,float[] uv,Direction normal,int color){}
    private record Models(List<Quad> general,Map<Direction,List<Quad>> sides,boolean simplified){}
    private record Face(int x,int y,int z,Quad quad){}
    private record Projected(float[] vertices,int color,float depth){}
    private final NovaPreviewCamera camera=new NovaPreviewCamera();
    private final Map<BlockState,Models> models=new HashMap<>();
    private final List<Face> faces=new ArrayList<>();
    private NovaBlueprint blueprint;
    private NovaBuildGeometry.Shape tunnelShape;
    private int width=5,height=5,depth=5,turns,layer=5,cursor,generation;
    private int simplified;private boolean capped;
    private Object modelGeneration;
    private List<Projected> projected=List.of();
    private int projectedCamera=-1,projectedGeneration=-1,lastW,lastH;
    private boolean box=true,grid=true;
    private boolean volumeOnly;
    private boolean interior,cutX,cutZ,cutTop=true;
    public void blueprint(NovaBlueprint bp,int rotation){volumeOnly=false;blueprint=bp;tunnelShape=null;setRotation(rotation);resetCamera();}
    public void tunnel(int w,int h,int d,NovaBuildGeometry.Shape shape){
        NovaBuildGeometry.volume(w,h,d);volumeOnly=false;blueprint=null;tunnelShape=shape;width=w;height=h;depth=d;turns=0;layer=h;restart();
    }
    public void volume(int w,int h,int d){NovaBuildGeometry.volume(w,h,d);blueprint=null;tunnelShape=null;volumeOnly=true;width=w;height=h;depth=d;layer=h;restart();}
    public void setRotation(int rotation){
        turns=Math.floorMod(rotation,4);
        if(blueprint!=null){width=turns%2==0?blueprint.width:blueprint.depth;height=blueprint.height;depth=turns%2==0?blueprint.depth:blueprint.width;}
        layer=height;restart();
    }
    public void layer(int change){layer=Math.max(1,Math.min(height,layer+change));restart();}
    public int layer(){return layer;}
    public int width(){return width;}public int height(){return height;}public int depth(){return depth;}
    public boolean hasModel(){return blueprint!=null||tunnelShape!=null||volumeOnly;}
    public boolean box(){return box;}public boolean grid(){return grid;}
    public void toggleBox(){box=!box;}public void toggleGrid(){grid=!grid;}
    public boolean interior(){return interior;}
    public void toggleInterior(){interior=!interior;cutSides();restart();}
    public void defaultYaw(double yaw){camera.defaultYaw(yaw);if(cutSides()&&interior)restart();}
    private boolean cutSides(){
        boolean x=camera.facing(1,0,0)>=0,z=camera.facing(0,0,1)>=0,top=camera.facing(0,1,0)>=0;
        boolean changed=x!=cutX||z!=cutZ||top!=cutTop;cutX=x;cutZ=z;cutTop=top;return changed;
    }
    public void orbit(double x,double y){camera.orbit(x,y);if(cutSides()&&interior)restart();}
    public void zoom(double amount){camera.zoom(amount);}
    public void resetCamera(){camera.reset();if(cutSides()&&interior)restart();}
    public void clear(){volumeOnly=false;blueprint=null;tunnelShape=null;faces.clear();models.clear();projected=List.of();generation++;}
    private void restart(){faces.clear();models.clear();projected=List.of();cursor=simplified=0;capped=false;generation++;}
    private int total(){return width*height*depth;}
    public String status(){
        if(!hasModel())return "Chọn hoặc quét một công trình để xem.";
        if(volumeOnly)return "Khung vùng chọn • Nhấn Quét vùng chọn để lấy mô hình.";
        if(cursor<total()&&!capped)return "Đang dựng mô hình: "+cursor*100/total()+"%";
        if(capped)return "Mẫu nhiều chi tiết: giảm lớp Y để xem phần còn lại.";
        if(simplified>0)return simplified+" khối đặc biệt hiển thị dạng đơn giản.";
        return interior?"Nội thất: ẩn hai vách gần và mái/sàn • Xoay 360°":"Kéo để xoay 360° • Lăn chuột để thu/phóng";
    }
    private BlockState state(int x,int y,int z){
        if(x<0||x>=width||y<0||y>=layer||z<0||z>=depth)return Blocks.AIR.defaultBlockState();
        if(interior&&((height>1&&y==(cutTop?height-1:0))
                ||(width>2&&x==(cutX?width-1:0))||(depth>2&&z==(cutZ?depth-1:0))))return Blocks.AIR.defaultBlockState();
        if(tunnelShape!=null)return NovaBuildGeometry.inside(tunnelShape,x,y,width,height)?Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState():Blocks.AIR.defaultBlockState();
        if(blueprint==null)return Blocks.AIR.defaultBlockState();
        int sx=switch(turns){case 1->z;case 2->blueprint.width-1-x;case 3->blueprint.width-1-z;default->x;};
        int sz=switch(turns){case 1->blueprint.depth-1-x;case 2->blueprint.depth-1-z;case 3->x;default->z;};
        return blueprint.state(sx,y,sz).rotate(Rotation.values()[turns]);
    }
    public void tick(){
        if(!hasModel()||volumeOnly)return;Minecraft mc=Minecraft.getInstance();
        Object sentinel=mc.getBlockRenderer().getBlockModel(Blocks.STONE.defaultBlockState());
        if(modelGeneration!=sentinel){modelGeneration=sentinel;restart();}
        if(capped||cursor>=total())return;
        long deadline=System.nanoTime()+2_000_000;int budget=1024;int before=faces.size();
        while(cursor<total()&&budget-->0&&System.nanoTime()<deadline){
            int i=cursor++,x=i%width,y=i/(width*depth),z=i/width%depth;
            if(y>=layer)continue;BlockState state=state(x,y,z);if(state.isAir())continue;
            Models baked=models.computeIfAbsent(state,this::bake);if(baked.simplified)simplified++;
            for(Quad q:baked.general)if(!addFace(x,y,z,q))break;
            if(capped)break;
            for(Direction direction:Direction.values()){
                BlockState neighbor=state(x+direction.getStepX(),y+direction.getStepY(),z+direction.getStepZ());
                if(neighbor.isSolidRender())continue;
                for(Quad q:baked.sides.getOrDefault(direction,List.of()))if(!addFace(x,y,z,q))break;
                if(capped)break;
            }
            if(capped)break;
        }
        if(before!=faces.size())generation++;
    }
    private boolean addFace(int x,int y,int z,Quad q){
        if(faces.size()>=MAX_FACES){capped=true;return false;}faces.add(new Face(x,y,z,q));return true;
    }
    private Models bake(BlockState state){
        var mc=Minecraft.getInstance();var model=mc.getBlockRenderer().getBlockModel(state);
        List<Quad> general=new ArrayList<>();Map<Direction,List<Quad>> sides=new EnumMap<>(Direction.class);
        for(var part:model.collectParts(RandomSource.create(42))){
            for(BakedQuad quad:part.getQuads(null))general.add(convert(quad,state));
            for(Direction direction:Direction.values())for(BakedQuad quad:part.getQuads(direction))
                sides.computeIfAbsent(direction,k->new ArrayList<>()).add(convert(quad,state));
        }
        boolean fallback=general.isEmpty()&&sides.isEmpty();
        if(fallback){
            // Chests/beds and other block-entity-only models need their own renderers; show an explicit textured placeholder.
            var sprite=model.particleIcon();
            for(Direction direction:Direction.values())sides.put(direction,List.of(cube(direction,sprite)));
        }
        return new Models(general,sides,fallback);
    }
    private static Quad convert(BakedQuad q,BlockState state){
        float[] xyz=new float[12],uv=new float[8];
        for(int i=0;i<4;i++){var v=q.position(i);xyz[i*3]=v.x();xyz[i*3+1]=v.y();xyz[i*3+2]=v.z();uv[i*2]=UVPair.unpackU(q.packedUV(i));uv[i*2+1]=UVPair.unpackV(q.packedUV(i));}
        int color=q.isTinted()?Minecraft.getInstance().getBlockColors().getColor(state,null,null,q.tintIndex()):0xffffff;
        return new Quad(xyz,uv,q.direction(),shade(color,q.shade()?q.direction():Direction.UP));
    }
    private static int shade(int color,Direction side){
        float f=switch(side){case UP->1;case DOWN->.55F;case NORTH,SOUTH->.78F;default->.88F;};
        return 0xff000000|((int)((color>>16&255)*f)<<16)|((int)((color>>8&255)*f)<<8)|(int)((color&255)*f);
    }
    private static Quad cube(Direction d,TextureAtlasSprite s){
        float[] xyz=switch(d){
            case UP->new float[]{0,1,0,0,1,1,1,1,1,1,1,0};
            case DOWN->new float[]{0,0,1,0,0,0,1,0,0,1,0,1};
            case NORTH->new float[]{1,1,0,1,0,0,0,0,0,0,1,0};
            case SOUTH->new float[]{0,1,1,0,0,1,1,0,1,1,1,1};
            case EAST->new float[]{1,1,1,1,0,1,1,0,0,1,1,0};
            case WEST->new float[]{0,1,0,0,0,0,0,0,1,0,1,1};
        };
        return new Quad(xyz,new float[]{s.getU0(),s.getV0(),s.getU0(),s.getV1(),s.getU1(),s.getV1(),s.getU1(),s.getV0()},d,shade(0xffffff,d));
    }
    private void project(int w,int h){
        if(projectedCamera==camera.revision()&&projectedGeneration==generation&&lastW==w&&lastH==h)return;
        ArrayList<Projected> result=new ArrayList<>();
        var transform=camera.transform(width,height,depth,w,h);
        for(Face face:faces){
            Quad q=face.quad;Direction normal=q.normal;
            if(transform.facing(normal.getStepX(),normal.getStepY(),normal.getStepZ())<=0)continue;
            float[] v=new float[16];float depth=0;
            for(int i=0;i<4;i++){
                var p=transform.project(face.x+q.xyz[i*3],face.y+q.xyz[i*3+1],face.z+q.xyz[i*3+2]);
                v[i*4]=p.x();v[i*4+1]=p.y();v[i*4+2]=q.uv[i*2];v[i*4+3]=q.uv[i*2+1];depth+=p.depth();
            }
            result.add(new Projected(v,q.color,depth*.25F));
        }
        result.sort(Comparator.comparingDouble(Projected::depth));projected=List.copyOf(result);
        projectedCamera=camera.revision();projectedGeneration=generation;lastW=w;lastH=h;
    }
    public void render(GuiGraphics g,int x,int y,int w,int h){
        g.fill(x,y,x+w,y+h,0xff102a3c);if(!hasModel())return;
        ScreenRectangle clip=new ScreenRectangle(x,y,w,h);Matrix3x2f pose=new Matrix3x2f(g.pose());
        if(grid){
            List<float[]> lines=new ArrayList<>();
            int step=Math.max(1,Math.max(width,depth)/16);
            for(int i=0;i<=width;i+=step)line(lines,i,0,0,i,0,depth,0xff23536e,w,h);
            for(int i=0;i<=depth;i+=step)line(lines,0,0,i,width,0,i,0xff23536e,w,h);
            submitLines(g,lines,pose,clip,x,y);
        }
        project(w,h);
        var atlas=Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);
        g.guiRenderState.submitGuiElement(new ModelState(projected,pose,clip,TextureSetup.singleTexture(atlas.getTextureView(),atlas.getSampler()),x,y));
        if(box){
            List<float[]> lines=new ArrayList<>();
            for(int yy:new int[]{0,height}){
                line(lines,0,yy,0,width,yy,0,0xffa8e8fa,w,h);line(lines,0,yy,depth,width,yy,depth,0xffa8e8fa,w,h);
                line(lines,0,yy,0,0,yy,depth,0xffa8e8fa,w,h);line(lines,width,yy,0,width,yy,depth,0xffa8e8fa,w,h);
            }
            for(int xx:new int[]{0,width})for(int zz:new int[]{0,depth})line(lines,xx,0,zz,xx,height,zz,0xffa8e8fa,w,h);
            line(lines,0,0,0,width,0,0,0xffff5858,w,h);line(lines,0,0,0,0,height,0,0xff75e86a,w,h);line(lines,0,0,0,0,0,depth,0xff5bb9ff,w,h);
            submitLines(g,lines,pose,clip,x,y);
        }
    }
    private void line(List<float[]> lines,float x0,float y0,float z0,float x1,float y1,float z1,int color,int w,int h){
        var a=camera.project(x0,y0,z0,width,height,depth,w,h);var b=camera.project(x1,y1,z1,width,height,depth,w,h);
        lines.add(new float[]{a.x(),a.y(),b.x(),b.y(),Float.intBitsToFloat(color)});
    }
    private static void submitLines(GuiGraphics g,List<float[]> lines,Matrix3x2f pose,ScreenRectangle clip,int x,int y){
        g.guiRenderState.submitGuiElement(new LinesState(List.copyOf(lines),pose,clip,x,y));
    }
    private record ModelState(List<Projected> faces,Matrix3x2f pose,ScreenRectangle bounds,TextureSetup textureSetup,int x,int y) implements GuiElementRenderState {
        public RenderPipeline pipeline(){return RenderPipelines.GUI_TEXTURED;}
        public ScreenRectangle scissorArea(){return bounds;}
        public void buildVertices(VertexConsumer out){
            for(Projected p:faces){float[] v=p.vertices;
                // Keep the GUI's native winding even for unusual resource-pack quad winding.
                boolean reverse=(v[4]-v[0])*(v[9]-v[1])-(v[5]-v[1])*(v[8]-v[0])>0;
                for(int j=0;j<4;j++){int i=(reverse?3-j:j)*4;vertex(out,pose,x+v[i],y+v[i+1]).setUv(v[i+2],v[i+3]).setColor(p.color);}
            }
        }
    }
    private record LinesState(List<float[]> lines,Matrix3x2f pose,ScreenRectangle bounds,int x,int y) implements GuiElementRenderState {
        public RenderPipeline pipeline(){return RenderPipelines.GUI;}
        public TextureSetup textureSetup(){return TextureSetup.noTexture();}
        public ScreenRectangle scissorArea(){return bounds;}
        public void buildVertices(VertexConsumer out){
            for(float[] line:lines){
                float ax=x+line[0],ay=y+line[1],bx=x+line[2],by=y+line[3],dx=bx-ax,dy=by-ay;
                float length=(float)Math.sqrt(dx*dx+dy*dy);if(length<.01)continue;
                float nx=-dy/length*.55F,ny=dx/length*.55F;int color=Float.floatToRawIntBits(line[4]);
                vertex(out,pose,ax-nx,ay-ny).setColor(color);vertex(out,pose,ax+nx,ay+ny).setColor(color);
                vertex(out,pose,bx+nx,by+ny).setColor(color);vertex(out,pose,bx-nx,by-ny).setColor(color);
            }
        }
    }
    private static VertexConsumer vertex(VertexConsumer out,Matrix3x2f pose,float x,float y){
        return out.addVertex(pose.m00()*x+pose.m10()*y+pose.m20(),pose.m01()*x+pose.m11()*y+pose.m21(),0);
    }
}
