"""Reproduce eight original Barovian scenes from Forge's existing pixel tiles.

--build writes geometry with empty dialogue placeholders. Embed campaign dialogue
after building. --check reads the resulting TMX (including embedded dialogue),
checks resources, collision geometry and routes to all fixed actors. Optional
--preview-dir writes static PNGs with labelled interaction points using Pillow.
No engine, game window, external assets or published map layouts are used.
"""
import argparse
import base64
from collections import deque
import json
from pathlib import Path
import struct
import xml.etree.ElementTree as ET
import zlib

ROOT = Path(__file__).resolve().parents[2]
COMMON = ROOT / "forge-gui/res/adventure/common"
MAPS = COMMON / "maps/map/strahd"
W, H, TILE = 40, 34, 16
NC = 11904
ACTORS = {
    "village": [(60, "ismark", 20, 12, "human_m"),
                (61, "ireena", 28, 19, "human_f"),
                (62, "practice", 10, 23, "Svalich Roadwarden"),
                (63, "notice", 22, 26, None)],
    "church": [(60, "doru", 20, 8, "Doru"),
               (61, "donavich", 24, 17, "human_m"),
               (62, "register", 30, 23, None)],
    "road": [(60, "wolves", 18, 17, "Mist-Stalked Pack"),
             (61, "waystone", 12, 26, None),
             (62, "strahd", 29, 9, "undead_m")],
    "tser-pool": [(60, "eva", 17, 11, "human_f"),
                  (61, "ireena", 20, 22, "human_f"),
                  (62, "sign", 10, 27, None)],
    "west-road": [(60, "ambush", 19, 16, "Western Road Ambush"),
                  (61, "sign", 24, 26, None),
                  (62, "raven", 11, 10, None)],
    "vallaki": [(60, "gate", 20, 25, "human_m"),
                (61, "urwin", 11, 15, "human_m"),
                (62, "ireena", 27, 12, "human_f"),
                (63, "notice", 28, 23, None)],
    "saint-andral": [(60, "lucian", 18, 9, "human_m"),
                     (61, "milivoj", 8, 20, "human_m"),
                     (62, "stalker", 29, 10, "Belfry Stalker"),
                     (63, "ireena", 22, 23, "human_f")],
    "coffin-shop": [(60, "henrik", 11, 23, "human_m"),
                    (61, "spawn", 24, 12, "Coffin Shop Spawn"),
                    (62, "relic", 29, 7, None)],
}
ENEMIES = {"Svalich Roadwarden", "Doru", "Mist-Stalked Pack",
           "Western Road Ambush", "Coffin Shop Spawn", "Belfry Stalker"}
PREFIXES = {"tser-pool": "pool", "west-road": "west",
            "saint-andral": "andral", "coffin-shop": "coffin"}
LANDMARK_FILE = ROOT / "forge-gui/res/adventure/CurseOfStrahd/story/landmarks.json"
LANDMARKS = json.loads(LANDMARK_FILE.read_text(encoding="utf-8")) if LANDMARK_FILE.is_file() else []
for landmark in LANDMARKS:
    if landmark.get("questArea"):
        continue
    scene_name = "landmark-" + landmark["slug"]
    ACTORS[scene_name] = [(60, "arrival", 20, 22, "human_m" if landmark["name"] == "Krezk" else None),
                          (61, "detail", 28, 13, None)]
    PREFIXES[scene_name] = "landmark." + landmark["slug"]

if any(p.get("questArea") for p in LANDMARKS):
    ACTORS.update({
        "landmark-krezk": [(60,"captain",20,13,"human_m"),(61,"wolves",20,23,"Krezk Mist Wolves"),(62,"sign",29,26,None)],
        "krezk-village": [(60,"dmitri",20,10,"human_m"),(61,"anna",11,18,"human_f"),(62,"notice",29,25,None)],
        "krezk-pool": [(60,"watcher",20,23,"Spring Watcher"),(61,"water",20,15,None),(62,"shrine",29,17,None)],
        "krezk-abbey": [(60,"abbot",13,11,"human_m"),(61,"sentinel",20,19,"Abbey Sentinel"),
                         (62,"vasilka",29,12,"human_f"),(63,"bell",27,26,None)]})
    PREFIXES["landmark-krezk"] = "krezk-gate"
    ENEMIES.update({"Krezk Mist Wolves","Spring Watcher","Abbey Sentinel"})


def property_node(parent, name, value, kind=None):
    attrs = {"name": name}
    if kind:
        attrs["type"] = kind
    node = ET.SubElement(parent, "property", attrs)
    if name in ("dialog", "defeatDialog"):
        node.text = value
    else:
        node.set("value", str(value))


class Scene:
    def __init__(self, name, base):
        self.name = name
        self.layers = {n: [0] * (W * H) for n in
                       ("Background", "Ground", "Foreground", "AboveSprites")}
        self.layers["Background"] = [NC + base] * (W * H)
        self.blocks = set()
        self.rectangles = []
        self.block(0, 0, W, 1)
        self.block(0, H - 1, W, 1)
        self.block(0, 1, 1, H - 2)
        self.block(W - 1, 1, 1, H - 2)

    def put(self, layer, x, y, gid):
        assert 0 <= x < W and 0 <= y < H, (self.name, x, y)
        self.layers[layer][y * W + x] = gid

    def area(self, x, y, w, h, gids, layer="Ground"):
        for py in range(y, y + h):
            for px in range(x, x + w):
                self.put(layer, px, py, gids[(px * 7 + py * 11) % len(gids)])

    def block(self, x, y, w, h):
        self.rectangles.append((x, y, w, h))
        self.blocks.update((px, py) for py in range(y, y + h) for px in range(x, x + w))

    def stamp(self, x, y, atlas_x, atlas_y, w, h, building=False):
        # Atlas graphics keep their native colours; explicit collision covers
        # their footprints, and the player can approach their visible fronts.
        columns, first = (28, 10113) if building else (158, NC + 1)
        for dy in range(h):
            for dx in range(w):
                self.put("Foreground", x + dx, y + dy,
                         first + (atlas_y + dy) * columns + atlas_x + dx)
        self.block(x, y, w, h)

    def tree(self, x, y, pine=False):
        self.stamp(x, y, 111, 9 if pine else 3, 3, 3)

    def bench(self, x, y, w=3):
        self.area(x, y, w, 1, [NC + 6184], "Foreground")
        self.block(x, y, w, 1)

    def prop(self, x, y, gid):
        self.put("Foreground", x, y, NC + gid)
        self.block(x, y, 1, 1)

    def save(self):
        root = ET.Element("map", version="1.10", tiledversion="1.10.1",
                          orientation="orthogonal", renderorder="right-down",
                          width=str(W), height=str(H), tilewidth="16", tileheight="16",
                          infinite="0", nextlayerid="6", nextobjectid=str(1000 + len(self.rectangles)))
        for first, source in [(1, "main"), (10113, "buildings"), (11905, "main-nocollide")]:
            ET.SubElement(root, "tileset", firstgid=str(first), source=f"../../tileset/{source}.tsx")
        for i, (name, cells) in enumerate(self.layers.items(), 1):
            layer = ET.SubElement(root, "layer", id=str(i), name=name, width=str(W), height=str(H))
            if name == "Foreground":
                property_node(ET.SubElement(layer, "properties"), "spriteLayer", "true", "bool")
            data = ET.SubElement(layer, "data", encoding="base64", compression="zlib")
            data.text = base64.b64encode(zlib.compress(struct.pack(f"<{W * H}I", *cells))).decode()
        objects = ET.SubElement(root, "objectgroup", id="5", name="Objects")
        ET.SubElement(objects, "object", id="38", template="../../obj/entry_up.tx", x="320", y="528")
        prefix = PREFIXES.get(self.name, self.name)
        for ident, key, x, y, sprite in ACTORS[self.name]:
            enemy = sprite in ENEMIES
            obj = ET.SubElement(objects, "object", id=str(ident),
                                template=f"../../obj/{'enemy' if enemy else 'dialog'}.tx",
                                x=str(x * TILE), y=str((y + 1) * TILE))
            props = ET.SubElement(obj, "properties")
            property_node(props, "cosDialogKey", f"{prefix}.{key}")
            property_node(props, "dialog", "[]")
            if enemy:
                property_node(props, "enemy", sprite)
                property_node(props, "defeatDialog", "[]")
            else:
                property_node(props, "hidden", "false", "bool")
                if sprite:
                    property_node(props, "sprite", f"sprites/heroes/{sprite}.atlas")
        for i, (x, y, w, h) in enumerate(self.rectangles, 1000):
            ET.SubElement(objects, "object", id=str(i), name="scenery footprint", type="collision",
                          x=str(x * TILE), y=str(y * TILE), width=str(w * TILE), height=str(h * TILE))
        ET.indent(root, space=" ")
        MAPS.mkdir(parents=True, exist_ok=True)
        ET.ElementTree(root).write(MAPS / f"{self.name}.tmx", encoding="utf-8", xml_declaration=True)


def compose():
    village = Scene("village", 1797)
    village.area(17, 3, 7, 30, [NC + 6611, NC + 6611, NC + 6638])
    village.area(7, 15, 27, 10, [NC + 6611, NC + 6611, NC + 6640])
    village.area(6, 8, 28, 3, [NC + 1230])
    village.area(8, 9, 4, 16, [NC + 1230])
    village.area(27, 8, 4, 18, [NC + 1230])
    # Distinct village street fronts flank the central square.
    for x, y, ax in [(6, 4, 15), (27, 4, 16), (5, 12, 17),
                     (30, 12, 21), (5, 27, 22), (29, 27, 23)]:
        village.stamp(x, y, ax, 33, 1, 2, building=True)
    village.stamp(14, 4, 18, 39, 2, 2, building=True)
    village.stamp(24, 4, 18, 39, 2, 2, building=True)
    village.prop(20, 19, 6182)
    village.prop(20, 20, 6183)
    for x, y in [(16, 16), (24, 16), (16, 24), (24, 24)]:
        village.prop(x, y, 6017)
    village.bench(12, 18)
    village.bench(25, 22)
    for x, y in [(1, 2), (34, 2), (1, 19), (35, 21), (10, 29), (25, 29)]:
        village.tree(x, y)
    village.save()

    church = Scene("church", 6611)
    # A long roofless cutaway sanctuary, altar and restrained crypt enclosure.
    church.area(8, 5, 25, 26, [NC + 6164])
    church.area(18, 5, 5, 27, [NC + 6611, NC + 6638])
    church.stamp(18, 1, 4, 37, 4, 4, building=True)
    for x in (7, 33):
        church.area(x, 5, 1, 26, [NC + 5697], "Foreground")
        church.block(x, 5, 1, 26)
    for y in (5, 30):
        for x, width in [(8, 10), (23, 10)]:
            church.area(x, y, width, 1, [NC + 5692], "Foreground")
            church.block(x, y, width, 1)
    # Pews leave a five-tile central aisle and side access to every row.
    for y in (16, 20, 24, 28):
        church.bench(11, y, 5)
        church.bench(25, y, 5)
    for x in (16, 24):
        church.prop(x, 11, 6017)
    church.bench(18, 12, 5)
    church.prop(29, 22, 6656)
    for x, y in [(3, 8), (3, 13), (36, 8), (36, 13), (3, 23), (36, 23)]:
        church.prop(x, y, 5702)
    church.save()

    road = Scene("road", 1797)
    # Wide switchbacks provide readable walking space without forced combat.
    for x, y, w, h in [(18, 26, 6, 7), (10, 24, 14, 6), (10, 16, 6, 12),
                       (10, 14, 21, 6), (26, 6, 6, 13), (19, 4, 13, 5)]:
        road.area(x, y, w, h, [NC + 1230])
    # Trees frame the route; no tree overlaps the broad bends or actor cells.
    for x, y in [(2, 2), (6, 2), (11, 3), (15, 1), (34, 2), (2, 7), (6, 8),
                 (18, 8), (22, 9), (34, 10), (2, 14), (6, 16), (34, 17),
                 (19, 21), (24, 21), (28, 23), (34, 25), (2, 23), (5, 28),
                 (11, 30), (26, 29), (32, 30)]:
        road.tree(x, y, pine=True)
    road.prop(11, 25, 5702)
    road.prop(30, 7, 5702)
    road.prop(17, 22, 3455)
    road.prop(32, 20, 3456)
    road.save()

    camp = Scene("tser-pool", 1797)
    # The eastern pool has a stepped shore and a west-bank camp gathering arc.
    for y in range(1, H - 1):
        start = 30 if y < 8 else 32 if y < 20 else 30 if y < 27 else 28
        camp.area(start, y, W - start - 1, 1, [NC + 1478])
        camp.block(start, y, W - start - 1, 1)
    camp.area(14, 9, 10, 20, [NC + 1230])
    camp.area(18, 27, 6, 6, [NC + 1230])
    camp.area(7, 24, 17, 5, [NC + 1230])
    camp.stamp(15, 5, 0, 25, 4, 4, building=True)
    # Coloured individual tents and wagon-like stalls surround a warm fire.
    for x, y, ax in [(8, 12, 20), (9, 18, 21), (24, 11, 24), (25, 18, 25)]:
        camp.stamp(x, y, ax, 16, 1, 1, building=True)
        camp.bench(x, y + 1, 2)
    camp.prop(18, 17, 6017)
    camp.bench(15, 19)
    camp.bench(21, 17)
    camp.prop(25, 25, 6182)
    camp.prop(26, 25, 6183)
    for x, y in [(2, 2), (7, 3), (23, 2), (2, 10), (2, 17), (3, 28), (8, 30), (25, 29)]:
        camp.tree(x, y)
    camp.save()


def compose_vallaki():
    road = Scene("west-road", 1797)
    # A broad causeway crosses marshy ground, then turns toward distant walls.
    road.area(17, 2, 7, 31, [NC + 1230])
    road.area(8, 7, 23, 5, [NC + 1230])
    road.area(21, 23, 8, 6, [NC + 1230])
    for x, y, w, h in [(3, 14, 10, 5), (27, 15, 9, 6), (5, 24, 7, 5)]:
        road.area(x, y, w, h, [NC + 1478])
        road.block(x, y, w, h)
    for x, y in [(2, 2), (7, 2), (27, 2), (33, 5), (2, 8), (33, 24),
                 (12, 28), (27, 29), (3, 29), (31, 10)]:
        road.tree(x, y, pine=True)
    road.prop(25, 25, 5702)
    road.prop(10, 9, 5702)
    road.save()

    town = Scene("vallaki", 1797)
    town.area(17, 2, 7, 31, [NC + 6611, NC + 6638])
    town.area(7, 10, 26, 15, [NC + 6611, NC + 6611, NC + 6640])
    # A walled town square, visible gate opening and lantern-lined inn frontage.
    for x in (3, 36):
        town.area(x, 3, 1, 26, [NC + 5697], "Foreground")
        town.block(x, 3, 1, 26)
    town.area(4, 3, 32, 1, [NC + 5692], "Foreground")
    town.block(4, 3, 32, 1)
    for x, w in [(4, 13), (24, 12)]:
        town.area(x, 28, w, 1, [NC + 5692], "Foreground")
        town.block(x, 28, w, 1)
    town.stamp(7, 7, 0, 25, 4, 4, building=True)
    town.stamp(27, 5, 4, 37, 4, 4, building=True)
    for x, y, ax in [(6, 20, 17), (31, 17, 21), (30, 25, 23)]:
        town.stamp(x, y, ax, 33, 1, 2, building=True)
    town.bench(8, 17, 4)
    town.bench(26, 19, 4)
    for x, y in [(15, 11), (24, 11), (15, 23), (24, 23)]:
        town.prop(x, y, 6017)
    town.prop(20, 16, 6182)
    town.prop(20, 17, 6183)
    town.save()

    church = Scene("saint-andral", 1797)
    # A bright nave with an open western graveyard and an eastern belfry court.
    church.area(12, 5, 14, 25, [NC + 6164])
    church.area(17, 4, 6, 29, [NC + 6611, NC + 6638])
    church.area(4, 15, 9, 12, [NC + 1230])
    church.area(26, 6, 8, 22, [NC + 6611])
    for x in (12, 25):
        for y, h in [(5, 12), (23, 7)]:
            church.area(x, y, 1, h, [NC + 5697], "Foreground")
            church.block(x, y, 1, h)
    church.stamp(17, 1, 4, 37, 4, 3, building=True)
    church.bench(17, 6, 6)
    for y in (13, 17, 21, 25):
        church.bench(14, y, 3)
        church.bench(23, y, 2)
    for x, y in [(5, 16), (9, 16), (5, 24), (9, 24)]:
        church.prop(x, y, 5702)
    for x, y in [(15, 8), (23, 8), (27, 6), (32, 6)]:
        church.prop(x, y, 6017)
    church.tree(2, 4)
    church.tree(33, 28)
    church.save()

    shop = Scene("coffin-shop", 6611)
    # Cutaway workshop with long timber stacks and separate open storage aisles.
    shop.area(5, 4, 30, 26, [NC + 6164])
    shop.area(18, 4, 5, 29, [NC + 6611])
    for x in (4, 35):
        shop.area(x, 3, 1, 27, [NC + 5697], "Foreground")
        shop.block(x, 3, 1, 27)
    shop.area(5, 3, 30, 1, [NC + 5692], "Foreground")
    shop.block(5, 3, 30, 1)
    for x, w in [(5, 13), (23, 12)]:
        shop.area(x, 29, w, 1, [NC + 5692], "Foreground")
        shop.block(x, 29, w, 1)
    for x in (7, 13, 26, 31):
        for y in (10, 16):
            shop.bench(x, y, 3)
            shop.bench(x, y + 1, 3)
    shop.bench(7, 25, 8)
    shop.bench(26, 24, 6)
    shop.prop(28, 6, 6656)
    shop.prop(6, 21, 6017)
    shop.prop(33, 21, 6017)
    shop.save()


def decode(layer):
    return struct.unpack(f"<{W * H}I", zlib.decompress(base64.b64decode(layer.find("data").text)))


def tilesets(path, root):
    result = []
    for ref in root.findall("tileset"):
        source = (path.parent / ref.get("source")).resolve()
        tree = ET.parse(source).getroot()
        atlas = source.parent / tree.find("image").get("source")
        assert atlas.is_file(), atlas
        blocked = {int(tile.get("id")) for tile in tree.findall("tile") if tile.find("objectgroup/object") is not None}
        result.append((int(ref.get("firstgid")), int(tree.get("tilecount")), int(tree.get("columns")), atlas, blocked))
    return result


def validate():
    for name, expected in ACTORS.items():
        path = MAPS / f"{name}.tmx"
        root = ET.parse(path).getroot()
        assert (int(root.get("width")), int(root.get("height"))) == (W, H)
        sets = tilesets(path, root)
        blocked = set()
        for layer in root.findall("layer"):
            for index, gid in enumerate(decode(layer)):
                if not gid:
                    continue
                first, count, _, _, collisions = max((t for t in sets if t[0] <= gid), key=lambda t: t[0])
                assert 0 <= gid - first < count, (name, "tile out of bounds", gid)
                if gid - first in collisions:
                    blocked.add((index % W, index // W))
        objects = {int(o.get("id")): o for o in root.findall("./objectgroup/object")}
        for o in objects.values():
            if o.get("type") == "collision":
                x, y, w, h = [int(o.get(k)) // TILE for k in ("x", "y", "width", "height")]
                assert 0 <= x < x + w <= W and 0 <= y < y + h <= H
                blocked.update((px, py) for py in range(y, y + h) for px in range(x, x + w))
        assert all((x, y) in blocked for y in (0, H - 1) for x in range(W))
        assert all((x, y) in blocked for x in (0, W - 1) for y in range(H))
        assert objects[38].get("template") == "../../obj/entry_up.tx"
        # Entry at (20,32) points north; ensure a clear three-tile-wide landing.
        assert not blocked.intersection((x, y) for x in range(19, 22) for y in range(30, 33))
        reachable, pending = {(20, 31)}, deque([(20, 31)])
        while pending:
            x, y = pending.popleft()
            for p in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
                if 0 <= p[0] < W and 0 <= p[1] < H and p not in blocked and p not in reachable:
                    reachable.add(p)
                    pending.append(p)
        for ident, key, x, y, sprite in expected:
            o = objects[ident]
            assert (int(o.get("x")) // TILE, int(o.get("y")) // TILE - 1) == (x, y)
            assert (x, y) in reachable, (name, key, "actor inaccessible")
            # Player-sized approach beside the actor, rather than diagonal-only access.
            assert any(all((x + dx * step, y + dy * step) in reachable for step in (1, 2))
                       for dx, dy in [(1, 0), (-1, 0), (0, 1), (0, -1)]), (name, key, "narrow approach")
            props = {p.get("name"): p.get("value", p.text) for p in o.findall("./properties/property")}
            prefix = PREFIXES.get(name, name)
            assert props["cosDialogKey"] == f"{prefix}.{key}"
            assert isinstance(json.loads(props["dialog"]), list)
            assert (path.parent / o.get("template")).is_file()
            if sprite in ENEMIES:
                assert props["enemy"] == sprite and isinstance(json.loads(props["defeatDialog"]), list)
                assert o.get("template").endswith("/enemy.tx")
            elif sprite:
                assert (COMMON / props["sprite"]).is_file()
        print(f"{name}: valid tiles/resources, enclosed boundary, clear entry 38, "
              f"{len(expected)} reachable actors; {len(reachable)} walkable cells")


def preview(directory):
    from PIL import Image, ImageDraw
    directory.mkdir(parents=True, exist_ok=True)
    for name in ACTORS:
        path = MAPS / f"{name}.tmx"
        root = ET.parse(path).getroot()
        sets = [(a, c, Image.open(d).convert("RGBA")) for a, _, c, d, _ in tilesets(path, root)]
        canvas = Image.new("RGBA", (W * TILE, H * TILE), "#111720")
        for layer in root.findall("layer"):
            for index, gid in enumerate(decode(layer)):
                if not gid:
                    continue
                first, columns, atlas = max((t for t in sets if t[0] <= gid), key=lambda t: t[0])
                local = gid - first
                x, y = local % columns * TILE, local // columns * TILE
                canvas.alpha_composite(atlas.crop((x, y, x + TILE, y + TILE)),
                                       (index % W * TILE, index // W * TILE))
        canvas = canvas.resize((W * 32, H * 32), Image.Resampling.NEAREST).convert("RGB")
        draw = ImageDraw.Draw(canvas)
        # Mark interactions and exit; these are a geometry review overlay, not
        # a claim that this static image reproduces animated in-engine sprites.
        for ident, key, x, y, _ in [(38, "EXIT / ENTRY", 20, 32, None)] + ACTORS[name]:
            draw.rectangle((x * 32, y * 32, x * 32 + 31, y * 32 + 31), outline="#ffe8a3", width=2)
            label = f"{ident} {key}"
            box = draw.textbbox((x * 32, y * 32 - 15), label)
            draw.rectangle(box, fill="#111720")
            draw.text((x * 32, y * 32 - 15), label, fill="#ffe8a3")
        canvas.save(directory / f"{name}.png")
        print(directory / f"{name}.png")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--build", action="store_true")
    parser.add_argument("--build-chapter-two", action="store_true",
                        help="Write only the new geometry, preserving opening maps")
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--preview-dir", type=Path)
    args = parser.parse_args()
    if args.build:
        compose()
        compose_vallaki()
        if LANDMARKS:
            from create_strahd_landmarks import compose as compose_landmarks
            compose_landmarks()
        if any(p.get("questArea") for p in LANDMARKS):
            from create_strahd_krezk import compose as compose_krezk
            compose_krezk()
    elif args.build_chapter_two:
        compose_vallaki()
    if args.check or args.build or args.build_chapter_two:
        validate()
    if args.preview_dir:
        preview(args.preview_dir)
