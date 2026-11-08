package forge.adventure.character;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;

/** The existing arrow/keyboard selector with its value represented by a centered sprite. */
public final class CharacterImageSelector extends CharacterOptionSelector {
    private Image image;
    public CharacterImageSelector() {
        getLabel().setVisible(false);
        image = new Image(); image.setName("appearancePreview"); image.setScaling(Scaling.fit);
        addActor(image);
        addCaptureListener(new InputListener() {
            @Override public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (getStage() != null) getStage().setKeyboardFocus(CharacterImageSelector.this);
                return false;
            }
        });
    }
    public void setRegion(TextureRegion region) { image.setDrawable(new TextureRegionDrawable(region)); }
    public void clearRegion() { image.setDrawable(null); }
    @Override public void layout() {
        super.layout();
        if (image != null) {
            float size = Math.max(0, Math.min(32, Math.min(getHeight(), getWidth() - 2 * getHeight())));
            image.setBounds((getWidth() - size) / 2, (getHeight() - size) / 2, size, size);
        }
    }
}
