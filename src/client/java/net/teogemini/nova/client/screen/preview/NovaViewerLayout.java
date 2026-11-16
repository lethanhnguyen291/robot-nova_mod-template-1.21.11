package net.teogemini.nova.client.screen.preview;

/** Layout in Minecraft GUI pixels, independent of the desktop resolution. */
public record NovaViewerLayout(int x,int y,int width,int height,int sidebar) {
    public static NovaViewerLayout fit(int screenWidth,int screenHeight){
        int w=Math.min(900,screenWidth-8),h=Math.min(520,screenHeight-8);
        return new NovaViewerLayout((screenWidth-w)/2,(screenHeight-h)/2,w,h,Math.max(132,Math.min(196,(int)(w*.29))));
    }
    public int viewX(){return x+sidebar+14;}public int viewY(){return y+26;}
    public int viewWidth(){return width-sidebar-20;}public int viewHeight(){return height-113;}
    public int toolsY(){return y+height-84;}
    public int cameraY(){return y+height-63;}public int actionY(){return y+height-42;}public int statusY(){return y+height-20;}
    public boolean containsView(double px,double py){return px>=viewX()&&px<viewX()+viewWidth()&&py>=viewY()&&py<viewY()+viewHeight();}
}
