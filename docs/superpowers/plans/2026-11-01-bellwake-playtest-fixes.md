# Bellwake first playtest follow-up

Implement the reported Load failure and friendly-practice recovery, detailed
postgame diagnostics without live hidden-card information, and discoverable match
guidance. Keep card rules/decks and phase/yield behavior unchanged.

- Reproduce Load through SaveLoadScene's real transition callback, not a direct
  WorldSave call. Check the outgoing stage before opening the loading overlay;
  remove that owned overlay before the guarded synchronous restore.
- Exercise a Tor loss through MapStage's result callback. Practice remains in
  the quay, preserves life/gold and keeps Tor for a rematch. Ordinary defeat
  recovery remains unchanged.
- Record named draws/discards/other detailed zone movements in an explicitly
  postgame-only log channel. Every ordinary/live getter, including maximum and
  custom verbosity and remote access, must filter those entries.
- Add match-menu Help and real hover/hold guidance for phases, stop state,
  separate player rows, OK, End Turn, Undo and the match menu. Preserve phase
  long-press yield actions and controller navigation.
- Run affected tests and native regressions, inspect guidance layouts, then
  provide a new complete Windows ZIP. Preserve old installations and 0.3.0 saves:
  no geometry, actor IDs, campaign version or save format changes are required.

The supplied real-match report establishes one Tor loss with red cards: the
outcome says Turn 9, the final history says Turn 17, and duration is 8 minutes
10 seconds. It does not justify a deck
rebalance before fixing guidance. Saving was reported successful; the screenshot
shows the unfinished-action guard during menu loading, not a corrupt-file error.

Root owns Adventure/save fixes and integration. Two independent agents own match
guidance and log privacy respectively. Heavy builds/tests remain sequential.
Native game probes use disposable profiles and no host desktop control.

## Completion evidence

Implementation committed as `ee8431f7625`; exact tests, RED/GREEN reproductions,
Help captures, older-client save compatibility and final standalone ZIP identity
are in `docs/Adventure/Progression-Validation.md`. The final extracted client
passed menu loading, practice-loss recovery, assisted story and fresh Continue.
The full reactor reported 1,419 cases, zero failures/errors and six existing skips.
Physical hover/controller use and real-match balance remain manual acceptance.
