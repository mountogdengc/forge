# Character creation verification — November 7, 2026

This report records validation of `codex/campaign-character-creation` before integration, over baseline `3984393bc50cda2cbef20ced49b36f527bddcebb`. The portable build was produced from that uncommitted snapshot; its BUILD-INFO.json and SOURCE-CHANGES preserve the exact tested source. Subsequent commits and integration do not alter that retained build.

## Implemented

- Campaign-configurable genders, races, portrait regions, independent sprites, starter-class metadata, import permission and initial match settings; omitted catalogs inherit existing campaign/common data.
- Native Adventure creator: campaign title, arrow/keyboard choices, optional race/gender portrait matching, centered unnamed sprite, Import image and Begin journey beneath the portrait, no duplicate summary or portrait gallery.
- Four painted Barovia portrait regions; Paladin/Necromancer refer to unchanged existing campaign starter files. No shared/default deck metadata or card lists changed.
- Scene2D PNG/JPEG picker, center-square 512px import, 10 MiB/4096px bounds, embedded portrait bytes, invalid-import preservation and missing-appearance recovery.
- Class/deck chooser with real RewardActor cards and fixed preview bounds; inherited modes/editions/custom/preconstructed choices retained. Difficulty requires reconfirmation only when effective starter source changes. The confirmed Deck is used when beginning.
- Additive saved identity, independent appearance, legacy fallback and old-save handling. Preview rules, overrides, mode/color and random providers are scoped without activating a campaign or advancing its random state.

## Automated results

Clean command: `mvn.cmd -B -ntp -pl forge-gui-mobile-dev,adventure-editor -am clean verify -Drevision=2.0.16-GITCharacterCreation`, Windows JDK 26, Java 17-compatible source, 1 GiB heap and two processors. Eight reactor modules succeeded. 286 cases: 285 passed, 0 failed, 0 errors, 1 skipped. Includes 21 new character configuration/catalog/identity/image/randomness cases. Skipped: forge.adventure.theme.ThemeResolverTest.symbolicLinkEscapeRejected (Windows symbolic-link privilege unavailable).

Authoring/package/navigation command: `python -m unittest discover -s tools/adventure -p test_*.py`: 42 passed.

Final native checks compiled only the smoke fixtures against the fresh assembled production JAR, without production source overlays. Hidden/unfocused windows, offline image fetching and disposable profiles were used:

1. Landscape creator: actual stage arrows and navigation, independent choices, intersection/no-match filters, three-file single-step picker keys, PNG/JPEG crop/import, invalid import preservation, invalid sprite recovery, deck confirm/cancel/error recovery, real card geometry, foil/name/preview random-state isolation, difficulty source changes and legacy source fallthroughs. New character and initial playthrough snapshot passed.
2. Fresh-process Continue after deleting the source portrait image: saved identity, embedded portrait, sprite, deck, health and currency passed.
3. Portrait orientation and inherited Bellwake campaign preview passed without campaign activation.
4. Copied pre-update Barovia save: legacy identity remains absent, original position/life/gold/deck and original town recovery passed. Village/church/road/Tser Pool rendered with animated fog and water checks. All 174 retained source-fixture files had unchanged SHA-256 hashes.

The final review found no remaining Critical or Important issues. Its last picker finding was reproduced against the old assembly (`Picker Down must move exactly one entry`), then covered by the final passing three-file regression. Earlier failed evidence was retained.

## Evidence and playtest

Queue jobs: clean reactor `ed927abfccb546b89a3eb7f5a412f87a`; assembly-only native/authoring/package job `71714360455b420c951794bb0f084a34`. Logs, screenshots, source-preservation proof, package hash/class/resource comparison and ZIP extraction verification are under `.build-cache/character-creation/`. The portable build is under `dist/adventure-windows/character-creation-20261107/`; its BUILD-INFO.json records exact modified source hashes and packaged JAR hashes.

Launch the new build's `Start-CurseOfStrahd.cmd`. Java is bundled; use the whole folder or extract the ZIP into a new writable directory. It uses package-local test-profile/test-cache and does not import or modify normal saves. READ-ME-FIRST.txt has numbered steps, expected behavior and report-back fields. [Campaign configuration and manual checks](Character-Creation.md) are included.

The fresh portrait-orientation profile logged an existing splash-screen FSkinFont null-font fallback before recovering to English and completing the checks (also present in the earlier portrait run). This work does not validate CJK/localization font startup. Expected invalid-resource tests also log deck/appearance errors while verifying recovery.

Physical controllers, platform-specific mobile image permissions/gallery integration, audio, subjective art/readability and a complete campaign playthrough remain manual checks. Native automation verifies logical controller bindings; it does not claim physical hardware acceptance. Barovia gameplay/map/deck content was not changed by this feature.
