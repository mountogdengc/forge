# Campaign Editor

Click **Campaign Editor** on the desktop startup menu. It opens separately from
the game, with its own 512 MB memory limit and two processors. Closing the editor
leaves the game running. The game keeps its 1920×1080, 30 FPS and audio settings.

## Open and edit an existing campaign

The wizard is optional. Choose **Open Campaign** to use the same Overview, World,
Locations, Items, Enemies, Quests, Allowed sets and Files tools on an existing
campaign. Saved-file checks start automatically and the toolbar shows their
status. Use **Validate Campaign** again after saving a structured tool.

Errors in Overview identify the file or object where possible. **Files / Repairs**
opens the JSON source even if a structured form cannot read it. Choose a listed
project-relative file, or type another .json path, then Open / Reload. The editor
checks JSON syntax before saving and refuses to overwrite a file that changed
on disk. Missing files can be created here. Inherited files save as local copies.
Reload a structured form before using it after editing its source in Files.

Hover over **Create Editable Map Copies** for an explanation: it copies maps for
independent editing while keeping quests, settings and campaign identity.

For an existing campaign with shared maps, **Create Editable Map Copies** copies maps and
dependencies into that campaign, keeping its identity and version. Loaded
Locations edits are saved first, then the form reloads the private references.
Save other work before this operation. Existing saved runs can retain the older
world/map layout; use a new run to test authored layout changes.

**Allowed sets** has search by name/code and checkboxes. Choose All sets or Only
checked sets, then Save allowed sets. Checked sets remain selected when hidden
by search. Existing campaign restrictions still apply. This policy filters
generated card pools; prebuilt decks and explicitly named rewards retain their
authored cards. Edit those resources separately when enforcing a strict theme.

## Create a campaign

1. Choose **New Campaign**.
2. Enter a title and description. Bellwake is the default template; Curse of
   Strahd and other complete local projects can also be selected.
3. Review the destination and click **Create Campaign**. Wait for copying to finish.
4. The new project's **Overview** shows its identity, version, folder, opening
   quests and locations. Its identity is generated automatically and its initial
   version is 0.1.0. It appears in the game's campaign list after restarting.
5. Use **Edit Details** to change the title or description. Keep the identity
   stable for updates; use New Campaign to make a separate adventure. Changing
   version affects saved-run compatibility and requires a migration decision.

Creation copies the template's story and card availability. Maps, linked maps,
tilesets, templates and images are copied privately, with their references
rewritten. The source campaign and shared map files remain unchanged. Reusable
art, items and other ordinary resources may still inherit the shared fallback.

## Edit and check

Overview connects **Locations**, **Quests** and **Allowed sets**. World, Items
and Enemies are also available as tabs. Each tool loads when selected. Save in
each tool before validating; tool fields are not automatically saved.

Select a location and choose **Open Selected Map** to open a private map in
Tiled (install Tiled and associate .tmx files first). Original installed campaigns
may use shared maps: use Create Editable Map Copies before editing those maps.
**Open Project Folder** gives access to story/dialogue and other authored files.
This pass does not add a visual dialogue editor or Play-from-here.

Choose **Validate Campaign**. It checks saved metadata, world/startup requirements,
quest prerequisites, location maps and map dependencies, enemy/deck references,
configured starter decks, named card rewards and dialogue choices. Card checks
use the installed card scripts with a cached index; they do not load the full
game database. The first check can take several seconds. Errors identify the
file, quest, stage or map object where possible. Validation cannot prove story
logic or balance: a manual playthrough is still required. Broken drafts with
readable metadata remain available in the editor for repairs.

Restart the game after saving edits. Select the new campaign and start a separate
new run. Existing saved games continue to use their matching campaign identity
and version. Keep an authoring copy separate from a released campaign.

## Install the authoring update

Close the game and editor. Extract the **Campaign-Authoring-Update** folder inside
your existing Strahd game folder, beside Start-CurseOfStrahd.cmd. Run
**Apply-Update.cmd** from that update folder. Launch normally afterward. The
updater validates and backs up replacements under .updates; it preserves saves,
cache, launchers, display/audio choices and the bundled runtime. Earlier startup
button and encounter cleanup updates are included.

## Manual acceptance checklist

1. At startup click Campaign Editor. Expect New Campaign and Open Campaign;
   the game remains usable and repeated clicks do not open another editor process.
2. Choose New Campaign, title it Test Adventure and leave Bellwake selected.
   Expect a destination preview, then a new Overview after Create Campaign.
3. Check the title, unique identity, version 0.1.0 and project folder. Expect
   Bellwake's opening quests and locations, with private map paths.
4. Open each tool tab, change the project description using Edit Details and
   save. Reopen the editor later and expect the changed description.
5. Select a location, Open Selected Map and confirm Tiled opens the copy inside
   your project. If Tiled is unavailable, expect actionable guidance.
6. Open Allowed sets. Expect the template's current policy. Save any test change,
   then return to Overview and Validate Campaign. Expect pass or actionable
   errors. Restore any experimental changes before the playtest.
7. Close the editor and restart the game. Select Test Adventure, create a new
   run and enter its starting location. Expect Bellwake's copied opening.
8. Travel between linked maps, save inside, quit and Continue. Expect the same
   location, position and story progress. Revisit Bellwake and your existing
   Strahd run; expect their original cards, maps and choices.

Report campaign title, project folder, last numbered step, expected/actual result,
error text and logs/campaign-editor.log. Map/story balance and the ordinary
click-through experience remain manual acceptance checks.

## Direct-editing acceptance checks

1. Open an existing campaign without choosing New Campaign. Expect all eight tabs
   and an automatic saved-file check, with its status visible in the toolbar.
2. Open Allowed sets, choose Only checked sets and search for a set name or code.
   Check it, change the search and return. Expect the check to remain. Save and
   Reload from disk; expect the same policy. Restore the original policy afterward.
3. Use Files to open a JSON resource. Try invalid JSON and Save: expect Not saved
   and the file unchanged. Repair it, save and expect a fresh campaign check.
4. On a disposable existing campaign, open Locations, then Overview > Create Editable Map Copies. Expect private paths and unchanged campaign identity/version. Return
   to Locations and save; expect private paths to remain. Open a private map in
   Tiled and confirm the path belongs to that campaign.
5. Restart the game and test a new run of the edited campaign. Also Continue an
   untouched saved run. Report the numbered step, project folder, last action,
   expected/actual result and any displayed error.
