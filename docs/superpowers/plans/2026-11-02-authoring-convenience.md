# Campaign Authoring Convenience Implementation Plan

> **For agentic workers:** use executing-plans for root integration and
> dispatching-parallel-agents for the independent filesystem creator unit.
> Tests before behavior changes, one final fresh review, no visible auto-launch.

**Goal:** Create a playable campaign copy and validate/edit it without manually
assembling manifests, identities and private map dependencies.

**Spec:** ../specs/2026-11-02-authoring-convenience-design.md
**Stack:** existing Java17/Swing editor, libGDX game, TestNG, Tiled XML/JSON.

1. Filesystem creator (agent owns CampaignProjectCreator.java and its tests).
   Test unique identity/folder, unknown-field preservation, unchanged sources,
   recursive private map/tileset/image/template copies, teleport rewriting and
   safe cleanup/rejection. Implement the spec's explicit interface, staging and
   `campaign:` root-map references. No game/UI globals or heavy/native tests.
2. Root map contract (root owns campaign map resolver, Config/TileMapScene,
   CampaignContentValidator and CampaignRegistry.readProject plus focused tests).
   Observe RED for owned-root maps and traversal, preserve common-map behavior,
   save/root comparisons and existing campaign identities. Add staged read API.
3. Editor workflow (root): campaign chooser New/Open, New Campaign wizard,
   Overview metadata/location navigation/open-map/project folder controls and
   worker-based actual-card validation using the lazy index/cache. Keep current
   six tools and local-write protections. No dialogue graph or Play-from-here.
4. Integration: affected capped/queued Maven suites, tiny Python tooling tests,
   actual lazy card checks, invisible Swing wizard/overview and hidden native
   generated-project play/map/save/reload. Fresh review, fix blockers and provide
   a verified save-preserving update; retain game caps and no source publishing.

Record progress in .superpowers/authoring-convenience/progress.md. Creator and
root map support share only the agreed campaign: contract/readProject API; edits
do not overlap. UI uses creator only after its interface is ready.
