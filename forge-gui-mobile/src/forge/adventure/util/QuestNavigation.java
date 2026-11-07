package forge.adventure.util;

import com.badlogic.gdx.math.Vector2;
import forge.adventure.data.AdventureQuestData;
import forge.adventure.data.AdventureQuestStage;
import forge.adventure.pointofintrest.PointOfInterest;

import java.util.function.Function;

/** Navigation hints are independent of the conditions that award quest progress. */
public final class QuestNavigation {
    private QuestNavigation() { }

    public static AdventureQuestData trackedQuest(Iterable<AdventureQuestData> quests) {
        for (AdventureQuestData quest : quests) {
            if (quest.isTracked && !quest.completed && !quest.failed && !quest.getActiveStages().isEmpty())
                return quest;
        }
        return null;
    }

    public static PointOfInterest destination(AdventureQuestData quest, Vector2 position) {
        AdventureQuestData template = quest.storyQuest
                ? AdventureQuestController.instance().getQuestTemplate(quest.getID()) : null;
        return destination(quest, template, name -> {
            PointOfInterest poi = Current.world().findPointsOfInterest(name);
            return poi != null && poi.getActive() ? poi : null;
        }, position);
    }

    static PointOfInterest destination(AdventureQuestData quest, AdventureQuestData template,
                                     Function<String, PointOfInterest> locations, Vector2 position) {
        if (quest.completed || quest.failed) return null;
        for (AdventureQuestStage stage : quest.getActiveStages()) {
            String target = stage.navigationTarget;
            // Saved quests predate navigation metadata. Read the current story template by
            // stable quest/stage IDs without regenerating the quest or touching its progress.
            if ((target == null || target.isBlank()) && template != null && template.getID() == quest.getID()) {
                for (AdventureQuestStage authored : template.stages) {
                    if (authored.id == stage.id) {
                        target = authored.navigationTarget;
                        break;
                    }
                }
            }
            if (target != null && !target.isBlank()) return locations.apply(target);
        }
        return quest.getClosestValidPOI(position);
    }
}
