"""Shipped chapter-two POIs must resolve to authored, embedded and reachable maps."""
import json
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

import create_strahd_maps as maps
from embed_strahd_dialogues import embed

ROOT = Path(__file__).resolve().parents[2]
CAMPAIGN = ROOT / 'forge-gui/res/adventure/CurseOfStrahd'


class VallakiMapsTest(unittest.TestCase):
    def test_runtime_dialogue_roots_are_exclusive_at_each_progression_boundary(self):
        # MapDialog.activate loads every matching root; a later fallback must
        # never replace an actionable conversation (or run its effects twice).
        story = json.loads((CAMPAIGN / 'story/dialogues.json').read_text())
        flags = ['cosChapterOneComplete', 'cosWestRoadCleared', 'cosVallakiEntered',
                 'cosBonesInvestigation', 'cosCoffinLead', 'cosHenrikConfessed',
                 'cosCoffinCleared', 'cosBonesRecovered', 'cosSanctuaryDefended',
                 'cosBonesRestored', 'cosChapterTwoComplete']

        def matches(node, evidence):
            for condition in node.get('condition', []):
                result = True
                if 'checkQuestFlag' in condition:
                    result = bool(evidence.get(condition['checkQuestFlag'], 0))
                if 'getCharacterFlag' in condition:
                    query = condition['getCharacterFlag']
                    result = evidence.get(query['key'], 0) == query['val']
                if condition.get('not'):
                    result = not result
                if not result:
                    return False
            return True

        for count in range(len(flags) + 1):
            for fate in (1, 2):
                for truth in (1, 2):
                    evidence = dict.fromkeys(flags[:count], 1)
                    evidence.update(cosDoruFate=fate, cosVallakiTruth=truth)
                    states = [evidence] + [{k: v for k, v in evidence.items() if k != missing}
                                          for missing in flags[:count]]
                    for state in states:
                        for key, actor in story.items():
                            if key.split('.')[0] not in ('west', 'vallaki', 'andral', 'coffin'):
                                continue
                            visible = [root for root in actor['dialog'] if matches(root, state)]
                            self.assertEqual(len(visible), 1, (key, state, [n['text'] for n in visible]))

    def test_eight_distinct_pois_exist_in_the_generated_biome(self):
        points = json.loads((CAMPAIGN / 'world/points_of_interest.json').read_text())
        self.assertEqual(len(points), 37)
        self.assertEqual(len({p['name'] for p in points}), 37)
        biome = json.loads((CAMPAIGN / 'world/barovia-mists.json').read_text())
        self.assertEqual(set(biome['pointsOfInterest']), {p['name'] for p in points})
        self.assertEqual({p['name'] for p in points[4:8]},
                         {'West Svalich Road', 'Vallaki', "Saint Andral's Church", "Coffin Maker's Shop"})
        for point in points:
            self.assertTrue((CAMPAIGN / point['map']).resolve().is_file(), point['name'])

    def test_new_actors_are_embedded_in_exactly_one_map(self):
        expected = {'west-road': {'west.ambush', 'west.sign', 'west.raven'},
                    'vallaki': {'vallaki.gate', 'vallaki.urwin', 'vallaki.ireena', 'vallaki.notice'},
                    'saint-andral': {'andral.lucian', 'andral.milivoj', 'andral.stalker', 'andral.ireena'},
                    'coffin-shop': {'coffin.henrik', 'coffin.spawn', 'coffin.relic'}}
        for name, keys in expected.items():
            path = maps.MAPS / (name + '.tmx')
            self.assertTrue(path.is_file(), name)
            root = ET.parse(path).getroot()
            found = {p.get('value') for p in root.findall(".//property[@name='cosDialogKey']")}
            self.assertEqual(found, keys)
        self.assertEqual(embed(CAMPAIGN / 'story/dialogues.json', maps.MAPS, check=True), 90)

    def test_all_eight_scenes_have_clear_entry_and_reachable_actors(self):
        self.assertEqual(len(maps.ACTORS), 37)
        maps.validate()


if __name__ == '__main__':
    unittest.main()
