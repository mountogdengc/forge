import json
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
CAMPAIGN = ROOT / 'forge-gui/res/adventure/CurseOfStrahd'


class BaroviaGeographyTest(unittest.TestCase):
    def test_generated_data_and_all_playable_routes_are_current(self):
        import create_strahd_overworld as author
        data = json.loads((CAMPAIGN / 'world/barovia-map.json').read_text())
        author.validate(data)
        points = json.loads((CAMPAIGN / 'world/points_of_interest.json').read_text())
        self.assertEqual({p['name'] for p in points}, {p['name'] for p in data['locations']})
        world = json.loads((CAMPAIGN / 'world/world.json').read_text())
        self.assertEqual(len(world['biomesNames']), 6)
        for path in world['biomesNames']:
            biome = json.loads((CAMPAIGN / path).read_text())
            self.assertEqual(biome['terrain'], [])
            self.assertIn(biome['tilesetName'], ['Red_2','Black','Colorless','Colorless_1','Blue_2','Base_1'])

    def test_fixed_geography_replaces_the_flat_world(self):
        world = json.loads((CAMPAIGN / 'world/world.json').read_text())
        self.assertEqual(world.get('authoredMap'), 'world/barovia-map.json')
        self.assertEqual((world['width'], world['height']), (300, 180))
        data = json.loads((CAMPAIGN / world['authoredMap']).read_text())
        points = {p['name']: (p['x'], p['y']) for p in data['locations']}
        self.assertEqual(len(points), 37)
        self.assertGreater(points['Spawn'][0], points['Tser Pool'][0])
        self.assertGreater(points['Tser Pool'][0], points['Vallaki'][0])
        self.assertGreater(points['Spawn'][1], points['Vallaki'][1])
        self.assertEqual(data['rows'][42][129], 'w', 'Lake Zarovich should lie north of Vallaki')
        self.assertIn(data['rows'][158][133], 'ns', 'Southern Ghakis range')
        self.assertIn(data['rows'][14][168], 'ns', 'Northern Baratok range')


if __name__ == '__main__':
    unittest.main()
