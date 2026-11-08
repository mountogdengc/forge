# Upstream battle fixes implementation plan

> **For agentic workers:** Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Import the next shared battle-correctness batch while preserving the Adventure integration already committed at `3a1fa7c7864feb43fb3ddaa23988148ba6f8666a`.

**Architecture:** Continue on the existing local integration branch, using selected patches and focused adaptations. Reuse the freshly verified unchanged baseline from the preceding batch; reproduce each new failure with real game objects and inline card scripts before applying production changes. Keep all test evidence in `.build-cache/upstream-battle-fixes-20261107`.

**Tech stack:** Java 17 source, Maven/TestNG, shared game/AI modules and desktop Adventure client, Windows JDK 26.

**Spec:** The approved next-step ordering in this chat; `docs/Adventure/Fork-Roadmap.md` and the first integration report remain preservation requirements.

## Constraints and review focus

- Preserve campaigns, separate saves, portraits, themes/navigation, deferred card loading and private match logs.
- No master merge, push, publishing, new gameplay content or broad AI feature import.
- Use the shared queue for noninteractive heavy checks; use the active source checkout.
- Waterbending limits survive mana taxes, while explicit incoming limits still take precedence.
- Extrapolate-style choices work without a destination and let an opponent choose the worse revealed card.
- Optional copies respect spare mana, ownership, unsupported/mana/copy abilities and spells still affordable from hand.
- Threat prediction uses copied targets and excludes the resolving ability even after it puts new objects on the stack.
- Looked-at cards remain private, including when none qualify; newly visible graveyard/exile actions do not reveal opponents' hidden information.
- Remove obsolete forced reveal flags only from existing scripts. Missing Rutherford/Spock upcoming scripts remain deferred to their new-card batch.

## Tasks

- [x] Establish lightweight real-game regression fixtures and observe failures against the unchanged baseline.
- [x] Import `409689c3464` (waterbend cap) and `08b825d1e95` (Extrapolate AI choices); verify focused regressions.
- [x] Adapt `1e8c35fca3e` (optional spell copies, copied-target prediction and resolving ability); retain current stack lifecycle behavior and verify positive/negative cases.
- [x] Import `45cd35c4af9` (Dig reveal and activatable zones) with available script changes, deferring only scripts absent from the fork; verify revealed-card privacy and eligibility boundaries.
- [x] Run clean relevant Maven verification, content checks and a focused native battle/UI check if shared rendering paths need it. Actual mobile controller dialog-boundary coverage is sufficient for the sequencing-only UI change; no new native rendering claim is made.
- [x] Obtain an independent review; resolve important findings with regression coverage; record exact sources, tests, limits and a local commit.

Final evidence: `docs/Adventure/Upstream-Battle-Fixes-2026-11-07.md`; `.build-cache/upstream-battle-fixes-20261107/final-registered/results.json`. Automatic discovery includes all 20 new cases. Clean verification: 316 Java passes, one existing Windows symlink skip, zero failures/errors; 42 authoring passes; 115 script changes audited. Reviewed Ivy correction is covered by a failing-before/passing-after real-card test.
