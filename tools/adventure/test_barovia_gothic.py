"""Presentation boundaries: routes, interactions, animation and resource isolation."""
import hashlib
import json
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

from PIL import Image
import create_strahd_maps as maps

ROOT = Path(__file__).resolve().parents[2]
ART = maps.COMMON / 'maps/tileset/strahd-gothic'
SCENES = ('village', 'church', 'road', 'tser-pool')


def digest(value):
    return hashlib.sha256(json.dumps(value, sort_keys=True).encode()).hexdigest()


def gameplay(path):
    root = ET.parse(path).getroot()
    blocked = set()
    sets = maps.tilesets(path, root)
    for layer in root.findall('layer'):
        for index, gid in enumerate(maps.decode(layer)):
            if not gid:
                continue
            first, _, _, _, collisions = max((s for s in sets if s[0] <= gid), key=lambda s: s[0])
            if gid - first in collisions:
                blocked.add((index % maps.W, index // maps.W))
    objects = []
    for obj in root.findall('./objectgroup/object'):
        props = {p.get('name'): p.get('value', p.text) for p in obj.findall('./properties/property')
                 if p.get('name') not in ('sprite', 'avatar')}
        objects.append([obj.attrib, props])
        if obj.get('type') == 'collision':
            x, y, w, h = [int(obj.get(k)) // 16 for k in ('x', 'y', 'width', 'height')]
            blocked.update((a, b) for b in range(y, y + h) for a in range(x, x + w))
    return {'objects': digest(objects), 'collision': digest(sorted(blocked))}


class BaroviaGothicTest(unittest.TestCase):
    def test_opening_scenes_use_noncolliding_animated_atmosphere(self):
        # Catches a missing tileset, opaque fog, or decoration added as a collider.
        for name in SCENES:
            root = ET.parse(maps.MAPS / (name + '.tmx')).getroot()
            refs = [r for r in root.findall('tileset') if 'strahd-gothic' in r.get('source')]
            self.assertEqual(len(refs), 1, name + ': missing gothic presentation')
            tsx = ET.parse((maps.MAPS / refs[0].get('source')).resolve()).getroot()
            self.assertEqual(tsx.findall('.//objectgroup'), [])
            self.assertGreaterEqual(len(tsx.findall('.//animation')), 4)
            fog = root.find("layer[@name='Barovian Mist']")
            self.assertIsNotNone(fog)
            self.assertLessEqual(float(fog.get('opacity')), .3)
            entry = 31 * maps.W + 20
            self.assertEqual(maps.decode(fog)[entry], 0, 'entry must stay unobscured')

    def test_all_interactions_and_effective_collisions_are_preserved(self):
        manifest = ROOT / 'tools/adventure/barovia_gothic_baseline.json'
        self.assertTrue(manifest.is_file(), 'missing pre-art geometry evidence')
        for name, expected in json.loads(manifest.read_text()).items():
            self.assertEqual(gameplay(maps.MAPS / (name + '.tmx')), expected, name)

    def test_npc_atlases_keep_native_size_and_directional_frames(self):
        for name in ('ismark', 'ireena', 'donavich', 'eva', 'strahd'):
            atlas = maps.COMMON / 'sprites/strahd' / (name + '.atlas')
            self.assertTrue(atlas.is_file(), name + ': missing distinct NPC sprite')
            text = atlas.read_text()
            for animation in ('IdleRight', 'IdleLeft', 'WalkRight', 'WalkLeft', 'Avatar'):
                self.assertIn('\n' + animation + '\n', text)
            page = Image.open(atlas.with_suffix('.png'))
            self.assertEqual(page.size, (64, 16))
            self.assertEqual(page.getchannel('A').getextrema(), (0, 255))

    def test_packed_environment_has_clean_alpha_and_small_texture_budget(self):
        page = ART / 'tiles.png'
        self.assertTrue(page.is_file(), 'missing production atlas')
        image = Image.open(page)
        self.assertLessEqual(image.width * image.height, 256 * 256)
        self.assertEqual(image.getchannel('A').getextrema(), (0, 255))
        # Native terrain cells must be fully opaque; props must have clear edges.
        self.assertEqual(image.crop((0, 0, 64, 16)).getchannel('A').getextrema(), (255, 255))
        self.assertEqual(image.crop((0, 16, 16, 48)).getpixel((0, 0))[3], 0)

    def test_authoring_pass_is_idempotent_and_preserves_dialogues(self):
        import create_barovia_gothic as art
        for name in SCENES:
            path = maps.MAPS / (name + '.tmx')
            original = path.read_bytes()
            root = ET.fromstring(original)
            art.present_map(root, name)
            ET.indent(root, space=' ')
            self.assertEqual(ET.tostring(root), ET.tostring(ET.fromstring(original)), name)

    def test_lake_and_camp_water_share_the_gothic_material(self):
        # Prevent a later packaging pass from retaining bright stock water in
        # the camp while the valley uses the new material, or vice versa.
        biome = json.loads((ROOT / 'forge-gui/res/adventure/CurseOfStrahd/world/barovia-water.json').read_text())
        self.assertEqual(biome['tilesetAtlas'], 'world/tilesets/gothic-terrain.atlas')
        root = ET.parse(maps.MAPS / 'tser-pool.tmx').getroot()
        ground = maps.decode(root.find("layer[@name='Ground']"))
        self.assertEqual(ground[16 * maps.W + 35], 23004)
        self.assertEqual(Image.open(ART / 'tiles.png').crop((64, 0, 80, 16)).getchannel('A').getextrema(), (255, 255))


if __name__ == '__main__':
    unittest.main()
