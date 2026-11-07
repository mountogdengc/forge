package forge.adventure.util;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.*;

public class MapDialogQuestIssueTest {
    @Test public void ordinaryMapActorsIssueTheirConfiguredQuestWithoutAQuestOfferListener() {
        MapDialog dialog = new MapDialog("[]", null, 60);
        List<String> issued = new ArrayList<>();
        dialog.issueQuest("8102", issued::add);
        assertEquals(issued, List.of("8102"));
        assertEquals(dialog.questAccepted, "8102");
    }

    @Test public void registeredQuestOfferListenersRetainOwnershipOfTheirPrebuiltQuest() {
        MapDialog dialog = new MapDialog("[]", null, 60);
        List<String> issued = new ArrayList<>();
        dialog.addQuestAcceptedListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                issued.add("prebuilt:" + dialog.questAccepted);
            }
        });
        dialog.issueQuest("8102", id -> fail("Default issuer would add the prebuilt quest twice"));
        assertEquals(issued, List.of("prebuilt:8102"));
    }
}
