"""Krezk progression contracts through actual authored dialogue trees."""
import json
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
CAMPAIGN = ROOT / 'forge-gui/res/adventure/CurseOfStrahd'
FLAGS = ['cosKrezkRoadCleared', 'cosKrezkAdmitted', 'cosKrezkHeardDmitri',
         'cosKrezkSpringCleared', 'cosKrezkWaterDrawn', 'cosKrezkAbbotHeard',
         'cosKrezkSentinelCleared', 'cosKrezkAccord', 'cosChapterThreeComplete']
ACTORS = ['krezk-gate.wolves', 'krezk-gate.captain', 'krezk-village.dmitri',
          'krezk-pool.watcher', 'krezk-pool.water', 'krezk-abbey.abbot',
          'krezk-abbey.sentinel', 'krezk-abbey.vasilka', 'krezk-village.dmitri']


def read(name):
    return json.loads((CAMPAIGN / name).read_text(encoding='utf-8'))


def matches(node, flags):
    for c in node.get('condition', []):
        result = True
        for kind in ['Quest', 'Character']:
            if 'check'+kind+'Flag' in c: result &= c['check'+kind+'Flag'] in flags
            if 'get'+kind+'Flag' in c:
                q = c['get'+kind+'Flag']
                if q['key'] not in flags: return False  # Runtime presence check precedes negation.
                result &= flags[q['key']] == q['val']
        if c.get('not'): result = not result
        if not result: return False
    return True


def actions(node, name):
    return [a[name] for a in node.get('action', []) if name in a] + [
        value for child in node.get('options', []) for value in actions(child, name)]


def apply(node, flags):
    for a in node.get('action', []):
        for kind in ['Quest', 'Character']:
            if 'set'+kind+'Flag' in a:
                f = a['set'+kind+'Flag']; flags[f['key']] = f['val']


class KrezkChapterTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls): cls.story = read('story/dialogues.json')

    def entry(self, key, flags):
        roots = [n for n in self.story[key]['dialog'] if matches(n, flags)]
        self.assertEqual(len(roots), 1, (key, flags, [r['text'] for r in roots]))
        return roots[0]

    def through(self, n):
        return dict(cosChapterTwoComplete=1, cosKrezkQuestAccepted=1, **dict.fromkeys(FLAGS[:n], 1))

    def test_four_quest_sites_and_nine_stage_journal(self):
        self.assertEqual(read('campaign.json')['version'], '0.5.0')
        self.assertEqual(read('config.json')['chapterCompletionQuestFlag'], 'cosChapterThreeComplete')
        self.assertEqual(len(read('world/points_of_interest.json')), 37)
        sites = read('story/landmarks.json')
        self.assertEqual(len([p for p in sites if p.get('questArea')]), 4)
        self.assertEqual(len([p for p in sites if not p.get('questArea')]), 25)
        quest = next(q for q in read('world/quests.json') if q['id'] == 8103)
        self.assertTrue(quest['autoTrack'] and quest['storyQuest'] and quest['isTemplate'])
        self.assertEqual([s['mapFlag'] for s in quest['stages']], FLAGS)
        for i, stage in enumerate(quest['stages']):
            self.assertEqual(stage.get('prerequisiteIDs', []), [i] if i else [])

    def test_handoff_is_explicit_and_once_only(self):
        roots = self.story['andral.ireena']['dialog']
        base = dict.fromkeys(['cosChapterOneComplete','cosWestRoadCleared','cosVallakiEntered',
                             'cosBonesInvestigation','cosCoffinLead','cosHenrikConfessed','cosCoffinCleared',
                             'cosBonesRecovered','cosSanctuaryDefended','cosBonesRestored','cosChapterTwoComplete'],1)
        offer = self.entry('andral.ireena', base)
        self.assertEqual(actions(offer, 'issueQuest'), ['8103'])
        self.assertNotIn('cosKrezkQuestAccepted',base)
        apply(offer['options'][0], base)
        self.assertEqual(base['cosKrezkQuestAccepted'],1)
        self.assertFalse(actions(self.entry('andral.ireena', base),'issueQuest'))
        for root in roots:
            if actions(root,'issueQuest') == ['8103']:
                self.assertTrue(any(c.get('checkQuestFlag')=='cosChapterTwoComplete' and not c.get('not') for c in root['condition']))

    def test_missing_any_prerequisite_blocks_progress_and_battle(self):
        for i, key in enumerate(ACTORS):
            for missing in ['cosChapterTwoComplete','cosKrezkQuestAccepted']+FLAGS[:i]:
                flags=self.through(i); flags.pop(missing)
                root=self.entry(key,flags)
                if key=='krezk-village.dmitri' and missing==FLAGS[2]:
                    self.assertEqual(actions(root,'setQuestFlag'),[{'key':FLAGS[2],'val':1}])
                    self.assertFalse(actions(root,'grantRewards'))
                    continue  # Dmitri may repair his earlier missing interview, never skip it.
                for name in ['battleWithActorID','setQuestFlag','setCharacterFlag','grantRewards','issueQuest']:
                    self.assertFalse(actions(root,name),(key,missing,name))

    def test_all_runtime_roots_are_exclusive_even_with_inconsistent_evidence(self):
        keys=[k for k in self.story if k.startswith('krezk-')]
        self.assertEqual(len(keys),13)
        for mask in range(1 << len(FLAGS)):
            for choice in [1,2]:
                flags=self.through(0);flags.update({f:1 for i,f in enumerate(FLAGS) if mask & (1<<i)})
                flags['cosKrezkChoice']=choice
                for key in keys: self.entry(key,flags)
        for key in keys: self.entry(key,{})

    def test_water_choice_and_reward_are_deliberate_and_repeat_safe(self):
        for choice in [1,2]:
            for color in [0,1]:
                flags=self.through(4)
                water=self.entry('krezk-pool.water',flags)
                apply(water,flags);self.assertNotIn(FLAGS[4],flags)
                apply(water['options'][0],flags);self.assertEqual(flags[FLAGS[4]],1)
                flags=self.through(7)
                accord=self.entry('krezk-abbey.vasilka',flags)
                apply(accord,flags);self.assertNotIn(FLAGS[7],flags)
                apply(accord['options'][choice-1],flags)
                self.assertEqual(flags['cosKrezkChoice'],choice);self.assertEqual(flags[FLAGS[7]],1)
                reward=self.entry('krezk-village.dmitri',flags)
                apply(reward,flags);self.assertNotIn(FLAGS[8],flags)
                option=reward['options'][color]
                self.assertEqual(actions(option,'grantRewards'),[[{'type':'card','cardName':
                    ['Thraben Inspector','Doomed Dissenter'][color],'count':2}]])
                apply(option,flags);self.assertEqual(flags[FLAGS[8]],1)
                self.assertFalse(actions(self.entry('krezk-village.dmitri',flags),'grantRewards'))
                self.assertFalse(actions(self.entry('krezk-abbey.vasilka',flags),'setCharacterFlag'))

    def test_enemy_removal_and_decks_are_separate_from_collection(self):
        for key, flag in [('krezk-gate.wolves',FLAGS[0]),('krezk-pool.watcher',FLAGS[3]),('krezk-abbey.sentinel',FLAGS[6])]:
            e=self.story[key];self.assertEqual(e['removeWhenQuestFlag'],flag)
            self.assertEqual(actions(e['defeatDialog'][0],'setQuestFlag'),[{'key':flag,'val':1}])
            self.assertEqual(actions(e['defeatDialog'][0],'deleteMapObject'),[-1])
        enemies=read('world/enemies.json')
        for name in ['Krezk Mist Wolves','Spring Watcher','Abbey Sentinel']:
            enemy=next(e for e in enemies if e['name']==name)
            text=(CAMPAIGN/enemy['deck'][0]).read_text()
            cards=text.split('[Main]\n')[1].splitlines()
            self.assertEqual(sum(int(line.split()[0]) for line in cards if line.strip()),40)
            self.assertEqual(enemy['rewards'],[])

    def test_completed_dialogue_recovers_missing_or_unknown_choice(self):
        for key in [k for k in self.story if k.startswith('krezk-')]:
            for value in [None,0,3]:
                flags=self.through(9)
                if value is not None: flags['cosKrezkChoice']=value
                root=self.entry(key,flags)
                self.assertFalse(actions(root,'grantRewards'))

    def test_four_scenes_embed_all_interactions_and_clear_exits(self):
        import create_strahd_maps as maps
        from embed_strahd_dialogues import embed
        self.assertTrue(all(k in maps.ACTORS for k in ['landmark-krezk','krezk-village','krezk-pool','krezk-abbey']))
        self.assertEqual(embed(CAMPAIGN/'story/dialogues.json',maps.MAPS,check=True),90)
        maps.validate()


if __name__=='__main__': unittest.main()
