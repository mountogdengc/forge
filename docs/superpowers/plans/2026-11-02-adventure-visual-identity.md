# Adventure Visual Identity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Native execution is recommended; execution choice and plan review are pending.

**Goal:** Ship the approved Folio visual family for Adventure, with inheritable local themes, an isolated preview workshop, and coherent Adventure duel presentation.

**Architecture:** A resource-only resolver produces an immutable theme and diagnostics. One Adventure runtime owns its startup selection and adapts the resolved theme to Scene2D styles, declared presentation assets, and the existing Adventure-only custom renderer colors. A workshop edits a separate draft and preview; applying changes records a pending setting for a full process restart.

**Tech Stack:** Java 17, existing libGDX 1.14.2/Scene2D and TextraTypist, existing JSON support, Maven, TestNG. No additional runtime dependencies or web frontend.

**Spec:** `docs/superpowers/specs/2026-11-02-adventure-visual-identity-design.md` (approved by David on 2026-11-02).

## Global Constraints

- Adventure only, including its desktop libGDX launcher, existing mobile layouts, Adventure-owned deck management, and Adventure duels; no Swing or Classic redesign.
- "Text and controls must remain legible with all themes and with ornament and texture disabled."
- "Target at least 4.5:1 contrast for ordinary text, 3:1 for large text and essential control boundaries."
- "Bound the inheritance chain to eight manifests; reject cycles, duplicate IDs, missing parents, malformed values, unsupported versions, and asset paths escaping their owning theme directory."
- "Theme recipes contain presentation only, never campaign/rule data."
- "All current named actors referenced by scene controllers keep their IDs and behavior."
- "Do not restart automatically, close a run, save gameplay, or reload a live duel."
- "Runtime hot switching is outside version one."
- "All GUI strings use localization keys with readable English defaults."
- "No gameplay changes, new quests, economy, movement, mana rules, campaign content, save migration, card frame redesign, or replacement battle interface."
- Keep existing audio, frame-rate, launcher settings, campaign layout/resource policies, and card art. Never load the full card database just to validate or preview themes.
- The mockup demonstrates materials and components; its fictional content, Supplies counter, and replacement scenic view are not implementation requirements.
- Preserve unrelated workspace changes. Inspect the execution workspace using the worktree skill before edits; use an already isolated worktree if present. Carry both approved spec and this plan into that workspace.
- No publishing, merging, or additional commits are requested by this plan. Review diffs and checks before any later integration decision.

## Review Focus

1. A saved pending theme, edited selected manifest, or menu/mode transition cannot activate different presentation in the same process; Task 2 freezes startup catalog bytes and selection.
2. A legacy campaign with its own `skin/ui_skin.json` keeps custom controls and layouts, and incompatible screens fail back coherently; Tasks 3 and 4 exercise compatibility rather than assuming the common atlas.
3. Preview rebuilds cannot mutate cached live `Skin`, drawable, font, palette, or shared texture state; Tasks 3 and 6 check ownership and disposal.
4. Parent assets resolve against their defining manifest, with cycles, junction/symlink escape, and duplicate IDs rejected; Task 1 exercises actual temporary directories.
5. Exiting Adventure cannot leave its style state in shared Classic controls, phase colors, or progress bars; Task 5 checks both mode directions and process restart behavior.

## Verification commands

Run from the actual execution source root with JDK 17. The user-supplied five-project test queue does not include Forge. Keep Forge's own heavy runs sequential and follow any additional instructions found at execution time.

For focused TestNG tests in `forge-gui-mobile/test`, use:

```powershell
mvn -B -ntp -pl forge-gui-mobile -am test "-Dtest=<ClassNames>" "-Dsurefire.failIfNoSpecifiedTests=false"
```

Replace `<ClassNames>` with the exact comma-separated names in the task. Upstream modules still compile; missing implementation during the initial red step may be a compilation error, but unrelated dependency/configuration failure is not acceptable red evidence.

Final reactor validation, following the existing Windows Adventure build instructions:

```powershell
mvn -B -ntp -pl forge-gui-mobile-dev -am verify "-Drevision=2.0.16-GITWindowsTest"
```

Do not clean/rebuild repeatedly after passing checks without a new reason. A passing build is not visual acceptance. Use the native disposable-profile harness patterns under `forge-gui-mobile-dev/test-unit/forge/app/` and `tools/adventure/run_bellwake_smoke.ps1` for graphical probes. Never use host desktop control for routine testing.

## File structure and responsibilities

New package: `forge-gui-mobile/src/forge/adventure/theme/`.

| File | Responsibility |
| --- | --- |
| `ThemeManifest.java` | JSON model and stable version-one role/slot declarations |
| `ResolvedTheme.java` | Immutable merged values and defining-directory asset references |
| `ThemeDiagnostic.java` | File/role-specific validation problems for CLI and GUI |
| `ThemeCatalog.java` | Snapshot built-in and user manifests, reject duplicate IDs |
| `ThemeResolver.java` | Inheritance, safe assets, complete semantic validation |
| `ThemeValidationMain.java` | Resource-only validation CLI |
| `ThemeStore.java` | Atomic save-as-new variants under user Adventure themes |
| `AdventureThemeRuntime.java` | Freeze process-start selection/catalog, expose active/pending state |
| `ThemeSkinAdapter.java` | Produce owned Scene2D style copies without mutating legacy skins |
| `ThemeDrawable.java` | Scalable native material/border/ornament drawing and optional asset slots |
| `ThemePresentation.java` | Explicit allowlist from existing UI actor/image roles to theme slots |
| `ThemePaletteBridge.java` | Map resolved roles to Adventure-only FSkin roles |
| `ThemeDraft.java` | Separate workshop draft, restore, validation, minimal overrides |
| `ThemePreview.java` | Owned component/Adventure/duel sample actors and asset lifecycle |

New scenes: `forge-gui-mobile/src/forge/adventure/scene/AppearanceScene.java` and `ThemeWorkshopScene.java`.
Matching tests live in `forge-gui-mobile/test/forge/adventure/theme/`.
Built-ins live in `forge-gui/res/adventure/common/themes/folio/`, `midnight/`, and `verdant/`.
Author documentation: `docs/Adventure/Themes.md` and `docs/Adventure/Theme-Validation.md`.

## Task 1: Resource-only manifest resolver and validation

**Files:** Create `ThemeManifest.java`, `ResolvedTheme.java`, `ThemeDiagnostic.java`, `ThemeCatalog.java`, `ThemeResolver.java`, and `ThemeValidationMain.java` in the new package. Create `ThemeResolverTest.java`.

**Interfaces:**

- `ThemeCatalog.snapshot(Path builtInRoot, Path userRoot)` returns an immutable catalog of manifest bytes, declaration roots, and catalog diagnostics; absent user root is empty, absent built-in Folio is a fatal installation diagnostic.
- `ThemeResolver.resolve(ThemeCatalog catalog, String id)` returns `ResolvedTheme` or throws `IllegalArgumentException` containing actionable diagnostics; callers never receive partial themes.
- `ResolvedTheme` exposes `id()`, `name()`, `colors()` (`Map<String,String>`), `assets()` (`Map<String,Path>`), `fonts()`, `components()`, and `presentation()`. Collections are immutable. Asset paths are validated, normalized, source-relative real paths.
- `ThemeValidationMain.main(String[] args)` accepts `--builtins <root> --users <root> --theme <id>`; returns exit zero only for a fully valid theme and prints diagnostic file/role on failure. It never initializes Forge, Gdx, or StaticData.

- [ ] Write failing TestNG tests: `childOverridesOneRoleAndInheritsTheRest`, `inheritedAssetKeepsParentDirectory`, `eightLevelsAllowedNinthRejected`, `cycleMissingParentAndDuplicateIdsRejected`, `invalidVersionAndUnknownGameplaySectionsRejected`, `assetTraversalAbsoluteUrlAndLinkEscapeRejected`, `immutableCatalogIgnoresLaterManifestEdits`, `contrastChecksAllDeclaredForegroundPairs`, and `rejectedThemeCannotExposePartialValues`.
- [ ] Run focused `ThemeResolverTest` and verify failures concern the missing resolver, not the build environment.
- [ ] Implement version-one manifests with `id`, `name`, `schemaVersion:1`, `extends`, `colors`, `fonts`, `materials`, `components`, and `presentation`. IDs match `[a-z][a-z0-9._-]{0,79}`; reserved ID `campaign` is not an authored theme. Use standard strict JSON for recipes. Unknown root sections fail validation so rule data cannot accidentally enter recipes.
- [ ] Define named color roles and required foreground pairs centrally. Accept opaque `#RRGGBB` colors; use renderer alpha for effects. Required surface pairs include panel/text, paper/onPaper, primary/onPrimary, secondary/onSecondary, selection/onSelected, disabled/onDisabled, phase labels, and frame/frameText. Use WCAG relative luminance, 4.5:1 ordinary text and 3:1 essential boundaries. Optional missing asset slots use Folio; explicit invalid asset references reject the theme.
- [ ] Preserve declaration provenance when merging maps. Resolve all asset dependencies, including atlas pages and bitmap font pages, inside the declaring theme root; permit standard existing common fonts only through named built-in font roles, not unrestricted filesystem paths. Validate symlink/junction real paths and chain length. Check manifest and referenced asset existence without loading textures or card data.
- [ ] Run `ThemeResolverTest`; require zero failures. Verify a malformed CLI input exits nonzero with a path/role diagnostic and without creating user folders.

## Task 2: User storage, process-start selection, and pending settings

**Files:** Create `ThemeStore.java`, `AdventureThemeRuntime.java`, `ThemeStoreTest.java`, and `AdventureThemeRuntimeTest.java`. Modify `forge-gui-mobile/src/forge/adventure/data/SettingData.java`, `forge-gui-mobile/src/forge/adventure/util/Config.java`, and `forge-gui-mobile/src/forge/Forge.java`.

**Interfaces:**

- `ThemeStore.saveAsNew(Path userRoot, ThemeCatalog catalog, ThemeManifest variant)` returns the saved manifest path with a new `user.<UUID>` ID; use a fresh directory and atomic manifest publication. Built-ins and existing theme directories are never rewritten. Keep user display names intact and disambiguate identical display names in the chooser with a suffix.
- `AdventureThemeRuntime.initialize(Path builtins, Path users, String startupId)` is idempotent for a process and captures manifest bytes/catalog once. It performs no game-save writes.
- `AdventureThemeRuntime.resolveActive(Config campaignConfig)` resolves the startup ID; missing ID selects Folio for common presentation or `campaign` for a campaign-owned `skin/ui_skin.json`. Explicit choices remain fixed across campaign changes. Legacy automatic compatibility can resolve per campaign, using the frozen startup catalog.
- `AdventureThemeRuntime.activeId()`, `pendingId()`, `requestSelection(String id)`, and `diagnostics()` expose active/pending choice. `requestSelection` validates against the current discovered catalog and persists only `SettingData.adventureThemeId` through existing Adventure settings serialization. It does not replace the startup catalog/selection.

- [ ] Write failing tests: `saveAsNewWritesOnlyThemeFolder`, `duplicateDisplayNameCannotOverwriteTheme`, `failedValidationLeavesNoPublishedTheme`, `startupChoiceSurvivesMenuAndModeTransitions`, `savingASelectedRecipeCannotChangeItsActiveBytes`, `newThemeSelectableButPendingUntilNewRuntime`, `missingStartupSelectionFallsBackWithNotice`, `automaticCompatibilityDoesNotBecomeExplicitPreference`, and `selectionPreservesUnrelatedSettingsAndSaveBytes`.
- [ ] Run focused `ThemeStoreTest,AdventureThemeRuntimeTest` and inspect red evidence.
- [ ] Add nullable `String adventureThemeId` to `SettingData`. Use `ForgeConstants.USER_ADVENTURE_DIR` plus `themes/` for user root; built-in root derives from `Config` common resources. Initialize runtime before Adventure skin/HUD construction, using an explicit settings read that does not eagerly initialize gameplay/card state. The implementation may add a small `Config` path/settings accessor to avoid constructor recursion.
- [ ] Keep a separate explicit pending ID, including when the requested ID equals active. Store new-theme recipes immediately but keep active resolved manifests and owned live resources unchanged. A full process restart captures new selection and catalog bytes. Switching modes or calling campaign resets must not reinitialize the startup snapshot.
- [ ] Run focused tests; assert unchanged `UI_SKIN`, campaign files, saves, audio/frame-rate settings, and unrelated settings fields. Exercise persistence with temporary profile roots, never the user's profile.

## Task 3: Scene2D materials, styles, and ownership

**Files:** Create `ThemeSkinAdapter.java`, `ThemeDrawable.java`, `ThemeSkinAdapterTest.java`, and `ThemeOwnershipTest.java`. Modify `forge-gui-mobile/src/forge/adventure/util/Controls.java`.

**Interfaces:**

- `ThemeSkinAdapter.adapt(Skin legacy, ResolvedTheme theme)` returns an owned skin/view wrapper that copies standard styles while borrowing legacy resources. Its disposal releases only newly owned resources.
- `ThemeDrawable` implements Scene2D `Drawable` using scalable border/fill rendering or a validated optional nine/ten-patch asset. Native rendering handles quiet/balanced/full ornament and no/subtle/weathered paper texture without editing source images.
- `Controls.getSkin()` continues to return `Skin`, delegating to the active adapter in themed Adventure and the unchanged legacy resource skin in `campaign` mode. Cache by campaign skin identity and active theme identity, not by a mutable workshop draft.

- [ ] Write failing tests: `adaptingCannotMutateLegacyStyleOrFont`, `customStyleNamesRemainAvailable`, `missingOptionalSlotsUseFolio`, `missingRequiredLegacyContractReturnsLegacySkinAndNotice`, and `disposingAdapterDoesNotDisposeBorrowedFontOrTexture`.
- [ ] Run focused tests. Use instrumented ownership abstractions for resource lifetimes; tests must not claim OpenGL rendering from mocked drawables.
- [ ] Inventory `ui_skin.json` standard styles: text buttons (including Textra), labels, windows, dialogs, scroll panes/paper, select boxes, text fields, checkboxes, sliders, item frames, selectors/arrows, and touchpad. Copy style objects and reference fonts through named roles. Maintain existing min sizes/content insets; decoration cannot alter hit regions.
- [ ] Implement `ThemeDrawable` so frame corners and pattern fills scale independently. Folio gets round bronze/glass details; Midnight gets silver angular details; Verdant gets botanical/wood details. Keep shape distinct in quiet mode while removing extra ornament. Provide clearly differentiated focus, selected, pressed, hovered, and unavailable states.
- [ ] Reuse existing packaged body fonts and their glyph fallbacks. Font role overrides may load an owned theme font but must not mutate the shared default font's scale/color. Preserve Textra inline mana/key/item glyph support.
- [ ] Run focused tests. Check adapter/cache retirement against `Forge.retireAdventureResource` and existing campaign scene reset behavior; release adapters only when no live scene uses them.

## Task 4: First-party themes and Adventure screen coverage

**Files:** Create `ThemePresentation.java`, `ThemePresentationTest.java`, `BundledThemeTest.java`, the three built-in `theme.json` manifests, and any required original UI-only material assets beneath their theme roots. Modify `UIActor.java`, `stage/GameHUD.java`, `scene/UIScene.java`, and affected Adventure scene classes that directly draw presentation. Do not rewrite campaign UI JSON.

**Interfaces:**

- `ThemePresentation.drawableFor(String uiPath, String actorName, String legacyAsset, Skin adaptedSkin)` returns an optional theme drawable only for centrally declared UI-only slots; no match means retain the original asset.
- `ThemePresentation.style(Actor actor, String uiPath, String actorName)` applies declared standard foreground/header roles without changing bounds, actor names, listeners, or navigation semantics. Unknown actors remain intact.

- [ ] Write failing tests: `allBundledThemesResolveAndPassContrast`, `variantsHaveDifferentShapesAndMaterials`, `presentationCannotReplaceMapActorCardOrStoryArt`, `actorNamesBoundsAndSelectionOrderPreserved`, and `bespokeCampaignRetainsUnknownDrawablesAndLayout`.
- [ ] Run focused `ThemePresentationTest,BundledThemeTest`.
- [ ] Author complete Folio and inheriting Midnight/Verdant manifests. Start from the approved study's visual relationships; adjust exact hex values where needed for the required contrast. Theme manifests expose no gameplay switches. Preserve existing gameplay icon meanings; use original scalable borders/ornaments rather than copied Shandalar art.
- [ ] Add the explicit presentation mapping to UIActor image/button/label construction after legacy properties are read. Audit start/setup/save-load/settings, HUD/minimap/notifications, inventory/items/journal/statistics, market/inn/events/rewards, and dialogs. Map known standard chrome; preserve campaign-specific images and all actor IDs. Record every covered screen/asset slot in `Theme-Validation.md` rather than assuming skin adaptation covers baked-in images.
- [ ] Replace relevant hardcoded UI text/background colors with semantic roles, including the HUD's enemy counter and standard paper labels. Preserve existing critical-life/quest state cues with text/symbols and contrast; no new resources or status mechanics.
- [ ] Verify portrait and landscape named actors match pre-change definitions. Add no fixed desktop sidebar; keep current responsive layouts and touchpad area.
- [ ] Run focused tests and inspect diffs to confirm maps, campaign content, rewards, decks, save formats, and card assets are unchanged.

## Task 5: Adventure duel and deck palette bridge

**Files:** Create `ThemePaletteBridge.java` and `ThemePaletteBridgeTest.java`. Modify `forge-gui-mobile/src/forge/assets/FSkinColor.java`, `assets/FSkin.java`, `toolbox/FProgressBar.java`, and the existing Adventure-aware controls in `screens/match/` and Adventure-owned deck management.

**Interfaces:**

- `ThemePaletteBridge.adventureColor(FSkinColor.Colors role, Color legacy)` returns a mapped active Adventure color only for `ADV_CLR_*` roles; in compatibility mode it returns legacy unchanged.
- `ThemePaletteBridge.presentationForAdventure()` exposes drawables/roles for custom-rendered Adventure widgets, using the same resolved theme as Scene2D. Classic callers continue using their original `CLR_*` and global skin assets.

- [ ] Write failing tests: `adventureRolesMapToTheResolvedTheme`, `classicRolesAndLegacyModeRemainUnchanged`, `phaseStatesRemainDistinctAndReadable`, `progressBarsUseActiveThemeInsteadOfStaleSpritePixels`, and `enterExitAdventureDoesNotMutateClassicPalette`.
- [ ] Run focused `ThemePaletteBridgeTest`.
- [ ] Map all existing `ADV_CLR_*` surface/text/phase/target roles explicitly. Apply after legacy sprite colors load and whenever their derived-color cache legitimately refreshes; do not change global `UI_SKIN` or override `CLR_*` values. Eliminate unthemed Adventure progress-bar pixel sampling where it would overwrite the bridge.
- [ ] Audit Adventure `MatchScreen`, `VPrompt`, `VPhaseIndicator`, `VPlayerPanel`, `VManaPool`, `VLog`, targeting/menu/help/win-lose controls, and Adventure deck entry routes. Theme their chrome and text with mode guards, preserving existing long-press phase/yield behavior, Help guidance, card rendering, layout, targets, and background source/day-night preferences.
- [ ] Run focused tests. Exercise Classic → Adventure → Classic in a disposable native harness; assert Classic colors/control states before and after match. Record any deliberately unthemed card/art surfaces.

## Task 6: Appearance chooser and isolated theme workshop

**Files:** Create `ThemeDraft.java`, `ThemePreview.java`, `ThemeDraftTest.java`, `ThemePreviewTest.java`, `scene/AppearanceScene.java`, and `scene/ThemeWorkshopScene.java`. Modify `scene/SettingsScene.java` and `forge-gui/res/languages/en-US.properties`. Add common portrait/landscape UI definitions only if the existing scene framework needs them; name new definitions `ui/appearance*.json` and `ui/theme_workshop*.json`.

**Interfaces:**

- `ThemeDraft.from(ThemeCatalog catalog, String baseId)` creates isolated editable data. `setColor(String role,String value)`, `setOrnament(String level)`, `setTexture(float amount)`, `setName(String name)`, `restore()`, `validate()`, and `toVariant(String newId)` implement edits and minimal inheritance overrides.
- `ThemePreview.rebuild(ThemeDraft draft, PreviewPage page)` constructs isolated component, Adventure HUD/dialog, and duel-chrome sample actors; `dispose()` releases preview-owned resources. `PreviewPage` values are `COMPONENTS`, `ADVENTURE`, and `DUEL`.
- `AppearanceScene` lists built-ins, `campaign`, and valid user themes, presents active/pending/compatibility diagnostics, and calls `requestSelection` only from **Apply after restart**. `ThemeWorkshopScene` can save but cannot apply a theme implicitly.

- [ ] Write failing tests: `minimalVariantContainsOnlyIntentionalOverrides`, `restoringOrSwitchingPresetDropsUnsavedChanges`, `invalidColorPairBlocksSaveWithRoleMessage`, `saveDoesNotSelectOrApply`, `previewDoesNotTouchActiveRuntimeOrCardDatabase`, `repeatedPreviewRebuildReleasesOnlyOwnedResources`, and `applyUpdatesPendingButNotActiveTheme`.
- [ ] Run focused `ThemeDraftTest,ThemePreviewTest`.
- [ ] Add a localized Appearance button in existing Settings with controller/keyboard/touch selection support. List active and pending names separately, including an explicit full-process-restart explanation. Do not offer a forced restart action.
- [ ] Build the workshop with name, paired accent/foreground and frame/frame-text color controls, quiet/balanced/full ornament, and texture amount 0..0.35. Native color entry must accept typed hex and show a swatch/validation message; provide focusable controls rather than assuming a desktop-only system color dialog. Keep all primary actions visible in portrait.
- [ ] Rebuild only isolated preview styles/drawables on valid edits. Show local representative data using existing resources (life, gold, shards, keys); no encounter actions, game loads, or card database lookup. Display the draft validation problem and retain the last valid preview when edits are invalid.
- [ ] Wire Restore, Save as new theme, Back, and Apply after restart semantics. Save variants using Task 2, refresh the selectable catalog for pending choices without changing the process-start active catalog. Handle duplicate names, missing built-ins, and filesystem failures inline without closing the workshop.
- [ ] Run focused tests and native probes for preview switching, saving/reopening a variant, pending state, Escape/Back, keyboard/controller focus, long labels, portrait, and texture/ornament disabled.

## Task 7: Author guide, validation entry point, and complete acceptance evidence

**Files:** Create `docs/Adventure/Themes.md`, `docs/Adventure/Theme-Validation.md`, a small starter variant under `docs/Adventure/theme-examples/moonlit-folio/theme.json`, and `tools/adventure/run_theme_smoke.ps1`. Add a native fixture `forge-gui-mobile-dev/test-unit/forge/app/AdventureThemeSmoke.java` following existing disposable-profile harness patterns; modify the mobile-dev POM only if needed to include the fixture in its existing native test mechanism.

**Interfaces:**

- Native smoke accepts explicit source root, Java home, output directory, theme ID, dimensions, and disposable profile root. It records source identity, resolved theme IDs, assertions, captures, exit code, and paths. It never attaches to the user's running game or opens saves from a normal profile.
- Author guide documents the actual manifest roles/slots, eight-level bound, source-relative assets, validation CLI, and copy → new ID/name → overrides → validate → workshop → select → restart workflow.

- [ ] Document the actual public format with one complete working minimal inherited example, clear supported assets/font roles, and the distinction between browser-study recipes and installed runtime themes. Include Campaign appearance compatibility and active/pending rules.
- [ ] Run the resource-only CLI for each built-in and the starter example, plus malformed input. Capture diagnostics/exit codes; prove this does not initialize card data or write a profile.
- [ ] Run the focused theme suites together, then the sequential final Adventure reactor `verify` command above. Record actual counts, failures, errors, skips, and any unresolved build/environment issues; do not infer passing from incomplete output.
- [ ] Use the native fixture to capture start/settings, HUD, journal/inventory, shop/rewards, deck management, duel prompt/phase/targeting, and dialog surfaces in each theme at 1920x1080 and 1280x720, plus a supported portrait size. Include long localized labels, all action states, no texture, and quiet ornament. Inspect captures and repair clipping, illegible text, or inconsistent chrome before claiming visual completion.
- [ ] Verify full-process persistence by saving a new variant, selecting it pending, checking mode transitions do not apply it, then starting a second disposable process. Verify bespoke campaign appearance and a Classic control comparison.
- [ ] Check source diff and recorded bytes for gameplay/campaign/save/global-skin invariants. Review the completed implementation against every approved spec section.
- [ ] Defer only device-specific physical controller/touch acceptance that the fixture cannot prove. If needed, supply exact build/launch prerequisites, numbered actions, expected outcomes, report-back fields, and dependent work. A native screenshot or desktop simulation is not proof of an Android/iOS device pass.

## Plan review and execution

All approved spec sections have an owning task: visual roles and coverage (3–5), resolver/security/inheritance (1), compatibility and startup/pending (2–4), theme authoring/preview (2 and 6), localization/accessibility (3–7), and acceptance/guide (7). No implementation step requires gameplay changes or a new frontend.

Native execution is recommended because the adapters, resource ownership, startup snapshot, and preview lifecycle share tightly coupled interfaces. The implementer should carry all tasks through verification before reporting completion, and request one independent whole-change review after implementation. The writing-plans workflow requires the user to review this plan and choose an execution method before implementation starts.
