"""Reject packaging paths that cannot support the current desktop defaults."""
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
import zipfile

import package_bellwake_windows as package


class BellwakeClientTest(unittest.TestCase):
    def test_client_argument_is_required_before_reading_archives(self):
        result = subprocess.run([sys.executable, str(Path(package.__file__)),
                                 '--base-zip', 'absent-base.zip', '--update-zip', 'absent-update.zip',
                                 '--output', 'absent-output', '--verify-dir', 'absent-verify'],
                                capture_output=True, text=True)
        self.assertEqual(result.returncode, 2)
        self.assertIn('--client-jar', result.stderr)
        self.assertIn('required', result.stderr)

    def test_policy_class_and_passing_normal_launcher_evidence_are_required(self):
        with tempfile.TemporaryDirectory(dir=package.ROOT / '.build-cache') as folder:
            root = Path(folder)
            jar = root / 'client.jar'
            with zipfile.ZipFile(jar, 'w') as archive:
                archive.writestr('unrelated.class', b'fixture')
            with self.assertRaisesRegex(ValueError, 'DesktopGraphicsPolicy'):
                package.check_desktop_defaults_client(jar, root)
            with zipfile.ZipFile(jar, 'w') as archive:
                archive.writestr('forge/app/DesktopGraphicsPolicy.class', b'fixture')
            with self.assertRaisesRegex(ValueError, 'normal-launcher'):
                package.check_desktop_defaults_client(jar, root)
            reports = root / 'forge-gui-mobile-dev/target/surefire-reports'
            reports.mkdir(parents=True)
            report = reports / 'TEST-forge.app.DesktopGraphicsPolicyTest.xml'
            report.write_text('<testsuite><testcase classname="forge.app.DesktopGraphicsPolicyTest" '
                              'name="normalLauncherHasAnIndependentFrameLimit"><failure/></testcase></testsuite>')
            with self.assertRaisesRegex(ValueError, 'normal-launcher'):
                package.check_desktop_defaults_client(jar, root)
            report.write_text('<testsuite><testcase classname="forge.app.DesktopGraphicsPolicyTest" '
                              'name="normalLauncherHasAnIndependentFrameLimit"/></testsuite>')
            package.check_desktop_defaults_client(jar, root)


if __name__ == '__main__':
    unittest.main()
