package net.teogemini.nova.client.screen;

import com.google.gson.*;
import java.util.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.teogemini.nova.client.screen.preview.*;
import net.teogemini.nova.entity.NovaEntity;
import net.teogemini.nova.entity.ai.build.*;
import net.teogemini.nova.network.NovaRodControl;

/** Structure-block-style editor for Java: controls on the left, interactive block models on the right. */
public class NovaStructureScreen extends Screen {
    private enum Page { LIBRARY, CONFIGURE, CAPTURE, NEARBY, MATERIALS }
    private record Pending(String op,JsonObject data){}
    private record Label(String text,int x,int y,int color){}
    private final int entityId;private final UUID robot;private final boolean building;
    private UUID token=new UUID(0,0);private int sequence,revision,age,pageNo,pages=1,materialPage;
    private boolean opened,ready,free,creative,confirmCancel,dragging,dirtyVolume;
    private Page page;private NovaViewerLayout layout;private int column;
    private int turns,front,shape,pendingTurns,done,total,omitted;
    private String selectedId="",selectedName="Chưa chọn mẫu",status="Đang kết nối với NOVA...",previewDraft="",captureDraft="";
    private String sceneContext="",pendingContext="",transferStatus="";
    private final Map<String,String> values=new HashMap<>();
    private final List<Label> labels=new ArrayList<>();
    private final ArrayDeque<Pending> pending=new ArrayDeque<>();
    private final NovaPreviewWire.Receiver receiver=new NovaPreviewWire.Receiver();
    private final NovaStructurePreview scene=new NovaStructurePreview();
    private JsonArray rows=new JsonArray(),regions=new JsonArray(),materials=new JsonArray();
    private Button startButton;
    private final NovaMaterialTable materialTable=new NovaMaterialTable();
    private int blueprintWidth,blueprintHeight,blueprintDepth;
    public NovaStructureScreen(NovaEntity nova,boolean building){
        super(Component.literal(building?"NOVA • Xây dựng":"NOVA • Đào hầm"));
        entityId=nova.getId();robot=nova.getUUID();this.building=building;page=building?Page.LIBRARY:Page.CONFIGURE;
        Direction facing=nova.getDirection();
        BlockPos ahead=nova.blockPosition().relative(facing,2);
        values.put("x",Integer.toString(ahead.getX()));values.put("y",Integer.toString(ahead.getY()));values.put("z",Integer.toString(ahead.getZ()));
        front=List.of(Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST).indexOf(facing);
        scene.defaultYaw(building?NovaGhostGeometry.cameraYaw(facing):0);
        values.put("w","3");values.put("h","3");values.put("d","8");values.put("name","Nhà của NOVA");
        if(!building){scene.tunnel(3,3,8,NovaBuildGeometry.Shape.BOX);sceneContext="tunnel";}
    }
    public static void register(){
        NovaWorldGhost.register();
        ClientPlayNetworking.registerGlobalReceiver(NovaConstructionService.Reply.TYPE,(packet,context)->{
            if(context.client().screen instanceof NovaStructureScreen screen)screen.receive(packet);
        });
    }
    private NovaEntity nova(){
        if(minecraft==null||minecraft.level==null||minecraft.player==null||!NovaRodControl.holdingRod(minecraft.player))return null;
        if(minecraft.level.getEntity(entityId) instanceof NovaEntity n&&n.getUUID().equals(robot)&&n.isAlive()
                &&n.isOwnedBy(minecraft.player)&&n.distanceToSqr(minecraft.player)<=64)return n;
        return null;
    }
    private void receive(NovaConstructionService.Reply p){
        if(p.entityId()!=entityId||!p.robot().equals(robot)||opened&&!p.token().equals(token)||p.revision()<=revision)return;
        try{
            JsonObject d=JsonParser.parseString(p.json()).getAsJsonObject();token=p.token();revision=p.revision();opened=true;
            switch(d.get("kind").getAsString()){
                case "message"->status=d.get("message").getAsString();
                case "status"->{status=d.get("message").getAsString();done=d.get("done").getAsInt();total=d.get("total").getAsInt();creative=d.get("creative").getAsBoolean();}
                case "list"->{rows=d.getAsJsonArray("rows");pageNo=d.get("page").getAsInt();pages=d.get("pages").getAsInt();if(page==Page.LIBRARY)rebuild();}
                case "selected"->{selectedId=d.get("id").getAsString();selectedName=d.get("name").getAsString();turns=0;readPreview(d,"build");ready=false;status="Đã chọn "+selectedName+". Thiết lập vị trí để xây.";rebuild();}
                case "preview"->{
                    if(!d.getAsJsonObject("draft").toString().equals(planData().toString()))break;
                    if(building)readPreview(d,"build");else updateVolume();
                    ready=d.get("ready").getAsBoolean();status="Đã xem trước. Kiểm tra mốc và hướng xây rồi Bắt đầu.";rebuild();
                }
                case "regions"->{regions=d.getAsJsonArray("regions");if(page==Page.NEARBY)rebuild();}
                case "capture"->{
                    if(!d.getAsJsonObject("region").toString().equals(captureData().toString())){status="Vùng chọn đã đổi; hãy quét lại.";break;}
                    readPreview(d,"capture");captureDraft=d.getAsJsonObject("region").toString();
                    status="Đã quét vùng; bỏ qua "+omitted+" khối không hỗ trợ. Đặt tên rồi Lưu mẫu.";rebuild();
                }
                case "model_part"->{
                    byte[] bytes=receiver.accept(UUID.fromString(d.get("id").getAsString()),d.get("part").getAsInt(),d.get("data").getAsString());
                    if(bytes!=null){scene.blueprint(NovaPreviewWire.decode(bytes),pendingContext.equals("build")?turns:pendingTurns);sceneContext=pendingContext;transferStatus="";rebuild();}
                    else if(!transferStatus.isEmpty())transferStatus="Đang nhận mô hình: "+receiver.percent()+"%";
                }
                default->{}
            }
        }catch(Exception ex){receiver.clear();transferStatus="";ready=false;if(startButton!=null)startButton.active=false;status="Không đọc được mô hình/phản hồi: "+(ex.getMessage()==null?"dữ liệu sai":ex.getMessage());}
    }
    private void readPreview(JsonObject d,String context){
        if(context.equals("build")){
            blueprintWidth=d.get("w").getAsInt();blueprintHeight=d.get("h").getAsInt();blueprintDepth=d.get("d").getAsInt();
        }
        if(d.has("materials")){materials=d.getAsJsonArray("materials");materialPage=0;materialTable.set(materials);}
        omitted=d.has("omitted")?d.get("omitted").getAsInt():0;
        if(!d.has("model")){status="Cần cập nhật phần máy chủ để dùng khung xem 3D.";return;}
        JsonObject model=d.getAsJsonObject("model");
        receiver.begin(UUID.fromString(model.get("id").getAsString()),model.get("bytes").getAsInt(),model.get("parts").getAsInt());
        pendingTurns=model.get("turns").getAsInt();pendingContext=context;transferStatus="Đang nhận mô hình...";
        scene.clear();sceneContext="";dirtyVolume=false;
    }
    private void queue(String op,JsonObject d){if(pending.size()<8)pending.add(new Pending(op,d.deepCopy()));}
    private void queue(String op){queue(op,new JsonObject());}
    private void rebuild(){clearWidgets();init();}
    private void navigate(Page next){
        page=next;confirmCancel=false;dragging=false;
        if(next==Page.CAPTURE&&!sceneContext.equals("capture")){captureDraft="";updateVolume();}
        rebuild();
    }
    @Override protected void init(){
        labels.clear();startButton=null;layout=NovaViewerLayout.fit(width,height);column=layout.sidebar()-12;
        if(layout.height()<228||layout.width()<310){label("Giảm GUI Scale để hiện đủ bảng.",10,50);button("Đóng",10,80,column,this::onClose);return;}
        button("Đóng",layout.width()-49,1,43,this::onClose);
        if(building){int w=(column-4)/3;button("Mẫu",12,29,w,()->navigate(Page.LIBRARY));button("Lưu",14+w,29,w,()->navigate(Page.CAPTURE));button("Vật liệu",16+w*2,29,w,()->navigate(Page.MATERIALS));}
        else label("Mốc: góc trái trước của hầm",12,32);
        switch(page){case LIBRARY->libraryPage();case CONFIGURE->configurePage();case CAPTURE->capturePage();case NEARBY->nearbyPage();case MATERIALS->materialsPage();}
        if(page!=Page.NEARBY&&page!=Page.MATERIALS){
            button("Y−",12,172,29,()->{scene.layer(-1);rebuild();});button("Y+",45,172,29,()->{scene.layer(1);rebuild();});label("Lớp "+scene.layer()+"/"+scene.height(),79,178);
        }
        int w=(column-4)/3,y=layout.height()-33;
        button("Dừng",12,y,w,()->queue("pause"));button("Tiếp",14+w,y,w,()->queue("resume"));
        button(confirmCancel?"Hủy?":"Hủy",16+w*2,y,w,()->{if(confirmCancel){queue("cancel");confirmCancel=false;}else confirmCancel=true;rebuild();});
        cameraButtons();actionButtons();
        extraPreviewButtons();
    }
    private void cameraButtons(){
        int x=layout.viewX()-layout.x(),y=layout.cameraY()-layout.y(),w=layout.viewWidth();
        button("Góc gốc",x,y,48,this::resetView);button("−",x+51,y,20,()->scene.zoom(-1));button("+",x+73,y,20,()->scene.zoom(1));
        int remaining=(w-100)/2;
        Button box=button("Khung",x+97,y,remaining,()->{scene.toggleBox();rebuild();});
        box.setTooltip(Tooltip.create(Component.literal("Khung giới hạn: "+(scene.box()?"đang hiện":"đang ẩn"))));
        Button grid=button("Lưới",x+100+remaining,y,remaining,()->{scene.toggleGrid();rebuild();});
        grid.setTooltip(Tooltip.create(Component.literal("Lưới sàn: "+(scene.grid()?"đang hiện":"đang ẩn"))));
    }
    private void actionButtons(){
        int x=layout.viewX()-layout.x(),y=layout.actionY()-layout.y(),w=(layout.viewWidth()-4)/2;
        switch(page){
            case LIBRARY->{button("Thiết lập vị trí",x,y,w,()->navigate(Page.CONFIGURE)).active=!selectedId.isEmpty();button("Lưu kiến trúc mới",x+w+4,y,w,()->navigate(Page.CAPTURE));}
            case CONFIGURE->{
                button("Xem trước",x,y,w,()->{try{JsonObject d=planData();previewDraft=d.toString();ready=false;queue("preview",d);}catch(Exception ex){status=ex.getMessage();}});
                startButton=button("Bắt đầu",x+w+4,y,w,()->{try{JsonObject d=planData();if(!previewDraft.equals(d.toString())){changed();status="Thông số đổi; hãy Xem trước lại.";return;}queue("start",d);ready=false;rebuild();}catch(Exception ex){status=ex.getMessage();}});startButton.active=ready&&transferStatus.isEmpty();
            }
            case CAPTURE->{
                button("Quét vùng chọn",x,y,w,()->{try{captureDraft="";updateVolume();queue("capture",captureData());}catch(Exception ex){status=ex.getMessage();}});
                button("Lưu mẫu",x+w+4,y,w,()->{try{
                    if(captureDraft.isEmpty()||!captureDraft.equals(captureData().toString())){status="Hãy quét đúng vùng chọn trước khi lưu.";return;}
                    JsonObject d=new JsonObject();d.addProperty("name",values.get("name"));queue("save",d);captureDraft="";
                }catch(Exception ex){status=ex.getMessage();}});
            }
            case NEARBY->{button("Tìm lại",x,y,w,this::scanNearby);button("Chọn vùng thủ công",x+w+4,y,w,()->navigate(Page.CAPTURE));}
            case MATERIALS->{button("Chọn mẫu khác",x,y,w,()->navigate(Page.LIBRARY));button("Thiết lập vị trí",x+w+4,y,w,()->navigate(Page.CONFIGURE)).active=!selectedId.isEmpty();}
        }
    }
    private void libraryPage(){
        label("Kiến trúc đã lưu",12,50);
        for(int i=0;i<rows.size();i++){
            JsonObject row=rows.get(i).getAsJsonObject();String id=row.get("id").getAsString(),name=row.get("name").getAsString();
            Button b=button((id.equals(selectedId)?"• ":"")+shorten(name,column-16),12,62+i*20,column,()->{JsonObject d=new JsonObject();d.addProperty("id",id);queue("select",d);});
            b.setTooltip(Tooltip.create(Component.literal(name+" • "+row.get("w")+" × "+row.get("h")+" × "+row.get("d"))));
        }
        button("<",12,145,29,()->requestPage(Math.max(0,pageNo-1)));label((pageNo+1)+" / "+pages,49,151);button(">",column-17,145,29,()->requestPage(Math.min(pages-1,pageNo+1)));
    }
    private void requestPage(int p){JsonObject d=new JsonObject();d.addProperty("page",p);queue("list",d);}
    private void coords(){int w=(column-8)/3;field("x","Mốc X",12,50,w,0xffff7a7a);field("y","Mốc Y",16+w,50,w,0xff83eb7f);field("z","Mốc Z",20+w*2,50,w,0xff73c4ff);}
    private void sizes(){int w=(column-8)/3;field("w","Rộng",12,84,w,0xffff7a7a);field("h","Cao",16+w,84,w,0xff83eb7f);field("d","Dài",20+w*2,84,w,0xff73c4ff);}
    private void configurePage(){
        coords();
        if(!building){sizes();button(new String[]{"Bắc","Đông","Nam","Tây"}[front],12,120,column/2-2,()->{front=(front+1)%4;changed();rebuild();});button(new String[]{"Hộp","Vòm","Bầu dục"}[shape],16+column/2,120,column/2-2,()->{shape=(shape+1)%3;changed();rebuild();});}
        else{label(shorten(selectedName,column),12,86);button("Hướng xây: "+turns*90+"°",12,102,column,()->{turns=(turns+1)%4;changed();if(sceneContext.equals("build"))scene.setRotation(turns);rebuild();});button("← Chọn mẫu khác",12,123,column,()->navigate(Page.LIBRARY));}
        Button mode=button(free?"Miễn phí • Sáng tạo":"Dùng vật liệu thật",12,143,column,()->{if(creative){free=!free;changed();rebuild();}});
        mode.setTooltip(Tooltip.create(Component.literal("Xây thật lấy vật liệu từ ba lô và rương liên kết. Chế độ miễn phí đặt khối thật, chỉ dùng khi chủ ở Sáng tạo.")));
    }
    private void capturePage(){coords();sizes();button("Tìm kiến trúc gần",12,120,column,this::scanNearby);label("Tên kiến trúc",12,143);edit("name",12,153,column,48);}
    private void scanNearby(){regions=new JsonArray();queue("scan");navigate(Page.NEARBY);}
    private void nearbyPage(){
        label("Các vùng gợi ý",12,51);
        for(int i=0;i<regions.size();i++){
            JsonObject r=regions.get(i).getAsJsonObject();String name="Vùng "+(i+1)+" • "+r.get("w")+"×"+r.get("h")+"×"+r.get("d");
            Button b=button(shorten(name,column-12),12,66+i*25,column,()->{
                for(String key:List.of("x","y","z","w","h","d"))values.put(key,r.get(key).getAsString());
                captureDraft="";sceneContext="";navigate(Page.CAPTURE);queue("capture",captureData());
            });b.setTooltip(Tooltip.create(Component.literal("Mốc: "+r.get("x")+", "+r.get("y")+", "+r.get("z"))));
        }
    }
    private void materialsPage(){
        label("Vật liệu xây dựng",12,51);
        button("<",12,172,29,()->{materialPage=Math.max(0,materialPage-1);rebuild();});
        label((materialPage+1)+" / "+materialTable.pages(),49,178);
        button(">",column-17,172,29,()->{materialPage=Math.min(materialTable.pages()-1,materialPage+1);rebuild();});
    }
    private void resetView(){
        NovaEntity n=nova();if(n==null)return;Direction facing=n.getDirection();
        double yaw=NovaGhostGeometry.cameraYaw(facing);
        if(!building){Direction travel=tunnelDirection(),right=travel.getClockWise();
            yaw=Math.atan2(facing.getStepX()*right.getStepX()+facing.getStepZ()*right.getStepZ(),
                    facing.getStepX()*travel.getStepX()+facing.getStepZ()*travel.getStepZ());}
        scene.defaultYaw(yaw);
    }
    private Direction tunnelDirection(){return new Direction[]{Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST}[front];}
    private void extraPreviewButtons(){
        int x=layout.viewX()-layout.x(),y=layout.toolsY()-layout.y(),w=(layout.viewWidth()-4)/2;
        Button ghost=button(NovaWorldGhost.active(robot)?"Khung ma: Bật":"Khung ma: Tắt",x,y,w,()->{
            if(NovaWorldGhost.active(robot))NovaWorldGhost.disable();
            else{NovaEntity n=nova();if(n==null)return;NovaWorldGhost.enable(n);syncGhost();status="Khung ma theo tọa độ đang chọn. Đóng bảng để quan sát ngoài thế giới.";}
            rebuild();
        });ghost.setTooltip(Tooltip.create(Component.literal("Hiện khung ngoài thế giới, kể cả khi đóng bảng. Chỉ đánh dấu vị trí; không đặt khối. Nhấn lại để tắt.")));
        Button inside=button(scene.interior()?"Nội thất: Bật":"Nội thất: Tắt",x+w+4,y,w,()->{scene.toggleInterior();rebuild();});
        inside.setTooltip(Tooltip.create(Component.literal("Ẩn mái/sàn và hai vách gần góc nhìn để thấy đồ bên trong. Y−/Y+ cắt từng tầng. Chỉ thay bản xem trước.")));
    }
    private void syncGhost(){
        if(!NovaWorldGhost.active(robot))return;
        try{
            boolean capture=page==Page.CAPTURE||page==Page.NEARBY||page==Page.MATERIALS&&sceneContext.equals("capture");
            if(building&&!capture&&selectedId.isEmpty()){NovaWorldGhost.update(robot,null);return;}
            BlockPos anchor=new BlockPos(integer("x"),integer("y"),integer("z"));
            if(Math.abs((long)anchor.getX())>29999900||Math.abs((long)anchor.getZ())>29999900||anchor.getY()< -2048||anchor.getY()>2048)throw new IllegalArgumentException();
            int w=capture||!building?integer("w"):(turns%2==0?blueprintWidth:blueprintDepth);
            int h=capture||!building?integer("h"):blueprintHeight;
            int d=capture||!building?integer("d"):(turns%2==0?blueprintDepth:blueprintWidth);
            NovaWorldGhost.update(robot,new NovaWorldGhost.Spec(anchor,w,h,d,building?null:tunnelDirection()));
        }catch(IllegalArgumentException ex){NovaWorldGhost.update(robot,null);}
    }
    private void changed(){ready=false;previewDraft="";if(startButton!=null)startButton.active=false;dirtyVolume=true;}
    private void updateVolume(){
        try{
            int w=integer("w"),h=integer("h"),d=integer("d");NovaBuildGeometry.volume(w,h,d);
            if(!building){scene.tunnel(w,h,d,NovaBuildGeometry.Shape.values()[shape]);sceneContext="tunnel";}
            else if(page==Page.CAPTURE){scene.volume(w,h,d);sceneContext="capture";receiver.clear();transferStatus="";}
        }catch(IllegalArgumentException ignored){}dirtyVolume=false;
    }
    private JsonObject captureData(){JsonObject d=new JsonObject();for(String key:List.of("x","y","z","w","h","d"))d.addProperty(key,integer(key));return d;}
    private JsonObject planData(){
        JsonObject d=new JsonObject();d.addProperty("mode",building?"build":"tunnel");for(String key:List.of("x","y","z"))d.addProperty(key,integer(key));d.addProperty("free",free);
        if(building){if(selectedId.isEmpty())throw new IllegalArgumentException("Chọn một kiến trúc trước.");d.addProperty("id",selectedId);d.addProperty("turns",turns);}
        else{for(String key:List.of("w","h","d"))d.addProperty(key,integer(key));d.addProperty("front",new String[]{"north","east","south","west"}[front]);d.addProperty("shape",new String[]{"BOX","ARCH","OVAL"}[shape]);}return d;
    }
    private int integer(String key){try{return Integer.parseInt(values.get(key));}catch(Exception ex){throw new IllegalArgumentException("Điền số nguyên vào ô "+key+".");}}
    private void field(String key,String text,int x,int y,int w,int color){labels.add(new Label(text,x,y,color));edit(key,x,y+11,w,10);}
    private void edit(String key,int x,int y,int w,int max){EditBox e=new EditBox(font,layout.x()+x,layout.y()+y,w,18,Component.literal(key));e.setMaxLength(max);e.setValue(values.getOrDefault(key,""));e.setResponder(v->{values.put(key,v);if(!key.equals("name"))changed();});addRenderableWidget(e);}
    private Button button(String text,int x,int y,int w,Runnable action){return addRenderableWidget(Button.builder(Component.literal(text),b->action.run()).bounds(layout.x()+x,layout.y()+y,Math.max(16,w),18).build());}
    private void label(String text,int x,int y){labels.add(new Label(text,x,y,0xffc7f1ff));}
    private String shorten(String text,int w){return font.width(text)<=w?text:font.plainSubstrByWidth(text,Math.max(8,w-10))+"…";}
    @Override public void tick(){
        if(nova()==null){onClose();return;}age++;
        if(dirtyVolume&&age%6==0)updateVolume();scene.tick();syncGhost();
        if(!opened){if(age==1||age%100==0)ClientPlayNetworking.send(new NovaConstructionService.Request(entityId,robot,new UUID(0,0),0,"open","{}"));return;}
        if(age%5==0&&!pending.isEmpty()){Pending p=pending.removeFirst();ClientPlayNetworking.send(new NovaConstructionService.Request(entityId,robot,token,++sequence,p.op,p.data.toString()));}
        else if(age%100==0&&pending.isEmpty())queue("status");
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean twice){if(e.button()==0&&layout.containsView(e.x(),e.y())){dragging=true;setFocused(null);return true;}return super.mouseClicked(e,twice);}
    @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){if(dragging&&e.button()==0){scene.orbit(dx,dy);return true;}return super.mouseDragged(e,dx,dy);}
    @Override public boolean mouseReleased(MouseButtonEvent e){if(dragging&&e.button()==0){dragging=false;return true;}return super.mouseReleased(e);}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){if(layout.containsView(x,y)){scene.zoom(vertical);return true;}return super.mouseScrolled(x,y,horizontal,vertical);}
    private static void frame(GuiGraphics g,int x,int y,int w,int h){g.fill(x,y,x+w,y+h,0xff2b6785);g.fill(x+1,y+1,x+w-1,y+h-1,0xff51cbe9);g.fill(x+2,y+2,x+w-2,y+h-2,0xff102a3c);}
    @Override public void render(GuiGraphics g,int mx,int my,float delta){
        renderTransparentBackground(g);int x=layout.x(),y=layout.y(),w=layout.width(),h=layout.height();
        g.fill(x-1,y-1,x+w+1,y+h+1,0xff51cbe9);g.fill(x,y,x+w,y+h,0xf217334a);g.fill(x,y,x+w,y+23,0xff1e4663);g.fill(x,y+22,x+w,y+23,0xff51cbe9);g.drawString(font,title,x+10,y+8,0xffd6f8ff,false);
        frame(g,x+6,y+26,layout.sidebar(),h-32);frame(g,layout.viewX(),layout.viewY(),layout.viewWidth(),layout.viewHeight());
        boolean show=page==Page.CAPTURE?sceneContext.equals("capture"):page==Page.CONFIGURE||page==Page.LIBRARY?sceneContext.equals(building?"build":"tunnel"):true;
        if(show)scene.render(g,layout.viewX()+4,layout.viewY()+4,layout.viewWidth()-8,layout.viewHeight()-8);
        String modelInfo=transferStatus.isEmpty()?(show?scene.status():"Chọn mẫu hoặc quét vùng để xem mô hình."):transferStatus;
        g.drawString(font,shorten(modelInfo,layout.viewWidth()-16),layout.viewX()+8,layout.viewY()+8,0xffd6f8ff,true);
        if(show&&scene.hasModel())g.drawString(font,shorten(scene.width()+" × "+scene.height()+" × "+scene.depth()+" | X đỏ • Y xanh lá • Z xanh dương",layout.viewWidth()-16),layout.viewX()+8,layout.viewY()+layout.viewHeight()-13,0xff9ee6ff,true);
        super.render(g,mx,my,delta);
        if(page==Page.MATERIALS)materialTable.render(g,font,x+12,y+64,column,materialPage,mx,my);
        for(Label l:labels)g.drawString(font,shorten(l.text,layout.sidebar()-l.x),x+l.x,y+l.y,l.color,false);
        String text=status+(total>0&&done>0?" ["+done+"/"+total+"]":"");var lines=font.split(Component.literal(text),layout.viewWidth()-6);
        for(int i=0;i<Math.min(2,lines.size());i++)g.drawString(font,lines.get(i),layout.viewX()+3,layout.statusY()+i*9,0xffffd995,false);
        if(mx>=layout.viewX()&&mx<layout.viewX()+layout.viewWidth()&&my>=layout.statusY())g.setTooltipForNextFrame(font,Component.literal(text),mx,my);
        else if(layout.containsView(mx,my)&&my<layout.viewY()+22)g.setTooltipForNextFrame(font,Component.literal(modelInfo),mx,my);
    }
    @Override public void removed(){receiver.clear();scene.clear();pending.clear();}
    @Override public void onClose(){syncGhost();if(opened)ClientPlayNetworking.send(new NovaConstructionService.Request(entityId,robot,token,++sequence,"close","{}"));super.onClose();}
    @Override public boolean isPauseScreen(){return false;}
}
