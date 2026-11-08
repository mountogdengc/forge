# Upstream Adventure fixes implementation plan

> **For agentic workers:** Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Integrate the first, Adventure-focused batch from the reviewed 548 upstream commits without losing the fork's campaigns, character creator, save isolation, themes or deferred card loading.

**Architecture:** Start from fork master `6f5ceb18f17c2f15f6d227e90a175b0e70c287c3` on `codex/upstream-adventure-fixes-20261107`. Adapt selected upstream patches instead of merging the entire upstream tip. Keep current source-root/workspace preferences; all evidence is isolated under `.build-cache/upstream-adventure-fixes-20261107`.

**Tech Stack:** Java 17 source, Maven, TestNG, libGDX desktop Adventure client; Windows JDK 26 for validation.

**Spec:** The upstream review and the user's instruction to start working through its recommendations in this chat; `docs/Adventure/Fork-Roadmap.md` and `docs/Adventure/Campaigns-and-Saves.md` remain the product constraints.

## Global constraints

- Preserve campaign selection, independent playthrough saves, character identity/imported portraits, authored content, navigation, appearance settings, private match logs and deferred card loading.
- Do not alter normal player profiles, frozen playtest builds, campaign decks or card-pool policy.
- No master merge, GitHub push or publication in this batch.
- Run heavy checks serially through the existing shared runner, with the actual checkout as working directory.
- Source imports: save preview `7acb3dbb61`, fight loading `8360afa305`, inventory `1f8a571cab`, HUD `5d892f0559`, printsheets `6264f0292f`.
- Defer `8360afa305`'s AiCardLists optimization and new AI card eligibility until the coherent AI bundle: those depend on newer AI APIs absent from this fork. Defer its background generation worker until generation can safely capture campaign/player/RNG context; the upstream worker reads mutable live globals while this fork can replace campaigns and scope character previews.

## Review focus

- A queued preview must not capture into a different playthrough or an obsolete save header.
- Forced quicksave captures bypass cooldown; previews read the graphics buffer only on its owner thread.
- Leaving, replacing or switching campaigns while a deck is generated must discard stale results, including old scenes no longer displayed.
- Reward pools retain campaign/mode invalidation and selected editions; unavailable scripts never become booster placeholders.
- Inventory descriptions and notifications retain theme controls, navigation and usable touch/controller input.

## Tasks

### Task 1: Establish baseline and regressions

- [x] Run a clean baseline for the Adventure client/editor reactor and retain results.
- [x] Add meaningful preview dispatch/session and printsheet regressions; observe failures before changes.
- [x] Reuse campaign-card-loading, save restoration, character creation and theme tests as preservation coverage.

### Task 2: Adapt preview and booster fixes

- [x] Adapt WorldSave preview timing and forced GameStage quicksave; remove the fork's now-obsolete firstCapture reset safely.
- [x] Guard queued preview callbacks against playthrough/header replacement.
- [x] Import PrintSheet null/unsupported filtering through every insertion path; preserve weights and copy isolation.
- [x] Run focused tests and inspect outcomes.

### Task 3: Import HUD/inventory corrections

- [x] Apply the inventory patch onto the fork's themed scene, preserving added code.
- [x] Apply notification z-order/width corrections onto the custom HUD.
- [x] Verify patch scope and retain native layout checks for the final assembled client.

### Task 4: Adapt enemy-pool caching

- [x] Verify enemy filtering is evaluated once per built pool rather than on each template scan.
- [x] Publish cached player/enemy pools together while retaining configuration/mode keys, preview isolation and existing AI filtering.
- [x] Retain synchronous deck generation until the subsequent async-context work can prove campaign/player/RNG isolation.
- [x] Run focused tests, then clean client/editor verify plus authoring checks.

### Task 5: Review and record

- [x] Inspect final diff and independently review preservation and async/session behavior.
- [x] Run hidden, isolated-profile native checks against the newly assembled production JAR; do not reuse old production bytecode as evidence.
- [x] Record exact imported revisions, adaptations, test counts, skipped/manual limits and remaining upstream batches.
- [x] Report branch and integration status honestly; master remains unchanged.
