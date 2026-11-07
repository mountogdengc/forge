# Curse of Strahd 0.2.0: A Light in Vallaki

David delegated the next chapter with “take the lead.” This is an original
card-adventure adaptation following the existing opening, using existing Forge
quest flags, dialogue, pixel tiles, Magic combat and save boundaries. No engine
feature, dependency, physical follower or real-time deadline is required. The
existing `issueQuest` action needs a dispatch repair for ordinary TMX actors:
issue the configured quest when no prebuilt-quest listener owns the action.
Existing listener-based quest offers retain their generated quest instances.

## Playable route

Eva's warning completes quest 8101 and issues quest 8102, **A Light in Vallaki**.
The player clears a western road ambush, speaks to Vallaki's gatekeeper, finds
free recovery with Urwin, and takes Ireena to Saint Andral's Church. Father
Lucian reveals the missing bones; Milivoj identifies Henrik's workshop. Henrik
confesses, a vampire-spawn encounter guards the relic, and the player explicitly
collects it. A final church encounter must be defeated before Lucian can restore
the sanctuary. The player chooses a private accounting or a public warning;
both protect the church and produce distinct repeat dialogue. Ireena explicitly
accepts sanctuary and grants two white Fiend Hunters or two black Falkenrath
Nobles once, to the collection without editing the selected deck. Her chapter
two dialogue also acknowledges the Doru fate. The chapter ends here.

New locations: West Svalich Road, Vallaki, Saint Andral's Church, Coffin Maker's
Shop. Distinct, original 40×34 maps reuse existing tiles/sprites, have entry/exit
38, reachable actors, and enclosed collision boundaries. Existing map geometry
and IDs remain unchanged. New encounter actors disappear after victory and stay
gone on revisit/load. Early visits explain prerequisites without starting fights
or granting progress. Losses allow recovery and rematches. Investigation and
collection must be deliberate interactions; arrival alone never advances them.

## State contract

New keys use `west.*`, `vallaki.*`, `andral.*`, `coffin.*`. Quest 8102 stages:
`cosWestRoadCleared`, `cosVallakiEntered`, `cosBonesInvestigation`,
`cosCoffinLead`, `cosHenrikConfessed`, `cosCoffinCleared`, `cosBonesRecovered`,
`cosSanctuaryDefended`, `cosBonesRestored`, `cosChapterTwoComplete`.
Character flag `cosVallakiTruth` is 1 for private accounting, 2 for public warning.
All roots gate prerequisite flags; reward and completion marker share an action.
Quest issuance occurs only in Eva's once-only chapter-one completion choice.

## Save/version decision (implementation choice)

Keep ID `fork.curse-of-strahd`, bump content to **0.2.0**. Existing worlds serialize
their POIs and cannot acquire these additions safely. Require a **new 0.2.0 run**;
do not pretend existing 0.1.0 saves migrate. Existing exact-version loading guard
remains. Package into a fresh folder, retaining the older installation/profiles.
Do not copy old profiles, rewrite version metadata, delete saves, or publish.
No commits or merges are part of this task. Preserve unrelated theme work.

## Evidence and limits

Verify tests first fail on absent chapter content; then run chapter data tests,
map/dialogue tooling, current reactor checks, actual card/resource validation,
and hidden native assisted progression for both starts, both Doru fates and both
truth choices. Check interrupted/early interactions, reward once, checkpoint
rewind, loss/rematch, actor cleanup, campaign isolation and fresh Continue.
Use the shared runner for heavy jobs, capped JVMs, disposable profiles, no visible
launch or desktop automation. Render static map previews and inspect them.
Package and verify a new playable Windows candidate with recorded hashes.
Manual walking, played battle balance, audio and responsiveness remain acceptance.

Setting reference: the [official adventure contents](https://www.dndbeyond.com/sources/dnd/cos)
identifies Vallaki as a campaign location. The chapter page redirects to the shop
without licensed access; no published chapter prose/maps are copied. The names,
missing-bones premise and workshop are high-level setting elements; dialogue,
encounters, choices, geometry and pacing here are newly authored.
