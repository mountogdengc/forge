# Barovia reference-map destinations (0.4.0)

David requested the other reference-map locations, explicitly including Castle
Ravenloft and Krezk, and selected **map locations with short arrival scenes**.
Retain the pixel style, two existing chapters and eight original POIs. Add 26
reachable destinations: the remaining named adventure sites, two Barovian gates,
road crossings, lakeside viewpoints and mountain trails (34 total POIs).

Castle Ravenloft has a forecourt, Krezk a walled approach. The winery, tower,
windmill, ruined sites, den, temple, hill and natural viewpoints each have a
small original exterior with two readable interactions and a clear southern
exit. Two root-dialogue actors per scene use existing MapDialog/TMX support.
The descriptions make the arrival area explorable and identify deeper interiors
or quests as future content. No new quest, battle, reward, timer, item gate,
forced ending or story flag is introduced. Visits before or after either chapter
must be safe and must not change progress, inventory, life or currency.

Use `CurseOfStrahd/story/landmarks.json` as the authoring inventory: stable name,
slug, reference label (where applicable), approximate authored-world coordinate,
scene archetype, existing building-atlas marker, original arrival/look text and
clearing radius. `create_strahd_overworld.py` consumes it to add anchors/roads;
`create_strahd_landmarks.py` builds 26 distinct local maps and their two dialogue
keys into the existing catalog. Keep all eight original scene geometries/IDs and
dialogues unchanged. All 34 world anchors must have a clear 3×3 approach and be
connected; all local actors and exits must be reachable. Avoid oversized icons
covering adjacent entrances and keep lake-bank clearings small.

Choose 0.4.0 under `fork.curse-of-strahd`, requiring a new run because the saved
world embeds its POI roster. Preserve frozen 0.3.0 and earlier packages/profiles.
No migration, commits, publishing, visible launches or unrelated theme changes.
Reuse the verified GITBaroviaMap client because production Java is unchanged;
verify its bytes against current compiled engine classes and package current
resources/source snapshots. Native fixture Java is compiled separately against
that client. Heavy jobs use the shared runner and capped, isolated native runs.

Verify missing destinations first fail, then metadata/catalog/map references,
local/global collision routes and no-effects dialogue contracts. Native checks
visit all 26 scenes and both interactions, exit each, save/reload Ravenloft and
Krezk, and exercise both chapter choices and fresh Continue with the exact 34-POI
roster. Inspect representative maps and a readable labelled world preview.
Final package hashes must match current inputs. Manual walking, scene/art
readability and travel pacing remain owner acceptance with exact launch/actions.

Setting references: David's supplied map and the [official adventure contents](https://www.dndbeyond.com/sources/dnd/cos)
confirm the major destinations. The licensed chapter text is not available via
the public link; these scenes are original short adaptations, not reproductions
of published descriptions, encounters or maps. No source image is imported.
