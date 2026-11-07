"""Pack original gothic artwork and apply presentation to the four opening maps.

Run after geometry generation and dialogue embedding. Repeated runs are safe.
Pillow only crops/scales/packs the generated sources; no network or game launch.
Standard Tiled animated tiles supply mist and lamplight, without engine changes.
"""
import argparse
import base64
import json
import math
from pathlib import Path
import struct
import xml.etree.ElementTree as ET
import zlib

from PIL import Image
import create_strahd_maps as maps

ROOT = maps.ROOT
COMMON = maps.COMMON
SOURCE = Path(__file__).parent / 'art/barovia'
ART = COMMON / 'maps/tileset/strahd-gothic'
NPC = COMMON / 'sprites/strahd'
CAMPAIGN = ROOT / 'forge-gui/res/adventure/CurseOfStrahd'
FIRST = 23000  # Beyond all three inherited tilesets; existing GIDs retain meaning.
SCENES = ('village', 'church', 'road', 'tser-pool')
NEAREST = Image.Resampling.NEAREST


def cell(sheet, col, row, rows=4):
    w, h = sheet.size
    return sheet.crop((round(col * w / 4), round(row * h / rows),
                       round((col + 1) * w / 4), round((row + 1) * h / rows)))


def fit(source, size):
    # Ignore almost-transparent generation fringe when finding the silhouette.
    bounds = source.getchannel('A').point(lambda a: 255 if a >= 32 else 0).getbbox()
    sprite = source.crop(bounds)
    sprite.thumbnail(size, NEAREST)
    result = Image.new('RGBA', size)
    result.alpha_composite(sprite, ((size[0] - sprite.width) // 2, size[1] - sprite.height))
    result.putalpha(result.getchannel('A').point(lambda a: 0 if a < 32 else 255 if a > 224 else a))
    return result


def local(x, y):
    return y // 16 * 16 + x // 16


def pack():
    ART.mkdir(parents=True, exist_ok=True)
    NPC.mkdir(parents=True, exist_ok=True)
    sheet = Image.open(SOURCE / 'environment.png').convert('RGBA')
    page = Image.new('RGBA', (256, 256))
    for x in range(4):
        tile = cell(sheet, x, 0).resize((16, 16), Image.Resampling.BOX)
        tile.putalpha(255)
        page.paste(tile, (x * 16, 0))
    water = Image.open(SOURCE / 'water.png').convert('RGBA').resize((16, 16), Image.Resampling.BOX)
    water.putalpha(255)
    page.paste(water, (64, 0))
    placements = [
        (0, 1, 0, 16, 16, 32), (1, 1, 16, 16, 16, 32),
        (2, 1, 32, 16, 16, 32), (3, 1, 48, 16, 32, 32),
        (0, 2, 0, 48, 48, 48), (1, 2, 48, 48, 48, 48),
        (2, 2, 96, 48, 16, 16), (3, 2, 112, 48, 16, 16),
        (0, 3, 128, 48, 16, 16), (1, 3, 144, 48, 16, 32),
        (2, 3, 160, 48, 16, 16), (3, 3, 176, 48, 16, 16),
        (2, 1, 192, 0, 64, 64),
    ]
    for col, row, x, y, w, h in placements:
        page.paste(fit(cell(sheet, col, row), (w, h)), (x, y))

    # Precomputed native-size effect tiles. The renderer animates these through
    # Tiled's normal clock; they create no actors, lights or collision objects.
    # Sixteen spatial samples of a 128x32 ribbon prevent a 16px repeating
    # "picket fence". Every sample has eight phase frames and shares the clock.
    for position in range(16):
        for phase in range(8):
            fog = Image.new('RGBA', (16, 16))
            for y in range(16):
                for x in range(16):
                    gx = position % 8 * 16 + x + phase * 16
                    gy = position // 8 * 16 + y
                    wave = .6 + .4 * math.sin(gx * math.tau / 128 + .4 * math.sin(gy / 10))
                    envelope = math.sin((gy + .5) * math.pi / 32) ** 2
                    fog.putpixel((x, y), (173, 187, 193, int(65 * wave * envelope)))
            index = 112 + position * 8 + phase
            page.paste(fog, (index % 16 * 16, index // 16 * 16))
    for phase in range(8):
        light = Image.new('RGBA', (16, 16))
        for y in range(16):
            for x in range(16):
                radius = math.hypot(x - 7.5, y - 7.5) / 11
                alpha = max(0, int((1 - radius) * (65 + 12 * math.sin(phase * 2))))
                light.putpixel((x, y), (244, 172, 72, alpha))
        page.paste(light, (phase * 16, 240))
    page.save(ART / 'tiles.png')
    tsx = ET.Element('tileset', version='1.10', tiledversion='1.10.1',
                     name='strahd-gothic', tilewidth='16', tileheight='16',
                     tilecount='256', columns='16')
    ET.SubElement(tsx, 'image', source='tiles.png', width='256', height='256')
    for position in range(16):
        base = 112 + position * 8
        tile = ET.SubElement(tsx, 'tile', id=str(base))
        anim = ET.SubElement(tile, 'animation')
        for phase in range(8):
            ET.SubElement(anim, 'frame', tileid=str(base + phase), duration='400')
    for offset in range(4):
        tile = ET.SubElement(tsx, 'tile', id=str(240 + offset))
        anim = ET.SubElement(tile, 'animation')
        for phase in range(8):
            ET.SubElement(anim, 'frame', tileid=str(240 + (phase + offset * 2) % 8), duration='130')
    write_xml(tsx, ART / 'tiles.tsx')

    sheet = Image.open(SOURCE / 'npcs.png').convert('RGBA')
    for row, name in enumerate(('ismark', 'ireena', 'donavich', 'eva', 'strahd')):
        page = Image.new('RGBA', (64, 16))
        for col in range(4):
            page.paste(fit(cell(sheet, col, row, rows=5), (16, 16)), (col * 16, 0))
        page.save(NPC / (name + '.png'))
        atlas = [name + '.png', 'size: 64,16', 'format: RGBA8888',
                 'filter: Nearest,Nearest', 'repeat: none']
        for animation in ('Idle', 'Walk', 'Attack', 'Hit', 'Death', 'Wake'):
            for direction, offset in [('Right', 0), ('Left', 2)]:
                for frame in range(2):
                    atlas += [animation + direction, '  xy: %d, 0' % ((offset + frame) * 16),
                              '  size: 16, 16', '  index: %d' % frame]
        atlas += ['Avatar', '  xy: 0, 0', '  size: 16, 16']
        (NPC / (name + '.atlas')).write_text('\n'.join(atlas) + '\n', encoding='utf-8')
    pack_world()


def pack_world():
    # Preserve the engine's existing autotile edge masks, including half-tile
    # corners, while supplying new material pixels from the generated sheet.
    source = Image.open(COMMON / 'world/tilesets/autotiles.png').convert('RGBA')
    tiles = Image.open(ART / 'tiles.png')
    page = Image.new('RGBA', (240, 64))
    atlas = ['gothic-terrain.png', 'size: 240,64', 'format: RGBA8888',
             'filter: Nearest,Nearest', 'repeat: none']
    for i, (name, maskxy, material) in enumerate([
            ('Red_2', (96, 128), 0), ('Black', (0, 320), 0),
            ('Colorless', (0, 64), 3), ('Road', (96, 512), 1),
            ('Base_1', (48, 0), 4)]):
        region = Image.new('RGBA', (48, 64))
        texture = tiles.crop((material * 16, 0, material * 16 + 16, 16))
        for y in range(0, 64, 16):
            for x in range(0, 48, 16):
                region.paste(texture, (x, y))
        x, y = maskxy
        region.putalpha(source.crop((x, y, x + 48, y + 64)).getchannel('A'))
        page.paste(region, (i * 48, 0))
        atlas += [name, '  xy: %d, 0' % (i * 48), '  size: 48, 64']
    target = CAMPAIGN / 'world/tilesets'
    target.mkdir(parents=True, exist_ok=True)
    page.save(target / 'gothic-terrain.png')
    (target / 'gothic-terrain.atlas').write_text('\n'.join(atlas) + '\n')
    # Retain every inherited region's dimensions/order so biome sprite placement
    # and random variant selection use the same catalog, with new tree pixels.
    target = CAMPAIGN / 'world/sprites'
    target.mkdir(parents=True, exist_ok=True)
    atlas = (COMMON / 'world/sprites/map_sprites.atlas').read_text()
    page = Image.open(COMMON / 'world/sprites/map_sprites.png').convert('RGBA')
    lines = atlas.splitlines()
    for i, line in enumerate(lines):
        if line.strip() in ('WoodTree', 'WoodTree2', 'MountainTree'):
            x, y = [int(v) for v in lines[i + 1].split(':')[1].split(',')]
            pine = line.strip() != 'WoodTree'
            tree = tiles.crop((48 if pine else 0, 48, 96 if pine else 48, 96))
            page.paste(tree.resize((16, 16), NEAREST), (x, y))
    page.save(target / 'gothic-sprites.png')
    (target / 'gothic-sprites.atlas').write_text(atlas.replace('map_sprites.png', 'gothic-sprites.png', 1))


def encode(layer, cells):
    data = layer.find('data')
    data.text = base64.b64encode(zlib.compress(struct.pack('<%dI' % len(cells), *cells))).decode()


def write_xml(root, path):
    ET.indent(root, space=' ')
    ET.ElementTree(root).write(path, encoding='utf-8', xml_declaration=True)


def present_map(root, name):
    if root.find("tileset[@source='../../tileset/strahd-gothic/tiles.tsx']") is None:
        root.insert(3, ET.Element('tileset', firstgid=str(FIRST), source='../../tileset/strahd-gothic/tiles.tsx'))
    for layer in list(root.findall('layer')):
        if layer.get('name') in ('Barovian Mist', 'Lantern Light'):
            root.remove(layer)
    layers = {l.get('name'): l for l in root.findall('layer')}
    cells = {n: list(maps.decode(l)) for n, l in layers.items()}
    old = maps.NC
    remap = {old + 1797: 0, old + 1230: 1, old + 6611: 2, old + 1478: 4,
             old + 6638: 2, old + 6640: 2, old + 6164: 3,
             old + 5702: local(96, 48), old + 6017: local(112, 48),
             old + 6184: local(176, 48), old + 6656: local(128, 48),
             old + 5697: local(160, 48), old + 5692: local(160, 48)}
    for layer, values in cells.items():
        for i, gid in enumerate(values):
            if gid in remap:
                values[i] = FIRST + remap[gid]

    foreground = cells['Foreground']
    # Existing stamped 3x3 trees keep their exact footprints and layer ordering.
    original = maps.decode(layers['Foreground'])
    for i, gid in enumerate(original):
        if gid in (old + 1 + 3 * 158 + 111, old + 1 + 9 * 158 + 111):
            pine = gid == old + 1 + 9 * 158 + 111
            for dy in range(3):
                for dx in range(3):
                    foreground[i + dy * maps.W + dx] = FIRST + local(48 if pine else 0, 48) + dy * 16 + dx
    if name == 'village':
        for x, y, variant in [(6, 4, 0), (27, 4, 1), (5, 12, 1),
                              (30, 12, 0), (5, 27, 1), (29, 27, 0)]:
            foreground[y * maps.W + x] = FIRST + local(variant * 16, 16)
            foreground[(y + 1) * maps.W + x] = FIRST + local(variant * 16, 32)
        for x in (14, 24):
            for dy in range(2):
                for dx in range(2):
                    foreground[(4 + dy) * maps.W + x + dx] = FIRST + local(48, 16) + dy * 16 + dx
        foreground[19 * maps.W + 20] = FIRST + local(144, 48)
        foreground[20 * maps.W + 20] = FIRST + local(144, 64)
    if name == 'church':
        # Chapel facade stays within the existing four-by-four blocked frontage.
        for dy in range(4):
            for dx in range(4):
                foreground[(1 + dy) * maps.W + 18 + dx] = FIRST + local(192, 0) + dy * 16 + dx
    for n, values in cells.items():
        encode(layers[n], values)

    for prop in root.findall(".//object/properties/property[@name='sprite']"):
        obj = next(o for o in root.findall('./objectgroup/object') if prop in o.findall('./properties/property'))
        key = obj.find("properties/property[@name='cosDialogKey']").get('value')
        who = key.split('.')[-1]
        if who in ('ismark', 'ireena', 'donavich', 'eva', 'strahd'):
            prop.set('value', 'sprites/strahd/' + who + '.atlas')

    actors = [(int(o.get('x')) // 16, int(o.get('y')) // 16 - 1)
              for o in root.findall('./objectgroup/object') if o.find("properties/property[@name='cosDialogKey']") is not None]
    fog = [0] * (maps.W * maps.H)
    glow = [0] * len(fog)
    for y in range(1, maps.H - 1):
        for x in range(1, maps.W - 1):
            # Leave entries, characters and interaction markers completely clear.
            safe = (abs(x - 20) <= 2 and y >= 29) or any(abs(x - a) <= 1 and abs(y - b) <= 1 for a, b in actors)
            if not safe and y % 8 in (2, 3):
                position = (y % 8 - 2) * 8 + (x + y // 8 * 3) % 8
                fog[y * maps.W + x] = FIRST + 112 + position * 8
            if cells['Foreground'][y * maps.W + x] == FIRST + local(112, 48):
                for dx, dy in [(0, 0), (0, 1), (-1, 0), (1, 0)]:
                    glow[(y + dy) * maps.W + x + dx] = FIRST + 240 + (x + y) % 4
    for ident, label, values, opacity in [(6, 'Lantern Light', glow, '0.7'),
                                         (7, 'Barovian Mist', fog, '0.28' if name != 'church' else '0.14')]:
        layer = ET.SubElement(root, 'layer', id=str(ident), name=label, width=str(maps.W),
                              height=str(maps.H), opacity=opacity)
        ET.SubElement(layer, 'data', encoding='base64', compression='zlib')
        encode(layer, values)
    root.set('nextlayerid', '8')


def build():
    pack()
    for name in SCENES:
        path = maps.MAPS / (name + '.tmx')
        root = ET.parse(path).getroot()
        present_map(root, name)
        write_xml(root, path)
    for name in ('mists', 'forest', 'hills', 'water'):
        path = CAMPAIGN / 'world' / ('barovia-' + name + '.json')
        data = json.loads(path.read_text())
        data['tilesetAtlas'] = 'world/tilesets/gothic-terrain.atlas'
        path.write_text(json.dumps(data, indent=2) + '\n')
    path = CAMPAIGN / 'world/world.json'
    data = json.loads(path.read_text())
    data['roadTileset']['tilesetAtlas'] = 'world/tilesets/gothic-terrain.atlas'
    path.write_text(json.dumps(data, indent=2) + '\n')
    path = CAMPAIGN / 'world/barovia-sprites.json'
    data = json.loads(path.read_text())
    data['textureAtlas'] = 'world/sprites/gothic-sprites.atlas'
    path.write_text(json.dumps(data, indent=2) + '\n')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--build', action='store_true')
    args = parser.parse_args()
    if args.build:
        build()
