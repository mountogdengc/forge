"""Check the launcher bytes consumed by Windows package assembly."""
import unittest

import package_windows_test as package


class WindowsDefaultsTest(unittest.TestCase):
    def test_normal_launcher_uses_the_tested_small_runtime_budget(self):
        launcher = package.LAUNCHER
        command = next(line for line in launcher.splitlines() if line.startswith('"runtime\\bin\\java.exe"'))
        for option in ('-Xms128m', '-Xmx1g', '-XX:ActiveProcessorCount=2'):
            self.assertIn(option, command.split())
        self.assertNotIn('-Dforge.cards.forceEager=true', command)

    def test_eager_launcher_retains_runtime_budget_and_enables_diagnostics(self):
        launcher = package.render_launcher(eager=True)
        self.assertIn('-Dforge.cards.forceEager=true', launcher)
        for option in ('-Xms128m', '-Xmx1g', '-XX:ActiveProcessorCount=2'):
            self.assertIn(option, launcher)

    def test_heap_template_changes_cannot_silently_remove_eager_diagnostics(self):
        tuned = package.LAUNCHER.replace('-Xms128m', '-Xms192m')
        original = package.LAUNCHER
        try:
            package.LAUNCHER = tuned
            command = next(line for line in package.render_launcher(eager=True).splitlines()
                           if line.startswith('"runtime\\bin\\java.exe"'))
        finally:
            package.LAUNCHER = original
        self.assertEqual(command.split().count('-Dforge.cards.forceEager=true'), 1)
        self.assertIn('-Xms192m', command.split())


if __name__ == '__main__':
    unittest.main()
