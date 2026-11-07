# Krezk: A Bell Against the Mists

David requested Krezk as the next full quest area and delegated the design and
execution. The following scope is the builder's choice, using the existing
Forge pixel maps, card combat, dialogue, journal and save system.

## Playable chapter

After finishing A Light in Vallaki, Ireena offers a separate journey west. She
stays at the sanctuary she chose; the player takes news to Krezk. Acceptance
explicitly issues quest 8103 once. Earlier visits explain this prerequisite.

Four scenes form a complete chapter: the existing Krezk gate, a new village
hub, the blessed pool and the Abbey of Saint Markovia. All have independent
overworld entrances and clear southern exits. Story access is enforced through
dialogue; visiting a scene early cannot grant progress or start a locked battle.
The village offers explicit free recovery, with no automatic admission/healing.

At the gate, drive away the mist wolves, then ask the captain for admission.
Dmitri explains that the Abbey's silent bell and failing spring have frightened
the village. Defeat the pool's unnatural watcher, deliberately collect clear
water, and take it to the Abbot. He admits his mechanical sentinel has become
dangerous. Disable it, speak to Vasilka about her own wishes, and decide whether
Krezk should keep a community watch or accept the Abbey's ward under public
oversight. Both outcomes protect the town, respect Vasilka's agency, and change
repeat conversations. This is an original adaptation, not a claim that the
published adventure has these exact encounters or endings.

Return to Dmitri to settle the accord and select two white Thraben Inspectors
or two black Doomed Dissidents. Completion and the selected two cards commit
together. No automatic deck edit, repeated reward, timer, purchase or hidden
permanent penalty. Full resurrection, a bride demand, Ireena's disappearance,
the winery chapter, relic systems and final Ravenloft combat remain outside scope.

## Data contract

ID `fork.curse-of-strahd`, content 0.5.0, new run required. Preserve frozen 0.4.0
and all earlier builds/profiles. Quest 8103 is storyQuest/isTemplate/autoTrack.
Acceptance flag `cosKrezkQuestAccepted`; ordered stage flags:

1. `cosKrezkRoadCleared` — defeat Krezk Mist Wolves at the gate.
2. `cosKrezkAdmitted` — speak to the captain after victory.
3. `cosKrezkHeardDmitri` — accept the village investigation.
4. `cosKrezkSpringCleared` — defeat the Spring Watcher at the pool.
5. `cosKrezkWaterDrawn` — deliberately draw water after victory.
6. `cosKrezkAbbotHeard` — question the Abbot with the water.
7. `cosKrezkSentinelCleared` — defeat the Abbey Sentinel.
8. `cosKrezkAccord` — choose the future watch with Vasilka.
9. `cosChapterThreeComplete` — return to Dmitri and take one reward.

Character flag `cosKrezkChoice` is 1 community watch or 2 supervised Abbey ward.
Every missing prerequisite has exactly one non-actionable guidance root; the
runtime activates all matching roots, so fallback roots must be exclusive.
Defeated enemies reconcile removal on revisits and save loads. Victory alone
does not collect water, make the accord or complete the chapter.

Existing 34 world sites become 37 with Krezk Village (24,43), Blessed Pool of
Krezk (15,34), Abbey of Saint Markovia (34,29). Krezk gate stays (28,55).
`story/landmarks.json` retains all site metadata and marks these four `questArea`.
The passive arrival generator skips those entries; the Krezk generator owns
their scene geometry, dialogue and POIs. Other 25 arrivals and original eight
maps remain byte-identical except the embedded Ireena handoff in Saint Andral.
All local actors have a clear two-tile approach; all 37 global anchors have a
connected 3×3 route. Original pixel assets only; no image replacement or engine
feature is needed. Three authored 40-card decks use known resolvable cards.

## Verification and delivery

Observe missing chapter contract failures, then check ordered stages, early and
inconsistent flag states, exclusive dialogue, deliberate acceptance/collection,
both accord/reward choices, repeat safety, version rejection and resource routes.
Run affected Java and Python checks through the shared runner. Verify production
class bytes match the reused client; compile native fixtures separately.

Hidden native checks cover prior chapters, Krezk early visits, loss/rematch,
post-victory save, interrupted collection/accord/reward, both choices, repeat
visits, earlier checkpoint rewind, exact world data and fresh-process Continue.
Skip the already-verified unchanged 25-site tour in this chapter-specific run.
Raise only the fixture's time budget for the longer journey. Validate both
reward colors; played-match balance remains manual acceptance.

Work in this existing dirty checkout to preserve the uncommitted campaign and
theme dependencies. No commits, merges, publishing, existing profile/package
overwrites or visible game launches. Cap native JVM at 1 GB/two processors,
1920×1080/30 FPS. One fresh whole-change reviewer at the end; repair important
findings. Package 0.5.0 with exact current resource/client/source hashes, no test
saves, map preview and numbered manual launch/route/report-back instructions.

Setting names are grounded in David's map and the
[official adventure contents](https://www.dndbeyond.com/sources/dnd/cos).
Original scenes/dialogue use no reproduced published prose, maps or stat blocks.
