import subprocess
import tempfile
import unittest
from pathlib import Path
import package_windows_test as package


class SourceIdentityTest(unittest.TestCase):
    def test_archive_retains_entire_campaign_version_and_source_id(self):
        folder = Path('output/CurseOfStrahd-Windows-0.1.0-34876cb2970')
        self.assertEqual(package.archive_for(folder), Path(str(folder) + '.zip'))

    def test_source_evidence_includes_staged_changes_and_untracked_files(self):
        with tempfile.TemporaryDirectory(prefix='forge-source-evidence-') as scratch:
            root = Path(scratch)
            def git(*args):
                subprocess.run(['git', *args], cwd=root, check=True, capture_output=True)
            git('init', '-q')
            (root / 'changed.txt').write_text('old')
            (root / 'deleted.txt').write_text('old')
            git('add', '.')
            git('-c', 'user.name=Fixture', '-c', 'user.email=fixture@example.invalid', 'commit', '-qm', 'fixture')
            (root / 'changed.txt').write_text('new')
            (root / 'deleted.txt').unlink()
            (root / 'added.txt').write_text('new')
            git('add', '-A')
            (root / 'untracked.txt').write_text('new')
            self.assertEqual(set(package.changed_sources(root)) - {''},
                             {'changed.txt', 'deleted.txt', 'added.txt', 'untracked.txt'})


if __name__ == '__main__':
    unittest.main()
