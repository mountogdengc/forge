# Campaign-first card loading implementation plan

> **For agentic workers:** Use superpowers:executing-plans; independent cache and campaign/editor units may use superpowers:dispatching-parallel-agents. Follow tests before implementation and a fresh final review.

**Goal:** Measure and reduce Windows Bellwake startup work with persistent safe indexing, on-demand card rules and campaign set selection, preserving card availability and other game modes.

**Architecture:** Keep one shared metadata catalog and lazy rule/printing lookup. Persist only a validated name-to-source index, never live game objects. Adventure startup defers unused rules; campaign pools preload selected edition names only when a complete pool is required. Ordinary full-catalog APIs retain complete results. Campaign authors select edition codes in the existing editor.

**Tech Stack:** Java 17 source, existing Maven/TestNG/libGDX/Swing tools; no runtime dependency added.

**Spec:** User request in this chat, November 1, 2026: measure first, cache, load Bellwake's cards first without breaking the rest, select allowed sets per campaign. User explicitly chose to preserve Bellwake's current availability.

## Constraints and review focus

- Keep Bellwake content version 0.3.0, existing saves, authored decks and rewards. Do not narrow allowed editions or silently drop collected cards.
- Preserve Classic/Quest/full deck tools, complete collection enumeration, custom cards, alternate/placeholder/split names, tokens and explicit saved printings.
- Invalid/stale/unwritable caches must fall back safely. Validate inventory, parser/schema identity and bounded payload integrity; no Java object deserialization or cached arbitrary paths.
- Campaign pool legality must not depend on which other campaign loaded cards first. Empty allowed-set selection retains existing unrestricted behavior; existing restriction precedence is retained.
- Windows desktop Adventure may defer rules; retain Android/iOS's current eager behavior. Provide a force-eager diagnostic fallback.
- Use capped isolated measurement processes (2 GB heap, four logical CPUs) and sequential heavy jobs; check commit headroom before a benchmark. No normal-profile writes or host desktop control.
- Do not confuse cold-index build, warm persisted-index reuse, OS cache state, card-database setup and whole-client startup. Report actual measured scopes and counts, not an unmeasured speedup claim.

## Task 1: Establish the unchanged baseline

- [x] Add headless `tools/adventure/CardLoadingBenchmark.java`, initialize the same language/type services, measure with monotonic milliseconds, count loaded rules/printings without forcing lazy enumeration.
- [x] Run unchanged shipped JAR eager/lazy in isolated capped processes; reject failures and retain setup logs.
- [x] Add hidden-window menu-ready measurement for the old and new clients, with no input capture and isolated profiles. Final cached median 12.105 s vs old 25.842 s; explicit converted-font cache seed used equally for both.

Baseline job `90d42c858dc34270be026aee5adaf3a1`: eager 18,091 ms total, 33,610 common rules / 97,271 printings, peak heap-pool usage 365 MiB; existing lazy 8,489 ms, including 6,137 ms parsing its name index, nine named lookups afterward, peak 134 MiB. These are first sequential samples under a CPU cap; repeat comparable runs before claiming final improvement. JSON/logs: `.superpowers/card-loading/baseline/`.

## Task 2: Cache the lazy name index

- [x] Test cold/warm parity, zero warm parses, inventory edits/add/delete, archive CRC changes, corrupt/read-only cache, face aliases and precedence; observe RED against the shipped reader.
- [x] Add `CardStorageReader(String, ProgressObserver, boolean, Path)` and a bounded checksummed binary index helper. Existing three-argument constructor remains compatible. Expose cache status/parse-count diagnostics.
- [x] Preserve existing source/parser semantics and fix seconds mislabeled as milliseconds. Verify the focused suite before integration (six focused checks passed).

## Task 3: Preserve complete APIs while enabling deferred startup

- [x] Test eager/lazy complete catalog parity and partial explicit-print lookup followed by full enumeration; named lookups must retain preferred-print and unknown-card behavior.
- [x] Root owns `StaticData`, `CardDb`, `FModel` and a pure startup policy. Add `StaticData.preloadEditionCards(Collection<String>)`, explicit `CardDb.getLoadedUniqueCards()` / `getLoadedAllCards()` snapshots, and complete ordinary enumeration fallback.
- [x] Default to deferred scripts for Windows libGDX Adventure startup; retain other startup defaults and mobile eager behavior. Supply the per-profile card-index cache directory and `-Dforge.cards.forceEager=true` fallback. Log the chosen mode and cache diagnostics.
- [x] Keep first-use loading for named chapter cards and all rules needed by saved decks, enemies and effects. Retain targeted authored loading; verify its counts natively below.

## Task 4: Campaign pools and authoring

- [x] Remove eager whole reward-pool setup from `Config.loadResources`; fixed cards/gold/items must not initialize it. Preserve custom-card exclusion, campaign/player-mode invalidation and unrestricted fallback.
- [x] For explicit `allowedEditions`, preload those editions and use loaded snapshots plus the existing allowed-print selection; preserve full pool behavior when no whitelist is configured.
- [x] Add an allowed-sets panel to `adventure-editor`, using edition metadata only. Roundtrip the original config JSON, preserve unknown fields/restrictions and absent selected codes, and do not change sibling campaign files.
- [x] Verify editor roundtrip (two focused checks) and campaign switching/fixed rewards/unrestricted completeness (three focused checks); full reactor/native regression pending.

Ruling: retain named first-use loading for authored starter/enemy/reward cards rather than speculatively opening every deck at the menu. Bellwake's existing fixed card paths naturally request the chapter's cards first; no legality changes or chapter-specific parser are needed. The native run will verify the deferred rule count through character creation and the first duel.

Core focused evidence: shipped JAR fails whole-catalog completeness (one printing instead of four) and lacks explicit snapshot APIs. Changed core passes the two tiny real-catalog checks. Startup policy: two RED (class missing) then two GREEN tests. Additional independent-first-use collection API cases added before the full gate. Language initialization was corrected in the fixture; no production workaround was introduced.

Fresh review found named scripts lacking an edition entry could not receive their UNKNOWN/upcoming fallback until full enumeration. Confirmed RED in a four-script fixture, extracted the existing fallback operation for a single rules object, and invoke it after named lookup has searched all editions. All nine core cases GREEN; a separate fixed reward/saved UNKNOWN printing case was added to the campaign suite. Explicit valid-set partial-print behavior remains unchanged. Final-source clean reactor run required after this correction.

Ruling: clearing the editor whitelist removes `allowedEditions` and sets its in-memory value to null, instead of persisting `[]`. Some legacy event/SpellSmith consumers interpret any non-null array as a whitelist; removal restores the exact original unrestricted semantics without changing those consumers. Updated roundtrip test failed against the empty-array writer before the correction. This editor change was made before the clean reactor reaches that module.

Valid old-client hidden Bellwake menu baseline: 30,898 ms main-to-StartScene, 25,175 ms to afterDBloaded, 33,610 loaded common rules, 97,788 printings, 1,163 MiB summed peak heap pools. Evidence `.superpowers/card-loading/startup-old-native-bellwake-1/run-1/`. Earlier sandbox/incorrect-CWD startup attempts failed and are excluded. Correct harness uses a resource junction into the pinned package, disposable working directory/profile/cache, no audio, no input or focus, 2 GB heap/four processors, bounded lifetime.

Native integration finding: initial optimized candidate still materialized 33,610 rules at the menu. `Forge.loadAdventureResources` explicitly preloaded `SpellSmithScene.loadEditions`, which requests the random acquisition pool. Removed that startup preload; the existing smith `enter()` already initializes it before use. Added native `RequireDeferred` assertions. Startup candidate recheck pending; do not present the earlier full-load candidate as optimized. Font PNG generation is a separate startup cost; paired menu comparisons can seed the same converted-font cache for both clients, recorded separately from card-index state.

Affected follow-up job `469d0718c1284a6982bf5c9583eb5429` failed in an existing asynchronous save test: its ten-second worker completion timed out, then cleanup found the still-writing metadata file open. Prior clean whole reactor passed that test. No save production code changed; rerunning the affected reactor with a fresh workspace temp directory, preserving the failed log. Root also identified that index parse errors must prevent persistence of a partial index; focused cache regression/correction delegated before final validation.

Final stable-source reactor job `0b30a039e0054e899e4585f2003ac52d` passed 1,443 cases with zero failures/errors and six existing optional desktop skips; the save test passed. It follows the clean whole-reactor run and rebuilds changed files normally. A concurrent intermediate run `f2e64b75ffbd46a48659dd7fa279f191` correctly failed the new cache-recovery test before its implementation was compiled. All seven focused cache cases passed after the incomplete-build guard. No failures are discarded from evidence.

Final actual menu and first-duel proof: old median 25.842 s; final cold index 17.173 s (one sample); final cached median 12.105 s. Both clients use the same converted-font seed and fresh profiles; final menus have zero common rules loaded, index warm runs have zero script parses. Native `dist/bellwake-smoke/card-loading-help` passed menu 0/starter 43/first duel 43 loaded unique cards, actual private draws/three Help dialogs, and practice loss without penalties. Final full story/save/package validation remains below.

## Task 5: Verify and hand off

- [x] Run affected core, game, GUI, Adventure, editor and desktop regressions sequentially; clean job `5ef7207356394b2ca08293147abec108`: 1,442 cases, zero failures/errors, six existing optional desktop skips.
- [x] Repeat eager/deferred cold/warm benchmarks with the same cap/data and report medians, counts and memory. Final database median 1.282 s cached vs 2.673 s old eager; cold index 4.138 s. Full counts match 97,271 printings/33,223 unique cards. Menu results are reported separately in Card-Loading.md.
- [x] Fresh review of cache trust/invalidation, full-catalog fallback, campaign legality, custom/face dependencies and editor writes; important unassigned-print finding fixed RED/GREEN and accepted by reviewer.
- [ ] Package and verify the complete Windows candidate, preserving older artifacts; test old saves and the full Bellwake native path. Record remaining manual acceptance and measured limitations.
