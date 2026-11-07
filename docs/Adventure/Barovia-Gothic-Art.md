# Barovia gothic presentation

The approved direction is a haunted, lived-in opening area: rain-dark stone,
mud, dead vegetation, slate and timber buildings, candlelight and cold mist.
The implementation uses existing libGDX/Tiled maps and texture atlases. It adds
no engine hooks, gameplay systems, dependencies or global theme overrides.

Implementation plan:

1. Record current interactions and effective collision cells before editing.
2. Pack generated environment and NPC art into native-size isolated atlases.
3. Reskin the village, church, Old Svalich Road and Tser Pool, with shared
   surroundings on the authored overworld. Preserve all map/actor identities.
4. Add low-opacity animated mist and lantern light with standard Tiled tile
   animations. Keep NPCs, signs, the journal and entry landing readable.
5. Validate all 37 routes and embedded dialogues, compare gameplay baselines,
   exercise hidden native chapters and Continue, and inspect engine screenshots.

The existing working tree contains unrelated and prerequisite work. This task
edits presentation resources and its authoring/verification tools in place;
it does not commit, publish, alter Windows permissions or change save identity.

## Implemented scope

- Village of Barovia: dead grass, muddy approaches, wet cobbles, lit and boarded
  timber cottages, small manor fronts, a stone well, iron lamps and bare trees.
- Village Church: worn stone floor, a pointed-arch chapel frontage occupying
  the existing four-by-four blocked area, iron enclosure and grave markers.
- Old Svalich Road and Tser Pool: muddy paths, bare trees/dark firs, grave markers,
  benches and lamps. Existing camp structures, shore, wolves and encounters stay.
- Ismark, Ireena (village and camp), Donavich, Madam Eva and Strahd have distinct
  full-body sprites and matching sprite avatars. Atlases retain 16-by-16 frames,
  paired Left/Right poses and the engine's existing directional fallbacks.
  This does not add painted dialogue portraits or replace enemy animations.
- Overworld: generated materials replace the mist-field, forest, hills, water and road
  textures; generated bare/dark trees retain the existing sprite catalog and
  region dimensions/order. Mountain, snow and POI markers retain their
  existing assets. Authored terrain/collision rows and all 37 POIs are unchanged.
  Tser Pool uses the same cold, murky water material as the overworld lakes.
- Low-opacity mist uses sixteen spatial samples of a 128-by-32 ribbon, each
  with eight animation frames. Lamps use eight-frame amber glow tiles. Tiled's
  existing animation clock drives both. These layers carry no collision objects;
  actor neighborhoods and the southern entry landing are excluded from mist.
  Fog stays below the HUD. There is no weather system or new light renderer.

The runtime environment atlas is 256-by-256 with nearest texture filtering.
NPC pages are 64-by-16. The generated source sheets and exact prompts live in
`tools/adventure/art/barovia`; their packaging is deterministic and needs Pillow.
These source sheets are not referenced at runtime. All common-path additions
are namespaced to Strahd; generic terrain, hero, enemy and UI assets are untouched.

After regenerating base maps and embedding dialogues, apply the presentation:

```powershell
python tools/adventure/create_barovia_gothic.py --build
python tools/adventure/create_strahd_maps.py --check
python -m unittest discover -s tools/adventure -p test_barovia_gothic.py -v
```

The presentation pass can also run repeatedly on already dressed maps. It
updates its derived layers without changing objects or duplicating layers.
`barovia_gothic_baseline.json` records the pre-art interaction/collision hashes
of all 37 maps. Update that evidence deliberately if future gameplay changes
are authorized; do not silently bless geometry differences as an art change.

## Automated evidence

November 7, 2026 candidate checks use the active source checkout. Full results
are retained in `.build-cache/barovia-gothic` and the shared runner's `forge`
result folders. The final evidence manifest is `verification.json` in that
candidate directory.

- Current-source Maven build and Checkstyle passed, with **54 selected tests**
  and no failures, errors or skips. Coverage includes the three chapters,
  authored world maps, campaign resources, quest progression, old-save navigation
  hints, dialogue quest issuance and the town-entry grace period. Queue result:
  `3f3ef744d2854d5680ae888f63823b24`.
- Full hidden native chapter run and fresh-process Continue passed. Both Doru
  outcomes, Vallaki choices, Krezk accords, interrupted choices, once-only rewards,
  losses/rematches, checkpoint restoration, campaign isolation, encounter cleanup
  and four real Krezk exits passed. Authored world arrays/POIs retain hash
  `1284314643842881109` in new, completed and continued states. Queue result:
  `224a46be4ec34dfa9b713437a65d1c8d`.
- That full chapter run preceded the final water-only palette adjustment.
  No gameplay code or geometry changed between those phases.
- **Final assets:** all **42 authoring tests** passed with no skips; collision
  cells and interaction records match the pre-art baseline on all **37 maps**.
  The 33 maps outside the four-scene slice remain byte-identical to their
  task-start copies. The final file hashes are recorded in `verification.json`.
- **Final hidden rendering probe:** the pre-art 0.5.0 save restores exact position,
  life, gold and selected deck. Village, church, road and camp all render with
  advancing eight-frame mist animations. Camp water uses the new material.
  Original free town healing from 17 to 20 life is preserved; subsequent scene
  visits preserve life, gold and deck. Final screenshots were inspected in
  `.build-cache/barovia-gothic/final-native-3`. Queue result:
  `77974337392443d2863c4487e1296cab`.
- Two earlier final-probe attempts remain as failed evidence: a test-only static
  reference compile error (`3ed0ac2c5fb24df58521985cdaaf536f`), then an incorrect
  assertion that rejected the original town healing
  (`ee01c94687654ab89aa1cac9da0c2d49`). The probe was corrected; gameplay was not
  changed to satisfy it. Their compile/runtime logs remain in `final-native`
  and `final-native-2` respectively.
- The initial restricted authoring run could not create/delete some disposable
  test folders and Git fixtures. The identical full suite passed with the
  required filesystem access (41 tests at that point; queue result
  `2b4d1032f93e4f4fbfc2a23b7ce71b47`). No Windows permissions were changed.
- Independent read-only reviews found no actionable defects in the presentation
  or subsequent water/probe/launcher changes. Static previews and first-pass
  native screenshots were inspected; an initial repeated-stripe fog appearance
  was corrected before the passing native chapter run.

Native fixtures are hidden/unfocused, isolated, audio off, 1920-by-1080/30 FPS,
with a 1-GB heap and two reported CPUs. Duel results are explicitly supplied;
POI entry is direct. These are integration/rendering checks, not played battles
or proof of physical-navigation feel. Baseline JDK/LWJGL native-access and JNI
warnings remain in their logs; they did not fail the completed chapter run.

## Save and integration boundaries

Campaign identity/version remain `fork.curse-of-strahd` / `0.5.0`. This art pass
adds no save fields and requires no new campaign run. Existing saves retain
their stored minimap bitmap; the main world renderer reads the updated atlases.
The campaign's authored geography, quests, dialogues, enemy data, POI roster,
entry-grace implementation and navigation implementation remain byte-identical
to the working tree at the beginning of this task. All other local maps retain
their pre-art presentation. No Git staging/commits/publishing were performed.

## Visual and physical acceptance

This is a current-source candidate, not an update to any frozen portable build.
Prerequisites: the compiled current reactor, its prepared
`forge-gui-mobile-dev/target/smoke-classpath.txt`, repository resources and a
Windows JDK. The launcher defaults to the JDK used for automated verification,
`C:/Program Files/Java/jdk-26`; use `-JavaHome` to select another installed JDK.
From `F:/Projects-PCGames/forge`, launch explicitly:

```powershell
pwsh -NoProfile -File tools/adventure/start_barovia_gothic.ps1
```

For a fresh checkout, prepare classes and the dependency file first (the current
candidate already has both). Use a Windows JDK in `JAVA_HOME` and Maven on PATH:

```powershell
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-26'
$env:MAVEN_OPTS = '-Xms128m -Xmx1g -XX:ActiveProcessorCount=2'
python F:/Tools/CodexTestRunner/runner.py exec --project forge --timeout 1800 -- cmd.exe /d /c mvn.cmd -B -ntp -pl forge-gui-mobile-dev -am test dependency:build-classpath '-Dmdep.outputFile=target/smoke-classpath.txt' '-Dtest=Strahd*Test,AuthoredWorldMapTest,CampaignContentValidatorTest,AdventureQuestProgressionTest,QuestNavigationTest,MapDialogQuestIssueTest,PointOfInterestEntryCooldownTest' '-Dsurefire.failIfNoSpecifiedTests=false'
```

This owner-invoked launcher opens the normal current-source game at 1920-by-1080,
30 FPS, audio enabled, a 1-GB maximum heap and two reported CPUs. Its review saves
are separate under `.build-cache/barovia-gothic/manual-profile`. Existing personal
saves and frozen distributions are not copied or replaced. Its syntax was
checked; a visible owner launch remains an acceptance step.

1. Start Curse of Strahd with either starter and enter Barovia. Expect distinct
   muddy/cobbled routes, slate/timber fronts, amber lamps and readable characters.
2. Wait ten seconds near a lamp, then walk across the square. Expect smooth,
   subtle moving mist and light variation, clear HUD/markers, no repeated bright
   vertical fog stripes, and no opaque boxes around sprites.
3. Speak to Ismark, Ireena and the Roadwarden, recover, and use the quest journal
   and map. Expect the original choices, rewards and named next destination.
4. Walk around the well, benches, houses and church enclosure. Expect the same
   barriers and open approaches as before; no new obstacle on an exit or actor.
5. Exit a town and stand on its overworld marker, then move clear and return.
   Expect the existing arrival grace behavior, with no immediate forced re-entry.
6. Visit the church, road and camp. Expect Donavich, Strahd and Eva to look
   distinct, both Ireena appearances to match, and original Doru/pack encounters
   and Strahd's mounted departure to work. Check forest/road readability too.
7. Save, quit, Continue, and revisit an older 0.5.0 save through its normal
   profile if desired. Expect unchanged position, quests, inventory and exits;
   the older stored minimap can retain its previous appearance.

Report starter, source/build, new or older save/version, numbered step, location,
last action, expected/actual behavior, screenshot and
`.build-cache/barovia-gothic/manual-playtest.log`. Those results unlock further
art/readability, collision-feel or performance refinements; they do not authorize
new chapters or gameplay. Automated duel outcomes are supplied, so balance,
physical controls, audio and subjective visual acceptance remain owner checks.
