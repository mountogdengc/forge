# Bellwake 0.3.0 manual playtests

Use this after installing the art/ambient update. Automated tests use supplied victories; these runs must play real matches. The purpose is to decide what needs tuning, not to prove you can win by retrying until a result looks good. Record every loss, restart and confusing moment.

**Standalone Windows build:** With `Bellwake-Windows-0.3.0.zip`, extract the entire
ZIP into a new writable folder and run `Start-Bellwake.cmd`. Java and the update
are already included. Choose New Game, then the preselected The Last Fare of
Bellwake and Create character. Skip installation steps 1-4 below and continue at
step 5. The ZIP also includes a copy of this guide with standalone setup steps.
Keep old installations for older saves; use fresh runs here.

## First playtest follow-up checks

For the playtest-fixes build, you may keep a current Bellwake 0.3.0 run: close
both clients, back up its old `test-profile` folder, then copy that folder into
the NEW extraction before launching. Keep the old installation too. Do not copy
your normal Forge profile. Older 0.1.0/0.2.0 content still needs its old client.

1. Load your existing run. Compare location, life, gold, deck, collection and
   objective before continuing. If there is a mismatch, preserve both copies.
2. Lose a practice match to Tor. Expected: you remain inside the quay, retain
   life and gold, and can move away, return and speak to Tor for a rematch.
3. Save in the quay, move, open the menu and use Load to select the checkpoint.
   Expected: the saved position restores without an unfinished-action error.
   Repeat inside the tollhouse, with F8 and with Continue after a full restart.
4. During a match open the match menu and **Help: playing a match**. Read all
   three topics. Expected: the phase sequence, stop settings and OK/End Turn
   explanations are readable and can be closed without advancing a choice.
5. Hover over OK, End Turn, Undo, the menu, the phase name and individual phase
   cells. Expected: delayed explanations. Holding prompt buttons explains them
   without acting; holding a phase cell retains its explained yield action.
6. Toggle stops in your row and the opponent's row separately. Expected: enabled
   cells have a bottom line and color; current-phase highlighting is distinct.
   These settings pause your decisions on the chosen player's turns.
7. During play check the small history: newly recorded private draw/discard
   details must not appear there, even with maximum/custom logging. After the
   match, the detailed results log should name actual drawn/discarded cards and
   other zone movements. Keep an example if a move is missing or duplicated.

These checks supplement the full runs below. No new deck balance is assumed.

## Install base plus update (alternative) and prepare

1. Keep your existing Forge/Bellwake installation intact. Extract the entire verified base client ZIP, `Forge-Adventure-Windows-Test-6747fa4da56e.zip`, into a **new writable folder**, for example `C:/Games/Bellwake-0.3-Test`. Java is included; do not install another Java or copy normal saves into this folder.
2. Close the test client if it is running. Extract **Bellwake-Art-Ambience-0.3.0.zip** separately. Copy its contents into the new base-client folder, beside `Start-Adventure-Test.cmd`, accepting replacements. Merge the `res` directory. Do not put the update in an extra nested subfolder. The update includes a compatible client JAR with the music-cache fix as well as the chapter assets.
3. Check `BUILD-INFO.json`: Bellwake content must be **0.3.0**. `res/adventure/Bellwake/campaign.json` must also say `0.3.0`. Keep the old folder to load 0.1.0/0.2.0 saves; exact content-version matching is intentional because map geometry changed.
4. Run **Start-Adventure-Test.cmd**. Choose Adventure if asked. Use English for the first report so the choice labels below match. Create a fresh Bellwake run; do not import an older run.
5. In Settings, select the default music/sound sets, turn music on, and use an audible music volume (start around 50%). Use speakers or headphones. Card art can download on demand; a missing image should be recorded separately from missing card text or a broken action.
6. Start at **1280x720**, keyboard/mouse. Write down Windows version, display scaling, input device and audio device. Use named runs from the matrix below. Record wall-clock start/end, including time spent reading, deck editing and matches separately.

## Run matrix

| Run | Starter | Preparation | Ending / special purpose |
| --- | --- | --- | --- |
| A - Lantern prepared | Lantern Watch | Win practice; choose two Pacifisms; edit deck | Ring her ashore, then rewind the ticket save to test the other choice |
| B - Ember direct | Ember Crew | Skip practice and deck editing | Send the empty boat; also skip the optional fare book |
| C - Lantern recovery | Lantern Watch | Skip practice and deck editing | Deliberately lose to the Keeper once, recover, rematch and finish |
| D - Ember early ticket | Ember Crew | Win practice; choose two Lightning Strikes | Leave before briefing with Mira; recover the ticket early, then complete the briefing/ending |

A+B are the first feedback pass. C+D, the save matrix and controller/presentation checks are release acceptance gates; leave any unrun case explicitly pending.

## A - Main walkthrough, upgrade and both endings

1. New Game -> **The Last Fare of Bellwake** -> **Lantern Watch**. Name the run `A - Lantern prepared`. Expected: one chapter difficulty, 20 starting life and a 40-card deck; no inherited Chaos/custom/Commander starting modes.
2. On arrival, identify the timber landing, moored ferry, warm braziers, Mira and Tor before opening documentation. Expected: the south landing reads as the exit; the quay has a coherent waterfront rather than unrelated buildings. Record any place you mistake for a door, exit or interactive object.
3. Approach the noticeboard beside the landing and choose **Read the directions.** Expected: the board is reachable and explains the eastward tollhouse route, practice and recovery. Close the conversation normally.
4. Speak to Mira and choose **I will find her ticket.**, then **Continue.** Expected: the journal now asks you to investigate/recover the ticket. Returning to the quay without it must not complete the chapter.
5. Approach Tor on the west side. Choose **Explain the two decks.**, read and close it; speak again and choose **Play a practice game.** Play a real match. Record duration, result, turns if visible and any rules/UI confusion. Expected: practice is optional, free and clearly a separate friendly encounter.
6. On the first practice win choose **Take two Pacifisms.**, finish collection and **Continue.** Expected: exactly two additional Pacifisms in the collection; the selected deck remains unchanged until you edit it.
7. Open Decks. Inspect the new cards, add the two copies and remove two nonland cards of your choice. Expected: the finished deck has 40 cards, no more than four Pacifisms, and remains selected after leaving the editor. Record what you removed and whether the editor made this understandable.
8. Play Tor again and win. Choose **Thank Tor.** Expected: no second preparation-card reward. Compare the collection count, not just the message. No grinding should be needed.
9. If life is reduced, ask Mira to **Rest by the stove.** Expected: free recovery to current maximum life; no story advance or repeated card reward. Record whether the source of healing was clear.
10. Make a named save `A - Quay ready`. Leave by the south landing and travel east across the marsh. Expected: the route is readable, with no required random encounter. Water in the location maps blocks walking off the landing/causeway without trapping you on it.
11. Enter the Drowned Tollhouse. Read the fare book and choose **Remember the entry.** Expected: the book has a clear reachable reading ledge; its clue is optional and does not release the fare by itself.
12. Make `A - Keeper before`. Approach and play the Keeper normally. Record all attempts, match length and which threats decided the game. Expected: the causeway and arena approach are unobstructed, threats are readable, and no debug/supplied victory is used.
13. After winning, finish loot collection and choose **Take Ada's ticket.** Expected: the Keeper disappears and the journal directs you back to Mira. Wait until all conversations/rewards close, then make `A - Ticket before choice` **inside the tollhouse**.
14. Exit to the marsh and return west to the quay. Expected: the return exit works. Merely entering the quay does not choose an ending. Speak to Mira.
15. Choose **Ring her ashore.**, then **Keep Ghoulcaller's Bell.** Collect it and finish **Continue.** Expected: one Bell added, no Amulet, ending-specific dialogue, and a persistent chapter-complete entry in the journal.
16. Make `A - Bell ending`. Exit through the game's Exit command and confirm. Wait for the game window to close, relaunch the same launcher, and choose **Continue**. Expected: the interior, position, collection, deck, completed journal and chosen ending restore; no loading overlay remains.
17. Speak to Mira again, walk away and return, and revisit the tollhouse. Expected: no repeated ending card, no resurrected Keeper and no return to the unfinished quest.
18. Load `A - Ticket before choice`. Expected: the old pre-choice state returns, without the later Bell. Return to Mira; choose **Send the empty boat.**, then **Keep Traveler's Amulet.** Finish collection/dialogue and save `A - Amulet ending`. Expected: one Amulet, no Bell from the discarded future, matching later dialogue, and the same completed chapter state. Restart/Continue once more.

## B - Red deck without preparation

1. Create `B - Ember direct` with **Ember Crew**. Record 20 life and the untouched 40-card deck.
2. Accept Mira's request, but do not play Tor or edit the deck. Leave south and travel east. Do not read the fare book in this run.
3. Save before the Keeper and play the match normally. Expected: the base red deck can finish without practice rewards, shopping or grinding. Record every attempt rather than hiding failed attempts behind reloads.
4. Take the ticket, return and choose **Send the empty boat.** Finish the Amulet reward and dialogue. Expected: the clue and preparation were optional; the ending completes normally.
5. Save inside, quit, restart and Continue. Confirm the Amulet count, selected deck, location, journal and empty Keeper arena.

## C - White deck loss and recovery

1. Create `C - Lantern recovery` with **Lantern Watch**. Brief with Mira, skip practice, retain the base deck and reach the Keeper.
2. Record current/maximum life, gold, collection and objective. Deliberately lose a real Keeper match by passing/declining blocks where possible; do not use a debug result or close the process to simulate defeat.
3. Expected: no ending/ticket progress, no gold or maximum-life penalty, a recoverable return location, and the Keeper still available for a rematch. Current life may change through normal defeat recovery; maximum life must not decrease.
4. Return to Mira and rest if necessary. Re-enter the tollhouse and win with the untouched starter. Expected: no grind or preparation dependency. Finish either ending and verify restart/Continue.
5. To make the zero-gold-loss check nontrivial, record gold after the Keeper's reward and deliberately lose a later Tor practice match. Expected: that positive gold balance and maximum life are retained; no completed-story rollback or repeated reward occurs.

## D - Red preparation and out-of-order story

1. Create `D - Ember early ticket` with **Ember Crew**. After any opening text, leave the quay without speaking to Mira. If you accidentally accept her request, record it and restart this specific case.
2. Visit Tor, avoiding Mira. Win practice and choose **Take two Lightning Strikes.** Expected: two copies added once, no automatic deck edit. Edit to 40 cards with at most four Lightning Strikes; record replacements. Repeating practice must not duplicate them.
3. Reach the tollhouse, win and take the ticket before Mira's briefing. Save `D - Early ticket` inside. Expected: the ticket/defeated Keeper persist, but the story is not already marked complete.
4. Return to Mira and accept **I will find her ticket.** Close the explanation, walk away and speak again. Expected: previously obtained evidence is recognized; you can choose an ending without another Keeper fight.
5. Finish either ending and restart/Continue. Confirm no repeated preparation or ending cards, correct completed journal and Keeper absence.

## Save-location matrix

At each checkpoint below: record position, current/maximum life, gold, selected deck/count, key card quantities, objective and visible actors; save while exploration is stable; quit normally; restart and Continue; compare those observations. Also exercise F5/F8 by moving or changing a harmless state before quickloading. Loading an older snapshot intentionally restores its older state.

| Checkpoint | Required comparison |
| --- | --- |
| Quay before practice | Exact location; life not silently healed by restore; correct NPCs and objective |
| Marsh before the tollhouse | Overworld position and route; no forced encounter/entry loop |
| Tollhouse before victory | Keeper present; safe entry; location/deck/life intact |
| Tollhouse after taking ticket | Keeper absent; return objective; return exit works |
| Quay after each ending | One correct ending card; journal complete; later dialogue correct |

1. In a safe interior, deliberately have less than maximum life, save and restart there. Expected: Continue preserves the saved reduced life. A later normal rest/entry may heal according to normal rules; loading itself must not.
2. During a conversation, reward choice or room transition, try the normal Adventure save/load action where it is available. Expected: a clear wait/finish reason or unavailable action; no partial ending checkpoint and no permanently frozen input. Finish the interaction and try again. During a match, confirm that Adventure saves are not offered; do not use F5/F8 there as if they were exploration commands, since the match screen can have its own shortcuts.
3. Make two independent Bellwake runs and alternate loads. Expected: their decks, collections, endings, saves and Continue stay separate. Do not delete a run as part of this test.
4. If testing an old 0.2.0 run, use a separate copy of its old installation. Expected: the 0.3.0 installation requests the matching content rather than silently applying new geometry to the old checkpoint. Do not overwrite the only copy of an old save.

## Art, ambience and input checks

1. At 1280x720, inspect quay and tollhouse, dialogue choices, journal, card inspection, saves and deck editor. Repeat at 1920x1080 and your normal Windows display scaling. Expected: readable text, complete choice labels, visible focus, recognizable NPCs and no clipped action buttons.
2. If portrait is in scope for your use, repeat at 720x1280. Expected: river/shoreline fills the map view rather than an empty lower half; HUD and all story/reward choices remain reachable. Record portrait issues separately from the Windows-landscape gate.
3. In the quay, wait through a full 48-second ambient cycle: water/wind, restrained warmth and sparse bell tones. Leave for the marsh, then the tollhouse. Expected: distinct outdoor reeds/water and a more hollow tollhouse atmosphere; no abrupt loud transient, clipped sound or obvious click at the loop boundary. You may need headphones to judge the quiet bed.
4. Play Tor and the Keeper. Expected: the training pulse and more ominous Keeper pulse replace exploration ambience; returning from a match restores the right location audio. Conversations should remain comfortable to read without competing loudness.
5. Set music volume to zero. Expected: ambience and music stop while effects remain governed by their own control. Restore volume, open/close a conversation, alt-tab and return, save/load, quit/restart. Expected: one correct stream, no stacked layers, stuck silence or increasingly loud loop. Do not judge the mix from an external WAV player alone.
6. Switch to another campaign and back through the normal menus, then visit town and dungeon contexts. Expected: Bellwake audio does not leak into the other campaign and its own tracks return correctly.
7. Connect your controller before launch. Using the displayed confirm/back prompts and D-pad/stick, navigate New Game, conversations, journal, inventory, saves, reward/card inspection and deck selection; walk the landing/causeway and return exit. Expected: every required action is reachable, focus is visible, Back returns somewhere sensible, and save/load does not leave remembered movement. Record controller model, connection and any keyboard/mouse fallback required. Do not call keyboard-only coverage a controller pass.

## Report back

For each run send: build/content version from BUILD-INFO; Windows/resolution/scaling; input/audio device; run name and starter; preparation taken/skipped and deck edits; each match result/attempt/approximate turns/duration; total chapter time split into reading/travel/editing/matches; ending; save-location matrix results; sound/context/volume result; the first confusing or blocked moment; and screenshots/logs for defects. Quote the action label immediately before a problem, expected outcome, actual outcome and whether reloading changes it.

Keep `BUILD-INFO.json`, `logs/launcher.log` and `test-profile/forge.log`, plus a **copy** of the relevant test profile before more play. Review logs/saves for character names/local paths before sharing. Never send your normal Forge profile just to reproduce a test failure.

| Finding | Dependent next work |
| --- | --- |
| Starter needs repeated luck or preparation | Tune authored deck/encounter balance; replay the affected base/prepared run |
| Save state differs or rewards repeat | Repair the specific persistence/interaction boundary; rerun that checkpoint and both endings |
| Route/sign/NPC is confusing or blocked | Adjust the authored map/cues; rerun navigation and the affected starter route |
| Audio too quiet/loud, clicks, wrong campaign or failed mute | Adjust asset gain/envelopes or mixer/cache lifecycle as indicated; repeat the audio sequence |
| Focus/controller/portrait clipping fails | Fix the concrete navigation/layout; repeat that device/resolution path |
| All required cases pass | Record chapter acceptance and prepare the accepted release; acceptance alone does not request a merge or publication |
