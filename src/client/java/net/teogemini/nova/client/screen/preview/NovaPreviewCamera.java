package net.teogemini.nova.client.screen.preview;

/** Orthographic orbit camera. Rotating the camera never changes the construction rotation. */
public final class NovaPreviewCamera {
    public record Point(float x,float y,float depth){}
    public record Transform(float cx,float cy,float cz,float rightX,float rightZ,float upX,float upY,float upZ,
                            float depthX,float depthY,float depthZ,float screenX,float screenY){
        public float facing(float x,float y,float z){return depthX*x+depthY*y+depthZ*z;}
        public Point project(float x,float y,float z){
            x-=cx;y-=cy;z-=cz;
            return new Point(screenX+rightX*x+rightZ*z,screenY+upX*x+upY*y+upZ*z,depthX*x+depthY*y+depthZ*z);
        }
    }
    private double yaw=Math.toRadians(35),pitch=Math.toRadians(25),zoom=1;
    private int revision;
    private double defaultYaw=yaw;
    public void defaultYaw(double angle){defaultYaw=angle;reset();}
    public void reset(){yaw=defaultYaw;pitch=Math.toRadians(25);zoom=1;revision++;}
    public void orbit(double dx,double dy){yaw=Math.IEEEremainder(yaw+dx*.012,Math.PI*2);pitch=Math.IEEEremainder(pitch+dy*.009,Math.PI*2);revision++;}
    public void zoom(double amount){zoom=Math.max(.35,Math.min(3.5,zoom*Math.pow(1.15,amount)));revision++;}
    public int revision(){return revision;}
    public float facing(float x,float y,float z){return (float)(Math.cos(pitch)*(Math.sin(yaw)*x+Math.cos(yaw)*z)+Math.sin(pitch)*y);}
    public Point project(float x,float y,float z,int w,int h,int d,int vw,int vh){
        return transform(w,h,d,vw,vh).project(x,y,z);
    }
    public Transform transform(int w,int h,int d,int vw,int vh){
        double sx=Math.cos(yaw),sz=Math.sin(yaw),cp=Math.cos(pitch),sp=Math.sin(pitch);
        double spanX=Math.abs(sx)*w+Math.abs(sz)*d,spanY=Math.abs(cp)*h+Math.abs(sp)*(Math.abs(sz)*w+Math.abs(sx)*d);
        double scale=Math.min(vw/Math.max(1,spanX),vh/Math.max(1,spanY))*.80*zoom;
        return new Transform(w*.5F,h*.5F,d*.5F,(float)(sx*scale),(float)(-sz*scale),
                (float)(sp*sz*scale),(float)(-cp*scale),(float)(sp*sx*scale),
                (float)(cp*sz),(float)sp,(float)(cp*sx),vw*.5F,vh*.5F);
    }
}
