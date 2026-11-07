package forge.adventure.scene;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.math.Vector2;
import forge.adventure.util.KeyBinding;

/** Native navigation for the two appearance tools, with dialog and list precedence. */
abstract class AppearanceBaseScene extends UIScene {
    protected AppearanceBaseScene(String path){super(path);}
    private boolean closeOpenChoice(Group group){
        for(Actor actor:group.getChildren()){
            if(actor instanceof SelectBox<?> box&&box.getScrollPane().hasParent()&&box.getList().isTouchable()){box.getScrollPane().hide();return true;}
            if(actor instanceof Group child&&closeOpenChoice(child))return true;
        }
        return false;
    }
    @Override public void selectActor(Selectable selection){
        super.selectActor(selection);
        if(selection==null)return;
        ScrollPane scroll=scrollPaneOfActor(selection.actor);
        if(scroll!=null&&scroll.getActor() instanceof Group content){
            scroll.validate();
            Vector2 point=selection.actor.localToAscendantCoordinates(content,new Vector2());
            scroll.setScrollY(content.getHeight()-point.y-selection.actor.getHeight()/2-scroll.getScrollHeight()/2);
        }
    }
    @Override public boolean keyPressed(int keycode){
        if(KeyBinding.Back.isPressed(keycode)){
            if(!dialogs.isEmpty()){removeDialog();return true;}
            if(closeOpenChoice(stage.getRoot()))return true;
            return back();
        }
        if(dialogs.isEmpty()&&stage.getKeyboardFocus() instanceof Slider slider){
            if(KeyBinding.Right.isPressed(keycode)){slider.setValue(slider.getValue()+slider.getStepSize());return true;}
            if(KeyBinding.Left.isPressed(keycode)){slider.setValue(slider.getValue()-slider.getStepSize());return true;}
        }
        return super.keyPressed(keycode);
    }
}
