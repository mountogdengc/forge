# Bellwake desktop slowdown investigation — November 1, 2026

David reports severe whole-desktop/cursor slowdown while the SSD client runs,
including several minutes to quit from the menu. Normal responsiveness returns
after closing it. Current installation:
`C:/tmp/Bellwake-Windows-0.3.0/Bellwake-Windows-0.3.0`.

The initial investigation did not relaunch the game. One subsequently approved
30-second run is recorded below; no second run has been made. After the original exit there was no Java
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

Before the first approved run, configuration tests failed
before the policy existed and passed afterward. Watchdog timeout/normal-exit checks
use harmless hidden PowerShell child processes; they do not exercise graphics
or prove desktop safety. Each physical run remains a separate, user-approved step.

The affected Maven reactor passed 115 cases with zero failures/errors/skips,
including the two new graphics configuration cases. Desktop/game behavior tests
outside that reactor retain their preceding evidence, not a fresh rerun here.
Log `.bellwake-desktop-diagnostic-build.log`. Windows PowerShell 5.1 watchdog
checks verify timeout, normal exit and termination after telemetry-directory
failure. Fresh review caught the latter lifecycle gap; its regression failed
before guarded Java-first cleanup and passed afterward. The watchdog and test
processes all exited. No user-installed game file was edited.

Prepared add-on: `dist/bellwake-desktop-diagnostic/first-candidate/Bellwake-Desktop-Diagnostic.zip`.
55,315,108 bytes; 12 payload files verified byte-for-byte and by ZIP CRC.
SHA256 `c5895fa6bdb7181386427525c417e32cde969cc1ff7ca0de2857790985fea6e9`.
Packaging job `c07ac8617cfa48088cb816b6f9c29dee` exits 0. Source
`da45f076fec0`; diagnostic JAR SHA256
`56cf4a42c4827efc871023bc631daf4e35134e7c170da8414fe88f90ad579304`.
The installed original JAR still matches its previous
`cb946014ef6e021853151ccbb547450cd3c58a75b79f86ec6fda33e07f7b15a2` hash.
No game Java process was running at the final read-only check.

## First approved visible run — 17:21 local time

The add-on's 12 files were installed beside the user's existing JAR; that
original JAR retained its prior hash. User explicitly approved one 30-second
run. The actual Main/GameLauncher log confirms 30 FPS, VSync true, audio false,
diagnostic true, a warm index hit and zero card-index script parses. The
watchdog recorded 30,237 ms and `stopped_at_limit=true`; its exit-code field was
null, so termination was independently verified by the absence of Java/Javaw.

David reported the desktop/cursor stayed responsive, and he created a new game
and played until automatic closure. GPU samples were 9–24% utilization and
3,189–4,021 MiB total VRAM use of 24,576 MiB. Java working set peaked around
1,353 MiB, private bytes around 2,397 MiB, with 20.92 process CPU seconds over
the sampled interval. These are not all Java heap memory. After closure, the
host had about 15 GB available RAM and zero sampled paging.

Logs: `C:/tmp/Bellwake-Windows-0.3.0/Bellwake-Windows-0.3.0/logs/diagnostic-20261101-172124-4413d3`.
Preserved raw copies and summary:
`.superpowers/desktop-slowdown/approved-run-20261101-172124/`.
This establishes a usable low-load baseline for that run, not the cause of the
original slowdown: frame pacing, audio, worker sizing, heap and cache state
were different from the ordinary run.

An optional audio-on comparison is prepared separately. It changes only audio
from the successful diagnostic settings; 30 FPS, window size, heap, processor
count and deadline stay the same. A configuration test asserts frame/VSync/audio
settings; the script keeps the other launch arguments unchanged. The comparison
uses a distinct `GITBellwakeAudioDiagnostic` revision, and packaging rejects the
earlier client so an audio-on label cannot silently use its audio-disabled code.
It has not been installed or run; approval for the first run is not treated as
permission for a second. If audio-on reproduces the problem, investigate the
audio backend/device path. If it stays responsive, retain the frame cap and
isolate other differences before declaring a root cause.
