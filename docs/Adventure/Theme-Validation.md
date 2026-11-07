# Validate and inspect Adventure themes

The resource-only validator does not start Forge, load cards, create a renderer,
or write a profile. After building the Adventure desktop application, run from
the repository root (replace the packaged JAR version if needed):

```powershell
$themeJar = 'forge-gui-mobile-dev/target/forge-gui-mobile-dev-2.0.16-GITWindowsTest-jar-with-dependencies.jar'
java -cp $themeJar forge.adventure.theme.ThemeValidationMain `
  --builtins forge-gui/res/adventure/common/themes `
  --users docs/Adventure/theme-examples --theme user.moonlit-folio
```

Exit 0 prints `VALID <id>` with resolved counts. Exit 1 prints `INVALID` with
the manifest path and problematic role/dependency. `--users` is optional.
Repeat with `--theme folio`, `midnight`, and `verdant` to inspect built-ins.
See [the author guide](Themes.md) for the schema and install workflow.

## Automated checks

Resolver, persistence, runtime snapshot, skin isolation, drawable ownership,
presentation, palette, draft, and preview tests live in
`forge-gui-mobile/test/forge/adventure/theme`. The native fixture lives in
`forge-gui-mobile-dev/test-unit/forge/app/AdventureThemeSmoke.java`.

Build sequentially with the repository's Maven settings and a configured JDK:

```powershell
mvn -B -ntp -pl forge-gui-mobile-dev -am verify -Drevision=2.0.16-GITWindowsTest
```

The project targets Java 17. An offline build needs an already populated local
Maven repository; this development environment uses `.build-cache/m2`.

After a current build, capture native screens with the provided script:

```powershell
./tools/adventure/run_theme_smoke.ps1 -SourceRoot . `
  -JavaHome 'C:/Program Files/Java/jdk-26' -ThemeId folio `
  -Width 1280 -Height 720 -FullScenes
```

Use `midnight` and `verdant`, 1920×1080, and a supported portrait size for the
other variants. `-OutputPath` must name a fresh evidence directory. The fixture
uses a hidden, unfocused native window, disables network image fetching, and
creates a marked disposable profile. It refuses normal profile overrides.
It does not control the desktop or attach to a running game. Full-scenes mode
creates gameplay fixtures only inside that disposable profile.

Evidence includes source identity, exact compile/runtime arguments, logs,
screenshots, saved recipe ID, and a `PASS` marker. Exit success requires both
exit code 0 and the marker. A screenshot by itself is not a passing test.
The script uses freshly built reactor class directories ahead of packaged
dependency JARs and asserts the active source class origin.

For a real restart check, take the ID in `selected-theme.txt` and run with a new
output directory, `-Resume`, `-ProfileRoot <first evidence directory>/profile`,
and `-ThemeId <saved ID>`. The second process checks the saved quiet ornament and
zero texture. Switching through Classic in the first process must leave the
pending selection unapplied and Classic's palette/preference unchanged.

## Device acceptance

Native desktop captures do not prove touch or physical controller behavior.
Use an Android test device (Android 8 or later), Android SDK platform 35 and
build-tools 35.0.0, platform-tools/adb, JDK 17, and Maven with the Android build
dependencies available. From these exact sources, the existing development
profile builds a separately named forge.app.dev APK:

~~~powershell
mvn -B -ntp -pl forge-gui-android -am verify -Pandroid-dev-build -Drevision=2.0.16-GITThemeTest
$themeApk = Get-ChildItem forge-gui-android/target/*debugSigned.apk | Select-Object -First 1
adb install -r $themeApk.FullName
adb shell am start -n forge.app.dev/forge.app.Launcher
~~~

This device build/install sequence has not been run by the Windows theme check.
Use a test device/profile and choose a new empty ForgeThemeTest storage folder
during setup; finish the app's normal resource setup before testing. Record
device, OS, orientation, build revision, storage folder, and input method. For
iOS, use the repository's existing signed-device build process and the same
actions below; no iOS build or physical-device pass is claimed here.

1. Open Adventure Settings → Appearance. Navigate every control using the
   device's touch input and, separately, its supported controller. Expected:
   focus is visible; Back, Customize, and Apply remain reachable.
2. Customize each preset in landscape and portrait. Scroll both panels, switch
   all preview tabs, enter invalid then valid color pairs, set Quiet and 0%
   texture, and Restore. Expected: readable text, one selected tab, visible
   swatches, invalid save disabled, no clipping outside scroll frames.
3. Save a named variant, return, select it, and Apply. Expected: pending changes,
   active theme remains unchanged. Return through Classic and Adventure.
4. Fully close and restart Forge. Expected: the saved variant is active. Open
   journal, inventory, shop/rewards, deck management, dialog, and a duel.
   Expected: original layout/input behavior with consistent readable theme.
5. Report step, expected/actual result, screenshot, device details, build, and
   input method for any failure. Device-specific fixes depend on this evidence;
   no mobile-device pass is inferred from desktop simulation.

## Verification record — 2026-11-02

Execution source: F:/Projects-PCGames/forge, existing feature checkout at
06aaacbba91b24d3cb91d393c0c3bfb5e5d19ed3 plus the uncommitted identity changes.
No gameplay, campaign content, global skin preference, normal-profile saves,
commit, merge, or publishing operation was part of this change.

The final offline Adventure reactor verify completed successfully. Only XML
reports written after that build's start were counted; older focused reports
remain in Surefire directories and must not be added again.

| Group | Tests | Failures/errors | Skips |
| --- | ---: | ---: | ---: |
| Game | 24 | 0 | 0 |
| GUI | 24 | 0 | 0 |
| Mobile | 158 | 0 | 1 |
| Mobile desktop development | 6 | 0 | 0 |
| **Total** | **212** | **0** | **1** |

211 tests passed. The focused theme group contains 60 tests: 59 pass, one
explicitly skips Windows symbolic-link creation because this account lacks the
privilege. A separate, actual Windows junction escape test passes. JDK 26 was
used with the repository's Java 17 release target; an actual Java 17 runtime
was not available.

The native fixture passed Folio, Midnight, and Verdant at 1920×1080 and
1280×720, plus Folio at 720×1280. Captures cover start/settings, authoring,
HUD/exploration, journal, inventory, deck/shop, rewards, dialog, initialized
duel, and targeting-color samples. Long captions, quiet ornament, zero
texture, pending selection, Classic return, and a fresh-process saved-variant
restart were checked. The targeting overlay sample is visual evidence of the
real renderer and palette, not automated live card-target selection.

Final targeted checks after that matrix covered all registered workshop form
controls (including wrapping from bottom back to name) in both orientations,
controller keyboard completion, independent campaign skins without the common
pixel, limited-font Greek glyph fallback, authored atlas/PNG tab/resource/glass
slots, frame captions, dark-paper ink, and borrowed-texture survival. A final
full Folio native run and final reactor build followed the last fixes.

Resource-only validation using the final packaged JAR accepts all three
built-ins and the starter, and rejects a malformed manifest with exit 1.
It starts no card database, renderer, or profile.

Local evidence is under .build-cache/adventure-themes and
.superpowers/sdd/2026-11-02-adventure-visual-identity. Native evidence includes
source hashes and a copy of the exact fixture source; its class origin must be
the fresh evidence directory. The older accepted-authored-assets run has an
INVALID marker because a test-source encoding failure exposed a stale-class
fallback; it is not acceptance evidence. The restored UTF-8 fixture, explicit
class-file guard, and fresh-source probes supersede it.

One independent whole-change review identified eight Important findings, all
addressed through the recorded red/green fix pass and full-suite verification.
The three deferred polish items are documented in Themes.md. Physical-device
touch/controller testing remains the separate acceptance procedure above.
