# Strahd encounter cleanup

User request: remove defeated wolves/vampire after their animation unless needed
for dialogue; Strahd rides away after speaking. Preserve the user's existing run.

1. Reproduce missing cleanup in actual story data: three new tests fail (observed
   6 pass/3 fail), then script removals at the correct conversation boundaries.
2. Add optional mounted departure action and persistent quest-flag map cleanup.
   Keep 0.1.0 world/object/save identities; reconcile already-completed old saves.
3. Run affected Java suites and authoring checks; hidden native proof for both
   fates, wolf disappearance, mounted movement, blocked partial saves, revisits,
   fresh Continue and an old completed profile. No visible game launch.
4. Review, then provide a verified update package preserving profiles. Keep
   windowed1080p/30FPS/audio and existing CPU/memory limits unchanged.
