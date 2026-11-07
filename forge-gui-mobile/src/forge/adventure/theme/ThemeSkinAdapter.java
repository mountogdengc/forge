package forge.adventure.theme;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import java.nio.file.Path;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;
import java.util.*;
import java.util.function.BiConsumer;

/** Owns copied standard styles and new resources; legacy resources are borrowed. */
public final class ThemeSkinAdapter extends Skin {
    private static final Set<String> STANDARD=Set.of("default","background","dialog","paper","gold","menu","translucent","nobg",
            "default-horizontal","default-vertical","leftarrow","rightarrow","roundhint","item_frame","item_frame_static");
    private final Skin legacy;
    private final ResolvedTheme theme;
    private final TextureRegion pixel;
    private final java.util.List<Disposable> owned=new ArrayList<>();
    private final Map<String,TextureRegion> artwork=new HashMap<>();
    private final Map<BitmapFont,BitmapFont> fontCopies=new IdentityHashMap<>();
    private boolean disposed;
    private final java.util.List<String> warnings=new ArrayList<>();
    public java.util.List<String> warnings(){return java.util.List.copyOf(warnings);}

    public static String compatibilityIssue(Skin legacy){
        return legacy.has("default",Label.LabelStyle.class)&&legacy.has("default",TextButton.TextButtonStyle.class)&&legacy.has("default",BitmapFont.class)
                ?"":"This campaign does not provide standard default label, button, and font styles; keeping its original appearance.";
    }
    public static Skin adapt(Skin legacy,ResolvedTheme theme){
        if(theme==null||!compatibilityIssue(legacy).isEmpty())return legacy;
        return new ThemeSkinAdapter(legacy,theme);
    }
    private ThemeSkinAdapter(Skin legacy,ResolvedTheme theme){
        this.legacy=legacy;this.theme=theme;
        try {
            TextureRegion available=legacy.optional("white-pixel",TextureRegion.class);
            if(available==null){
                Pixmap solid=new Pixmap(1,1,Pixmap.Format.RGBA8888);
                try{solid.setColor(Color.WHITE);solid.fill();Texture texture=new Texture(solid);owned.add(texture);available=new TextureRegion(texture);}
                finally{solid.dispose();}
            }
            pixel=available;
            add("white-pixel",pixel,TextureRegion.class);
            add("white-pixel",new TextureRegionDrawable(pixel),Drawable.class);
            ObjectMap<String,BitmapFont> fonts=legacy.getAll(BitmapFont.class);
            if(fonts!=null)for(var e:fonts)add(e.key,copyFont(e.value),BitmapFont.class);
            loadFontRole("body","default");loadFontRole("heading","theme.heading");
            for(String slot:ThemeManifest.ASSET_SLOTS)add("theme."+slot,drawable(slot,null),Drawable.class);
            add("theme.chrome",drawable("chrome",null),Drawable.class);
            copyStyles();
            TextButton.TextButtonStyle primary=new TextButton.TextButtonStyle(get(TextButton.TextButtonStyle.class));
            primary.up=drawable("primary",primary.up);primary.fontColor=color("action.onPrimary");
            add("theme.primary",primary,TextButton.TextButtonStyle.class);
            TextButton.TextButtonStyle tab=new TextButton.TextButtonStyle(get(TextButton.TextButtonStyle.class));
            tab.up=drawable("tab",tab.up);add("theme.tab",tab,TextButton.TextButtonStyle.class);
        }catch(RuntimeException failure){dispose();throw failure;}
    }
    private void loadFontRole(String role,String key){
        String source=theme.fonts().get(role);
        if(source==null||source.equals("legacy:default")){if(!super.has(key,BitmapFont.class))add(key,getFont("default"),BitmapFont.class);return;}
        BitmapFont font;
        if(source.toLowerCase(Locale.ROOT).endsWith(".fnt")){
            BitmapFont.BitmapFontData data=ThemeResolver.bitmapFontData(Path.of(source),Path.of(source).getParent());
            font=new BitmapFont(data,(TextureRegion)null,false);
        }
        else{
            FreeTypeFontGenerator generator=new FreeTypeFontGenerator(new FileHandle(source));
            try{FreeTypeFontGenerator.FreeTypeFontParameter p=new FreeTypeFontGenerator.FreeTypeFontParameter();
                p.size=Math.max(8,Math.round(legacy.getFont("default").getLineHeight()));
                StringBuilder chars=new StringBuilder();
                for(BitmapFont.Glyph[] page:legacy.getFont("default").getData().glyphs)if(page!=null)for(BitmapFont.Glyph glyph:page)if(glyph!=null)chars.append((char)glyph.id);
                p.characters=chars.toString();font=generator.generateFont(p);
            }finally{generator.dispose();}
        }
        if(compatibleBodyFont(font,legacy.getFont("default"))!=font){
            font.dispose();warnings.add("The "+role+" font lacks existing localized glyphs; using the original font.");
            if(!key.equals("default"))add(key,getFont("default"),BitmapFont.class);
            return;
        }
        owned.add(font);add(key,font,BitmapFont.class);
    }
    static BitmapFont compatibleBodyFont(BitmapFont candidate,BitmapFont reference){
        for(BitmapFont.Glyph[] page:reference.getData().glyphs)if(page!=null)for(BitmapFont.Glyph glyph:page)
            if(glyph!=null&&candidate.getData().getGlyph((char)glyph.id)==null)return reference;
        return candidate;
    }
    private BitmapFont copyFont(BitmapFont original){
        if(original==null)return null;
        BitmapFont existing=fontCopies.get(original);if(existing!=null)return existing;
        BitmapFont.BitmapFontData from=original.getData(),data=new BitmapFont.BitmapFontData();
        // All public font data is copied; glyphs may be remapped by BitmapFont's constructor.
        data.name=from.name;data.fontFile=from.fontFile;data.imagePaths=from.imagePaths==null?null:from.imagePaths.clone();data.flipped=from.flipped;
        data.padTop=from.padTop;data.padRight=from.padRight;data.padBottom=from.padBottom;data.padLeft=from.padLeft;
        data.lineHeight=from.lineHeight;data.capHeight=from.capHeight;data.ascent=from.ascent;data.descent=from.descent;data.down=from.down;
        data.blankLineScale=from.blankLineScale;data.scaleX=from.scaleX;data.scaleY=from.scaleY;data.markupEnabled=from.markupEnabled;
        data.cursorX=from.cursorX;data.spaceXadvance=from.spaceXadvance;data.xHeight=from.xHeight;
        data.breakChars=from.breakChars==null?null:from.breakChars.clone();data.xChars=from.xChars==null?null:from.xChars.clone();data.capChars=from.capChars==null?null:from.capChars.clone();
        Map<BitmapFont.Glyph,BitmapFont.Glyph> glyphCopies=new IdentityHashMap<>();
        for(int page=0;page<from.glyphs.length;page++)if(from.glyphs[page]!=null){
            data.glyphs[page]=new BitmapFont.Glyph[from.glyphs[page].length];
            for(int i=0;i<from.glyphs[page].length;i++)if(from.glyphs[page][i]!=null)data.glyphs[page][i]=glyphCopies.computeIfAbsent(from.glyphs[page][i],ThemeSkinAdapter::copyGlyph);
        }
        data.missingGlyph=from.missingGlyph==null?null:glyphCopies.computeIfAbsent(from.missingGlyph,ThemeSkinAdapter::copyGlyph);
        // Glyphs already have texture coordinates; avoid loading/remapping packed atlas glyphs again.
        BitmapFont copy=new BitmapFont(data,original.getRegions(),original.usesIntegerPositions()){
            @Override protected void load(BitmapFont.BitmapFontData ignored){ }
        };
        copy.setOwnsTexture(false);owned.add(copy);fontCopies.put(original,copy);return copy;
    }
    private static BitmapFont.Glyph copyGlyph(BitmapFont.Glyph g){
        BitmapFont.Glyph c=new BitmapFont.Glyph();c.id=g.id;c.srcX=g.srcX;c.srcY=g.srcY;c.width=g.width;c.height=g.height;
        c.u=g.u;c.v=g.v;c.u2=g.u2;c.v2=g.v2;c.xoffset=g.xoffset;c.yoffset=g.yoffset;c.xadvance=g.xadvance;c.page=g.page;c.fixedWidth=g.fixedWidth;
        if(g.kerning!=null){c.kerning=g.kerning.clone();for(int i=0;i<c.kerning.length;i++)if(c.kerning[i]!=null)c.kerning[i]=c.kerning[i].clone();}
        return c;
    }
    private Color color(String role){return Color.valueOf(theme.color(role));}
    private Drawable drawable(String kind,Drawable metrics){
        String slot=switch(kind){case "primary"->"buttonPrimary";case "pressed"->"buttonPressed";case "hover"->"buttonHover";case "disabled"->"buttonDisabled";case "selected"->"selection";default->kind;};
        TextureRegion image=artwork.get(slot);
        if(image==null&&theme.assets().containsKey(slot)){
            String source=theme.assets().get(slot).toString();
            if(source.toLowerCase(Locale.ROOT).endsWith(".atlas")){
                TextureAtlas atlas=new TextureAtlas(ThemeResolver.atlasData(Path.of(source),Path.of(source).getParent()));owned.add(atlas);
                image=atlas.findRegion(slot);if(image==null&&!atlas.getRegions().isEmpty())image=atlas.getRegions().first();
            }else{Texture texture=new Texture(new FileHandle(source));owned.add(texture);image=new TextureRegion(texture);}
            artwork.put(slot,image);
        }
        String state=switch(kind){case "buttonPrimary"->"primary";case "buttonPressed"->"pressed";case "buttonHover"->"hover";
            case "buttonDisabled"->"disabled";case "selection"->"selected";default->kind;};
        return new ThemeDrawable(theme,pixel,state,metrics,image);
    }
    private void button(Button.ButtonStyle s){
        s.up=drawable("button",s.up);s.down=drawable("pressed",s.down==null?s.up:s.down);
        s.over=drawable("hover",s.over==null?s.up:s.over);s.checked=drawable("selected",s.checked==null?s.up:s.checked);
        s.disabled=drawable("disabled",s.disabled==null?s.up:s.disabled);s.focused=drawable("selected",s.focused==null?s.up:s.focused);
    }
    private void textButton(TextButton.TextButtonStyle s){
        button(s);s.font=getFont("default");s.fontColor=color("action.onSecondary");s.downFontColor=color("text.primary");
        s.overFontColor=color("text.primary");s.checkedFontColor=color("state.onSelected");s.disabledFontColor=color("state.onDisabled");
        s.focusedFontColor=color("state.onSelected");s.checkedFocusedFontColor=color("state.onSelected");
    }
    private <T> void each(Class<T> type,BiConsumer<String,T> action){
        ObjectMap<String,T> styles=legacy.getAll(type);
        if(styles!=null)for(var e:styles)if(STANDARD.contains(e.key))action.accept(e.key,e.value);
    }
    private void copyStyles(){
        each(Label.LabelStyle.class,(name,old)->{Label.LabelStyle s=new Label.LabelStyle(old);s.font=getFont("default");
            s.fontColor=color(name.equals("dialog")||old.background!=null?"text.onPaper":"text.primary");if(old.background!=null)s.background=drawable("paper",old.background);add(name,s);});
        each(Button.ButtonStyle.class,(name,old)->{Button.ButtonStyle s=new Button.ButtonStyle(old);button(s);add(name,s);});
        each(TextButton.TextButtonStyle.class,(name,old)->{TextButton.TextButtonStyle s=new TextButton.TextButtonStyle(old);textButton(s);add(name,s);});
        each(ImageButton.ImageButtonStyle.class,(name,old)->{ImageButton.ImageButtonStyle s=new ImageButton.ImageButtonStyle(old);button(s);add(name,s);});
        each(ImageTextButton.ImageTextButtonStyle.class,(name,old)->{ImageTextButton.ImageTextButtonStyle s=new ImageTextButton.ImageTextButtonStyle(old);textButton(s);add(name,s);});
        each(CheckBox.CheckBoxStyle.class,(name,old)->{CheckBox.CheckBoxStyle s=new CheckBox.CheckBoxStyle(old);s.font=getFont("default");s.fontColor=color("text.primary");s.disabledFontColor=color("state.onDisabled");s.checkboxOver=drawable("hover",old.checkboxOver);add(name,s);});
        each(Window.WindowStyle.class,(name,old)->{Window.WindowStyle s=new Window.WindowStyle(old);s.titleFont=getFont("theme.heading");s.titleFontColor=color("text.onPaper");s.background=drawable("dialog",old.background);add(name,s);});
        each(ScrollPane.ScrollPaneStyle.class,(name,old)->{ScrollPane.ScrollPaneStyle s=new ScrollPane.ScrollPaneStyle(old);if(!name.equals("nobg"))s.background=drawable(name.equals("paper")||name.equals("gold")?"paper":"panel",old.background);add(name,s);});
        each(List.ListStyle.class,(name,old)->{List.ListStyle s=new List.ListStyle(old);s.font=getFont("default");s.fontColorSelected=color("state.onSelected");s.fontColorUnselected=color("text.onPaper");s.selection=drawable("selected",old.selection);s.background=drawable("paper",old.background);add(name,s);});
        each(SelectBox.SelectBoxStyle.class,(name,old)->{SelectBox.SelectBoxStyle s=new SelectBox.SelectBoxStyle(old);s.font=getFont("default");s.fontColor=color("action.onSecondary");s.overFontColor=color("action.onSecondary");s.disabledFontColor=color("state.onDisabled");s.background=drawable("button",old.background);s.backgroundOver=drawable("secondaryFocus",old.backgroundOver);s.backgroundOpen=drawable("secondaryFocus",old.backgroundOpen);s.backgroundDisabled=drawable("disabled",old.backgroundDisabled);s.listStyle=get(List.ListStyle.class);s.scrollStyle=get(ScrollPane.ScrollPaneStyle.class);add(name,s);});
        each(TextField.TextFieldStyle.class,(name,old)->{TextField.TextFieldStyle s=new TextField.TextFieldStyle(old);s.font=getFont("default");s.fontColor=color("text.primary");s.disabledFontColor=color("state.onDisabled");s.messageFontColor=color("text.secondary");s.background=drawable("panel",old.background);s.focusedBackground=drawable("focused",old.focusedBackground);s.selection=drawable("selected",old.selection);add(name,s);});
        each(ProgressBar.ProgressBarStyle.class,(name,old)->{ProgressBar.ProgressBarStyle s=new ProgressBar.ProgressBarStyle(old);s.background=drawable("panel",old.background);if(old.knobBefore!=null)s.knobBefore=drawable("selected",old.knobBefore);add(name,s);});
        each(Slider.SliderStyle.class,(name,old)->{Slider.SliderStyle s=new Slider.SliderStyle(old);s.background=drawable("panel",old.background);s.knob=drawable("selected",old.knob);add(name,s);});
        each(Touchpad.TouchpadStyle.class,(name,old)->{Touchpad.TouchpadStyle s=new Touchpad.TouchpadStyle(old);s.background=drawable("medallion",old.background);add(name,s);});
    }
    @Override public <T> T get(String name,Class<T> type){T own=super.optional(name,type);return own!=null?own:legacy.get(name,type);}
    @Override public <T> T optional(String name,Class<T> type){T own=super.optional(name,type);return own!=null?own:legacy.optional(name,type);}
    @Override public boolean has(String name,Class type){return super.has(name,type)||legacy.has(name,type);}
    @Override public <T> ObjectMap<String,T> getAll(Class<T> type){ObjectMap<String,T> result=new ObjectMap<>();ObjectMap<String,T> parent=legacy.getAll(type);if(parent!=null)result.putAll(parent);ObjectMap<String,T> own=super.getAll(type);if(own!=null)result.putAll(own);return result;}
    @Override public Drawable getDrawable(String name){Drawable own=super.optional(name,Drawable.class);return own!=null?own:legacy.getDrawable(name);}
    public java.util.List<BitmapFont> ownedFonts(){return owned.stream().filter(BitmapFont.class::isInstance).map(BitmapFont.class::cast).toList();}
    public boolean ownsFont(BitmapFont font){return owned.contains(font);}
    @Override public void dispose(){if(disposed)return;disposed=true;for(Disposable resource:owned)resource.dispose();owned.clear();}
}
