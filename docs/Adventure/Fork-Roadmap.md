# Adventure fork roadmap

## Direction

Develop a compelling single-player card-battling adventure using Forge's shared foundation: an explorable campaign with dependable progression, a coherent story, useful rewards, and reasons to build and improve decks.

The inherited Adventure campaign and implementation are candidates for reuse, substantial rework, or complete replacement. Its story, setting, quests, maps, progression systems, interface, and campaign structure are all open to change. Judge each component by its value to the chosen experience. The progression issue review establishes defects in the current implementation; it does not establish the campaign's overall quality or that replacement is preferable. Make those decisions through assessment and playtesting.

Adventure is the fork's product scope. Multiplayer and standalone Duel modes are outside that scope. Adventure's card battles still depend on the shared rules engine, computer opponent, card data, and relevant platform support; maintain those systems for the Adventure experience.

The fork follows its own campaign and interface goals. Submitting changes upstream is not planned, and upstream acceptance is not a design constraint. Selectively import useful upstream fixes when they benefit Adventure and respect the fork's chosen progression design and save policy.

Plan an Adventure-centered application flow: start or continue a campaign, manage saves, and adjust settings. Existing standalone-mode code may remain as inherited infrastructure while shared dependencies are understood. Removing code or modules requires a concrete Adventure benefit and a dependency review; the immediate work remains campaign development and reliability.

This is the fork's working direction and implementation sequence. Proposed milestones are not claims that the features already exist or have passed playtesting. Verification results belong in [Progression validation](Progression-Validation.md).

## Baseline and current work

The progression foundation branch starts from upstream commit [`4fcb76673693cb21fd0e31e50b72774fc02053a1`](https://github.com/Card-Forge/forge/commit/4fcb76673693cb21fd0e31e50b72774fc02053a1). The parent POM identifies this baseline as `2.0.16-SNAPSHOT`. Record the actual tested commit and packaged version separately; a snapshot version alone does not identify the code or resource files.

| Area | Evidence at the reviewed baseline | Disposition |
| --- | --- | --- |
| Early castle completion and captive rescue, [#12311](https://github.com/Card-Forge/forge/issues/12311) | A player reports being unable to complete the green-castle rescue after clearing it before receiving the quest. Flag-based progression needs to account for facts already recorded in the save. | Current foundation scope: reconcile eligible quest stages with existing quest flags and correct relevant encounter data. Verify normal order, early exploration, and existing saves. |
| Library of Varsil boss, [#7709](https://github.com/Card-Forge/forge/issues/7709) | The boss-removal dialog requires a particular story stage. A maintainer says the Library quest must be active before the fight; the encounter's own dialog calls it optional. | Current foundation scope: record a genuine victory and remove the defeated boss even outside that story stage. Preserve appropriate story text and reward behavior. |
| The Drunken Plea, [#12306](https://github.com/Card-Forge/forge/issues/12306) | The return-to-town stage lacks a prerequisite on clearing the target location. | Current foundation scope: enforce the intended stage order in each affected quest definition. Do not claim that editing a definition repairs an already serialized quest. |
| Flower Fetching, [#9141](https://github.com/Card-Forge/forge/issues/9141) | The report describes flowers collected before quest acceptance. The baseline already calls `resetDeletedObjectsRecursive` when initializing eligible target locations. | Current foundation scope: recognize an existing local flag in the selected cave's saved root/floor records after the objective becomes eligible. Retest the actual cave and existing saves before declaring the whole report resolved; do not add another reset without evidence. |
| Quest-list crash, [#12296](https://github.com/Card-Forge/forge/issues/12296) | A Windows snapshot report describes a crash when opening the quest list, without a stack trace or attached reproduction save. | Obtain a failing save and exception trace, then isolate the cause. Keep this separate from the flag and prerequisite fixes until evidence connects them. |
| Quests disappear after an upgrade, [#12453](https://github.com/Card-Forge/forge/issues/12453) | [PR #12463](https://github.com/Card-Forge/forge/pull/12463) already repaired the save-deserialization regression, and is present in the baseline. | Preserve that compatibility fix. Loading an intact old save and reconstructing a quest list already overwritten with empty data are different recovery problems. |
| New Game+ quest availability, [#9286](https://github.com/Card-Forge/forge/issues/9286) | [PR #11886](https://github.com/Card-Forge/forge/pull/11886) already clears the character flags that prevented the main quest being offered again. | Preserve and check the existing reset behavior when adding persistent completion or defeat tracking. |
| On the Hunt save/load, [#5440](https://github.com/Card-Forge/forge/issues/5440) | [PR #7516](https://github.com/Card-Forge/forge/pull/7516) already added recovery for the documented save-before-first-movement case. The code is in the baseline. | Preserve and regression-check the existing repair. An open issue alone does not establish that a new fix is required. |
| Malformed map dialogs, [#11670](https://github.com/Card-Forge/forge/issues/11670) | Follow-up testing with libGDX rejected far fewer strings than the original strict-JSON scan. [PR #12128](https://github.com/Card-Forge/forge/pull/12128) already repaired the Demon Tower portal and Skep reward dialogs. | Keep the existing repairs. Validate remaining data with Forge's actual parser and dialog behavior before assigning campaign impact. |
| One-time boss quests, [PR #12260](https://github.com/Card-Forge/forge/pull/12260) | Open upstream work adds one-time quests, required enemies, saved completion/defeat flags, and editor controls. Its author reports no manual in-game validation and excludes old-save migration/duplicate cleanup. | Evaluate as an optional candidate against the chosen Adventure design. Inspect the current PR revision and compatibility with this fork; do not count an open PR as integrated functionality. |

### Progression rules for this fork

- Completing a real world action early must not make the campaign impossible. Where a saved fact satisfies a newly eligible objective, recognize it after the objective's prerequisites are met.
- Missing flags must not be mistaken for completed objectives. Respect each objective's comparison or threshold and the scope of the recorded state.
- Record a defeated boss independently of whether the player currently has a particular quest. Story dialogue can depend on quest state; the fact of victory must remain consistent.
- Completing an objective, showing its dialogue, and granting its rewards must not repeat simply because the player reloads, revisits a location, or triggers another update.
- Existing saves are part of the test surface for retained systems and compatibility claims. Distinguish code that reads existing saved facts from a data change that only affects newly created quest instances. A replacement campaign needs an explicit save policy; entering it with an old character is a design choice, not an automatic commitment.
- Reset behavior must be deliberate. A location reset, a repeatable quest, and New Game+ have different purposes; one must not silently substitute for another.

## Priority 0: Assess Adventure and validate the reusable foundation

Before committing to a campaign direction, assess a representative portion of the existing Adventure: the opening, a town, exploration and travel, a dungeon and boss, side quests, rewards, and save/resume behavior. Examine story coherence, player choice, location variety, navigation, repetition, deck growth, difficulty pacing, and the cost of authoring new content. Assess quest execution, map generation, encounters, UI, and persistence separately from the story they currently support.

Produce a short decision record describing the desired experience, evidence from the assessment, and which components to retain, rework, replace, or omit. Compare targeted improvement with a replacement campaign or Adventure layer. Choose a small playable example that will test the proposed direction. No completed quality assessment or replacement decision is implied by this roadmap.

The existing change series is intentionally bounded: quest-state reconciliation, affected boss dialogs, and the Drunken Plea dependency. It repairs the current implementation and provides regression coverage for systems we may reuse. Review common resources and plane overrides together so an equivalent encounter does not behave differently solely because one copy was missed. Continuing every inherited quest or repairing all upstream content is not a prerequisite for assessing or designing a replacement.

The content work covers the Drunken Plea definitions in Shandalar, Shandalar Old Border, Realm of Legends, Crystal Kingdoms, Innistrad, and the Shandalar Spanish override. The Library boss correction covers the common map and the Old Border override, including their difficulty variants. Preserve the story-specific defeat branches and keep fallback branches mutually exclusive; map-dialog evaluation can process more than one matching root. These changes do not add a migration for previously serialized Drunken Plea stages.

Before treating the series as ready for a player run:

1. Build the affected Java modules and load the changed quest/map data with the game's parsing behavior.
2. Exercise the focused regressions needed to prove the state transitions, including already-set flags and dependent stages. Follow [CONTRIBUTING.md](../../CONTRIBUTING.md): add tests for meaningful future regressions, not redundant wiring checks.
3. Run the applicable manual cases in [Progression validation](Progression-Validation.md), preserving the original saves used for comparison.
4. Record remaining failures and untested cases. A passing build does not establish that an existing campaign is recoverable.

Track the two remaining evidence gaps: Flower Fetching against the existing recursive reset and the quest-list crash without a reproduction. Prioritize them when they affect retained systems, selected content, or a compatibility promise we choose to make. A failure in a retained quest system or UI can block release of the chosen experience; an investigation into content we may retire does not block campaign design.

Evaluate PR #12260 as an optional integration if its one-time quest approach fits the chosen design. Its documented lack of old-save migration is a separate design problem. If adopting it for established campaigns, decide how to handle previously completed quests and already accepted duplicates. Preserve intended repeatable quests and verify applicable New Game+ resets.

## Priority 1: Prototype the chosen design and complete a chapter

Upstream [#9871](https://github.com/Card-Forge/forge/issues/9871) identifies the unfinished main story and proposes a more open campaign, with current story locations becoming side quests and stronger foreshadowing for the end boss. Use that discussion as design input; it is not a finished campaign specification or an adopted fork-wide rewrite.

After the assessment, build a small playable example of the chosen Adventure design, then develop it into one complete chapter. It may use retained locations, redesigned content, entirely new content, or a combination. Demonstrate exploration, a meaningful quest, card battles, useful rewards, and saving and resuming. The chapter should give the player a clear initial problem, multiple useful places to explore, a recognizable turning point, and a satisfying conclusion. Play through that experience before committing to a larger production scope.

| Design area | Deliverable for the chapter | Acceptance question |
| --- | --- | --- |
| Application flow | An Adventure-focused launch and menu path for starting, continuing, saves, and settings | Can the player enter and resume Adventure directly without navigating unrelated modes? |
| Story | A short chapter outline, quest relationships, NPC motivations, and a defined ending | Can a player explain what they are pursuing, why it matters, and when this chapter is complete? |
| Exploration | A documented route through the chapter plus supported alternate orders and optional encounters | Can players explore early without losing essential objectives or being forced to guess the intended order? |
| Dungeons | Distinct encounters, environmental identity, useful navigation, and appropriate exits | Does each dungeon offer a different challenge or discovery, and can the player leave and resume safely? |
| Deck growth | A reward plan covering cards, items, resources, and access to further opportunities | Do rewards create useful deck decisions without requiring one specific starting deck? |
| Pacing | A playthrough record that identifies repetitive travel, unclear objectives, and difficulty spikes | Does the chapter remain engaging between battles, and do setbacks leave a reasonable path forward? |
| Continuity | An explicit policy for fresh campaigns, supported existing characters, and retained saves | Are supported entry paths understandable and completable, and are new-campaign requirements clear? |

Decide tutorial handling, the relationship between main and side quests, and the chapter's connection to the larger campaign in that outline. A later overarching story can build on the chapter's proven structure.

## Priority 2: Strengthen exploration and rewards

Once the chapter works end to end, improve the systems that make further adventuring worthwhile:

- Give one-time encounters, repeatable jobs, and long quest chains explicit completion and reward policies.
- Review dungeon and boss rewards against the choices they add to a player's deck or equipment, including rewards that remain useful for different starting colors.
- Use towns, optional encounters, and discoveries to provide understandable leads and alternatives when the player cannot yet defeat a boss.
- Improve quest descriptions and navigation so they reflect actual saved progress and identify the next meaningful action.
- Add more chapters and locations using the same completion, save compatibility, and playthrough criteria.

Specific new mechanics belong in their own proposals with player-facing benefits and acceptance criteria. Reuse or replace Adventure systems according to the chosen experience, implementation cost, and evidence from the playable chapter. Keep changes focused enough to evaluate their effect on play and persistence.

## Priority 3: Make content safer to author

Invest in content tools when they remove a demonstrated source of Adventure defects or materially improve authoring the chosen content. Extend the existing editor and formats where useful; replace them when their limitations justify doing so.

The initial targets are invalid prerequisites, missing targets, unreachable stages, inconsistent one-time rewards, and differences between common maps and plane overrides. Validators should understand libGDX's accepted syntax and separately check dialog semantics such as unnamed choices; strict JSON compliance alone is not a reliable runtime-failure count.

Provide authors with a short quest checklist and reproducible save scenarios. Expand automated coverage around those failure modes as needed. The goal is to make a new chapter easier to finish and maintain, with less reliance on a particular quest order remembered by its author.

## Fork maintenance and selective upstream imports

Keep Adventure changes in focused branches and commits. Separate general engine repairs, content corrections, and campaign additions so each can be reviewed or carried forward independently.

Before taking an upstream change, inspect its current status, dependencies, changed save behavior, and overlap with the fork. Re-run the relevant progression cases after integrating changes to quest execution, map persistence, rewards, or Adventure resources. Continue taking useful card-rule, AI, performance, and platform fixes even when they are not labeled Adventure.

Keep development branches and pull requests within this fork for review and tracking. Upstream submissions are not a planned activity. Multiplayer and standalone Duel features do not receive development milestones or determine Adventure feature acceptance. Preserve shared functionality that Adventure uses, and record substantial AI assistance in this fork's change descriptions for provenance.

## Selected references

darklurker11. (2026, October 20). *Main quest softlock bug* [Issue #12311]. GitHub. https://github.com/Card-Forge/forge/issues/12311

fserron. (2025, May 27). *Myr Superion doesn't dissapear when defeated* [Issue #7709]. GitHub. https://github.com/Card-Forge/forge/issues/7709

Nerdslayer. (2026, October 14). *Add engine and editor support for one-time Adventure quests* [Pull request #12260]. GitHub. https://github.com/Card-Forge/forge/pull/12260

Simisays. (2026, February 22). *Main story rework* [Issue #9871]. GitHub. https://github.com/Card-Forge/forge/issues/9871
