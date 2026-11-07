import json
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
CAMPAIGN = ROOT / 'forge-gui/res/adventure/CurseOfStrahd'
MAPS = CAMPAIGN.parent / 'common/maps/map/strahd'
EXPECTED = {'Castle Ravenloft', 'Old Bonegrinder', 'Argynvostholt',
            'Tsolenka Pass', 'Ruins of Berez', "Van Richten's Tower", 'Wizard of Wines',
            'Amber Temple', 'Yester Hill', 'Werewolf Den', 'Gates of Barovia East',
            'Gates of Barovia West', 'Svalich Woods', 'River Ivlis', 'River Ivlis Crossroads',
            'Tser Falls', 'Black Carriage', 'Gates of Ravenloft', 'Lake Zarovich',
            'Mount Baratok Trail', 'Luna River Crossroads', 'Raven River Crossroads',
            'Lake Baratok', 'Lake Luna', 'Mount Ghakis Lookout'}


class ReferenceLandmarksTest(unittest.TestCase):
    def metadata(self):
        return [p for p in json.loads((CAMPAIGN / 'story/landmarks.json').read_text()) if not p.get('questArea')]

    def test_all_named_reference_sites_have_exact_world_anchors(self):
        landmarks = self.metadata()
        self.assertEqual({p['name'] for p in landmarks}, EXPECTED)
        self.assertEqual(len(landmarks), 25)
        points = json.loads((CAMPAIGN / 'world/points_of_interest.json').read_text())
        self.assertEqual(len(points), 37)
        anchors = {p['name']: (p['x'], p['y']) for p in json.loads((CAMPAIGN/'world/barovia-map.json').read_text())['locations']}
        for item in landmarks:
            self.assertNotIn(item['type'], ('town','capital'), 'Arrival visits must not trigger automatic healing')
            self.assertEqual(anchors[item['name']], (item['x'], item['y']))
            self.assertEqual(item['route'][-1], [item['x'], item['y']])
        labels = {p['reference'] for p in landmarks}
        self.assertTrue(set('BCDFHIJKLMOPQRTUVWXYZ').issubset(labels), labels)

    def test_every_arrival_has_two_safe_embedded_interactions_and_an_exit(self):
        story = json.loads((CAMPAIGN/'story/dialogues.json').read_text())
        def safe(node):
            self.assertFalse(node.get('action'), 'Arrival scenes must not change gameplay state')
            for child in node.get('options', []): safe(child)
        for item in self.metadata():
            path = MAPS / ('landmark-' + item['slug'] + '.tmx')
            self.assertTrue(path.is_file(), item['name'])
            tree = ET.parse(path)
            self.assertIsNotNone(tree.find(".//object[@id='38']"))
            for kind, actor in [('arrival',60),('detail',61)]:
                key = 'landmark.' + item['slug'] + '.' + kind
                nodes = story[key]['dialog']; self.assertEqual(len(nodes), 1)
                safe(nodes[0])
                props = {p.get('name'):p.get('value',p.text) for p in tree.findall(f".//object[@id='{actor}']/properties/property")}
                self.assertEqual(props['cosDialogKey'],key)
                self.assertEqual(json.loads(props['dialog']), nodes)
            arrival = story['landmark.' + item['slug'] + '.arrival']['dialog'][0]
            self.assertIn('future chapter', arrival['text'])
            self.assertIn('southern', arrival['text'])


if __name__ == '__main__':
    unittest.main()
