"""Exercise the real Windows updater against disposable tiny game folders."""
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest


@unittest.skipUnless(sys.platform == 'win32', 'Windows updater')
class StrahdUpdateTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory(prefix='strahd-updater-')
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.game = self.root / 'game'
        self.patch = self.root / 'patch'
        self.game.mkdir(); self.patch.mkdir()
        (self.game / 'res/adventure/CurseOfStrahd').mkdir(parents=True)
        (self.game / 'res/adventure/CurseOfStrahd/campaign.json').write_text(json.dumps({'id':'fork.curse-of-strahd','version':'0.1.0'}))
        (self.game / 'forge.profile.properties').write_text('userDir=./test-profile\ncacheDir=./test-cache\n')
        (self.game / 'forge-adventure-test.jar').write_bytes(b'old client')
        (self.game / 'SHA256SUMS.txt').write_text(self.sha(b'old client')+'  forge-adventure-test.jar\n')
        (self.game / 'test-profile').mkdir()
        (self.game / 'test-profile/keep.save').write_bytes(b'my completed chapter')
        self.payload = self.patch / 'payload'
        self.payload.mkdir()
        (self.payload / 'forge-adventure-test.jar').write_bytes(b'new client')
        self.manifest = {'base_jar_sha256':self.sha(b'old client'),'jar_sha256':self.sha(b'new client'),
                         'files':{'forge-adventure-test.jar':self.sha(b'new client')}}
        self.manifest_file = self.patch / 'PATCH-MANIFEST.json'
        self.manifest_file.write_text(json.dumps(self.manifest))
        shutil.copyfile(Path(__file__).with_name('Apply-Strahd-Cleanup.ps1'), self.patch / 'Apply-Update.ps1')

    @staticmethod
    def sha(data): return hashlib.sha256(data).hexdigest()

    def apply(self):
        return subprocess.run(['C:/Windows/System32/WindowsPowerShell/v1.0/powershell.exe', '-NoProfile',
            '-ExecutionPolicy','Bypass','-File',str(self.patch / 'Apply-Update.ps1'),'-GameFolder',str(self.game)],
            capture_output=True,text=True,timeout=30)

    def test_replaces_verified_client_backs_up_original_and_preserves_save(self):
        result = self.apply()
        self.assertEqual(result.returncode,0,result.stdout+result.stderr)
        self.assertEqual((self.game / 'forge-adventure-test.jar').read_bytes(),b'new client')
        self.assertEqual((self.game / 'test-profile/keep.save').read_bytes(),b'my completed chapter')
        backups = list((self.game / '.updates').glob('*/forge-adventure-test.jar'))
        self.assertEqual(len(backups),1)
        self.assertEqual(backups[0].read_bytes(),b'old client')
        self.assertIn(self.sha(b'new client'),(self.game / 'SHA256SUMS.txt').read_text(encoding='utf-8'))

    def test_damaged_payload_is_rejected_before_any_game_file_changes(self):
        (self.payload / 'forge-adventure-test.jar').write_bytes(b'corrupt')
        result = self.apply()
        self.assertNotEqual(result.returncode,0)
        self.assertEqual((self.game / 'forge-adventure-test.jar').read_bytes(),b'old client')
        self.assertEqual((self.game / 'test-profile/keep.save').read_bytes(),b'my completed chapter')
        self.assertFalse((self.game / '.updates').exists())

    def test_declared_compatible_prior_patch_can_be_updated(self):
        (self.game / 'forge-adventure-test.jar').write_bytes(b'prior compatible patch')
        self.manifest['compatible_base_jars'] = [self.sha(b'prior compatible patch')]
        self.manifest_file.write_text(json.dumps(self.manifest))
        result = self.apply()
        self.assertEqual(result.returncode,0,result.stdout+result.stderr)
        self.assertEqual((self.game / 'test-profile/keep.save').read_bytes(),b'my completed chapter')

    def test_windows_separators_cannot_bypass_player_data_exclusion(self):
        (self.payload / 'test-profile').mkdir()
        (self.payload / 'test-profile/keep.save').write_bytes(b'forbidden replacement')
        self.manifest['files']['test-profile\\keep.save'] = self.sha(b'forbidden replacement')
        self.manifest_file.write_text(json.dumps(self.manifest))
        result = self.apply()
        self.assertNotEqual(result.returncode,0,result.stdout+result.stderr)
        self.assertEqual((self.game / 'test-profile/keep.save').read_bytes(),b'my completed chapter')


if __name__ == '__main__': unittest.main()
