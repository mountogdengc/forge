"""Build short, noncombat arrival scenes for Barovia's reference-map sites."""
import argparse
from functools import lru_cache
import json
from pathlib import Path
import re

import create_strahd_maps as maps
from embed_strahd_dialogues import embed

ROOT = Path(__file__).resolve().parents[2]
CAMPAIGN = ROOT / 'forge-gui/res/adventure/CurseOfStrahd'
INVENTORY = CAMPAIGN / 'story/landmarks.json'


def inventory():
    return json.loads(INVENTORY.read_text(encoding='utf-8'))


@lru_cache(maxsize=None)
def region(name):
    name = {'farm':'PlainsTown', 'ruinedcity':'SwampCapital'}.get(name,name)
    lines = (maps.COMMON / 'maps/tileset/buildings.atlas').read_text().splitlines()
    for i,line in enumerate(lines):
        if line.strip() == name:
            xy = size = None
            for field in lines[i+1:i+7]:
                match = re.match(r'\s+xy:\s*(\d+),\s*(\d+)', field)
                if match: xy = tuple(map(int,match.groups()))
                match = re.match(r'\s+size:\s*(\d+),\s*(\d+)', field)
                if match: size = tuple(map(int,match.groups()))
                if xy and size:
                    if all(value % 16 == 0 for value in xy + size): return xy + size
                    break
    raise ValueError('Missing building region '+name)


def building(scene, x, y, name):
    px,py,w,h = region(name)
    assert all(value % 16 == 0 for value in (px,py,w,h)), name
    scene.stamp(x,y,px//16,py//16,w//16,h//16,building=True)


def compose():
    for index,item in enumerate(inventory()):
        if item.get('questArea'): continue
        kind = item['archetype']
        scene = maps.Scene('landmark-'+item['slug'], 6611 if kind in ('castle','temple','mountain','ruins') else 1797)
        scene.area(17,3,7,30,[maps.NC+1230] if kind not in ('castle','temple','mountain','ruins') else [maps.NC+6611])
        scene.area(8,19,24,7,[maps.NC+1230] if kind not in ('castle','temple','mountain','ruins') else [maps.NC+6164])
        if kind == 'castle':
            for x,w in [(5,12),(24,11)]:
                scene.area(x,10,w,1,[maps.NC+5692],'Foreground');scene.block(x,10,w,1)
            for x in (5,34):
                scene.area(x,10,1,18,[maps.NC+5697],'Foreground');scene.block(x,10,1,18)
            building(scene,17,2,'VampireCastle')
            for x,y in [(12,15),(27,15),(12,24),(27,24)]: scene.prop(x,y,6017)
            scene.bench(8,26,5);scene.bench(26,26,5)
        elif kind == 'gate':
            for x,w in [(5,12),(24,11)]:
                scene.area(x,9,w,1,[maps.NC+5692],'Foreground');scene.block(x,9,w,1)
            building(scene,18,5,'Gate')
            building(scene,6,4,'Fort');building(scene,29,4,'Fort')
            scene.bench(8,24,5);scene.bench(26,24,5)
            for x in (15,25): scene.prop(x,16,6017)
        elif kind in ('river','lake','tower'):
            for x,w in [(3,10),(32,5)]:
                for y,h in [(4,10),(19,10)]:
                    scene.area(x,y,w,h,[maps.NC+1478]);scene.block(x,y,w,h)
                scene.area(x,14,w,5,[maps.NC+6184])
            if kind == 'tower': building(scene,17,4,'MageTower')
            else:
                scene.tree(5,1,pine=True);scene.tree(29,2,pine=True)
            scene.prop(30,20,6182);scene.prop(30,21,6183)
        elif kind == 'winery':
            building(scene,17,3,'farm')
            for x in (5,32):
                for y in (7,13,19): scene.tree(x,y)
            for x,y in [(12,10),(26,8),(12,24),(26,24)]: scene.prop(x,y,6182)
            scene.bench(8,17,5)
        elif kind == 'mill':
            building(scene,18,4,'farm')
            # Reused timber planks make the mill's stationary wooden vanes.
            scene.bench(17,5,7)
            for y in range(2,10): scene.prop(20,y,6184)
            for x,y in [(7,8),(31,8),(7,19),(31,23)]: scene.tree(x,y)
            scene.prop(13,15,6182);scene.prop(25,25,6183)
        elif kind in ('ruins','marsh'):
            building(scene,16,3,'ruinedcity')
            for x,y,w in [(6,11,7),(29,9,5),(8,24,4),(27,25,5)]: scene.bench(x,y,w)
            if kind == 'marsh':
                for x,y,w,h in [(3,14,9,4),(31,19,6,4)]:
                    scene.area(x,y,w,h,[maps.NC+1478]);scene.block(x,y,w,h)
            for x,y in [(12,8),(25,8),(6,21)]: scene.prop(x,y,5702)
        elif kind in ('temple','mountain'):
            building(scene,17,3,'StonePyramid' if kind=='temple' else 'SnowAbbey')
            for x,y in [(5,5),(30,4),(6,18),(32,23)]: scene.tree(x,y,pine=True)
            for x,y in [(14,11),(25,11),(12,25),(27,25)]: scene.prop(x,y,5702)
            scene.bench(7,14,5);scene.bench(30,17,4)
        elif kind == 'hill':
            scene.area(12,7,16,12,[maps.NC+6611])
            for x,y in [(14,8),(20,7),(26,8),(13,13),(27,13),(14,18),(26,18)]: scene.prop(x,y,5702)
            scene.tree(4,24);scene.tree(33,5)
        elif kind == 'den':
            building(scene,17,4,'Cave')
            for x,y in [(5,5),(30,4),(6,18),(32,23)]: scene.tree(x,y,pine=True)
            for x,y in [(13,10),(26,9),(11,25)]: scene.prop(x,y,3455)
        elif kind == 'carriage':
            building(scene,17,6,'VehicleShop')
            scene.area(9,9,23,6,[maps.NC+1230])
            scene.bench(10,17,4)
            for x,y in [(3,4),(30,3),(5,22),(33,23)]: scene.tree(x,y,pine=True)
        else:
            if kind == 'crossroads':
                scene.area(3,14,34,5,[maps.NC+1230])
            for x,y in [(3,4),(31,3),(5,20),(33,23),(9,8)]: scene.tree(x,y,pine=True)
            scene.bench(8,26,5)
        # Individual scenery variations distinguish scenes of the same archetype.
        scene.prop(5+(index%4)*2,27,3455)
        scene.prop(31-(index%3)*2,27,3456)
        scene.prop(28,12,6656)
        scene.save()


def build():
    compose()
    story_path = CAMPAIGN / 'story/dialogues.json'
    story = json.loads(story_path.read_text(encoding='utf-8'))
    points_path = CAMPAIGN / 'world/points_of_interest.json'
    points = [p for p in json.loads(points_path.read_text()) if 'CosReferenceArrival' not in p.get('questTags', [])]
    for item in inventory():
        if item.get('questArea'): continue
        prefix = 'landmark.'+item['slug']
        arrival = item['arrival'] + ' This arrival area is open. Further stories here continue in a future chapter. The southern exit returns to the overworld.'
        story[prefix+'.arrival'] = {'dialog':[{'text':arrival,'options':[
            {'name':'Look around.','text':item['look'],'options':[{'name':'Continue.'}]},
            {'name':'Where does the current story continue?',
             'text':"Your journal follows the village church, Eva's camp and Vallaki's sanctuary. These arrival visits do not advance or replace those objectives.",
             'options':[{'name':'Continue.'}]},
            {'name':'Return to exploring.'}]}]}
        story[prefix+'.detail'] = {'dialog':[{'text':item['look'], 'options':[{'name':'Continue.'}]}]}
        points.append({'name':item['name'],'displayName':item['name'],'type':item['type'],'count':1,
                       'spriteAtlas':'maps/tileset/buildings.atlas','sprite':item['sprite'],
                       'map':'../common/maps/map/strahd/landmark-'+item['slug']+'.tmx',
                       'radiusFactor':0,'questTags':['CosReferenceArrival']})
    story_path.write_text(json.dumps(story,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
    points_path.write_text(json.dumps(points,indent=2)+'\n',encoding='utf-8')
    manifest_path = CAMPAIGN/'campaign.json'; manifest=json.loads(manifest_path.read_text())
    if not any(p.get('questArea') for p in inventory()):
        manifest['version']='0.4.0'
        manifest['description']='Two playable chapters from Barovia to Vallaki, plus 26 explorable arrival areas across the valley, including Castle Ravenloft and Krezk. Further stories at those new destinations continue in future chapters.'
    manifest_path.write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf-8')
    maps.validate()
    print('Embedded', embed(story_path,maps.MAPS), 'dialogue actors')


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--build',action='store_true')
    parser.add_argument('--check',action='store_true');args=parser.parse_args()
    if args.build: build()
    if args.check:
        maps.validate();print('Checked',embed(CAMPAIGN/'story/dialogues.json',maps.MAPS,check=True),'dialogue actors')
