# Curse of Strahd: Into the Mists — opening chapter

## Intent

David selected Curse of Strahd as the next campaign after completing Bellwake
and delegated the first playable chapter's scope. This design chooses arrival
in Barovia, a village hub and church conflict, and escorting Ireena to Tser Pool.
The playable opening ends there; it does not claim to implement the entire
published adventure. Dialogue, encounter scripts and maps are newly authored
for this card-battle adaptation. Published names and high-level setting facts
are used, not copied adventure prose, maps or stat blocks.

Primary reference: the official adventure listing at
https://www.dndbeyond.com/sources/dnd/cos and Wizards' public introductory
adventure at https://media.wizards.com/2016/downloads/DND/Curse%20of%20Strahd%20Introductory%20Adventure.pdf.
The first implementation uses existing Forge/libGDX rules, AI, quest flags,
dialogues, maps, save boundaries and authored decks. No D&D combat/party engine.

## Opening experience

Start at the village of Barovia after the mists close behind the protagonist.
Ismark offers shelter and asks the player to investigate the church before
Ireena will leave. Donavich's son Doru is a dangerous vampire spawn. Subdue him,
then choose whether he is restrained or put to rest. Both choices advance the
story and receive different later dialogue; there is no pretend cure.

Return to Ireena and explicitly agree to help her travel. Clear an encounter on
the old road, then speak to Madam Eva at Tser Pool to finish the opening chapter.
Strahd appears as a noncombat presence on the road, not a supposedly final boss
the player can defeat in the tutorial. Eva's warning points toward the later
campaign, without pretending that the complete Tarokka/relic system exists yet.

The village offers free recovery and optional repeatable friendly practice.
The first practice victory grants a chosen two-card upgrade once, never edits
the deck automatically, and losing practice keeps the player in the village.
Use two fixed 40-card starts: white protective humans/spirits and black graveyard
creatures/removal. Card pools use ISD, DKA, AVR, SOI, EMN, MID, VOW, AFR and CLB;
named starts/rewards/enemies remain resolvable on demand. Do not change Bellwake.

## Content contract

- Folder `forge-gui/res/adventure/CurseOfStrahd`, ID `fork.curse-of-strahd`,
  version `0.1.0`, title `Curse of Strahd: Into the Mists`.
- Standalone quest/world catalogs, `inheritCommonWorld=false`,
  `inheritCommonQuests=false`, `allowNewGamePlus=false`, normal discovery.
- Quest 8101; stable root POIs: `Spawn` (Village of Barovia), `Village Church`,
  `Old Svalich Road`, `Tser Pool`. Spawn is the defeat/recovery location.
- Common namespaced root maps `common/maps/map/strahd/village.tmx`,
  `church.tmx`, `road.tmx`, `tser-pool.tmx`. Four distinct, connected walkable
  scenes; no edits to inherited maps or Bellwake geometry.
- Village actor 60 Ismark, 61 Ireena, 62 practice Roadwarden, 63 notice/hints.
  Church actor 60 Doru, 61 Donavich, 62 register/clue. Road actor 60 wolf pack,
  61 waystone/clue, 62 Strahd. Pool actor 60 Eva, 61 Ireena, 62 westward sign.
  Entrance/overworld exit actor 38 on every map, within a clear walkable route.
- Geometry identifies authored dialogue by `cosDialogKey`: `village.ismark`,
  `village.ireena`, `village.practice`, `village.notice`, `church.doru`,
  `church.donavich`, `church.register`, `road.wolves`, `road.waystone`,
  `road.strahd`, `pool.eva`, `pool.ireena`, `pool.sign`.
  Authoring JSON `CurseOfStrahd/story/dialogues.json` maps each key to `dialog`
  and optional `defeatDialog` arrays. A build tool embeds these arrays in TMX;
  runtime MapDialog still receives inline JSON and needs no engine feature.
- Enemy names `Svalich Roadwarden` (practice), `Doru`, `Mist-Stalked Pack`.
  Corresponding actors carry enemy properties and use existing enemy templates.
- Progress flags in order: `cosBriefed`, `cosDoruSubdued`,
  `cosChurchResolved`, `cosEscortAccepted`, `cosRoadCleared`,
  `cosChapterOneComplete`. Character `cosDoruFate` = 1 restrained / 2 slain.
  Optional `cosTrainingRewarded`, `cosRegisterRead`, `cosWaystoneRead`.
  Completion requires deliberate Eva interaction, never just reaching the POI.
- Choice/reward and its once-only marker commit in the same action. Interrupted
  conversations, early visits, losses, repeat visits and loading earlier saves
  cannot skip prerequisites, duplicate rewards or strand a run.

## Quality and scope limits

Compatible 0.1.0 encounter cleanup: wolves disappear when their victory animation
finishes. Doru remains for the fate choice and its final line, then leaves the
visible scene. Strahd rides away on a black horse after his conversation and
does not return on revisits. The geometry, object IDs, campaign/save identities
and progression prerequisites stay unchanged. `removeWhenQuestFlag` reconciles
resolved actors when older 0.1.0 saves load; Strahd's new departure flag is set
when he is next spoken to. A timed `departMapObject` dialogue action presents the
mounted exit while blocking partial saves. This is a compatible bug-fix patch,
not a new chapter/world version.

Reuse existing pixel tiles/sprites and ambient infrastructure for this first
playable candidate; maps must communicate village, church, forest road and camp
as different places. Use original concise horror dialogue, clear objectives,
explicit reward colors and no permanent hidden penalties. Actual match balance,
timing, audio and visual acceptance remain human playtests.

This is an opening-chapter test version, not stable full-campaign progression.
Future Vallaki, winery, Krezk, relic hunt and Ravenloft work needs a separate
content plan and an explicit save/world migration decision. Do not silently
change version 0.1.0 beneath its saves or publish unfinished areas as playable.

## Desktop baseline

Use 1920×1080 windowed and an independent 30-FPS cap by default, audio enabled,
128-MB initial/1-GB maximum Java heap and two reported processors in packaged
launchers. Preserve explicit saved size choices; native mobile retains its old
resolution defaults. Do not start a visible game automatically. The user has
completed the whole Bellwake quest with the capped setup and reports 1080p is
working; the original slowdown's root cause remains unconfirmed.
