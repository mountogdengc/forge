package forge.adventure.theme;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TransformDrawable;
import com.badlogic.gdx.math.Matrix4;

/** Scalable original material treatment; decoration never controls actor bounds. */
public final class ThemeDrawable extends BaseDrawable implements TransformDrawable {
    private final ResolvedTheme theme;
    private final TextureRegion pixel, artwork;
    private final String kind;
    private final Color fill, frame, highlight, jewel;
    public ThemeDrawable(ResolvedTheme theme,TextureRegion pixel,String kind,Drawable metrics) {
        this(theme,pixel,kind,metrics,null);
    }
    public ThemeDrawable(ResolvedTheme theme,TextureRegion pixel,String kind,Drawable metrics,TextureRegion artwork) {
        super(metrics==null?new BaseDrawable():metrics);
        this.theme=theme;this.pixel=pixel;this.kind=kind;this.artwork=artwork;
        String role=switch(kind){case "paper","dialog"->"surface.paper";case "primary"->"action.primary";case "selected"->"state.selected";case "button","tab","secondaryFocus"->"action.secondary";case "frame"->"border.frame";
            case "phase.activeEnabled","phase.inactiveEnabled"->kind;
            case "pressed","hover"->"state.hover";case "disabled"->"state.disabled";default->"surface.panel";};
        fill=Color.valueOf(theme.color(role));frame=Color.valueOf(theme.color((kind.equals("focused")||kind.equals("secondaryFocus"))?"state.focus":"border.frame"));
        highlight=Color.valueOf(theme.color("border.highlight"));jewel=Color.valueOf(theme.color("action.primary"));
    }
    @Override public void draw(Batch batch,float x,float y,float width,float height) {
        if(width<=0||height<=0||pixel==null||pixel.getTexture()==null)return;
        Color prior=new Color(batch.getColor());
        try {
            float alpha=prior.a;
            if(!kind.equals("minimap")&&!kind.equals("medallion")&&!kind.equals("chrome")&&!kind.equals("glass"))rect(batch,fill,x,y,width,height,alpha);
            if(artwork!=null){batch.setColor(1,1,1,alpha);
                if(kind.equals("glass"))batch.draw(artwork,x,y+height-Math.min(3,height),width,Math.min(3,height));
                else batch.draw(artwork,x,y,width,height);
            }
            boolean paper=kind.equals("paper")||kind.equals("dialog");
            if(theme.texture()>0&&(paper||theme.shape().equals("leaf"))) {
                Color grain=paper?Color.valueOf(theme.color("text.onPaper")):frame;
                // A fixed sparse pattern remains stable at different frame rates and viewport sizes.
                for(int i=0;i<18;i++){
                    float yy=y+height*((i*37%97)/97f), xx=x+width*((i*23%71)/100f);
                    rect(batch,grain,xx,yy,Math.min(width*.22f,width-(xx-x)),.3f,alpha*theme.texture()*.45f);
                }
            }
            boolean quiet=theme.ornament().equals("quiet");float border=quiet?.5f:1f;
            rect(batch,frame,x,y,width,border,alpha);rect(batch,frame,x,y+height-border,width,border,alpha);
            rect(batch,frame,x,y,border,height,alpha);rect(batch,frame,x+width-border,y,border,height,alpha);
            if(!quiet&&width>10&&height>10){
                rect(batch,highlight,x+1,y+height-2,width-2,.4f,alpha*.6f);
                rect(batch,highlight,x+1,y+1,.4f,height-2,alpha*.45f);
                if(theme.ornament().equals("full")&&width>35){
                    float s=theme.shape().equals("angular")?2.5f:1.7f;
                    diamond(batch,x+4,y+height-4,s,alpha);diamond(batch,x+width-4,y+4,s,alpha);
                }
            }
            if(kind.equals("glass")||kind.equals("chrome")&&!theme.ornament().equals("quiet")){
                float glassHeight=Math.min(3,height);
                float glassY=y+height-glassHeight;
                for(float xx=x;xx<x+width;xx+=12){
                    rect(batch,jewel,xx+1,glassY+.5f,Math.min(6,x+width-xx-1),Math.max(0,glassHeight-1),alpha*.7f);
                    rect(batch,frame,xx+7,glassY,1,glassHeight,alpha);
                }
            }
            if(kind.equals("medallion")&&width>8&&height>8){
                int sides=theme.shape().equals("angular")?4:theme.shape().equals("leaf")?6:12;
                float radius=Math.min(width,height)*.39f,cx=x+width/2,cy=y+height/2;
                for(int i=0;i<sides;i++){
                    double a=2*Math.PI*i/sides,b=2*Math.PI*(i+1)/sides;
                    line(batch,frame,cx+(float)Math.cos(a)*radius,cy+(float)Math.sin(a)*radius,
                            cx+(float)Math.cos(b)*radius,cy+(float)Math.sin(b)*radius,.8f,alpha);
                }
            }
        }finally{batch.setColor(prior);}
    }
    public boolean isPaper(){return kind.equals("paper")||kind.equals("dialog");}
    @Override public void draw(Batch batch,float x,float y,float originX,float originY,float width,float height,
                               float scaleX,float scaleY,float rotation){
        Matrix4 prior=new Matrix4(batch.getTransformMatrix());
        Matrix4 transform=new Matrix4(prior).translate(x+originX,y+originY,0).rotate(0,0,1,rotation)
                .scale(scaleX,scaleY,1).translate(-originX,-originY,0);
        batch.flush();batch.setTransformMatrix(transform);
        try{draw(batch,0,0,width,height);}finally{batch.flush();batch.setTransformMatrix(prior);}
    }
    private void diamond(Batch batch,float x,float y,float size,float alpha){
        batch.setColor(jewel.r,jewel.g,jewel.b,alpha);
        batch.draw(pixel,x-size/2,y-size/2,size/2,size/2,size,size,1,1,45);
    }
    private void line(Batch batch,Color color,float x,float y,float x2,float y2,float thickness,float alpha){
        batch.setColor(color.r,color.g,color.b,alpha);
        batch.draw(pixel,x,y,0,thickness/2,(float)Math.hypot(x2-x,y2-y),thickness,1,1,(float)Math.toDegrees(Math.atan2(y2-y,x2-x)));
    }
    private void rect(Batch batch,Color color,float x,float y,float width,float height,float alpha){
        if(width<=0||height<=0)return;
        batch.setColor(color.r,color.g,color.b,color.a*alpha);batch.draw(pixel,x,y,width,height);
    }
}
