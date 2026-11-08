package forge.adventure.character;

import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import forge.adventure.util.Controls;
import forge.adventure.util.Selector;

/** Compact creator rows must honor their allocated bounds even with large theme button minimums. */
public class CharacterOptionSelector extends Selector {
    private final Label leftGlyph;
    private final Label rightGlyph;

    public CharacterOptionSelector() {
        TextButton.TextButtonStyle button = Controls.getSkin().get(TextButton.TextButtonStyle.class);
        ImageButton.ImageButtonStyle arrow = new ImageButton.ImageButtonStyle();
        arrow.up = button.up; arrow.down = button.down; arrow.over = button.over;
        arrow.focused = button.focused; arrow.disabled = button.disabled;
        getLeftArrow().setStyle(arrow); getRightArrow().setStyle(arrow);
        leftGlyph = glyph("<", button); rightGlyph = glyph(">", button);
        getLeftArrow().addActor(leftGlyph); getRightArrow().addActor(rightGlyph);
    }
    private static Label glyph(String value, TextButton.TextButtonStyle style) {
        Label label = Controls.newLabel(value); label.setAlignment(Align.center);
        if (style.fontColor != null) label.setColor(style.fontColor);
        label.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
        return label;
    }
    @Override public void layout() {
        float h = getHeight();
        getLeftArrow().setBounds(0, 0, h, h);
        getRightArrow().setBounds(getWidth() - h, 0, h, h);
        getLabel().setBounds(h, 0, Math.max(0, getWidth() - 2 * h), h);
        if (leftGlyph != null) leftGlyph.setBounds(0, 0, h, h);
        if (rightGlyph != null) rightGlyph.setBounds(0, 0, h, h);
    }
}
