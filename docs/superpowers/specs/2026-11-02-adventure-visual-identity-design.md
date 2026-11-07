# Adventure visual identity and reusable themes

## Intent and scope

Give Forge Adventure the atmosphere of a planeswalker's traveling folio:
stained glass, aged metal, parchment, jewel accents, and illustrated tools.
Keep cards, rules, resources, and actions clear. Make alternate identities easy
to author without copying screens or editing Java for every variation.

David requested a Shandalar-inspired identity, liked the interactive study,
and selected **Adventure** as the implementation scope. The proposed default
is **Planeswalker's Folio**. Midnight Grimoire and Verdant Sanctuary are two
bundled alternatives demonstrating material and shape changes as well as color.
These defaults and the technical decisions below are the designer's proposals;
the complete design still needs approval before implementation.

The target is the existing libGDX Adventure application, including its desktop
launcher and existing mobile layouts. Adventure duels and Adventure-owned deck
management are included. The Swing desktop application's appearance, Classic
mode, the standalone campaign editor, and general Forge skin selection are
outside this change. Shared renderer changes must be conditional on Adventure.

## Visual reference

The interactive study is `forge-identity-v1.html`, preserved in the thread's
visualization directory under `.superpowers/brainstorm/forge-identity/content/`.
It demonstrates three identities across Adventure, Duel, and Components, with
an editable recipe. It is a visual reference, not a replacement application.
Its recipe is explicitly marked `forge-visual-study` and is not a runtime format.

Carry forward its palette relationships, glass and metal trim, paper treatment,
shared control styles, and theme family. Preserve actual Adventure screen
functions, navigation, map presentation, and current resource semantics. The
study's fictional sanctuary, quests, Supplies counter, and static village menu
are illustrative content. Do not add them, add a duel shortcut to exploration,
or replace the playable world with the study's scenic backdrop.

## Visual language

| Role | Planeswalker's Folio | Midnight Grimoire | Verdant Sanctuary |
| --- | --- | --- | --- |
| Structure | Bronze fittings and glass | Cut silver and geometric seals | Carved wood and botanical inlays |
| Accent | Emerald and teal | Amethyst | Jade and sage |
| Reading surface | Warm parchment | Cool vellum | Pale botanical paper |
| Shape | Round medallions, restrained bevels | Angular medallions, fine straight trim | Leaf-like medallions, softened corners |
| Mood | Adventurous and well traveled | Arcane and scholarly | Sheltered and living |

Illustration and ornament belong at frame edges, heading areas, and navigation
anchors. Body text, card rules, lists, and the battlefield get quiet backgrounds.
Decorative display lettering is limited to short headings. Body fonts retain
Forge's localization and glyph coverage, with an existing font fallback.

Use original art or appropriately licensed existing Forge assets. The identity
may evoke the old game's craftsmanship without copying its frames or logos.
Mana symbols and established gameplay icons retain their recognizable meanings.

Text and controls must remain legible with all themes and with ornament and
texture disabled. Target at least 4.5:1 contrast for ordinary text, 3:1 for large
text and essential control boundaries. Focus, selection, disabled state, active
phase, and targeting must have cues beyond hue alone. Invalid custom color pairs
produce an inline explanation and block saving that recipe until repaired.

## Screen coverage

- **Entry and setup:** Adventure start, campaign chooser, character creation,
  save/load, settings, and pause/menu surfaces share the same headings, buttons,
  selection states, and frame treatment.
- **Exploration:** theme the existing HUD, navigation icons, resource backing,
  minimap frame, notifications, and dialogs. Keep the map viewport as large as
  practical. Use actual life, gold, shards, keys, and equipment abilities.
  Decorative additions cannot hide actors, targets, or touch controls.
- **Journal and inventory:** paper surfaces, clear section titles, selected rows,
  item frames, tabs, and coherent empty states. Preserve quest and item logic.
- **Shops and rewards:** shared panels and actions, readable prices, existing
  buy/sell/availability feedback, consistent reward and encounter dialogs.
- **Deck management:** apply themed chrome and readable selection to the
  existing Adventure deck experience. Preserve sorting, filters, card previews,
  card art, costs, and existing editing rules.
- **Adventure duels:** match surrounding chrome, prompts, menus, life/resource
  surfaces, phase states, log, and win/lose surfaces to the selected theme.
  Keep battlefield layout, card scaling, targeting semantics, and existing
  background selection behavior. Do not impose the study's tiny sample cards.

On narrow screens, reduce ornament and adapt the existing portrait layout
instead of shrinking text or squeezing a desktop sidebar into the viewport.
Keep keyboard, controller, and touch navigation functional. All current named
actors referenced by scene controllers keep their IDs and behavior.

## Theme system

Use one resolved Adventure theme to supply three consumers: Scene2D styles,
Adventure-specific `FSkinColor`/custom-rendered controls, and HUD/presentation
assets. Do not create independent palettes for exploration and duels.

The conceptual layers are:

1. **Foundation:** semantic color roles, font roles, sizing constraints, control
   states, standard component names, and asset slots.
2. **Theme:** materials, ornaments, compatible drawable/font assets, palette,
   and component variant choices. Folio is the complete bundled foundation theme.
3. **Variant:** a small manifest inheriting a theme and overriding selected roles
   or assets. Midnight and Verdant are first-party examples.

Proposed built-in location: `forge-gui/res/adventure/common/themes/<id>/`.
User themes live under the existing writable Adventure user-data root in a
separate `themes/<id>/` tree, independent of campaign/save folders. Derive the
absolute path from Forge's profile APIs; never hardcode a user directory.

A versioned `theme.json` contains stable `id`, display `name`, `schemaVersion`,
optional `extends`, and sections for `colors`, `fonts`, `materials`, `components`,
and `presentation`. Use named roles such as `surface.panel`, `surface.paper`,
`text.primary`, `text.onPaper`, `border.frame`, `action.primary`,
`action.onPrimary`, `state.selected`, and explicit phase/target roles. A color
used as a fill needs a corresponding foreground role. The builder chooses exact
class and field names and documents them with a working example.

Drawable slots include panel, paper, primary/secondary button states, tab,
selection, dialog frame, medallion, minimap frame, and HUD/resource backing.
Nine/ten-patch borders stretch without distorting corners; tileable fills must
stay subtle. Theme artwork does not replace maps, enemies, cards, story images,
or campaign backgrounds unless a slot is expressly defined as UI decoration.

Inheritance merges named values by role: child overrides parent, omitted values
inherit. Resolve relative asset paths against the manifest declaring the value,
including inherited values. Bound the inheritance chain to eight manifests;
reject cycles, duplicate IDs, missing parents, malformed values, unsupported
versions, and asset paths escaping their owning theme directory. Disallow parent
traversal, absolute paths, symlink escape, remote URLs, and executable content.
For version one, user themes can extend installed built-ins or other local user
themes. Theme recipes contain presentation only, never campaign/rule data.

Validate the fully resolved result before publishing it to any live consumer.
Missing optional slots inherit from Folio. An invalid whole theme does not
partially apply: retain the active valid theme, identify the problem, and offer
Folio. A missing selected theme at launch falls back to Folio with a short notice.
Campaign appearance is the selectable compatibility option reproducing the
existing presentation; it is not a second theme named Classic.

## Existing integration and campaign compatibility

`Controls.getSkin()` currently loads `skin/ui_skin.json` through `Config`.
Scene layouts and HUD assets are separately loaded through `Config.getFile()`.
Adventure match colors already use `ADV_CLR_*` roles and conditional rendering.
Keep these integration boundaries, adding a resolved theme adapter rather than
replacing the global `FSkin` loader or decoding palette colors from sprite pixels
as the new authoring interface.

Preserve campaign resource policies and resolution for ordinary resources.
Campaign-owned layout JSON and gameplay art remain authoritative. Theme assets
may replace only declared UI presentation slots. A theme adapter supplies the
standard named styles to the resolved campaign skin while preserving unrelated
custom styles and drawable names; never blindly discard a campaign's skin.
Inventory the standard styles and directly drawn UI images before migration,
including plain `Color.BLACK` and unthemed text paths.

Provide **Campaign appearance** as the compatibility choice for
campaigns with bespoke presentation. It uses their current resource resolution,
not a generated Folio skin. Explicit Folio/Midnight/Verdant selection themes
standard controls while retaining custom campaign layout and nonstandard art.
If a campaign lacks the standard skin contract, retain its legacy presentation
for the affected screen and show a useful compatibility notice in Appearance.
Do not silently restyle arbitrary unknown custom actors.

Store `adventureThemeId` in Adventure settings, separate from the global
`UI_SKIN` preference and save data. For existing installations without that
setting, resolve Folio for standard/shared presentation and Campaign appearance
for campaigns supplying a bespoke `skin/ui_skin.json`. Missing means automatic
compatibility selection; do not persist an explicit ID until the user selects
one. Treat this migration rule as a proposed default. User selection thereafter
is explicit and persistent.
Changing campaigns does not silently replace an explicit user selection.

## Selecting and making alternate versions

Add **Appearance** inside Adventure Settings. It shows theme name, a compact
preview, and selections for Folio, Midnight, Verdant, Campaign appearance, and
user-created recipes. Describe Campaign appearance as the existing campaign
presentation. This selection must not change the Classic-mode skin.

**Customize** opens an isolated theme workshop, using the existing Adventure UI
stack. Show a component gallery and representative Adventure and duel panels,
with local sample data. Edits update this preview immediately; they must not
rebuild or mutate a live gameplay stage or active duel.

First-version editing controls are name, paired accent/foreground and
frame/foreground colors, ornament level (quiet/balanced/full), and paper texture
amount (including none). Changing preset loads that preset's defaults. Restore
reverts unsaved changes. The complete manifest supports optional replacement
art/fonts for file-based authors; an image/atlas editing application is outside
the first version.

**Save as new theme** validates and atomically creates a user theme manifest with
a new stable ID and only the intentional overrides. Do not overwrite a built-in
or an existing ID. On name collision create a distinct ID and clearly display
the resulting name. Saving does not select or apply the new theme.

**Apply after restart** records the selected ID and explains that it takes effect
after a **full Forge process restart**. Read the setting on process startup and
hold that selection for the process lifetime; returning to Adventure's start
menu or leaving/re-entering Adventure must not activate a pending selection.
Show the pending theme separately from the active theme in Appearance. Do not
restart automatically, close a run, save gameplay, or reload a live duel.
Runtime hot switching is outside version one. This activation boundary is the
designer's default to avoid unnecessary cache and scene reconstruction.
All GUI strings use localization keys with readable English defaults.

Include a short author guide and a complete starter variant: duplicate the
manifest, choose a new ID/name and parent, change named values, optionally add
assets, validate, and inspect the gallery. Provide a resource-only validation
entry point usable without loading the card database or starting a game.

## Acceptance and verification

The implementation is complete when all three themes cover the listed standard
Adventure surfaces, theme selection persists, a workshop-created variant can be
saved/reopened, and its resolved values reach both Scene2D and Adventure duels.
Verify the following with evidence appropriate to each layer:

- Focused resolver/manifest tests cover inheritance, source-relative assets,
  invalid/cyclic parents, unsupported versions, missing selected themes, valid
  and invalid contrast pairs, and rejection without partial application.
- Persistence checks prove that theme creation writes only user-theme files and
  selection writes only Adventure appearance settings. Existing saves, campaign
  content, and global `UI_SKIN` do not change.
- Lifecycle checks prove repeated previews release owned textures/fonts, shared
  cached assets are not disposed, and the theme is resolved before relevant
  screens/cache entries are constructed on the next startup.
- Visual review covers start/settings, HUD, journal/inventory, shop/rewards,
  Adventure deck management, a duel with active prompts/phase/targeting, and a
  sample dialog in all three themes. Review landscape at 1920x1080 and a compact
  existing supported resolution, plus the existing portrait layout. Check long
  translated labels, focus, unavailable actions, quiet ornament, and no texture.
- A Classic-mode control check confirms that returning from Adventure restores
  Classic colors and does not leak Adventure style state into shared widgets.
  A campaign with bespoke UI exercises the compatibility path.

Do not treat successful manifest validation as proof of visual layout, mobile
device behavior, or gameplay correctness. Use the project's applicable existing
build and test instructions during implementation; no heavy builds are needed
to approve this design.

## Boundaries and decisions left to implementation

No gameplay changes, new quests, economy, movement, mana rules, campaign content,
save migration, card frame redesign, or replacement battle interface. No new web
runtime, hosted theme marketplace, remote theme downloads, or Swing redesign.
Keep existing audio, frame-rate, and launcher settings.

The builder may choose implementation class names, exact atlas packaging,
rendering helpers, supported font assets, localized wording, and division of
work into increments. Retain the visible material distinctions and safety of
theme inheritance. If fitting the identity requires a material navigation or
layout redesign, show that change separately before implementing it.
