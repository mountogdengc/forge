# The Last Fare of Bellwake — 0.1.0

An original self-contained Adventure chapter. Five passengers return from the night crossing. The sixth leaves a wet coat. Start in Bellwake Quay, investigate the Drowned Tollhouse to the east, and bring Ada Vale's last ticket back to Mira. The ending is the player's choice, not a sequel hook. No New Game+ or imported Shandalar quest line.

## Play on the Windows test build

The data-only add-on targets build `8f260e762907`. Close the game and copy the add-on's `res` into the directory containing `Start-Adventure-Test.cmd`, alongside the existing `res`. The files added are under `res/adventure/Bellwake` and `res/adventure/common/maps/map/bellwake` only. Reopen the game and select New Game → The Last Fare of Bellwake. Use a fresh character. A separate game client, new Java installation, and changes to the profile are unnecessary. Keep the add-on installed while using its saves.

To try both endings without replaying the battle, make a named overworld save after retrieving the ticket and before entering the quay. Revisit Mira after the ending. Come out to the overworld again before saving. Version matching is exact; do not replace the content version under existing saves.

## Implementation and scope

Stable campaign ID: `fork.bellwake`. Opening quest: `7001` (The Sixth Coat). Spawn is the quay so the existing defeat/respawn path can find it. The tollhouse is placed 15 tiles east on a 120 × 120 marsh. The chapter reuses Forge's starter options, art, and base tile layers from `towns/swamp_town.tmx` and `evilgrove/evilgrove_8_troll.tmx`. Its chapter maps, actors, quests, deck, and consequences are separate files. The two map copies reside under a uniquely named common subfolder because the target client resolves root POI maps from the common map directory. No existing maps are replaced.

## Verification and limits (ending spoilers)

A Linux graphical smoke used the Java client and assets from the Windows test ZIP, system Java 21, Xvfb, and an isolated test profile. Campaign selection, character creation, opening dialogue, an overworld checkpoint, both maps and the Keeper's actual duel startup were exercised. An assisted progression test invoked the map's post-victory dialogue, took the ticket, and tested each ending from the same saved checkpoint. The first branch restored ending 1 with one Ghoulcaller's Bell and zero Traveler's Amulets; the second restored ending 2 with the inverse counts. Re-entry restored the matching quay dialogue without awarding a second card, and the tollhouse enemy stayed removed after loading the pre-choice checkpoint. This is not an unassisted match win, a balance assessment, or native Windows graphical validation. The final boss greeting and the roaming rat deck choice are content-only polish.

Native Windows acceptance: play a complete match against the Keeper, return for an ending, save outside, quit, and use Continue. Check that revisiting either location does not repeat the fight or grant a second ending reward. Test a loss/rematch separately. No changes to multiplayer, engine code, or the product name.
