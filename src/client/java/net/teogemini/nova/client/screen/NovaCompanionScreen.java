package net.teogemini.nova.client.screen;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.teogemini.nova.entity.NovaEntity;
import net.teogemini.nova.network.NovaCompanionCommands;

public final class NovaCompanionScreen extends Screen {
    private final NovaCompanionCommands.Page page;
    private int scroll, contentHeight;
    public NovaCompanionScreen(NovaCompanionCommands.Page page) {
        super(Component.literal(page.name()+" • Kỷ niệm")); this.page=page;
    }
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(NovaCompanionCommands.Page.TYPE,(page,context)->{
            var client=context.client();
            if (!(client.screen instanceof NovaControlScreen || client.screen instanceof NovaCompanionScreen)
                    || client.level==null || client.player==null
                    || !net.teogemini.nova.network.NovaRodControl.holdingRod(client.player)
                    || !(client.level.getEntity(page.entityId()) instanceof NovaEntity nova)
                    || !nova.getUUID().equals(page.uuid()) || !nova.isAlive() || !nova.isOwnedBy(client.player)
                    || client.player.distanceToSqr(nova)>64) return;
            client.setScreen(new NovaCompanionScreen(page));
        });
    }
    private NovaEntity nova() {
        if (minecraft.level==null || minecraft.player==null
                || !net.teogemini.nova.network.NovaRodControl.holdingRod(minecraft.player)) return null;
        return minecraft.level.getEntity(page.entityId()) instanceof NovaEntity nova && nova.getUUID().equals(page.uuid())
                && nova.isAlive() && nova.isOwnedBy(minecraft.player) && minecraft.player.distanceToSqr(nova)<=64 ? nova : null;
    }
    @Override protected void init() {
        int x=width/2-160;
        addRenderableWidget(Button.builder(Component.literal("Tính cách: "+page.personality()), b->send(1))
                .bounds(x,height-54,158,20).build());
        addRenderableWidget(Button.builder(Component.literal("Tự đổi đồ: "+(page.autoTools()?"Bật":"Tắt")), b->send(2))
                .bounds(x+162,height-54,158,20).build());
        addRenderableWidget(Button.builder(Component.literal("Quay lại"), b->{
            var nova=nova(); if (nova==null) onClose(); else minecraft.setScreen(new NovaControlScreen(nova));
        }).bounds(x,height-30,320,20).build());
    }
    private void send(int action) {
        if (nova()!=null) ClientPlayNetworking.send(new NovaCompanionCommands.Request(page.entityId(),page.uuid(),action));
    }
    @Override public void tick() { if (nova()==null) onClose(); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float delta) {
        super.render(g,mouseX,mouseY,delta);
        int x=width/2-160, top=55, bottom=height-64;
        g.drawCenteredString(font,title,width/2,14,0xFF80E5FF);
        g.drawCenteredString(font,"Gắn bó: "+page.bond()+"/100 • Rương phụ: "+page.deposits()+" • Nút: "+page.buttons(),width/2,32,0xFFFFFFFF);
        List<String> text=new ArrayList<>();
        text.add(page.totals()); text.add("");
        text.add("KỶ NIỆM GẦN ĐÂY");
        if (page.entries().isEmpty()) text.add("Chúng mình sẽ có nhiều kỷ niệm cùng nhau.");
        for (String entry:page.entries()) text.add("• "+entry);
        text.add(""); text.add("Công cụ: chỉ tự đổi sang món dự phòng cùng loại khi bạn bật cho phép.");
        text.add("Gậy điều khiển chọn NOVA → Shift + bấm rương/nút để thêm hoặc bỏ quyền sử dụng (tối đa 9 mỗi loại).");
        text.add("Tính cách thay đổi cử chỉ và thói quen; không giảm kỹ năng làm việc.");
        List<FormattedCharSequence> lines=new ArrayList<>();
        for (String line:text) lines.addAll(font.split(Component.literal(line.isEmpty()?" ":line),312));
        contentHeight=lines.size()*13;
        scroll=Mth.clamp(scroll,0,Math.max(0,contentHeight-(bottom-top)));
        g.fill(x-6,top-5,x+326,bottom+4,0xBB071E2B);
        g.enableScissor(x,top,x+320,bottom);
        for (int i=0;i<lines.size();i++) g.drawString(font,lines.get(i),x+4,top+i*13-scroll,0xFFE0F7FF,false);
        g.disableScissor();
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {
        scroll=Mth.clamp(scroll-(int)(vertical*26),0,Math.max(0,contentHeight-(height-119)));
        return true;
    }
}
