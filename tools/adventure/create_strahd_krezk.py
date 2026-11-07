"""Author Krezk's complete third chapter; preserve other campaign content."""
import argparse
import copy
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CAMPAIGN = ROOT / 'forge-gui/res/adventure/CurseOfStrahd'
FLAGS = ['cosKrezkRoadCleared', 'cosKrezkAdmitted', 'cosKrezkHeardDmitri',
         'cosKrezkSpringCleared', 'cosKrezkWaterDrawn', 'cosKrezkAbbotHeard',
         'cosKrezkSentinelCleared', 'cosKrezkAccord', 'cosChapterThreeComplete']
BASE = ['cosChapterTwoComplete', 'cosKrezkQuestAccepted']
HINTS = ["Finish A Light in Vallaki. Ireena waits at Saint Andral's Church.",
         "Speak to Ireena at Saint Andral's Church and explicitly accept the journey to Krezk. She will stay in her chosen sanctuary.",
         'Defeat the Krezk Mist Wolves outside Krezk gate. You can withdraw and recover in Barovia or with Urwin in Vallaki.',
         'After clearing the wolves, speak to the captain at Krezk gate for admission.',
         'Meet Dmitri in Krezk Village, northwest of the gate, to hear what troubles the town.',
         'Defeat the Spring Watcher at the Blessed Pool of Krezk, northwest of the village. Anna offers free recovery in Krezk Village.',
         'Draw clear water at the Blessed Pool of Krezk. Victory alone did not collect it.',
         'Take the water to the Abbot at the Abbey of Saint Markovia, northeast of Krezk Village.',
         'Disable the Abbey Sentinel in the Abbey of Saint Markovia. Anna can help you recover between attempts.',
         'Speak to Vasilka in the Abbey and decide how Krezk will keep watch. The decision is yours to make explicitly.',
         'Return to Dmitri in Krezk Village to settle the accord and select your two-card reward.']
SITES = [
    ('Krezk', 'krezk', 28,55,'gate','PlainsTown',[[35,59],[28,55]]),
    ('Krezk Village','krezk-village',24,43,'gate','Spawn',[[28,55],[24,49],[24,43]]),
    ('Blessed Pool of Krezk','krezk-pool',15,34,'lake','Waypoint',[[24,43],[15,43],[15,34]]),
    ('Abbey of Saint Markovia','krezk-abbey',34,29,'temple','SnowAbbey',[[24,43],[34,39],[34,29]])]


def read(name): return json.loads((CAMPAIGN/name).read_text(encoding='utf-8'))
def write(name,value): (CAMPAIGN/name).write_text(json.dumps(value,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
def check(flag, absent=False): return {'checkQuestFlag':flag, **({'not':True} if absent else {})}
def flag(key): return {'key':key,'val':1}
def node(text, options=None, action=None, name=None):
    result={'text':text,'options': options if options is not None else [{'name':'Continue.'}]}
    if action: result['action']=action
    if name: result['name']=name
    return result
def advance(label,text,key): return node(text,name=label,action=[{'setQuestFlag':flag(key)}])
def battle(text,label): return node(text,[{'name':label,'action':[{'battleWithActorID':-1}]},{'name':'Withdraw and prepare.'}])


def roots(events, final):
    required=BASE+FLAGS; result=[]
    for i,key in enumerate(required):
        root=copy.deepcopy(events.get(i-2,node(HINTS[i])))
        if root.get('action'):
            root=node(HINTS[i], [root, {'name':'Return later.'}])
        root['condition']=[check(f) for f in required[:i]]+[check(key,True)]
        result.append(root)
    for choice,ending in [(1,'The village keeps a community watch. Vasilka chooses her own work; the Abbey shares its stores.'),
                          (2,'The Abbey tends a public ward. Its work is inspected by villagers, and Vasilka may refuse or leave.')]:
        root=node(final+' '+ending)
        root['condition']=[check(f) for f in required]+[{'getCharacterFlag':{'key':'cosKrezkChoice','op':'=','val':choice}}]
        result.append(root)
    root=node(final)
    root['condition']=[check(f) for f in required]+[{'checkCharacterFlag':'cosKrezkChoice'}]+[{'getCharacterFlag':{'key':'cosKrezkChoice','op':'=','val':v},'not':True} for v in [1,2]]
    absent=node(final)
    absent['condition']=[check(f) for f in required]+[{'checkCharacterFlag':'cosKrezkChoice','not':True}]
    return result+[root,absent]


def build_content():
    sites=read('story/landmarks.json')
    for name,slug,x,y,archetype,sprite,route in SITES:
        site=next((p for p in sites if p['name']==name),None)
        if site is None: site={};sites.append(site)
        site.update(name=name,slug=slug,x=x,y=y,archetype=archetype,sprite=sprite,
                    reference='S' if name=='Krezk' else '',type='dungeon',clearingRadius=3,
                    route=route,questArea=True)
    write('story/landmarks.json',sites)
    manifest=read('campaign.json');manifest.update(version='0.5.0',description=
        'Three playable chapters from Barovia through Vallaki to Krezk. Investigate the blessed spring and the Abbey of Saint Markovia, then decide how Krezk keeps watch. Explore 25 other short arrival areas across the valley.')
    write('campaign.json',manifest)
    config=read('config.json');config.update(chapterCompletionQuestFlag=FLAGS[-1],chapterCompletionText=
        'A Bell Against the Mists - Chapter Three Complete\n\nKrezk has clear water and a watch it chose. The Abbey must answer to the people below it, and Vasilka has a voice in her future. Your accord is remembered. You can revisit all three chapters, recover with Anna in Krezk Village, or explore the other arrival areas. Full winery, relic and Ravenloft stories remain future content.')
    write('config.json',config)
    labels=['Gate wolves','Enter Krezk',"Dmitri's request",'The spring watcher','Draw clear water',
            'Question the Abbot','The Abbey sentinel','Choose the watch','Return to Dmitri']
    objectives=['Defeat the wolves at Krezk gate.','Speak to the captain at Krezk gate.',
                'Meet Dmitri in Krezk Village.',"Defeat the Blessed Pool's watcher.",
                'Draw water from the Blessed Pool.','Speak to the Abbot at the Abbey.',
                'Defeat the Abbey Sentinel.','Choose an accord with Vasilka.','Settle the accord with Dmitri.']
    quests=[q for q in read('world/quests.json') if q['id']!=8103]
    destinations=['Krezk','Krezk','Krezk Village','Blessed Pool of Krezk','Blessed Pool of Krezk',
                  'Abbey of Saint Markovia','Abbey of Saint Markovia','Abbey of Saint Markovia','Krezk Village']
    quests.append({'id':8103,'isTemplate':True,'storyQuest':True,'autoTrack':True,'name':'A Bell Against the Mists',
                   'description':'Investigate Krezk\'s failing spring and silent Abbey. Help its people choose how to keep watch.',
                   'stages':[{'id':i+1,'name':labels[i],'description':objectives[i],'objective':'QuestFlag',
                              'navigationTarget':destinations[i],
                              'mapFlag':f,'mapFlagValue':1,**({'prerequisiteIDs':[i]} if i else {})} for i,f in enumerate(FLAGS)]})
    write('world/quests.json',quests)
    points=[p for p in read('world/points_of_interest.json') if p['name'] not in {s[0] for s in SITES}]
    for name,slug,*_ in SITES:
        filename='landmark-krezk' if slug=='krezk' else slug
        points.append({'name':name,'displayName':name,'type':'dungeon','count':1,'spriteAtlas':'maps/tileset/buildings.atlas',
                       'sprite':next(p['sprite'] for p in sites if p['name']==name),
                       'map':'../common/maps/map/strahd/'+filename+'.tmx','radiusFactor':0,'questTags':['CosKrezkChapter']})
    write('world/points_of_interest.json',points)
    decks={
        'krezk-wolves':('Wolves at the Western Wall',['17 Forest|ISD','4 Darkthicket Wolf|ISD','4 Ambush Viper|ISD',
            '3 Somberwald Spider|ISD','3 Festerhide Boar|ISD','3 Hollowhenge Scavenger|ISD','4 Prey Upon|ISD','2 Spidery Grasp|ISD']),
        'spring-watcher':('Below the Still Water',['17 Island|ISD','4 Deranged Assistant|ISD','4 Armored Skaab|ISD',
            '3 Makeshift Mauler|ISD','3 Selhoff Occultist|ISD',"3 Stitcher's Apprentice|ISD",'3 Claustrophobia|ISD','3 Think Twice|ISD']),
        'abbey-sentinel':('The Unsleeping Watch',['9 Plains|ISD','8 Swamp|ISD','4 Walking Corpse|ISD','4 Unruly Mob|ISD',
            '3 Voiceless Spirit|ISD','3 Village Bell-Ringer|ISD','3 Manor Gargoyle|ISD','3 Dead Weight|ISD','3 Rebuke|ISD'])}
    for slug,(title,cards) in decks.items():
        assert sum(int(c.split()[0]) for c in cards)==40
        (CAMPAIGN/'decks'/(slug+'.dck')).write_text('[metadata]\nName='+title+'\n[Main]\n'+'\n'.join(cards)+'\n')
    names={'Krezk Mist Wolves','Spring Watcher','Abbey Sentinel'}
    enemies=[e for e in read('world/enemies.json') if e['name'] not in names]
    for name,deck,sprite,life,color in [('Krezk Mist Wolves','krezk-wolves','beast/largemammals/wolf',18,'G'),
                                       ('Spring Watcher','spring-watcher','undead/ghost',18,'U'),
                                       ('Abbey Sentinel','abbey-sentinel','construct/gargoyle',20,'WB')]:
        enemies.append({'name':name,'deck':['decks/'+deck+'.dck'],'sprite':'sprites/enemy/'+sprite+'.atlas',
                        'life':life,'speed':0,'spawnRate':0,'difficulty':.3,'colors':color,'questTags':['CosKrezkChapter'],'rewards':[]})
    write('world/enemies.json',enemies)
    story=read('story/dialogues.json')
    for key in ['landmark.krezk.arrival','landmark.krezk.detail']: story.pop(key,None)
    # Replace only the completed-sanctuary repeat; earlier progression is intact.
    prior=story['andral.ireena']['dialog']
    completed=next(r for r in prior if any(c.get('checkQuestFlag')=='cosChapterTwoComplete' and not c.get('not') for c in r.get('condition',[])))
    base=[c for c in completed['condition'] if c.get('checkQuestFlag') not in ['cosKrezkQuestAccepted','cosChapterThreeComplete']]
    prior[:]=[r for r in prior if not any(c.get('checkQuestFlag')=='cosChapterTwoComplete' and not c.get('not') for c in r.get('condition',[]))]
    offer=node('Ireena lights a candle at the restored altar. "I am staying here, by my choice. Krezk has sent no answer to Lucian\'s letters. Will you carry our news west? Ask the captain at Krezk gate what has closed the road."',
               [node('A Bell Against the Mists begins in your journal. Travel to Krezk gate in the western valley. Ireena remains at Saint Andral\'s Church.',
                     name='Take the journey to Krezk.',action=[{'issueQuest':'8103','setQuestFlag':flag('cosKrezkQuestAccepted')}]),
                {'name':'Stay in Vallaki for now.'}])
    offer['condition']=base+[check('cosKrezkQuestAccepted',True)];prior.append(offer)
    for done in [False,True]:
        repeat=node('Ireena keeps her place at the altar. '+('"Krezk chose its own watch. That is news worth carrying home."' if done else
                     '"Your journal follows Krezk now. The captain waits at its gate; Anna offers recovery once you reach the village."'))
        repeat['condition']=base+[check('cosKrezkQuestAccepted'),check('cosChapterThreeComplete',not done)];prior.append(repeat)
    events={
        'krezk-gate.wolves':{0:battle('The wolves stand too still beneath the watch lamps. Their shadows strain toward the closed gate while their bodies face you. Break the pack so the captain can open a safe path.','Drive the wolves from the gate.')},
        'krezk-gate.captain':{1:advance('Enter Krezk.','The captain lifts the bar. "Dmitri is in Krezk Village, northwest of here. We have a spring that whispers and an Abbey that has stopped ringing. Anna will tend your wounds without a charge."',FLAGS[1])},
        'krezk-village.dmitri':{2:advance('Investigate the spring and Abbey.','Dmitri unfolds an unanswered letter. "Our water tastes of iron. The Abbot says the bell cannot ring until his new watch is ready. Start at the Blessed Pool, northwest of here. If you find clear water, take it to him. Ask what he means by ready."',FLAGS[2])},
        'krezk-pool.watcher':{3:battle('A shape turns below the reflection of a cloudless sky. Water climbs the shrine steps against the slope. Drive out the Spring Watcher before drawing from the pool.','Face the Spring Watcher.')},
        'krezk-pool.water':{4:advance('Draw clear spring water.','You fill a stoppered vessel. The reflection now follows the sky, and the iron taste is gone. Take this evidence to the Abbot at the Abbey of Saint Markovia, northeast of Krezk Village.',FLAGS[4])},
        'krezk-abbey.abbot':{5:advance('Question the Abbot about the silent bell.','The Abbot studies the clear water. "Then the disturbance is not yours. My sentinel was meant to watch without sleep. It obeys the last command it heard, even when a person begs it to stop." He looks toward Vasilka. "Disable it. Afterward, hear her before deciding what this Abbey owes the village."',FLAGS[5])},
        'krezk-abbey.sentinel':{6:battle('The sentinel turns in a circle of broken bell ropes. A voice inside its metal chest repeats: STAND WATCH. ADMIT NO CHANGE. Vasilka steps clear of the aisle. Disable the construct; she is not your enemy.','Disable the Abbey Sentinel.')},
        'krezk-abbey.vasilka':{7:node('Vasilka rests a hand on the silent bell. "He made me for a story I did not choose. Let me choose my work. The village can keep its own watch, or inspect a ward the Abbey tends. I will help while I wish to; no promise here will bind me to Strahd or anyone else."',[
            node('The bell sounds once. Villagers will keep a community watch; the Abbey must share stores and answer questions. Vasilka may choose her work freely. Return to Dmitri to settle this accord.',name='Support a community watch.',action=[{'setCharacterFlag':{'key':'cosKrezkChoice','val':1},'setQuestFlag':flag(FLAGS[7])}]),
            node('The bell sounds twice. The Abbey will tend a ward under public inspection. Vasilka may refuse, leave or change her mind. Return to Dmitri to settle this accord.',name='Accept an Abbey ward under public oversight.',action=[{'setCharacterFlag':{'key':'cosKrezkChoice','val':2},'setQuestFlag':flag(FLAGS[7])}]),
            {'name':'I need time to consider.'}])}}
    events['krezk-village.dmitri'][8]=node('Dmitri listens to the accord. "A watch must answer to the people it protects. We will remember what you chose." He opens a small case of recovered spells. Settle the chapter with two cards of either color; your selected deck will stay unchanged.',[
        node('Two Thraben Inspectors enter your collection. Their arrival investigates for a Clue; use Decks to choose replacements yourself. Krezk\'s chapter is complete.',name='Settle the accord; take two Thraben Inspectors (white).',action=[{'setQuestFlag':flag(FLAGS[8]),'grantRewards':[{'type':'card','cardName':'Thraben Inspector','count':2}]}]),
        node('Two Doomed Dissenters enter your collection. Their deaths leave Zombies behind; use Decks to choose replacements yourself. Krezk\'s chapter is complete.',name='Settle the accord; take two Doomed Dissenters (black).',action=[{'setQuestFlag':flag(FLAGS[8]),'grantRewards':[{'type':'card','cardName':'Doomed Dissenter','count':2}]}]),
        {'name':'Return before settling.'}])
    endings={'krezk-gate.captain':'The captain unbars the gate when she sees you. "Krezk remembers its friends."',
             'krezk-gate.wolves':'Only ordinary tracks remain outside the gate.',
             'krezk-gate.sign':'KREZK: Village northwest, pool beyond the village, Abbey northeast. All local southern exits return to the overworld.',
             'krezk-village.dmitri':'Dmitri folds the written accord into the village ledger. Your reward has already been given.',
             'krezk-village.notice':'KREZK WATCH: Bring concerns to Dmitri. Anna offers free recovery. Full winery and Ravenloft chapters are still future content.',
             'krezk-pool.watcher':'The disturbed reflection has gone.',
             'krezk-pool.water':'Clear water laps against the shrine steps. You need no second vessel.',
             'krezk-pool.shrine':'A weathered sun is carved above a cup. This shrine offers remembrance, not a forced journey or a cure for Barovia.',
             'krezk-abbey.abbot':'The Abbot leaves the bell rope within reach of the village visitors. "My work must be questioned. I will begin there."',
             'krezk-abbey.sentinel':'The dismantled sentinel cannot issue another command.',
             'krezk-abbey.vasilka':'Vasilka chooses a seat by an open window. "Tomorrow I will decide what I want to learn."',
             'krezk-abbey.bell':'The bell rings when someone chooses to pull its rope. No enchantment moves your hand.'}
    for key,final in endings.items(): story[key]={'dialog':roots(events.get(key,{}),final)}
    for key,n in [('krezk-gate.wolves',0),('krezk-pool.watcher',3),('krezk-abbey.sentinel',6)]:
        story[key].update(defeatDialog=[node('',[],[{'setQuestFlag':flag(FLAGS[n]),'deleteMapObject':-1}])],removeWhenQuestFlag=FLAGS[n])
    story['krezk-village.anna']={'dialog':[node('Anna sets clean bandages beside the stove. "No coin. No promise. You can rest here before returning to the spring or Abbey. Recovery does not grant admission or advance the investigation."',[
        node('Anna tends your wounds without a charge. Consult your journal for the next step.',name='Rest and recover for free.',action=[{'addLife':1000}]),{'name':'Back to the village.'}])]}
    write('story/dialogues.json',story)


def compose():
    import create_strahd_maps as maps
    from create_strahd_landmarks import building
    for name in ['landmark-krezk','krezk-village','krezk-pool','krezk-abbey']:
        scene=maps.Scene(name,6164 if name=='krezk-abbey' else 1797)
        scene.area(17,4,7,29,[maps.NC+6611,maps.NC+6638])
        scene.area(5,17,30,10,[maps.NC+6611])
        if name=='landmark-krezk':
            for x,w in [(2,16),(23,15)]:
                scene.area(x,8,w,2,[maps.NC+5692],'Foreground');scene.block(x,8,w,2)
            building(scene,18,4,'Gate')
            for x,y in [(3,2),(32,2),(4,13),(32,14),(3,27),(32,27)]:scene.tree(x,y,pine=True)
            scene.prop(13,20,6017);scene.prop(26,20,6017)
        elif name=='krezk-village':
            for x,y,ax in [(6,5,15),(29,5,16),(5,13,17),(30,13,21),(7,28,22),(29,28,23)]:
                scene.stamp(x,y,ax,33,1,2,building=True)
            scene.area(5,10,30,5,[maps.NC+1230]);scene.bench(5,21,5);scene.bench(30,21,5)
            scene.prop(20,13,6182);scene.prop(20,14,6183)
            for x,y in [(2,2),(34,2),(2,27),(34,27)]:scene.tree(x,y,pine=True)
        elif name=='krezk-pool':
            scene.area(8,4,25,10,[maps.NC+1478]);scene.block(8,4,25,10)
            scene.area(18,10,5,4,[maps.NC+6184])
            # Dock stays scenery: actors are south of the blocked water rectangle.
            for x,y in [(3,3),(34,3),(3,19),(33,24)]:scene.tree(x,y,pine=True)
            scene.bench(5,26,5);scene.prop(27,16,6656);scene.prop(10,15,5702)
        else:
            scene.area(5,3,31,27,[maps.NC+6164]);scene.area(17,3,7,29,[maps.NC+6611])
            for x in [4,36]:scene.area(x,3,1,28,[maps.NC+5697],'Foreground');scene.block(x,3,1,28)
            for y in [3,30]:
                for x,w in [(5,12),(24,12)]:scene.area(x,y,w,1,[maps.NC+5692],'Foreground');scene.block(x,y,w,1)
            for y in [16,22]:scene.bench(8,y,5);scene.bench(28,y,5)
            building(scene,18,4,'SnowAbbey');scene.prop(25,6,6656)
            for x,y in [(15,12),(25,12),(15,27),(25,27)]:scene.prop(x,y,6017)
        scene.save()


def build():
    build_content()
    from create_strahd_overworld import build as build_world
    build_world()
    compose()
    import create_strahd_maps as maps
    from embed_strahd_dialogues import embed
    print('Embedded',embed(CAMPAIGN/'story/dialogues.json',maps.MAPS),'actors')
    maps.validate()


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--build',action='store_true');args=parser.parse_args()
    if args.build:build()
