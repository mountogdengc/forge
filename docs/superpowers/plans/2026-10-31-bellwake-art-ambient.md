# Bellwake art and ambient implementation plan

> **For agentic workers:** Use superpowers:executing-plans. This executes the remaining art/ambient scope of the approved chapter-polish plan and the user's request to finish it.

**Goal:** Make Bellwake visibly a working ferry quay and ruined tollhouse, give exploration restrained environmental sound, and hand over a precise manual acceptance checklist.

**Architecture:** Data-only TMX edits reuse the existing pixel tiles; campaign-owned WAV files use existing TOWN/BLACK/CAVE/BOSS/MATCH playlists and existing fades/mute controls. Reuse existing audio behavior; a reproduced playlist-cache invalidation defect requires a minimal shared Java fix. Regenerators and structural/decoder checks remain developer tools.

**Spec:** 2026-10-31-bellwake-chapter-polish.md, Tasks 4/6/7; current direct request.

**Calls made:** Warm quay lamps against cool water; compact raised stone paths and a timber landing; ruined tollhouse causeway and arena; 30x30 maps fill portrait without changing existing world-space interaction positions. Original deterministic procedural ambience, no external samples or new runtime dependency. Content 0.3.0 requires a fresh run; retain the old installation for 0.2.0 saves. Deliver a small update for the verified 6747fa4da56e client, including a JAR with only the corrected MusicPlaylist class replaced, avoiding another full resource/runtime copy.

**Global constraints:** Keep libGDX and Forge rules/AI; retain story/deck/reward semantics; preserve actor IDs and safe approaches; no normal-profile writes; no host desktop control; no claim that supplied victories establish played-match balance. The five-project test queue does not include Forge; run Forge's heavy checks sequentially.

**Review focus:** Water cannot allow walking off causeways; decor cannot block actors/exits; portrait coverage must not shift player/NPC world coordinates; custom audio must resolve by campaign, decode and honor existing mixer behavior; the overlay replaces only the documented compatible JAR and chapter resources, never older user saves.

- [x] Add failing asset contracts for safe approaches, water boundaries, portrait dimensions, version and audio levels/formats.
- [x] Rework quay/toll maps with deterministic tooling and inspect rendered previews.
- [ ] Generate and validate original campaign audio; prove routing/decoding with existing runtime APIs.
- [ ] Run chapter content/regression tests and native map/restore/story checks sequentially; capture updated previews.
- [ ] Produce a small verified overlay and exact manual test instructions, including setup, numbered actions, expected results, report fields and dependent follow-up work.
- [ ] Fresh review, fix significant findings, record actual evidence and remaining manual acceptance.

Ruling: The existing shared invalidation flag was consumed by the first playlist refreshed after a campaign switch. Native RED warmed common playlists, activated Bellwake, refreshed BLACK, then TOWN incorrectly returned common/town/town5.mp3. Per-playlist generation tracking fixes this without new audio controls or gameplay behavior. Atomic generation + per-playlist synchronized lookup avoids clearing arrays under a playback callback. The update therefore includes a reassembled compatible JAR, not assets alone.

Evidence so far: five asset contracts and eight chapter/validator tests passed. Native source run 20261031-213313-404a01 passed all five playlist routes and libGDX PCM decoding, assisted story/restoration, and fresh-process Continue. Final delivery-JAR/geometry proof and manual handoff remain.
