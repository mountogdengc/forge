# Curse of Strahd 0.5.0 — Krezk and three-chapter playtest

This candidate covers the opening in Barovia and **A Light in Vallaki**: the
western road, Vallaki, Saint Andral's Church and the coffin shop. It ends when
Ireena chooses the restored sanctuary, then offers **A Bell Against the Mists**:
a complete Krezk chapter across its gate, village, blessed pool and Abbey.
There are also **25 short arrival areas**, including Castle Ravenloft's forecourt.
Their full stories remain future chapters. The escort is tracked through dialogue and the journal; Ireena
does not walk behind the player. Combat uses Forge's Magic card rules.

## Start

1. Extract the whole new ZIP into a **new writable folder**. Keep your working
   Bellwake and earlier Strahd folders and their saves. No Java installation is needed.
   **Start a new 0.5.0 run.** Old worlds do not contain the expanded Krezk area. Old
   saves are not migrated; keep using their original installation to play them.
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

### Geographic map check

Before progressing the story, use the HUD map button and its zoom-out controls
to inspect the whole overworld. Expected: the village
lies in the east, Vallaki is northwest below the large northern lake, Tser Pool
is beside the eastern river valley, and the south/west have mountain ranges.
Roads wind through woods and cross rivers at passable bridges. Named destinations
now open short arrival areas; their dialogue identifies the limits of this version.

1. Leave Barovia and follow the road toward the Old Svalich Road and Tser Pool.
   Expected: winding turns have space to walk, and map markers match the actual
   entrances. The Village Church is northeast of Barovia.
2. Walk along a riverbank and a mountain edge. Expected: water and mountain
   cores block movement. A road crossing or mountain pass remains walkable.
   Watch for trees/rocks obscuring paths, catches at bends or oversized markers.
3. Make a named overworld checkpoint near a bridge. Walk away, reload, then
   exit/relaunch and Continue. Expected: exact position and geography restore;
   roads and entrances do not move. Revisit a saved interior as well.
4. Later follow the western road to Vallaki. Expected: its church and coffin
   shop are clustered nearby, not spaced along a straight continental line.
   Report travel time and any stretch that feels too long or hard to navigate.

### New destination tour

You can make these visits before starting the church investigation or after
finishing either chapter. Each scene has an arrival conversation near the center,
a second detail interaction northeast of it and a southern exit. Use the map to
visit the following sites; enter and leave each once.

| Destinations | Arrival scene |
| --- | --- |
| Castle Ravenloft; Gates of Ravenloft | Forecourt; outer gate |
| Gates of Barovia East; Gates of Barovia West | Valley gates |
| Old Bonegrinder; Wizard of Wines | Mill approach; winery exterior |
| Argynvostholt; Ruins of Berez | Ruined manor approach; marsh ruins |
| Van Richten's Tower; Amber Temple; Werewolf Den | Tower shore; temple threshold; cave mouth |
| Yester Hill; Tsolenka Pass | Hilltop stones; mountain crossing |
| Black Carriage; Svalich Woods | Roadside carriage; forest clearing |
| Tser Falls; River Ivlis; River Ivlis Crossroads | Falls overlook; riverbank; crossroads |
| Luna River Crossroads; Raven River Crossroads | Road junctions |
| Lake Zarovich; Lake Baratok; Lake Luna | Shoreline clearings |
| Mount Baratok Trail; Mount Ghakis Lookout | Mountain approach; snowy lookout |

1. At each site, speak to both interactions. Try all three arrival choices.
   Expected: readable, dismissible dialogue, a clear explanation that further
   stories are future content, and journal guidance back to the existing chapters.
2. Visit while injured and note life, gold, cards, selected deck and journal
   before/after. Expected: these visits grant no healing, cards, rewards, quests
   or story progress. Existing free rest in Barovia/Vallaki remains available.
3. Leave through the southern exit. Expected: you return beside the corresponding
   overworld entrance and can walk away without reentering immediately. Report
   any obstructed approach, overlapping marker or wrong return location.
4. Save inside **Castle Ravenloft**, quit normally, relaunch and Continue.
   Expected: the forecourt, both interactions, position and player state restore.
   Leave after reloading. Krezk's four quest scenes have their own checks below.
5. Return to the existing chapters. Expected: visits have not unlocked or skipped
   any objective. Report travel time and whether the new sites are distinct enough
   to recognize in the pixel style.

The story checks below retain their original numbering for reports.

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
    The journal now contains **A Light in Vallaki**, starting with the western
    road ambush. Speaking to Eva again must not add another copy of that quest.
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

## Vallaki: ordinary route

Prerequisite: finish the opening above, including Eva's deliberate warning.
Keep the selected deck at 40 cards unless you choose to edit it yourself.

1. Visit **West Svalich Road** and fight the **Western Road Ambush**. Expected:
   victory clears the encounter and the wolves disappear. A loss leaves it
   available for a rematch. Recover for free with Ismark in Barovia or Urwin in
   Vallaki. The opening's Old Svalich Road is a different place.
2. In **Vallaki**, speak to the gatekeeper by the southern entrance and agree
   to enter with Ireena. Expected: the journal advances to Father Lucian.
   Speak to Urwin by the square and try free recovery; no gold is required.
3. At **Saint Andral's Church**, speak to Father Lucian and agree to investigate
   the missing bones. Expected: the next objective is Milivoj in the churchyard.
   Speak to Milivoj and follow his lead to Henrik. No fight or payment is needed.
4. At the **Coffin Maker's Shop**, confront Henrik and make him identify the
   hiding place. Save a named **Henrik confessed** checkpoint, leave, and load.
   Expected: the confession remains and the spawn offers a fight or withdrawal.
5. Defeat the **Coffin Shop Spawn**. Expected: the figure disappears; the bones
   remain uncollected. Save **Before collecting bones** and reload it. Approach
   the reliquary at the back, leave the conversation once, then return and
   explicitly collect the bones. Expected: only collection advances the journal.
6. Return to **Saint Andral's Church**. Speak to Lucian before fighting the
   **Belfry Stalker**. Expected: he asks you to defend the church first. Fight
   the stalker; a loss keeps the recovered bones and allows another attempt.
   Victory removes the intruder and advances the journal to returning the bones.
7. Make a named **Before Vallaki truth** checkpoint. Speak to Lucian and choose
   **private accounting**. Expected: the bones are restored, his repeat dialogue
   acknowledges a private accounting, and the chapter remains unfinished.
8. Speak to Ireena at the church. Expected: she refers to your Doru choice and
   decides to stay. Leave once without settling, then return. Choose two white
   **Fiend Hunters** or two black **Falkenrath Nobles**. Expected: two cards enter
   the collection, the selected deck stays unchanged, and chapter two completes.
9. Revisit Ireena, Lucian, the coffin shop and both roads. Expected: no duplicate
   cards, repeated quest, respawned defeated figures or unfinished objectives.
   Save at the church, exit normally, relaunch and **Continue**. Position, life,
   currency, cards, truth choice and sanctuary completion must restore.
10. Load **Before Vallaki truth** and choose **warn the town openly**. Expected:
    restoration succeeds again, Lucian's response changes, the later private
    choice/reward has not leaked backward, and Ireena can finish this branch.

## Vallaki: early visits and alternate start

1. In a fresh run, visit the western locations before hearing Eva. Expected:
   guidance sends you back to her; no locked battle, relic or completion is
   offered. Urwin still provides free recovery.
2. After Eva but before clearing the western ambush, visit the gatekeeper,
   Lucian and the workshop. Expected: they identify the ambush as the missing
   step. Return to each as the investigation proceeds; guidance must name the
   next missing step, never an inaccessible later objective.
3. Visit the spawn before Henrik confesses, the relic before defeating the
   spawn, and the stalker before collecting the bones. Expected: each explains
   its prerequisite and does not start a battle or grant progress.
4. Play both chapters with **Grave Resolve** as a separate run. Choose the
   alternate Doru fate and the black sanctuary reward. Report the three new
   battles' difficulty separately from the white start. There is no hidden
   timer, required purchase, permanent gold/life loss or automatic deck edit.

## Krezk: A Bell Against the Mists

Prerequisites: use this 0.5.0 build and a new run. Finish both earlier chapters,
including Ireena's sanctuary reward. Ireena stays at Saint Andral's Church.
The four Krezk scenes are separate overworld entrances in the northwest:
**Krezk** (gate), **Krezk Village**, **Blessed Pool of Krezk**, and
**Abbey of Saint Markovia**. Each southern exit returns to its own map marker.

1. Before completing Vallaki, visit Krezk's four scenes. Expected: conversations
   name the missing prerequisite; no locked battle, water, accord, quest or
   completion is granted. Anna's explicit free recovery remains available.
2. After sanctuary, speak to Ireena at Saint Andral's Church. Decline the western
   journey once. Expected: no new quest. Speak again and accept. Expected:
   **A Bell Against the Mists** appears and tracks the gate wolves. Speak again;
   it must not issue a second copy. Ireena does not move from the church.
3. At Krezk gate, question the captain before clearing the wolves. Expected:
   she asks you to clear them. Fight the **Krezk Mist Wolves** and deliberately
   lose once. Expected: return to overworld exploration, no road-cleared flag,
   wolves remain for a rematch. Recover with Urwin or in Barovia if needed.
4. Win the gate battle, then speak to the captain and explicitly enter Krezk.
   Expected: winning alone does not grant admission; the captain advances it.
   Travel northwest to Krezk Village and hear Dmitri's request.
5. Injure yourself in a battle, then choose Anna's free recovery. Expected:
   restored life, unchanged gold, no automatic objective advance. Read the notice.
6. At the Blessed Pool, try drawing water before defeating the **Spring Watcher**.
   Expected: a warning, no collection. Fight it, lose/rematch if useful, then win.
7. Make a named **Before Krezk Water** save after victory. Exit/revisit and reload.
   Expected: the defeated watcher stays gone; the journal still asks for water.
   Open the water interaction and choose Return later. Nothing is collected.
8. Deliberately draw water. Expected: the next objective becomes the Abbot.
   Load Before Krezk Water; collection must rewind while victory stays recorded.
   Draw it again. The nearby shrine does not force an Ireena event or ending.
9. At the Abbey, approach the **Abbey Sentinel** before speaking to the Abbot.
   Expected: a prerequisite warning. Question the Abbot with the spring water,
   then defeat the sentinel. A loss permits recovery and another attempt.
10. Save **Before Krezk Accord** after victory. Leave/revisit and reload.
    Expected: sentinel stays gone, Vasilka is present, no accord is chosen.
    Vasilka is a conversation, never a required enemy or reward item.
11. Speak to Vasilka, leave to consider, then return and choose **community watch**.
    Expected: its choice is remembered, chapter remains unfinished. Visit the
    Abbot/bell and return to Dmitri in Krezk Village.
12. Defer Dmitri's reward once. Expected: no cards/completion. Settle the accord
    and take **two Thraben Inspectors (white)**. Expected: exactly two new cards
    in the collection, selected deck unchanged, chapter three complete.
13. Revisit Dmitri, the captain, pool and Abbey. Expected: completed responses,
    the community watch remembered, no repeated reward or defeated enemies.
14. Load Before Krezk Accord. Expected: later choice, reward and completion are
    gone; sentinel remains defeated. Choose **Abbey ward under public oversight**,
    then return to Dmitri and take **two Doomed Dissenters (black)**. Expected:
    the alternate accord is remembered and no white reward leaks from the rewind.
15. Make a named save in Krezk Village, quit normally, relaunch and Continue.
    Expected: exact location, life, gold, cards, deck, accord, journal and chapter
    completion restore. Revisit all four Krezk scenes and leave through each
    southern exit; markers must not overlap or trap the player on return.

Repeat the three new battles with Grave Resolve as a separate new run. Report
actual played-match difficulty, especially the Spring Watcher's graveyard
creatures and the sentinel's late-game threat. Automated outcomes do not prove
balance. No winery quest, resurrection system, relic hunt or Ravenloft finale
is claimed by this chapter. Travel, art, sound and pacing results unlock the
next refinement pass; new chapters remain separate work.

## Record and report

Report the build name from BUILD-INFO.json, starting deck, run/save name,
numbered step, last action, expected result and actual result. Include whether
the desktop/cursor stayed responsive, startup time, sound, blocked paths,
hard-to-read text, duel wins/losses and any confusing choices. Keep
logs/launcher.log and test-profile/forge.log; a named checkpoint and screenshot
help reproduce a problem.

Automated story checks supply duel results. They do not establish played-match
balance, manual navigation, audio quality or long-session performance. Successful
manual passes unlock travel/art refinement and planning full destination chapters.
Version 0.5.0 requires a new run; future world additions will also need an
explicit compatibility decision.

## Build provenance

BUILD-INFO.json identifies the source baseline and hashes of included source
changes; SOURCE-CHANGES contains the modified/new source files used for this
candidate. SHA256SUMS.txt covers initial packaged files. Mutable profiles and
logs will naturally change during play. Keep the included Forge and Java license
notices with the build.
