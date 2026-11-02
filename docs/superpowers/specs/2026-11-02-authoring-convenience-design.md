# Authoring convenience: create and check a campaign

## Intent and authorization

David accepted the previously proposed authoring-convenience work: campaign
creation, connected editing and useful validation. This first usable increment
covers creating a playable project from a template and checking/editing it.
The implementation choices below are the builder's defaults, not additional
preferences attributed to David. Keep the existing desktop editor and game.

## Author experience

- The Campaign Editor startup button opens a campaign chooser with **New
  Campaign** and **Open Campaign**. Existing campaigns remain available, and
  rows distinguish their title, version and folder.
- New Campaign asks for title, a short description and an installed explicit
  template. Bellwake is the default because it is a small complete adventure;
  Curse of Strahd is another supported template. Legacy inherited worlds are
  not creation templates in this increment. Templates must own their manifest,
  config, world, quests, enemy and location catalogs so creation stays bounded;
  ordinary shared art/items may still inherit. No campaign/quest/actor IDs need
  to be typed or renumbered when cloning a separate campaign.
- The wizard shows the destination before creation. The editor creates projects
  under the existing userplanes discovery folder, not over installed content.
  It generates a unique `user.<UUID>` ID, content version0.1.0 and a safe,
  unique folder derived from the title. A copied template retains its story,
  encounters, opening quests and decks until the author changes them.
- Card rules initially inherit the template. The new Overview has a clear
  **Allowed sets** action into the existing set editor; no raw config edits are
  needed. It explicitly explains that decks and the story are copied too.
- Overview shows identity, version, project folder, starting location and a
  list of locations. Actions: edit metadata, go to relevant existing tool tabs,
  open project folder, open the selected map in the system's map editor, and
  **Validate Campaign**. File opening uses OS associations; if Tiled is absent,
  show a useful message and the map path. Do not install external software.
- Metadata editing preserves unknown manifest fields and the campaign ID.
  Title/description/version can be saved explicitly. Validation errors appear
  in the editor with project-relative paths; UI remains responsive while
  checking references/cards. A pass means the stated checks passed, not that
  match balance or all story branches have been played.

## Project creation and independent maps

Use a focused `CampaignProjectCreator` with explicit input roots and no live
game/UI state. Public operation accepts template root, common root, projects
root, title and description; returns the created root and ID. It copies owned
campaign data and preserves unrecognized configuration fields.
The concrete interface is `CampaignProjectCreator.create(Path templateRoot,
Path commonRoot, Path projectsRoot, String title, String description)` returning
`CreatedProject(Path root, String id)`. Root supplies a resource-only
`CampaignRegistry.readProject(Path root, Path commonRoot)` API for staged
definition validation; it must not activate a campaign or write settings.

Create in a hidden staging directory under the destination parent; publish
only after identity/resources validate. Never overwrite an existing project.
Reject source links and dependencies outside the template/common roots.
On failure clean up only the verified owned staging directory. Template,
shared assets, existing projects and saves remain unchanged.

Copy every referenced root map and recursively referenced Tiled dependency
(`source`, `template`, including tilesets/images/templates) into the project.
Rewrite XML references to their private destinations. Shared dependencies may
live under `assets/shared/`; editable root maps live under `maps/locations/`.
Follow and rewrite `teleport` map properties too. Detect conflicting sources,
missing references, cycles and unsafe paths. Shared runtime sprites/audio that
are ordinary resource names can continue using read-only common fallback.

The runtime currently assumes root POI maps are common. Add an explicit
`campaign:` root-map reference, e.g. `campaign:maps/locations/quay.tmx`, for
owned maps. Ordinary legacy references retain their existing common semantics.
A shared resolver validates that explicit campaign maps stay inside the campaign
and common maps inside common. Use it for root-map loading, restoration,
content validation and the editor's Open Map action. Do not change Bellwake or
Strahd's existing map references, IDs, geometry, campaign versions or saves.

## Validation

Reuse `CampaignContentValidator`, extending root-map resolution as above.
Validate manifest/declared starting quests, quest prerequisites, catalog/deck
and map references, dialogue choices and actual card names. The editor must
not report card validation success with a predicate that accepts every name.

Use a lazy CardStorageReader with the existing validated binary name-index cache;
initialize card type names as required and parse only referenced cards. Do not
initialize a full StaticData/FModel card database. Run the check in a SwingWorker
and disable repeated validation until it finishes. Cache only bounded named
lookups/index state; do not enable eager global card loading.

## Boundaries

Keep the existing World/Locations/Items/Enemies/Quests/Allowed sets forms. This
increment does not promise a visual dialogue graph, automatic story-ID repair,
new Tiled replacement, map painting in Swing, a new battle system or a working
Play-from-here launcher. Those are later authoring increments. Ordinary playtests
use a separate new run after restarting the game so content caches refresh.

The existing editor launcher, independent process lifecycle,512MB/2CPU editor
limit, game1080p/30FPS/audio settings and save-preserving updater remain.
Opening or creating a project must not modify an active/continued playthrough.

## Evidence and handoff

- RED/GREEN filesystem tests: unique identity/folder, preserved unknown fields,
  source/common unchanged, private maps/dependencies, safe rejection/cleanup.
- Map resolver tests: legacy unchanged, campaign-owned maps and save restoration,
  absent/escaping references rejected. Existing Bellwake/Strahd tests stay green.
- Real-name validation detects a bad deck card and missing map; no eager catalog.
- Invisible editor test creates/opens project and checks Overview/control state.
  Hidden native game test discovers the generated project, starts it, enters its
  private map and saves/restores it; outcomes remain assisted where relevant.
- Fresh review and verified Windows update including both matching JARs.
  Test update over prior builds and preserve saves/settings/launchers/runtime.
  Manual checklist covers the actual wizard, OS map association and new run.

Builder check: fresh reviewer found nothing important left unanswered. Retain
the already-authorized feature branch and existing work; no merge/push or
source commits are required for this deliverable.
