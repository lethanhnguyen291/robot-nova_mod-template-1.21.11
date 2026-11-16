package net.teogemini.nova.client.screen.preview;

import com.google.gson.*;
import java.util.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Inventory item models, localized names and exact quantities; no custom icon textures. */
public final class NovaMaterialTable {
    public static final int ROWS=4;
    private record Row(ItemStack icon,String name,int have,int need){}
    private final List<Row> rows=new ArrayList<>();
    public void set(JsonArray materials){
        rows.clear();
        for(JsonElement e:materials){
            JsonObject r=e.getAsJsonObject();ItemStack icon=ItemStack.EMPTY;
            Identifier id=r.has("item")?Identifier.tryParse(r.get("item").getAsString()):null;
            if(id!=null&&BuiltInRegistries.ITEM.containsKey(id))icon=new ItemStack(BuiltInRegistries.ITEM.getValue(id));
            rows.add(new Row(icon,icon.isEmpty()?r.get("name").getAsString():icon.getHoverName().getString(),r.get("have").getAsInt(),r.get("need").getAsInt()));
        }
    }
    public int pages(){return Math.max(1,(rows.size()+ROWS-1)/ROWS);}
    public void render(GuiGraphics g,Font font,int x,int y,int width,int page,int mx,int my){
        int iconEnd=x+24,quantityStart=x+width-44,bottom=y+17+ROWS*22;
        g.fill(x,y,x+width,bottom,0xff0a2130);g.renderOutline(x,y,width,bottom-y,0xff51cbe9);
        g.vLine(iconEnd,y,bottom-1,0xff2b6785);g.vLine(quantityStart,y,bottom-1,0xff2b6785);
        fit(g,font,"Khối",x+2,y+5,20,0xff9ee6ff);fit(g,font,"Tên",iconEnd+4,y+5,quantityStart-iconEnd-8,0xff9ee6ff);
        fit(g,font,"Có/cần",quantityStart+3,y+5,38,0xff9ee6ff);
        for(int i=0;i<ROWS;i++){
            int rowY=y+17+i*22,index=page*ROWS+i;g.hLine(x+1,x+width-2,rowY,0xff2b6785);
            if(index>=rows.size())continue;Row r=rows.get(index);int color=r.have>=r.need?0xff9becb9:0xffffcd8b;
            if(!r.icon.isEmpty())g.renderItem(r.icon,x+4,rowY+3);
            fit(g,font,r.name,iconEnd+4,rowY+8,quantityStart-iconEnd-8,color);
            String count=compact(r.have)+"/"+compact(r.need);
            float scale=Math.min(1,38F/Math.max(1,font.width(count)));
            g.pose().pushMatrix();g.pose().translate(quantityStart+3,rowY+8);g.pose().scale(scale,scale);
            g.drawString(font,count,0,0,color,false);g.pose().popMatrix();
            if(mx>=x&&mx<x+width&&my>=rowY&&my<rowY+22)
                g.setComponentTooltipForNextFrame(font,List.of(Component.literal(r.name),Component.literal("Đang có: "+r.have+" • Cần: "+r.need),Component.literal("Còn thiếu: "+Math.max(0,r.need-r.have))),mx,my);
        }
    }
    private static String compact(int value){
        if(value<1000)return Integer.toString(value);
        return value<10000?String.format(Locale.ROOT,"%.1fk",value/1000.0):(value/1000)+"k";
    }
    private static void fit(GuiGraphics g,Font font,String s,int x,int y,int width,int color){
        if(font.width(s)>width)s=font.plainSubstrByWidth(s,Math.max(0,width-font.width("…")))+"…";
        g.drawString(font,s,x,y,color,false);
    }
}
