# Campaign character creation

The creator uses the Adventure skin and Scene2D rendering pipeline. It shows the campaign title, name, gender, race, portrait, in-world sprite, starting class/deck and difficulty. Options use arrows and left/right input; the sprite sits between its arrows without a name. Import image and Begin journey are below the portrait. Back cancels an open chooser before leaving the creator.

Race and gender are descriptive choices. Each has a Match portraits checkbox, off by default. Both enabled means intersecting portrait tags; Unspecified gender imposes no gender filter. Empty or `*` tags mean unrestricted. Imported images remain selected when filters change. No race/class bonuses, backgrounds or new gameplay restrictions are introduced.

## Campaign configuration

Add optional `characterCreation` to a campaign's `config.json`:

```json
"characterCreation": {
  "allowPortraitImport": true,
  "matchPortraitRace": false,
  "matchPortraitGender": false,
  "genders": [
    {"id":"unspecified", "label":"Unspecified", "namePool":"Any"},
    {"id":"female", "label":"Female", "namePool":"Female"},
    {"id":"male", "label":"Male", "namePool":"Male"},
    {"id":"nonbinary", "label":"Nonbinary", "namePool":"Any"}
  ],
  "races": [{"id":"human", "label":"Human"}, {"id":"elf", "label":"Elf"}],
  "portraits": [
    {"id":"traveler", "label":"The Traveler", "image":"portraits/traveler.png",
     "races":["human"], "genders":["female","male","nonbinary"]}
  ],
  "appearances": [
    {"id":"human-traveler", "sprite":"sprites/heroes/human_f.atlas", "heroIndex":2, "female":true}
  ],
  "starterClasses": [
    {"id":"paladin", "label":"Paladin", "deck":"decks/my-paladin.dck", "color":"W", "mode":"Constructed"}
  ]
}
```

IDs must be nonempty and unique within each collection. Keep IDs stable when reordering or relabeling options. Labels support existing `tr(...)` localization. Omitted genders use Unspecified, Female, Male and Nonbinary. Omitted races, appearances and portraits derive from the campaign's existing `world/heroes.json` and its avatar atlas, including the common-resource fallback. A race does not restrict the independently selected sprite.

Portrait `image` and appearance `sprite` paths resolve within the selected campaign/common roots. A portrait may use `x`, `y`, `width`, `height` to select a high-resolution region from a shared image; omit them for the entire image. New image portraits use linear sampling; inherited pixel avatars and animated sprite atlases keep their existing sampling. `heroIndex`/`female` on an authored appearance provide a legacy fallback if the new sprite resource is later unavailable. They do not set the new gender choice.

`starterClasses` maps campaign class names to campaign-owned deck files. Its default mode is Constructed; other existing mode names can be specified, subject to `allowedStarterModes`. The class window shows only class name, color and card count, lists the cards, and renders the selected card in a fixed frame through RewardActor. The confirmed deck is copied into the new playthrough, so generated/random choices do not reroll on Begin journey. Class is starter-deck metadata; normal deck editing remains available.

When `starterClasses` is omitted, the campaign's existing starter modes, colors, editions, preconstructed and custom choices remain available. Only applicable edition selectors appear. Changing difficulty retains a supported mode and the confirmed deck if its source remains the same; a changed/unavailable source requires choosing and confirming a deck for that difficulty. Generated previews use scoped campaign rules, booster overrides and random providers, and restore the outgoing context even if generation fails.

Barovia supplies four painted portrait regions in `CurseOfStrahd/portraits/travelers.png` and Paladin/Necromancer class metadata referencing its current white/black campaign starter files. Those card lists are placeholders for later campaign deck authoring. No existing shared/default `.dck` metadata or card list has been renamed or changed. The four current portraits are human; race matching on another race can legitimately produce no matches until more portraits are supplied.

## Imported portraits and saves

The picker accepts PNG/JPEG images up to 10 MiB and 4096 pixels per dimension. Import center-crops the image to a square, normalizes it to 512×512, and previews it with smooth sampling. Invalid imports leave the prior portrait intact. The normalized PNG is embedded in the playthrough save; the original file can be moved or removed after saving. It is never modified.

The picker runs in the Adventure UI, with a path field, folder/image list, Up, Locations, Open and Cancel. Up/Down and the existing directional bindings select entries; Enter/Use opens the selected entry. Escape cancels the topmost dialog. Mobile access depends on the platform's filesystem permissions; native gallery/content-URI integration is outside this change. A missing configured appearance clears its preview and disables Begin journey until a valid appearance is selected.

The player's optional `characterIdentity` section stores stable identity IDs/labels, sprite path, portrait descriptor and imported bytes independently. Legacy `heroRace`, `isFemale` and `avatarIndex` fields remain. Saves without the new section use their original portrait/sprite/race paths; they are not migrated on load. Existing campaign IDs/versions and quest/world data are unchanged.

## Validation and playtest

Automated coverage includes configuration round trips, filter intersection and no matches, identity/byte-array round trips, PNG/JPEG header limits, scoped randomness, real stage key events, fixed card geometry, unavailable-deck recovery, difficulty retention/reconfirmation, new save/fresh-process Continue, inherited campaign preview, portrait orientation, and a copied pre-update Barovia save. Native windows stay hidden/unfocused and use disposable profiles. Validation never writes the user's normal saves or the retained source fixture.

For a physical playtest, launch the rebuilt Adventure client with a separate profile, choose Barovia, and:

1. Cycle gender, race, portrait, sprite and difficulty using mouse arrows and keyboard left/right. The focused selector should retain focus, and other identity choices should stay unchanged.
2. Enable either/both Match portraits checkboxes. A missing match should explain how to broaden choices or import; an imported portrait should stay selected.
3. Import a PNG/JPEG, inspect the crop, then try an invalid file. The valid portrait should remain after the error.
4. Open Choose class, browse cards, cancel with Back/Escape, then confirm another class. The card frame should remain the same size, and the main screen should show the confirmed class/color/count without an extra deck title.
5. Begin the journey, navigate the playthrough-name prompt using keys, and follow the normal opening conversation. Save once normal exploration permits it, close the client, remove/move the source image, and Continue. Identity, portrait, sprite, deck and quest progress should remain intact.

Report screen resolution/orientation, input device, campaign/class, imported format/dimensions, the first failed step, expected/actual behavior and a screenshot. Physical controller behavior, picker access on each mobile platform, audio and complete campaign playthrough remain separate acceptance checks.
