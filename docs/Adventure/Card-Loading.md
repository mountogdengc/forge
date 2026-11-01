# Adventure card loading

Windows libGDX Adventure starts with deferred card rules. Edition metadata and
tokens remain available at startup; named deck, reward, saved-card and effect
lookups prepare the rules and printings they need. Bellwake keeps its existing
card availability, campaign version and save format.

The persistent cache stores normalized card names and source ordinals, not live
game objects or serialized rules. It lives at
`test-cache/card-index/common/lazy-card-name-index.bin` in the portable build.
The first run builds it. Later runs validate source inventory, archive metadata
and parser identity before reuse. A missing, stale, corrupt or unwritable cache
falls back to the source scripts. Deleting this index while the client is closed
is safe; the next launch rebuilds it.

Ordinary full-catalog APIs still materialize the complete database, including
printings omitted by an earlier explicit-set lookup. Classic/Quest, collection
tools, unrestricted random rewards and deck generators can therefore still pay
that cost when first used. The change defers unused work; it does not remove
other cards from the installed game. Android/iOS and Swing startup defaults are
preserved, including the existing desktop lazy-loading preference.

## Campaign authors

With the game closed, open `Edit-Campaign-Sets.cmd` in the Windows candidate.
Open **Allowed sets** and check the displayed campaign folder before editing.
Select one or more editions (Ctrl/Shift for multiple selections), then click
**Save allowed sets**. **All sets (clear selection)** followed by Save restores
the unrestricted selection. Back up authored campaign data before editing.

This changes only `allowedEditions` in that campaign's `config.json`, preserving
other fields and unknown selected codes. Unknown codes stay visible as
“Unavailable edition”; they do not acquire cards until that edition is installed.
No selection uses the existing unrestricted/restricted-set behavior. An explicit
whitelist retains the existing precedence over `restrictedEditions`; individual
card restrictions and other reward legality filters still apply.

When a random reward/deck pool is requested, a whitelist prepares every name in
the selected editions and filters actual printings to those editions. Cards
loaded by another campaign cannot leak into that pool. Named saved cards,
authored decks, tokens and effects remain resolvable even when they refer outside
the random acquisition pool. This is an author setting, not a player-facing
character-creation choice. Bellwake's whitelist is unchanged.

## Diagnosis and manual checks

Use `Start-Bellwake.cmd` for normal playtests. `Start-Bellwake-Eager.cmd` is a
diagnostic fallback using `-Dforge.cards.forceEager=true`. Both use the included
Java runtime and isolated profile. Avoid double-clicking a JAR, which bypasses
these launcher settings. Logs identify `Card scripts: deferred/eager`, index
`rebuilt/hit`, and the number of scripts parsed for the index.

1. Extract to a new writable folder. Start normally and time from launch to the
   usable Bellwake menu. Quit and repeat twice. Record first and later times;
   first launch should log `rebuilt`, later launches `hit` with zero index parses.
2. Complete the existing Bellwake manual checklist, including both starter
   decks, Tor practice loss, named ending rewards, save/quit/Continue and older
   0.3.0 saves. Report missing cards, delayed first-use pauses or changed rewards.
3. For author acceptance, use a disposable campaign copy. Save a small set
   whitelist, restart, and check generated cards have allowed printings. Switch
   campaigns and back; the original pool must return. Clear selection, save and
   restart; unrestricted acquisition must return. Preserve the original config.
4. If startup or a card fails, close the game and compare the eager launcher.
   Report launcher used, elapsed times, campaign, chosen sets, card name/edition,
   first/repeat run, Java error text, `logs/launcher.log` and `test-profile/forge.log`.

These checks complement automated catalog, cache, reward and save tests. They
still require a person to assess responsiveness, pacing and actual match play.

## Measurement method

`tools/adventure/CardLoadingBenchmark.java` measures database setup and a fixed
nine-card named lookup workload without a profile or graphics. Optional cache
directory enables cold/warm comparisons; `-Dforge.benchmark.complete=true`
also records full-catalog materialization and counts.

`tools/adventure/run_startup_benchmark.ps1` measures JVM main to the actual
Bellwake StartScene surviving an additional rendered frame. It uses a hidden,
unfocused window, disabled audio, an isolated profile/cache, a 2 GB heap and four
reported JVM processors (worker sizing, not OS CPU affinity). `-IndexCacheFrom` reuses only a prior run's validated card
index in another fresh profile. Every run preserves logs and JSON, checks commit
headroom, and has a bounded lifetime. OS file caches are not flushed; results
describe the measured machine and workload, not a guaranteed startup time.

`-FontCacheFrom` separately supplies converted font bitmaps from a prior isolated
run. The same seed was used for both clients in the controlled menu comparison;
it does not supply cards, a card-name index, a player profile or a saved game.

### Actual Bellwake menu results

| Client/cache condition | Main to usable menu | Common rules loaded |
| --- | ---: | ---: |
| Previous client, three runs | **25.842 s median** (28.752 / 25.842 / 25.053 s) | 33,610 |
| Final client, absent card-name index | **17.173 s**, one run | 0 |
| Final client, cached index, three runs | **12.105 s median** (11.404 / 12.105 / 12.289 s) | 0 |

These hidden, unfocused, audio-disabled runs used JDK 26, the same pinned package
resources, a 2 GB heap/four reported processors and the same preconverted fonts.
Each run had a fresh profile and cache except the explicitly seeded fonts/index.
This is about **53% less startup time** in the measured cached comparison.
A completely fresh extraction can also generate fonts and incur other first-use
costs; the table does not promise a 17-second complete first launch.

Warm index runs report `hit` and zero index script parses. All final menu probes
asserted that startup had not materialized the full catalog. Median summed peak
heap pools were 1,164 MiB old versus 392 MiB final cached; these are Java heap
observations, not total system/GPU memory. Evidence:
`.superpowers/card-loading/startup-old-final-repeats/`, `startup-final-cold/`,
and `startup-final-warm/`.

The real duel-opening probe also passed: zero loaded unique cards at the menu,
43 after preparing the starter, and 43 at the first duel. Named opening draws
remain private, three match Help dialogs render, and losing practice to Tor
leaves the player in the quay without penalties. Evidence:
`dist/bellwake-smoke/card-loading-help/`. Story victory checks remain assisted;
actual played matches and human responsiveness assessment remain manual.

### Database results, November 1, 2026

Sequential JDK 26 processes, 2 GB heap, four reported processors, same workspace
resources and nine-card lookup workload. Three paired samples used warm OS file
caches; only the new warm samples reused a persistent card-name index.

| Workload | Total median | Samples | Summed peak heap pools, median |
| --- | ---: | --- | ---: |
| Previous eager database | 2,673 ms | 2,673 / 3,097 / 2,643 ms | 704 MiB |
| Previous lazy database, no persistent index | 3,296 ms | 3,212 / 3,296 / 3,997 ms | 139 MiB |
| New deferred database, cached index | 1,282 ms | 1,336 / 1,282 / 1,270 ms | 126 MiB |

New cold-index setup took 4,138 ms in one sample, including 3,500 ms to parse
34,132 scripts and build the index. Warm samples parsed zero index scripts and
materialized only the nine requested common card rules. Cold here means an absent
application index, not a flushed OS disk cache.

Full fallback produced the same **97,271 printings / 33,223 unique cards** as the
eager database. Materializing that whole catalog after a nine-card lazy start
took another 6,879 ms in the compatibility sample. That first-use cost remains
when a full collection or unrestricted random pool is needed.

Earlier initial samples were substantially slower (18.09 seconds eager database,
30.90 seconds old menu startup), so they are retained as diagnostic observations,
not used as the repeated database speedup baseline. Host load and OS caching
matter. Database timings do not establish the complete application's startup time.
Raw final comparison evidence: `.superpowers/card-loading/comparison-final/`, runner job
`e00605295ba345d3871607e77cfadddb`. The earlier comparison is retained separately.

The nine-module reactor passed with 1,443 reported cases, zero
failures/errors and six existing optional desktop skips after the final startup
and incomplete-index guards. It follows a clean whole-reactor build. It includes the cache,
deferred catalog, campaign reward/pool, startup policy and editor roundtrip cases.
Final build job: `0b30a039e0054e899e4585f2003ac52d`.
