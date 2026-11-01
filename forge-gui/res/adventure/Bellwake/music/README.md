# Bellwake environmental music beds

Original procedural soundscapes authored for this chapter; no sampled recordings or downloaded music. Source: `tools/adventure/build_bellwake_presentation.py`. These assets follow the repository GPL-3.0 license; existing pixel tiles retain their original repository credits.

| Existing playlist | Asset | Intent |
| --- | --- | --- |
| TOWN | river-lanterns.wav | River wash, soft wind, sparse warm bell and faint hearth texture |
| BLACK | reed-water.wav | Open marsh wind/water, without the town bell |
| CAVE | last-fare.wav | Hollow water/wind and a lower bell with restrained echoes |
| MATCH | tor-training.wav | Quiet, regular training pulse |
| BOSS | keeper-pulse.wav | Slower low pulse and a darker bell |

Files are mono 16-bit PCM, 22050 Hz, 48 seconds. Endpoints fade to zero; peaks and RMS are checked to avoid clipping or startling jumps. The existing music mixer, dialogue fades, shelved/resumed tracks and mute/volume settings own playback. No new background thread or parallel ambience player is introduced. Campaign-owned folders override shared tracks through the existing resolver; user music overrides retain their usual precedence.

Regeneration uses NumPy; optional TMX diagnostic previews use Pillow. Neither is required to play. A developer can run `python tools/adventure/build_bellwake_presentation.py --build` from the full checkout. Validate with `python tools/adventure/test_bellwake_presentation.py` and the native `-PresentationProbe` check. Hearing the in-game mix, looping, transitions and device behavior remains part of the manual checklist.
