package forge.adventure.scene;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import forge.Forge;
import forge.adventure.theme.*;
import java.util.*;

/** Small native controls shared by the two Adventure appearance screens. */
final class AdventureAppearanceUi {
    private AdventureAppearanceUi(){ }
    record Choice(String id,String label){@Override public String toString(){return label;}}
    static String text(String key,String fallback){return Forge.getLocalizer().getMessageorUseDefault(key,fallback);}
    static Label label(String value,Skin skin){Label label=new Label(value,skin);label.setWrap(true);return label;}
    static TextButton button(String caption,Skin skin,Runnable action,boolean primary){
        TextButton button=new TextButton(caption,skin,primary&&skin.has("theme.primary",TextButton.TextButtonStyle.class)?"theme.primary":"default");
        button.getLabel().setWrap(true);
        button.addListener(new ChangeListener(){@Override public void changed(ChangeEvent event,Actor actor){
            if(!button.isChecked())return;
            button.setChecked(false);
            action.run();
        }});
        return button;
    }
    static Array<Choice> choices(AdventureThemeRuntime runtime,boolean campaign){
        ThemeCatalog catalog=runtime.discover();Array<Choice> result=new Array<>();
        if(campaign)result.add(new Choice("campaign",text("lblThemeCampaignAppearance","Campaign appearance")));
        Map<String,Integer> counts=new HashMap<>();for(ThemeManifest m:catalog.themes())counts.merge(m.name(),1,Integer::sum);
        for(ThemeManifest manifest:catalog.themes()){
            try{ThemeResolver.resolve(catalog,manifest.id());String label=manifest.name();
                if(counts.get(label)>1)label+=" ("+manifest.id().substring(Math.max(0,manifest.id().length()-8))+")";
                result.add(new Choice(manifest.id(),label));
            }catch(IllegalArgumentException e){runtime.reportCompatibility(e.getMessage());}
        }
        catalog.diagnostics().forEach(d->runtime.reportCompatibility(d.toString()));
        return result;
    }
    static String name(AdventureThemeRuntime runtime,String id){
        if(id==null)return text("lblThemeAutomatic","Automatic");
        if(id.equals("campaign"))return text("lblThemeCampaignAppearance","Campaign appearance");
        for(ThemeManifest m:runtime.discover().themes())if(m.id().equals(id))return m.name();
        return id;
    }
}
