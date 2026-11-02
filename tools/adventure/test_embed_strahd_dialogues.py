"""Authoring checks: catch missing actors before shipping a story catalog."""
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
import xml.etree.ElementTree as ET

SPEC = importlib.util.spec_from_file_location("embed", Path(__file__).with_name("embed_strahd_dialogues.py"))


class EmbeddingTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.maps = self.root / "maps"
        self.maps.mkdir()
        self.map = self.maps / "village.tmx"
        self.map.write_text('<map><objectgroup><object id="60"><properties>'
                            '<property name="cosDialogKey" value="village.ismark"/>'
                            '<property name="dialog" value="[]"/>'
                            '</properties></object></objectgroup></map>', encoding="utf-8")
        self.catalog = self.root / "dialogues.json"
        self.catalog.write_text(json.dumps({"village.ismark": {"dialog": [{"text": "A <safe> road?"}]}}), encoding="utf-8")
        self.module = importlib.util.module_from_spec(SPEC)
        SPEC.loader.exec_module(self.module)

    def test_embeds_round_trip_and_check_detects_stale_maps(self):
        with self.assertRaisesRegex(ValueError, "stale"):
            self.module.embed(self.catalog, self.maps, check=True)
        self.assertEqual(self.module.embed(self.catalog, self.maps), 1)
        self.assertEqual(self.module.embed(self.catalog, self.maps, check=True), 1)
        prop = ET.parse(self.map).find(".//property[@name='dialog']")
        self.assertEqual(json.loads(prop.get("value")), [{"text": "A <safe> road?"}])

    def test_unknown_key_rejected_without_partial_writes(self):
        before = self.map.read_bytes()
        self.catalog.write_text('{}', encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "missing"):
            self.module.embed(self.catalog, self.maps)
        self.assertEqual(self.map.read_bytes(), before)

    def test_unused_keys_rejected(self):
        data = json.loads(self.catalog.read_text())
        data["missing.actor"] = {"dialog": []}
        self.catalog.write_text(json.dumps(data), encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "unused"):
            self.module.embed(self.catalog, self.maps)

    def test_duplicate_actor_key_rejected(self):
        (self.maps / "duplicate.tmx").write_bytes(self.map.read_bytes())
        with self.assertRaisesRegex(ValueError, "duplicate"):
            self.module.embed(self.catalog, self.maps)


if __name__ == "__main__":
    unittest.main()
