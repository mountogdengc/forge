# Bellwake desktop slowdown investigation — November 1, 2026

David reports severe whole-desktop/cursor slowdown while the SSD client runs,
including several minutes to quit from the menu. Normal responsiveness returns
after closing it. Current installation:
`C:/tmp/Bellwake-Windows-0.3.0/Bellwake-Windows-0.3.0`.

No game was relaunched during this investigation. After exit there is no Java
game process, about 14 GB available physical memory, zero paging/disk queue in
the captured sample, and the RTX 3090 reports 3,410/24,576 MiB VRAM and 6% GPU
utilization. These are idle observations and cannot rule out resource pressure
while the game runs. No matching recent display-reset event was found.

The current launcher log identifies `GITBellwakeCardLoading`, reaches the menu,
and does not request full-catalog materialization. The 2 GB heap/four reported
processors are configured in the CMD launcher. Slowdown also occurs via CMD,
so changing the JAR association alone does not explain it.

One verified test/production mismatch: GameLauncher relied on the LWJGL backend's
zero/unlimited foreground-FPS default. The hidden tests explicitly capped 60 FPS
and disabled hardware audio. VSync defaults true; its presence alone does not
establish actual frame rate on this machine. Mouse confinement is restricted to
fullscreen and current settings are windowed. `useGlfwAsync()` is a macOS-only
operation in this installed backend and is not a Windows explanation.

The desktop launcher now sets an independent 60-FPS limit. Opt-in
`-Dforge.desktop.diagnostic=true` selects 30 FPS, disabled audio and windowed
mode. This corrects a definite configuration gap but is **not a confirmed fix
for the system slowdown**. The low-load diagnostic changes several subsystems
for initial safety/triage; if it is responsive, reintroduce one variable at a
time to identify the cause. Do not infer whether rendering or audio caused it
from that result alone.

## Diagnostic add-on

The add-on contains a separately named client JAR and a watchdog launcher.
It leaves the original game JAR and launchers in place. It uses the existing
portable profile/cache; stay at the menu and do not load/create a character,
start a match or change settings during this diagnostic.

1. Extract the add-on contents beside the current `forge-adventure-test.jar`,
   where the existing `runtime`, `res` and `forge.profile.properties` reside.
2. Run `Start-Bellwake-Diagnostic.cmd` only when ready for a controlled test.
   Keep the diagnostic console open; closing it can cancel the watchdog.
   It starts the real Main/GameLauncher at 960×540, 30 FPS, no audio, a 1 GB heap
   and two reported processors. It requests termination after 30 seconds,
   including loading. A driver/kernel stall can delay a user-space watchdog.
3. Remain at the menu. Observe desktop/cursor responsiveness outside the game.
   The first run may end during loading; record that rather than extending it.
4. Keep the newest `logs/diagnostic-...` folder. It contains game stdout/stderr,
   one-second CPU/memory samples, optional bounded NVIDIA GPU queries and a
   termination result. Report whether the menu appeared, when slowdown began,
   whether the desktop stayed responsive, and whether automatic termination
   restored it. No older process is terminated by this script.

The game has not been launched with this candidate. Configuration tests fail
before the policy exists and pass afterward. Watchdog timeout/normal-exit checks
use harmless hidden PowerShell child processes; they do not exercise graphics
or prove desktop safety. Physical run remains a separate, user-approved step.

The affected Maven reactor passed 115 cases with zero failures/errors/skips,
including the two new graphics configuration cases. Desktop/game behavior tests
outside that reactor retain their preceding evidence, not a fresh rerun here.
Log `.bellwake-desktop-diagnostic-build.log`. Windows PowerShell 5.1 watchdog
checks verify timeout, normal exit and termination after telemetry-directory
failure. Fresh review caught the latter lifecycle gap; its regression failed
before guarded Java-first cleanup and passed afterward. The watchdog and test
processes all exited. No user-installed game file was edited.
