package forge.adventure.scene;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.github.tommyettinger.textra.TextraButton;
import forge.Forge;
import forge.adventure.campaign.CampaignDefinition;
import forge.adventure.campaign.CampaignManager;
import forge.adventure.campaign.CampaignRegistry;
import forge.adventure.util.Controls;

import java.util.Collections;
import java.util.List;

/** Chooses content for a new run without changing the currently active game. */
public final class CampaignSelectionScene extends UIScene {
    private static CampaignSelectionScene object;
    private final SelectBox<String> campaignPicker = Controls.newComboBox();
    private final Label description = Controls.newLabel("");
    private final Label errors = Controls.newLabel("");
    private final TextraButton next;
    private List<CampaignDefinition> campaigns = Collections.emptyList();

    private CampaignSelectionScene() {
        super(Forge.isLandscapeMode() ? "ui/campaign_select.json" : "ui/campaign_select_portrait.json");
        Window content = ui.findActor("content");
        content.pad(12);
        Table body = new Table();
        body.top().left();
        // The Window supplies the frame; a second framed ScrollPane consumed
        // the fixed-width body's inset and clipped the first letters.
        float width = content.getWidth() - 36;
        body.add(Controls.newLabel(text("lblChooseCampaign", "Choose a campaign"))).left().padBottom(8).row();
        body.add(campaignPicker).width(width).padBottom(10).row();
        description.setWrap(true);
        errors.setWrap(true);
        body.add(description).width(width).left().padBottom(10).row();
        body.add(errors).width(width).left().row();
        ScrollPane.ScrollPaneStyle scrollStyle = new ScrollPane.ScrollPaneStyle(
                Controls.getSkin().get(ScrollPane.ScrollPaneStyle.class));
        scrollStyle.background = null;
        ScrollPane scroll = new ScrollPane(body, scrollStyle);
        scroll.setScrollingDisabled(true, false);
        scroll.setFadeScrollBars(false);
        content.add(scroll).grow();
        next = ui.findActor("next");
        next.setText(text("lblCreateCharacter", "Create character"));
        ui.onButtonPress("next", this::choose);
        ui.onButtonPress("back", this::back);
        campaignPicker.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                updateDescription();
            }
        });
        addToSelectable(campaignPicker);
    }

    public static CampaignSelectionScene instance() {
        if (object == null)
            object = new CampaignSelectionScene();
        return object;
    }

    public static void resetForCampaign() {
        Forge.retireAdventureResource(object);
        object = null;
    }

    @Override
    public void enter() {
        CampaignRegistry registry = CampaignRegistry.discover();
        campaigns = registry.getCampaigns();
        Array<String> titles = new Array<>();
        for (CampaignDefinition campaign : campaigns)
            titles.add(campaign.getTitle());
        campaignPicker.setItems(titles);
        if (!campaigns.isEmpty()) {
            CampaignDefinition active = CampaignManager.current();
            for (int i = 0; active != null && i < campaigns.size(); i++) {
                if (campaigns.get(i).getId().equals(active.getId())) {
                    campaignPicker.setSelectedIndex(i);
                    break;
                }
            }
        }
        next.setDisabled(campaigns.isEmpty());
        errors.setText(registry.getErrors().isEmpty() ? "" :
                text("lblUnavailableCampaigns", "Some campaigns could not be loaded:")
                        + "\n" + String.join("\n", registry.getErrors()));
        updateDescription();
        unselectActors();
        super.enter();
    }

    private void updateDescription() {
        if (campaigns.isEmpty() || campaignPicker.getSelectedIndex() < 0) {
            description.setText(text("lblNoCampaigns", "No playable campaigns were found."));
            return;
        }
        CampaignDefinition campaign = campaigns.get(campaignPicker.getSelectedIndex());
        description.setText(campaign.getDescription());
    }

    private boolean choose() {
        if (campaigns.isEmpty() || campaignPicker.getSelectedIndex() < 0)
            return false;
        try {
            Forge.switchScene(NewGameScene.forCampaign(campaigns.get(campaignPicker.getSelectedIndex())));
        } catch (RuntimeException e) {
            showDialog(createGenericDialog(text("lblError", "Error"), e.getMessage(),
                    text("lblOK", "OK"), null, this::removeDialog, null, false, "", true));
        }
        return true;
    }

    @Override
    public boolean back() {
        Forge.switchScene(StartScene.instance());
        return true;
    }

    private static String text(String key, String fallback) {
        return Forge.getLocalizer().getMessageorUseDefault(key, fallback);
    }
}
