# Curse of Strahd: Into the Mists — opening chapter playtest

This candidate covers Barovia village, the church, the Old Svalich Road and Tser
Pool. It ends after Madam Eva's warning. Western Barovia and Castle Ravenloft are
future chapters. The escort is tracked through dialogue and the journal; Ireena
does not walk behind the player. Combat uses Forge's Magic card rules.

## Start

1. Extract the whole new ZIP into a **new writable folder**. Keep your working
   Bellwake folder and its saves. No Java installation is needed.
2. Double-click **Start-CurseOfStrahd.cmd**, leaving its console open. Use this
   launcher rather than opening the JAR. The defaults are windowed 1920×1080,
   30 FPS, sound on, 1 GB maximum Java heap and two reported processors.
3. Choose **New Game → Curse of Strahd: Into the Mists**. Choose **Village Vigil**
   for the first run. Expect 20 life and a fixed 40-card white deck. Use a new run
   name so reports and saves are easy to identify.

Move as in Bellwake and walk into characters to speak. The village exit is at
the bottom. Other locations are on the overworld. The journal gives the current
objective. Use named saves through the menu; F5/F8 provide quick save/load during
settled exploration. Finish conversations, battles, reward collection and brief
result animations before saving.

## First pass: ordinary route

1. In the village, speak to Ismark and agree to investigate the church. Expected:
   the journal advances to Doru. Try free rest after taking damage; it restores
   life without charging gold.
2. Find the Svalich Roadwarden and play practice. Deliberately lose one game.
   Expected: you remain in the village with the same life and gold; practice
   remains available. This is the safest first check of an actual duel.
3. Win practice and choose the two white Rebukes. Expected: two cards enter your
   collection, while your selected deck stays at 40 cards until you edit it.
   Win another practice game: the special two-card reward must not repeat.
4. Leave the village and visit the Village Church. Speak to Donavich and read
   the register. Expected: both conversations dismiss normally and explain the
   danger; the register is an optional clue, not a progression requirement.
5. Make a named **Before Doru** save while exploration is settled. Fight Doru.
   If you lose, expect to leave the church with an available rematch. Return
   to the village to rest, then try again; record whether the match feels fair
   and understandable.
6. Win against Doru and choose **Restrain Doru**. Expected: the journal records
   the church resolution. This does not claim to cure him. Speak to Donavich
   again; his response should match restraint. Doru stays for his fate dialogue,
   then disappears after its final line; he must not reappear after loading.
7. Save inside the church, leave, and load that save. Expected: church location,
   life, cards and restraint choice restore. Doru must not force a fresh battle.
8. Return to Ireena in the village and explicitly agree to escort her. Expected:
   the journal advances to the Old Svalich Road.
9. Visit the road, read the waystone and speak to Strahd. Expected: Strahd is a
   dialogue encounter, with no boss battle. When you finish speaking he rides
   away on a black horse and stays gone on revisits. Fight and defeat the
   Mist-Stalked Pack. After its death animation the pack disappears without a
   follow-up conversation. A loss must permit recovery and a later rematch.
10. Visit Tser Pool. Speak to Ireena, then finish that conversation and make a
    named save **before speaking to Madam Eva**. Expected: arriving alone has
    not completed the chapter. Speak to Eva and deliberately hear her warning
    to finish it.
11. Revisit Eva and the village. Expected: completion remains recorded, no
    duplicate practice upgrade appears, and dialogue acknowledges the result.
    The game clearly says this playable chapter ends here.
12. Save inside Tser Pool, exit normally, relaunch and choose **Continue**.
    Expected: the same run, location, life, collection and completed chapter.

## Second pass: alternate choices and recovery

1. Load **Before Doru**, win again, and choose **Put Doru to rest**. Expected:
   Donavich and Ireena later acknowledge that choice; no restraint dialogue or
   future completion from the later save leaks backward into this checkpoint.
2. Create a separate new run with **Grave Resolve**, the black deck. Expected:
   a fresh 40-card deck, separate collection and untouched first-run saves.
3. Before speaking to Ismark, visit Tser Pool and the road. Expected: clear
   guidance about what is missing; neither arrival completes the chapter nor
   the pack encounter. Return visits remain possible.
4. In that run, subdue Doru and resolve his fate **before** Ismark's briefing.
   Then return to Ismark and Ireena. Expected: the journal catches up and lets
   you accept the escort, without requiring another Doru win.
5. Choose the two black Murders after a practice win. Expected: two added to
   the collection once, with no silent deck edits. Complete the road and camp
   using the black deck; report difficulty separately from the white deck.
6. From New Game, create a separate Bellwake run; then load your Strahd run.
   Expected: each campaign keeps its own world, quests, collection and saves.
   Do not copy either profile over your existing Bellwake installation.

## Record and report

Report the build name from BUILD-INFO.json, starting deck, run/save name,
numbered step, last action, expected result and actual result. Include whether
the desktop/cursor stayed responsive, startup time, sound, blocked paths,
hard-to-read text, duel wins/losses and any confusing choices. Keep
logs/launcher.log and test-profile/forge.log; a named checkpoint and screenshot
help reproduce a problem.

Automated story checks supply duel results. They do not establish played-match
balance, manual navigation, audio quality or long-session performance. Successful
manual passes unlock balance/art refinement and planning the route toward
Vallaki. Fixes that change this initial world may require a new test run; this
0.1.0 opening is not a promise of final full-campaign save compatibility.

## Build provenance

BUILD-INFO.json identifies the source baseline and hashes of included source
changes; SOURCE-CHANGES contains the modified/new source files used for this
candidate. SHA256SUMS.txt covers initial packaged files. Mutable profiles and
logs will naturally change during play. Keep the included Forge and Java license
notices with the build.
