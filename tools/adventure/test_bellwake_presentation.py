"""Asset safety contracts for Bellwake's authored maps and streamed ambience."""
import array
import base64
from collections import deque
import hashlib
import json
from pathlib import Path
import struct
import unittest
import wave
import xml.etree.ElementTree as ET
import zlib

ROOT = Path(__file__).resolve().parents[2]
ADVENTURE = ROOT / "forge-gui/res/adventure"
MAPS = ADVENTURE / "common/maps/map/bellwake"


def inspect_map(path):
    root = ET.parse(path).getroot()
    width, height = int(root.get("width")), int(root.get("height"))
    collisions = {}
    for reference in root.findall("tileset"):
        first = int(reference.get("firstgid"))
        tileset = ET.parse(path.parent / reference.get("source")).getroot()
        for tile in tileset.findall("tile"):
            rects = []
            for obj in tile.findall("./objectgroup/object"):
                rects.append(tuple(float(obj.get(k, 0)) for k in ("x", "y", "width", "height")))
            collisions[first + int(tile.get("id"))] = rects
    blocked = set()
    layers = {}
    for layer in root.findall("layer"):
        data = zlib.decompress(base64.b64decode(layer.find("data").text))
        cells = struct.unpack("<" + str(width * height) + "I", data)
        layers[layer.get("name")] = cells
        for index, gid in enumerate(cells):
            for x, y, w, h in collisions.get(gid & 0x1fffffff, []):
                # An eight-pixel foot box must fit at the cell center.
                if x < 12 and y < 12 and x + w > 4 and y + h > 4:
                    blocked.add((index % width, index // width))
    return root, width, height, blocked, layers


def reachable(width, height, blocked, start):
    seen = set()
    queue = deque([start])
    while queue:
        x, y = queue.popleft()
        if (x, y) in seen or (x, y) in blocked or not (0 <= x < width and 0 <= y < height):
            continue
        seen.add((x, y))
        queue.extend(((x-1, y), (x+1, y), (x, y-1), (x, y+1)))
    return seen


class BellwakePresentationTest(unittest.TestCase):
    def test_new_geometry_has_a_new_content_identity(self):
        self.assertEqual(json.loads((ADVENTURE / "Bellwake/campaign.json").read_text())["version"], "0.3.0")

    def test_maps_fill_portrait_and_preserve_actor_world_positions(self):
        expected = {"quay": {38: (208, 0), 60: (224, 51), 61: (112, 64), 62: (176, 32)},
                    "toll": {38: (192, -9), 60: (214, 169), 61: (160, 40)}}
        for name, actors in expected.items():
            root, width, height, _, _ = inspect_map(MAPS / (name + ".tmx"))
            self.assertEqual((width, height), (30, 30), name)
            actual = {int(o.get("id")): (float(o.get("x")), height * 16 - float(o.get("y")))
                      for o in root.findall("./objectgroup/object")}
            self.assertEqual(actual, actors, name)

    def test_all_interactions_and_three_tile_landing_are_accessible(self):
        for name in ("quay", "toll"):
            root, width, height, blocked, _ = inspect_map(MAPS / (name + ".tmx"))
            seen = reachable(width, height, blocked, (13, height - 2))
            for obj in root.findall("./objectgroup/object"):
                if int(obj.get("id")) == 38:
                    continue
                target = (int(float(obj.get("x")) / 16), int(float(obj.get("y")) / 16))
                self.assertTrue(target in seen, (name, obj.get("id"), target))
            for x in (12, 13, 14):
                for y in range(height - 4, height):
                    self.assertNotIn((x, y), blocked, (name, x, y))
            self.assertTrue((15, 5) in blocked, "River must keep the player on the quay/causeway")

    def test_route_check_rejects_a_sealed_landing(self):
        _, width, height, blocked, _ = inspect_map(MAPS / "toll.tmx")
        sealed = blocked | {(x, height - 3) for x in range(width)}
        seen = reachable(width, height, sealed, (13, height - 2))
        self.assertNotIn((13, height - 6), seen)

    def test_streamed_audio_has_safe_levels_fades_and_distinct_contexts(self):
        fingerprints = set()
        for context in ("town", "black", "cave", "boss", "match"):
            files = list((ADVENTURE / "Bellwake/music" / context).glob("*.wav"))
            self.assertEqual(len(files), 1, context)
            with wave.open(str(files[0]), "rb") as audio:
                self.assertEqual((audio.getnchannels(), audio.getsampwidth(), audio.getframerate()), (1, 2, 22050))
                self.assertGreaterEqual(audio.getnframes(), 48 * 22050)
                pcm = audio.readframes(audio.getnframes())
            samples = array.array("h", pcm)
            peak = max(abs(v) for v in samples) / 32768
            rms = (sum((v / 32768) ** 2 for v in samples) / len(samples)) ** 0.5
            self.assertLess(peak, 0.27, context)
            self.assertGreater(rms, 0.005, context)
            self.assertLess(rms, 0.09, context)
            self.assertLess(abs(samples[0]), 100)
            self.assertLess(abs(samples[-1]), 100)
            fingerprints.add(hashlib.sha256(pcm).hexdigest())
        self.assertEqual(len(fingerprints), 5)


if __name__ == "__main__":
    unittest.main()
