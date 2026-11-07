# Curse of Strahd — candidate validation


## Krezk 0.5.0 — November 3, 2026

**A Bell Against the Mists** is the complete third chapter, following an explicit
handoff from Ireena at Saint Andral's after sanctuary. Four quest scenes cover
Krezk gate, village, blessed pool and Abbey of Saint Markovia. Three new 40-card
encounters, nine journal objectives, free recovery with Anna, two final accords
and two-card white/black rewards use the existing engine. Ireena stays where she
chose sanctuary. There are 37 world sites and 25 remaining passive arrivals.

**A new 0.5.0 run is required.** Previous saves embed the earlier world roster.
Frozen 0.4.0 and earlier packages/profiles remain untouched. No migration,
commits, publishing or visible game launch was performed.

- Final affected Java suite: **102 cases, zero failures/errors/skips**, queue
  `521632fe77ed4da599af447acfc5cdfd`; final authoring suite: **36 Python checks
  passed**, queue `3dc8cd7911e540529786eb17a1ac5528`. Package reports total 123
  cases, including 21 editor/desktop checks from the unchanged engine build.
- Initial missing-chapter RED evidence: `fd458e1bedbb4992b849d53e0a7d92d7`.
  Later checks exposed automatic water progression and compact-HUD overflow;
  both were fixed. All progression actions now require explicit choices.
- All 90 dialogue actors embed correctly; 37 local scenes have validated actors,
  collision routes and southern exits. All 37 fixed world approaches connect.
  All 32 preexisting maps outside the expanded gate and sanctuary handoff remain
  byte-identical. Labelled map and Krezk scene previews were inspected.
- Fresh independent read-only review found an absent-choice fallback mismatch
  with runtime flag-presence semantics and two native-driver assumptions about
  the new handoff/reward screen. Fallback regression failed first, then passed;
  explicit missing/unknown-choice roots now dismiss safely. The driver declines
  the handoff on sanctuary revisit and explicitly finishes both reward screens.
  The obsolete first native run was stopped; the second exposed an extra test-driver Continue after an effect-only victory. Both are retained as failed evidence;
  only the corrected run below is passing evidence. No findings remain deferred.
- Hidden native queue `316b5f500aa545ed9be38657a7b156d6` passes prior chapters,
  both Doru/Vallaki outcomes, early Krezk visits, deliberate handoff exactly once,
  loss/rematch, free recovery, interrupted water/accord/reward choices, saved
  post-victory cleanup, both accords and both card colors, unchanged selected
  deck, repeat safety, checkpoint rewind and all four real southern exits.
  Fresh-process Continue restores completed Krezk Village, exact position,
  life/currency/cards/deck, accord and chapter state. World arrays and 37 POIs
  match authored data in new/completed/continued phases with hash `1284314643842881109`;
  all three exported engine minimap PNGs are byte-identical.
- Evidence: `.build-cache/strahd-krezk-native-3`. Production Java is unchanged;
  all 3,440 current production class files match the reused client. Fixtures are
  separately compiled, hidden/unfocused, isolated, audio off, 1920×1080/30 FPS,
  JVM capped at 1 GB and two processors. Duel outcomes are supplied; native
  POI entry is direct. No played-match balance or physical-navigation pass is
  implied. The unchanged 25-arrival tour retains its earlier 0.4.0 evidence.
- Portable archive/current client, campaign, map and source hashes, complete
  manifest coverage, ZIP checksum and no-test-save checks are in the sibling
  verification.json after success. Actual packaged launch, manual walking,
  collision feel, travel/scene readability, audio and battle difficulty remain
  owner acceptance. The [playtest guide](Curse-of-Strahd-Playtests.md) includes
  prerequisites, 15 Krezk steps, both choices, expected outcomes and report fields.
  Results unlock balance, travel and art refinements; further chapters remain
  separate work.

Candidate: `dist/adventure-windows/strahd-0.5.0-krezk/CurseOfStrahd-Windows-0.5.0-06aaacbba91b`.
Launch `Start-CurseOfStrahd.cmd`; Java is bundled. Historical evidence follows.



## Valley destinations 0.4.0 — November 3, 2026

Adds **26 short arrival areas, 34 locations total**, including Castle Ravenloft,
Krezk, the winery, Amber Temple, Berez, Argynvostholt, the tower, den, windmill,
passes, gates, rivers and lakes. Uses the existing pixel assets and reference
geography. Each new area has two interactions and a southern overworld exit;
further stories/interiors are explicitly future content. Original eight TMX
maps remain byte-identical; both quests retain their progression and rewards.

**Start a new 0.4.0 run.** Older saves embed the earlier POI roster. Frozen
0.3.0 and earlier builds and user profiles remain untouched. No migration,
publishing, commits or visible game launch was performed.

- Current affected Java suite: **100 tests, no failures/errors/skips**, queue
  `7285d55107b5402fb3ea9a2639456571`; current Python suite: **28 passed**,
  queue `78580cfffae64425a1b68b0561b50027`. The package also retains 21 passing
  editor/desktop checks from the unchanged engine build, for 121 report cases.
  The two initial missing-content failures are retained separately as RED
  evidence; both new cases pass in the current combined suite.
- All 34 local maps validate actor approaches and exits; all 79 dialogue actors
  match the embedded catalog. All 34 overworld anchors and 3×3 paths are
  connected. Generation and source checks preserve the existing chapter maps.
- Hidden native tour visited **all 26** new scenes and both actors, exercised
  every arrival choice, and used the real southern entry exits. Injured visits
  preserved life, gold, collection, deck and chapter/quest state. Ravenloft and
  Krezk manual checkpoints restored; each exit retained its overworld position.
- Both original chapter branches/choices pass with supplied duel outcomes.
  Fresh-process Continue restores the completed run inside Krezk; an earlier
  Ravenloft checkpoint also restores its two interactions and pre-quest state
  in that process. New/completed/continued world arrays and all 34 named POIs
  match authored data with unchanged geography hash `17629805221738728738`.
- Evidence: `.build-cache/strahd-landmarks-native`, queue
  `195967ec887a4a1188cc377a75a3c66b`. Actual engine map and
  Ravenloft/Krezk screenshots inspected, along with the labelled world preview
  and all-scene contact sheet. A fresh independent read-only review found no
  scoped defects; it performed no native execution or visual acceptance.
- Production Java is unchanged for this content extension. All **3,440 current
  production class files** match the reused client JAR; native test fixture
  classes are compiled separately. Native runs use disposable profiles,
  hidden/unfocused 1920×1080/30-FPS windows, audio off, 1-GB/two-processor JVMs.
- Native entry uses direct POI loading. Physical walking, marker readability,
  scene quality, travel pacing, played-duel balance, audio and packaged launch
  remain manual acceptance. Follow the exact numbered
  [playtest guide](Curse-of-Strahd-Playtests.md), including the new destination
  table. Report build, starter, run/save, numbered step, last action,
  expected/actual result and logs; this unlocks travel/art fixes and planning
  full chapters at these destinations.

Candidate: `dist/adventure-windows/strahd-0.4.0-landmarks/CurseOfStrahd-Windows-0.4.0-06aaacbba91b`.
Launch `Start-CurseOfStrahd.cmd`; Java is bundled. The sibling verification.json
records successful archive/manifest/current resource, client and source hashes;
test saves are excluded. Historical 0.3.0/0.2.0/0.1.0 evidence follows below.


## Barovian geography 0.3.0 — November 3, 2026

David supplied a reference map and selected matching geography in the existing
pixel style. This candidate replaces the straight, flat overworld with a fixed
300×180 landscape: eastern Barovia, Tser Pool in the river valley, Vallaki beneath
Lake Zarovich, western lakes and ridges, northern Baratok and southern Ghakis.
The roads wind through forests, passes and bridges. All eight existing playable
locations and both chapters remain; further settlements are not added as quests.

**New 0.3.0 run required.** Existing 0.2.0 terrain is serialized in its saves;
the prior candidate and its profiles remain untouched. No save migration,
publishing, commits or visible game launch was performed.

- Final affected build: **119 cases, zero failures/errors/skips**, checkstyle
  clean. Queue: `584ab4bf87c54981b2a001d8ccf13555`.
- **26 Python tests pass**, including stable generated geography, references,
  eight reachable POIs and road/river crossings. Independent review confirmed
  all eight approaches with a clear 3×3-tile movement footprint.
- New schema and renderer regressions were observed failing first. Review
  regressions also failed before fixes for procedural padding modifying authored
  terrain, edge anchors and disconnected POIs. Fixed worlds now reject those
  inputs before placement. Blank/missing authored fields retain procedural mode.
- Hidden native white start passes both chapters, both Doru fates, both Vallaki
  truth choices, rewards, checkpoint rewinds and a Bellwake switch. Every actual
  terrain cell/collision bit and the exact POI roster match the authored file;
  eight paths are reachable in the engine. Fresh-process Continue retains the
  same map hash `6706115543985500737` and interior, position/life/currency, cards and flags.
  Evidence: `.build-cache/strahd-geography-native`. Actual engine minimap exported
  and inspected alongside the labelled pixel preview.
- Native outcomes are supplied, audio disabled and the window hidden/unfocused,
  with isolated profiles, 1920×1080/30 FPS and a 1-GB/two-processor JVM cap. Earlier
  0.2.0 W/B battle/deck evidence below remains separate from this map-only run.
- The new portable archive's manifest, client/campaign/map/source hashes and
  no-test-save assertion are recorded in the sibling `verification.json` only
  after verification succeeds. Packaged launch, actual walking, travel pacing,
  mountain/river collisions and audio remain manual acceptance; follow the
  [numbered guide](Curse-of-Strahd-Playtests.md).

Candidate: `dist/adventure-windows/strahd-0.3.0-geography/CurseOfStrahd-Windows-0.3.0-06aaacbba91b`.
Launch `Start-CurseOfStrahd.cmd`; Java is bundled. Report build name, starter,
run/save, geographic/story step, last action, expected/actual result and logs.
Manual results unlock map/pacing refinements; new gameplay remains separate.

Minor deferred: the generator still performs its ordinary biome work before
the authored raster replaces it. Canonical Barovia biomes have no structures;
this does not change the result. A future optimization may skip that work.

The 0.2.0 and 0.1.0 sections below are historical chapter evidence.

## A Light in Vallaki 0.2.0 — November 3, 2026

The current candidate adds West Svalich Road, Vallaki, Saint Andral's Church and
the Coffin Maker's Shop: eight locations, two quests and 27 dialogue actors.
Three new encounters lead through the missing-bones investigation, sanctuary
restoration and Ireena's explicit decision. The private/public truth choice and
the earlier Doru fate produce different dialogue. The final choice grants two
Fiend Hunters or two Falkenrath Nobles once without editing the selected deck.

**Compatibility:** campaign ID stays `fork.curse-of-strahd`; content is **0.2.0**.
A new run is required because old serialized worlds lack the new locations.
Exact-version loading checks remain in place. The candidate uses a new portable
folder/profile; existing 0.1.0 installations and saves are not rewritten.

### Current automated evidence

- Final affected reactor verify: **111 cases, zero failures/errors/skips**;
  checkstyle reports zero violations. Queue evidence:
  `F:/Tools/CodexTestRunner/state/results/forge/86c3b968fb534757a1b824387c60731f`.
  This covers campaign discovery/validation, save boundaries/repository,
  opening/new chapter data, quest-issuance routing and desktop defaults.
- **24 Python authoring/package tests pass.** All eight maps have valid resource
  references, enclosed boundaries, clear entries and reachable interaction
  actors. All 27 dialogue catalogs match their embedded TMX properties.
- Four new static map previews and native Vallaki/workshop images inspected.
  The original four maps retain their geometry and object IDs (XML compared to
  HEAD with only dialogue contents normalized).
- New content tests first failed on the absent chapter. Fresh review exposed
  overlapping dialogue roots and a missing ordinary-TMX quest recipient; both
  now have regression coverage. Quest-copy tracking also failed before the
  one-field copy repair. The native fixture confirms real quest issuance,
  automatic tracking and exact selected-deck contents.
- A broader earlier reactor run also completed before the two small shared
  quest repairs, with opt-in coverage skipped. Its older desktop reports were
  archived separately and are excluded from the candidate's 111-case totals.

### Current hidden native evidence

The fixtures use invisible, unfocused libGDX windows, disposable profiles,
1920×1080/30 FPS, at most 1 GB heap and two reported processors. Duel outcomes
are supplied; these are **assisted** journeys. Audio is disabled in the fixture.

- **White start passed** both chapters with both Doru fates and both truth
  choices. Checks include early visits, withdrawal, losses/rematches at all
  three new encounters, free recovery, confession-only reload, post-victory
  relic checkpoint, deferred collection/settlement, truth rewind, once-only
  rewards, exact deck preservation and Bellwake isolation. Fresh-process
  Continue restored the church interior, exact position/life/currency,
  completion/choice flags and collection; defeated figures stayed absent.
  Evidence: `.build-cache/strahd-vallaki-native-white-2`, queue
  `fbf18578bbb4426a8afb69fd4f62875a`.
- **Black start passed** the same two-chapter/fate/truth routes, loss/recovery,
  checkpoints, black reward and exact deck preservation, and independent fresh
  Continue/actor reconciliation. Evidence: `.build-cache/strahd-vallaki-native-black`,
  queue `96ef5a780cb64b7985a55e4884207d11`.
- Portable archive results are recorded in the sibling `verification.json`
  only after all manifest entries, the client, current campaign/maps and source
  snapshots match and the ZIP SHA-256 is checked. The archive contains no test saves.
  Native journeys above use the tested source resources/client; packaged launch
  and a full manual playthrough remain owner acceptance.

An initial native invocation stopped on a Java startup warning under Windows
PowerShell 5. Retrying with PowerShell 7 resolved the harness issue; the failed
attempt remains recorded separately. No visible game was launched.

### Manual acceptance and artifact

Actual walking/collisions, played-duel balance with both decks, choice clarity,
audio and long-session responsiveness remain manual acceptance. Follow the
[updated numbered guide](Curse-of-Strahd-Playtests.md), including prerequisites,
expected outcomes, both truth choices and checkpoint/Continue checks. Report
BUILD-INFO build name, starter, run/save, numbered step, last action, expected
and actual result, plus launcher/game logs. These results unlock balance/art
refinement and the next winery/Krezk chapter; they are not claims of a complete
published campaign.

Candidate destination:
`dist/adventure-windows/strahd-0.2.0-candidate/CurseOfStrahd-Windows-0.2.0-06aaacbba91b`.
Use `Start-CurseOfStrahd.cmd`; Java is bundled. The frozen ZIP and sibling SHA-256
file are the portable handoff. Packaging records the uncommitted source changes
(including pre-existing theme work used by this client) and their exact hashes.

## Historical 0.1.0 opening evidence

Date: November 1, 2026. Source baseline `34876cb2970`, plus the source changes
identified in the candidate's BUILD-INFO.json. This is a playable opening test
chapter, with existing pixel art and Magic combat. It is not the full adventure.

## Automated checks

- Clean Maven verify across Adventure Editor, mobile/dev, desktop and required
  reactor modules: **1,457 cases, zero failures/errors, six skipped**. The six
  skips are the existing opt-in `NetworkPlayIntegrationTest` stress cases
  (`analyzeLog`, comprehensive/quick delta sync, configurable parallel/sequential,
  unified harness local mode). They were not enabled for this content change.
- New chapter data tests: isolation, both 40-card starts, quest flag progression,
  late briefing, both resumable Doru choices, explicit camp completion and atomic
  once-only training rewards. Desktop tests cover normal 30 FPS/audio and fresh
  desktop/mobile resolution defaults while retaining explicit saved preferences.
- Four original maps: tile/resource references, enclosed collision boundaries,
  clear entries and flood-fill access to all 13 interaction actors. All 13
  dialogue catalogs embed into TMX and match in check mode.
- Authoring/package regression tests cover stale/missing/duplicate dialogue,
  launcher options, historical-client rejection, complete versioned archive
  names and staged/untracked source provenance.

## Hidden native checks

All native fixtures use invisible, unfocused libGDX windows with input capture
disabled, 30 FPS, at most 1 GB heap and two reported processors. They use
disposable profiles. They supply duel outcomes; they do not play matches.

- White start: actual card/resource validation; friendly loss; first upgrade and
  repeat win; early road/camp visits; both Doru fates; late briefing; explicit
  escort/road/Eva completion; interior checkpoint rewind; Bellwake campaign
  switch and return; fresh-process Continue with exact position/life/currency
  and unchanged cards. Passed, including a separately started Continue process.
- Black start: the same assisted chapter checks and fresh-process Continue
  passed. Bellwake's existing hidden interior/story fixture also passed both
  endings, reward-once guards, checkpoint restoration and fresh Continue.
- Follow-up after shortening objective prose: six chapter data tests passed;
  all 16 Python tool tests passed with successful process exit.
- Fresh Windows extraction: ZIP checksum/CRC, all 56,444 manifest-covered files,
  both campaign versions, source-change hashes and bundled Java 26 verified.
  The packaged native run passed default Strahd selection and portable profile
  assertions, settled objective-panel bounds, ordinary church-loss return,
  the assisted chapter paths and separate-process Continue. Screenshots were
  inspected. The ordinary loss returns outside the church; practice stays in
  the village. The guide makes that distinction.
- Final archive refresh changes guide/evidence/source snapshots only. Its
  finalization gate compares every game file with the native-tested payload,
  then reads all ZIP entries for CRC and SHA verification. The archive checksum
  is stored beside the ZIP and in the final verification report.

## Manual acceptance

## Compatible encounter-cleanup patch

The owner completed the original opening successfully, then requested that
defeated figures leave unless required for conversation, and Strahd ride away.
The patch keeps campaign/save version0.1.0, map geometry and actor IDs unchanged.

- Three cleanup regressions first failed against the shipped story; all nine
  chapter data tests now pass. Affected reactor build:131 tests, zero failures,
  errors or skips; checkstyle reports zero violations. The earlier full desktop
  regression evidence above remains separate from this affected-module run.
- Hidden white/black runs pass Doru's decision and final-line removal, immediate
  post-animation wolf cleanup, mounted Strahd exit, blocked partial saves,
  revisit/load persistence and fresh Continue. The mounted image was inspected.
- An actual completed profile from the original0.1.0 build loads successfully;
  its resolved Doru and wolves disappear when their maps load. Strahd leaves
  after his next conversation because older saves have no departure flag.
- All18 Python tooling tests pass, including real PowerShell updater tests for
  verified replacement, backup of the original client, unchanged save bytes and
  rejection of a damaged update before any game-file change.

The targeted update changes the client, the church/road scripts and provenance
documents. It excludes profiles, settings, caches, launchers and the Java runtime.
It validates compatible client hashes, backs up replaced files and rolls back
copy failures. Run it with the game closed; no new playthrough is needed.

## Remaining manual acceptance

Pending: actual walking and interaction, played duels with both decks, choice
clarity, art/audio quality and long-session responsiveness. The owner already
completed Bellwake with the tested 1080p/30-FPS setup; this does not establish
Strahd balance. Follow [the manual checklist](Curse-of-Strahd-Playtests.md).

The working installed Bellwake launchers were updated to the tested capped
1080p configuration with backups. Its saves and JARs were preserved; it was not
automatically launched. The initial severe slowdown's root cause is unconfirmed.
