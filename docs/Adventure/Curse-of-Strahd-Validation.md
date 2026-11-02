# Into the Mists 0.1.0 — candidate validation

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
