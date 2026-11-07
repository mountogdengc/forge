# Adventure themes

Adventure has three related identities: **Planeswalker's Folio** (bronze, teal,
parchment), **Midnight Grimoire** (silver and amethyst), and **Verdant Sanctuary**
(wood and jade). They use original, scalable UI decoration over Forge's existing
Adventure screens. Maps, cards, campaign art, layouts, and game rules stay under
the campaign's existing resource policy.

Open Adventure **Settings → Appearance**. The screen shows the active theme and
the selection for the next launch separately. **Apply after restart** saves the
choice; close and reopen the entire Forge application to activate it. Returning
to the start menu or switching through Classic does not activate the choice.
Classic's skin preference is separate.

**Campaign appearance** uses the campaign's original skin. With no saved choice,
standard campaigns use Folio, while campaigns supplying their own
`skin/ui_skin.json` retain their original appearance. Explicit theme choices
persist across campaign changes. Campaigns without standard default label,
button, and font styles keep their original skin and report a compatibility
message. An invalid selection falls back to Folio, then campaign appearance if
Folio cannot load.

## Make a version in Forge

1. Choose a preset and press **Customize**.
2. Edit the name, jewel/foreground and frame/foreground color pairs. Hex colors
   are opaque `#RRGGBB`. Swatches show the entered colors.
3. Choose Quiet, Balanced, or Full ornament and 0–35% paper texture. Components,
   Adventure, and Duel tabs use local sample data. Scroll a compact preview or
   form to see its remaining content.
4. Validation explains unreadable color pairs. Invalid edits leave the last
   valid preview visible and disable saving. **Restore** discards unsaved edits.
5. **Save as new theme** creates a new recipe with a unique `user.<UUID>` ID.
   Existing recipes are never overwritten. Saving does not select the result.
6. Return to Appearance, select the result, press **Apply after restart**, then
   restart Forge when ready. No run is saved, closed, or reloaded by these actions.

## Make a version with files

Installed recipes live in `res/adventure/common/themes/<folder>/theme.json`.
Local recipes live in `<Forge user directory>/adventure/themes/<folder>/theme.json`
(normally `%APPDATA%/Forge/adventure/themes` on Windows). The folder can also
contain its artwork and fonts. Discovery reads immediate child folders only.

Copy [the complete starter](theme-examples/moonlit-folio/theme.json) into a new
user-theme folder, choose a unique ID and name, and change only desired roles:

```json
{
  "schemaVersion": 1,
  "id": "user.moonlit-folio",
  "name": "Moonlit Folio",
  "extends": "midnight",
  "colors": { "surface.paper": "#e2dfeb", "text.onPaper": "#252131" },
  "presentation": { "ornament": "quiet", "paperTexture": 0 }
}
```

Validate using [Theme validation](Theme-Validation.md), inspect it in Customize,
then select it and restart. Browser concept studies are design references;
their downloaded JSON is not an installed runtime recipe. Use the schema above.

## Recipe format, version 1

Required fields are `schemaVersion: 1`, `id`, and `name`. IDs contain lowercase
letters, digits, dots, underscores, and hyphens, start with a letter, and are at
most 80 characters. `campaign` is reserved. Names are at most 120 characters.
Use `user.` for personal IDs. Duplicate IDs are rejected rather than overriding
an installed theme.

`extends` names an installed or local recipe. Omitted values inherit by role;
an omitted parent defaults to Folio, except Folio itself. A chain can contain
at most eight manifests, including Folio. Cycles and missing parents fail.
Unknown fields, unknown roles, unsupported versions, and malformed JSON fail.
Only presentation data belongs in a theme.

| Section | Supported roles or values |
| --- | --- |
| `colors` | All roles below, opaque `#RRGGBB` strings |
| `fonts` | `body`, `heading`: `legacy:default` or a local `.fnt`, `.ttf`, `.otf` file |
| `materials` | Slots below: local `.png`, `.jpg`, `.jpeg`, or `.atlas` artwork |
| `components` | `shape`: `round`, `angular`, or `leaf` |
| `presentation` | `ornament`: `quiet`, `balanced`, or `full`; `paperTexture`: number from 0 to 0.35 |

Color roles:

| Group | Roles |
| --- | --- |
| Surfaces | `surface.canvas`, `surface.panel`, `surface.paper` |
| Text | `text.primary`, `text.secondary`, `text.onPaper` |
| Frames | `border.frame`, `border.frameText`, `border.highlight` |
| Actions | `action.primary`, `action.onPrimary`, `action.secondary`, `action.onSecondary` |
| States | `state.hover`, `state.selected`, `state.onSelected`, `state.disabled`, `state.onDisabled`, `state.focus` |
| Phases | `phase.activeEnabled`, `phase.activeDisabled`, `phase.inactiveEnabled`, `phase.inactiveDisabled` |
| Targets | `target.normal`, `target.combat`, `target.planeswalker` |

Ordinary foreground/background pairs must reach 4.5:1 contrast; frame and focus
boundaries against the panel must reach 3:1. Phase foregrounds use
`action.onPrimary` for active/enabled, `text.onPaper` for active/disabled, and
`text.primary` for inactive phases. Change corresponding foregrounds when
changing those fills. The workshop synchronizes jewel selection/active phase
colors and their selected foreground.

Material slots are `panel`, `paper`, `button`, `buttonPressed`, `buttonHover`,
`buttonDisabled`, `buttonPrimary`, `tab`, `selection`, `dialog`, `medallion`,
`minimap`, `hud`, `resource`, and `glass`. Omitted slots use scalable native
decoration. Atlas regions use the slot name, or the first region if absent.
Use low-contrast artwork behind text. Materials are UI decoration, never map,
story, enemy, or card replacement slots. Named slots are available to standard
styles and explicit HUD mappings; arbitrary campaign actors are preserved.

Asset paths are relative to the theme that declares them, including inherited
assets. Atlas and bitmap-font image pages must remain inside that same folder.
Absolute paths, `..`, URLs, executable formats, and symbolic-link/junction
escapes are rejected. Validation checks file existence and dependency paths;
actual image/font decoding is checked when the native preview loads. Replacement
fonts that lack the original localized glyph coverage fall back to the original
font with an author-facing notice. A native
decode failure rejects the active recipe for the process and uses the fallback.
Changes to active manifest values take effect on the next process startup.
Do not edit or remove active asset files while Forge is open.

The skin adapter owns its style/font copies and optional assets; legacy campaign
resources are borrowed. Custom campaign style names and actor bounds are kept.
The Adventure duel/deck palette consumes the same resolved semantic colors as
Scene2D. Theme files never write campaign content, global skin settings, or saves.

## Version-one polish limits

Atlas split metadata is not used for authored material artwork; it scales as a
whole region, while the native border keeps its own thickness. Prefer simple
fills over artwork with elaborate fixed corners. The Active label can pick up
an edited file's display name before restart even though its visual recipe is
still the startup snapshot. Same-name choices show ID suffixes, but the save
confirmation currently shows only the name.
