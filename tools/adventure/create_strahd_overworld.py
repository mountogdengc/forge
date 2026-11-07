"""Original fixed Barovian geography using Forge's existing pixel terrain.

No reference-image pixels are imported. Shapes below are authored polygons,
ellipses and polylines; --check verifies committed data and walking routes.
"""
import argparse
from collections import deque
import json
import math
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
CAMPAIGN = ROOT / 'forge-gui/res/adventure/CurseOfStrahd'
COMMON = ROOT / 'forge-gui/res/adventure/common'
W, H = 300, 180
LOCATIONS = [('Spawn', 242, 113), ('Village Church', 254, 105),
             ('Old Svalich Road', 222, 126), ('Tser Pool', 196, 115),
             ('West Svalich Road', 169, 79), ('Vallaki', 125, 61),
             ("Saint Andral's Church", 113, 56), ("Coffin Maker's Shop", 138, 63)]
LANDMARK_FILE = CAMPAIGN / 'story/landmarks.json'
LANDMARKS = json.loads(LANDMARK_FILE.read_text(encoding='utf-8')) if LANDMARK_FILE.is_file() else []
LOCATIONS += [(p['name'],p['x'],p['y']) for p in LANDMARKS]
ROADS = [
    [(293, 99), (282, 103), (275, 113), (258, 114), (242, 113),
     (233, 114), (229, 121), (222, 126), (212, 130), (204, 126),
     (196, 115), (190, 108), (192, 100), (185, 94), (182, 86),
     (175, 77), (169, 79), (165, 70), (158, 68), (154, 73),
     (147, 70), (145, 62), (138, 63), (125, 61), (113, 56),
     (105, 59), (97, 65), (88, 64), (80, 57), (65, 57), (53, 60), (35, 59), (15, 54)],
    [(242, 113), (251, 114), (254, 105)],
    [(125, 61), (131, 69), (138, 63)],
    [(192, 100), (181, 106), (177, 116), (185, 129), (197, 137), (204, 126)],
    [(185, 94), (195, 91), (202, 82), (214, 84), (224, 91), (235, 89)],
    [(97, 65), (101, 80), (93, 93), (94, 111), (84, 128), (72, 143),
     (80, 151), (89, 163), (107, 174)],
    [(80, 57), (74, 73), (65, 85), (58, 102), (50, 115), (49, 127)],
]
ROADS += [p['route'] for p in LANDMARKS]
BIOMES = [('g', 'barovia-mists', 'Red_2', '6c7564', False, []),
          ('f', 'barovia-forest', 'Black', '253f38', False, ['WoodTree', 'WoodTree2']),
          ('m', 'barovia-hills', 'Colorless', '757775', True, ['MountainTree', 'MountainRock']),
          ('n', 'barovia-mountains', 'Colorless_1', '959993', True, ['LargeMountainRock']),
          ('s', 'barovia-snow', 'Blue_2', 'd6ddd9', True, ['SnowMountain']),
          ('w', 'barovia-water', 'Base_1', '739eaf', True, [])]


def geography():
    image = Image.new('L', (W, H), ord('f'))
    draw = ImageDraw.Draw(image)
    # Clear eastern valley and two smaller western/northern settlement basins.
    draw.polygon([(211, 95), (257, 96), (271, 108), (292, 112), (290, 144),
                  (251, 144), (229, 136), (210, 132), (202, 112)], fill=ord('g'))
    draw.polygon([(102, 48), (130, 46), (149, 55), (148, 70), (128, 78), (106, 67)], fill=ord('g'))
    draw.polygon([(23, 48), (60, 44), (81, 52), (73, 65), (44, 67), (22, 60)], fill=ord('g'))

    def mountain(cx, cy, rx, ry, symbol, phase=0):
        for y in range(max(0, int(cy-ry-3)), min(H, int(cy+ry+4))):
            for x in range(max(0, int(cx-rx-3)), min(W, int(cx+rx+4))):
                angle = math.atan2((y-cy)/ry, (x-cx)/rx)
                edge = 1 + .08*math.sin(angle*7+phase) + .04*math.cos(angle*13)
                if ((x-cx)/rx)**2 + ((y-cy)/ry)**2 < edge:
                    image.putpixel((x, y), ord(symbol))
    # Baratok across the north; western ridges; Ghakis in the south.
    for args in [(199, 13, 60, 20, 'm'), (201, 11, 39, 13, 'n'), (205, 9, 18, 8, 's'),
                 (68, 109, 27, 35, 'm'), (70, 110, 17, 27, 'n'), (70, 108, 9, 18, 's'),
                 (132, 160, 47, 28, 'm'), (134, 160, 33, 23, 'n'), (138, 161, 19, 17, 's'),
                 (193, 69, 14, 33, 'm'), (216, 86, 23, 10, 'm')]:
        mountain(*args)
    # Lake Zarovich, Lake Baratok and Lake Luna; winding riverbeds.
    mountain(128, 41, 36, 11, 'w', 1)
    mountain(65, 45, 7, 10, 'w', 2)
    mountain(66, 145, 19, 5, 'w', 1)
    for river in [[(197, 55), (202, 68), (198, 82), (200, 99), (202, 112),
                   (213, 126), (238, 131), (259, 128), (286, 138)],
                  [(61, 62), (65, 78), (59, 95), (63, 114), (57, 134), (66, 145)]]:
        draw.line(river, fill=ord('w'), width=2)
    # Anchor clearings prevent the generic spawn/POI padding changing the raster.
    for name, x, y in LOCATIONS:
        radius = 11 if name == 'Spawn' else next((p['clearingRadius'] for p in LANDMARKS if p['name']==name), 4)
        draw.rectangle((x-radius, y-radius, x+radius, y+radius), fill=ord('g'))
    draw.rectangle((0, 0, W-1, H-1), outline=ord('n'), width=2)
    return {'width': W, 'height': H,
            'palette': [{'symbol': symbol, 'biome': i, 'collision': blocked}
                        for i, (symbol, _, _, _, blocked, _) in enumerate(BIOMES)],
            'rows': [''.join(chr(image.getpixel((x, y))) for x in range(W)) for y in range(H)],
            'locations': [{'name': name, 'x': x, 'y': y} for name, x, y in LOCATIONS],
            'roads': [{'radius': 1, 'points': [list(point) for point in road]} for road in ROADS]}


def road_mask(data):
    mask = Image.new('1', (W, H)); draw = ImageDraw.Draw(mask)
    # Stamp a square around every Bresenham cell, as the runtime does.
    for road in data['roads']:
        for a, b in zip(road['points'], road['points'][1:]):
            x, y = a; endx, endy = b; dx, dy = abs(endx-x), -abs(endy-y)
            sx, sy = (1 if x < endx else -1), (1 if y < endy else -1); err = dx+dy
            while True:
                r = road['radius']; draw.rectangle((x-r,y-r,x+r,y+r), fill=1)
                if (x,y) == (endx,endy): break
                e = 2*err
                if e >= dy: err += dy; x += sx
                if e <= dx: err += dx; y += sy
    return mask


def validate(data):
    assert data == geography(), 'Authored geography is stale: regenerate explicitly'
    roads = road_mask(data)
    blocked = {p['symbol'] for p in data['palette'] if p['collision']}
    reachable, pending = {LOCATIONS[0][1:]}, deque([LOCATIONS[0][1:]])
    def clear(x,y):
        return 0 <= x < W and 0 <= y < H and (roads.getpixel((x,y)) or data['rows'][y][x] not in blocked)
    while pending:
        x,y = pending.popleft()
        for p in ((x-1,y),(x+1,y),(x,y-1),(x,y+1)):
            if p not in reachable and clear(*p): reachable.add(p); pending.append(p)
    for name,x,y in LOCATIONS:
        assert (x,y) in reachable, 'Unreachable '+name
        assert all(clear(x+dx,y+dy) for dx in (-1,0,1) for dy in (-1,0,1)), 'Tight approach '+name
    assert any(roads.getpixel((x,y)) and data['rows'][y][x]=='w' for y in range(H) for x in range(W)), 'No bridges'
    assert not any(roads.getpixel((x,y)) for x in range(W) for y in (0,H-1))
    print(f'Fixed Barovia: {W}x{H}, six terrains, {len(LOCATIONS)} reachable POIs, winding roads and bridges')


def build():
    def write(name, data):
        (CAMPAIGN/name).write_text(json.dumps(data, indent=2)+'\n', encoding='utf-8')
    data = geography(); write('world/barovia-map.json', data)
    names = [name for name,_,_ in LOCATIONS]
    for i, (_, name, tile, color, blocked, sprites) in enumerate(BIOMES):
        write('world/'+name+'.json', {'name': name, 'startPointX': .5, 'startPointY': .5,
              'width': 1 if i==0 else 0, 'height': 1 if i==0 else 0, 'noiseWeight':0, 'distWeight':0,
              'color':color, 'tilesetAtlas':'world/tilesets/terrain.atlas', 'tilesetName':tile,
              'collision':blocked, 'terrain':[], 'spriteNames':sprites, 'enemies':[],
              'pointsOfInterest': names if i==0 else []})
    source = json.loads((COMMON/'world/sprites/map_sprites.json').read_text())
    sprites = []
    for name in ['WoodTree','WoodTree2','MountainTree','MountainRock','LargeMountainRock']:
        template = next(s for s in source['sprites'] if s['name']==name).copy()
        template.update(startArea=0, endArea=1, density=.26 if 'WoodTree' in name else .09, resolution=4)
        sprites.append(template)
    sprites.append({'name':'SnowMountain','startArea':0,'endArea':1,'layer':1,'resolution':4,'density':.10})
    write('world/barovia-sprites.json', {'textureAtlas':source['textureAtlas'], 'sprites':sprites})
    world = json.loads((CAMPAIGN/'world/world.json').read_text())
    world.update(width=W, height=H, playerStartPosX=242/W, playerStartPosY=(H-1-113)/H,
                 authoredMap='world/barovia-map.json', biomesSprites='world/barovia-sprites.json',
                 biomesNames=['world/'+name+'.json' for _,name,*_ in BIOMES])
    write('world/world.json', world)
    manifest = json.loads((CAMPAIGN/'campaign.json').read_text())
    manifest['version']='0.5.0' if any(p.get('questArea') for p in LANDMARKS) else '0.4.0'
    write('campaign.json', manifest)
    validate(data)


def preview(path):
    data = geography(); roads = road_mask(data)
    atlas = Image.open(COMMON/'world/tilesets/autotiles.png').convert('RGBA')
    regions = {'Red_2':(96,128), 'Black':(0,320), 'Colorless':(0,64),
               'Colorless_1':(48,64),'Blue_2':(96,384),'Base_1':(48,0),'Road':(96,512)}
    result = Image.new('RGBA',(W*4,H*4))
    for y,row in enumerate(data['rows']):
        for x,char in enumerate(row):
            tile = 'Road' if roads.getpixel((x,y)) else next(b[2] for b in BIOMES if b[0]==char)
            px,py = regions[tile]; result.paste(atlas.crop((px+16,py+32,px+20,py+36)), (x*4,y*4))
    result = result.convert('RGB'); draw = ImageDraw.Draw(result)
    occupied = []
    for name,x,y in LOCATIONS:
        if name=='Spawn': name='Village of Barovia'
        draw.ellipse((x*4-3,y*4-3,x*4+3,y*4+3),fill='#e5c998',outline='#312b24')
        ref = next((p['reference'] for p in LANDMARKS if p['name']==name), '')
        if ref: name = ref + ' ' + name
        width = draw.textbbox((0,0),name)[2]
        candidates = [(x*4+dx,y*4+dy) for dy in (2,-12,14,-24,26,-36,38,-48,50,-60)
                      for dx in (5,-width-5,17,-width-17)]
        for pos in candidates:
            box=draw.textbbox(pos,name)
            if box[0]<2 or box[1]<2 or box[2]>=W*4-2 or box[3]>=H*4-2: continue
            if any(box[0]<other[2]+3 and box[2]+3>other[0] and box[1]<other[3]+3 and box[3]+3>other[1] for other in occupied): continue
            occupied.append(box);break
        else: raise ValueError('No readable preview label placement: '+name)
        if abs(pos[0]-x*4)>12 or abs(pos[1]-y*4)>14:
            draw.line((x*4,y*4,max(box[0],min(x*4,box[2])),max(box[1],min(y*4,box[3]))),fill='#cdbb91')
        draw.rectangle(box,fill='#17201f');draw.text(pos,name,fill='#f0e6cb')
    for label,x,y in [('LAKE ZAROVICH',105,37),('MOUNT BARATOK',185,12),('MOUNT GHAKIS',120,159),
                      ('Lake Baratok',46,35),('Lake Luna',48,141),('SVALICH WOODS',214,151)]:
        draw.text((x*4,y*4),label,fill='#e8e1ce',stroke_width=1,stroke_fill='#242d2d')
    path.parent.mkdir(parents=True,exist_ok=True);result.save(path);print(path)


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--build',action='store_true')
    parser.add_argument('--check',action='store_true');parser.add_argument('--preview',type=Path);args=parser.parse_args()
    if args.build: build()
    if args.check: validate(json.loads((CAMPAIGN/'world/barovia-map.json').read_text()))
    if args.preview: preview(args.preview)
