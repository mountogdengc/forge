package forge.adventure.data;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static forge.adventure.util.AdventureQuestController.ObjectiveTypes.MapFlag;
import static forge.adventure.util.AdventureQuestController.ObjectiveTypes.QuestFlag;
import static forge.adventure.util.AdventureQuestController.QuestStatus.*;

/** State transitions shared by quest activation and save restoration, without UI effects. */
final class AdventureQuestProgression {
    private AdventureQuestProgression() {
    }

    static List<AdventureQuestStage> activateNextStages(AdventureQuestData quest,
                                                       Predicate<AdventureQuestStage> reconcileState) {
        List<AdventureQuestStage> activated = new ArrayList<>();
        for (AdventureQuestStage stage : quest.stages) {
            quest.failed |= stage.getStatus() == FAILED;
        }
        if (quest.failed) {
            quest.completed = false;
            return activated;
        }

        boolean changed;
        do {
            changed = false;
            for (AdventureQuestStage stage : quest.stages) {
                boolean newlyActivated = false;
                if (stage.getStatus() == INACTIVE) {
                    stage.checkPrerequisites(quest.getCompletedStageIDs());
                    if (stage.getStatus() == ACTIVE) {
                        activated.add(stage);
                        newlyActivated = true;
                        changed = true;
                    }
                }
                if (stage.getStatus() == ACTIVE
                        && (newlyActivated || stage.objective == QuestFlag || stage.objective == MapFlag)
                        && reconcileState.test(stage)) {
                    changed = true;
                }
            }
            // A newly completed flag can unlock an earlier array entry. Stop once no
            // stage changes; cyclic or unmet prerequisites remain inactive.
        } while (changed);

        // These aggregate fields are transient and must also be restored from saved
        // stage statuses. Leave empty, dialogue-only quests on their existing lifecycle.
        if (quest.stages.length > 0) {
            quest.completed = true;
            for (AdventureQuestStage stage : quest.stages) {
                quest.completed &= stage.getStatus() == COMPLETE;
            }
        }
        return activated;
    }
}
