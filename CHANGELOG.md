# Changelog

Notable changes to Lutherverse, newest first.

The format loosely follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Release tags will use [Semantic Versioning](https://semver.org/spec/v2.0.0.html). Merge checkpoints are tagged `cpd-sync-base-*` (and later `spd-sync-*`).

---

## [Unreleased]

Everything on `main` since the fork base (`cpd-sync-base-2025-08-15`, CPD commit `c97fb83`).

### 2026-10-24: theme doc and README

- New `docs/THEME.md`: the pitch, five house rules, tone and naming, palette, audio direction, and a status list that keeps shipped features apart from plans. It also lists the in-game strings that don't match the code yet.
- `README.md` now leads with the cosmic-horror angle. Sections are numbered like floors, quoted creature and item text is what actually ships, and anything not built yet is marked as planned.
- New concept title card in `docs/assets/lutherverse-title.svg`: a square stair winding in to a gold keyhole, with the inherited floors 001 to 025 lit. The lettering is drawn from rects (no fonts, no scripts), so GitHub renders it the same everywhere.

### 2026-10-23: upstream catch-up to v2.5.4

**Ported**
- The 23 tuning batches left over from July. About 245 upstream commits reviewed, roughly 170 ported, and every remaining one tracked as an open issue.
- v2.3.0's two-deck potion and scroll drops, the damage-icon call sites, trap depth scaling, shop and Warlock parity, and the hero-null guards.
- The badge ending flow, with upstream d029c0d7d pulled in early so resizing the window can't record a second win.
- Translations synced to v2.5.4 wherever CPDU's English matches upstream's.

**Fixed**
- The shop could pick a different bag for the same seed because of an identity-hashed tie break. It's a `LinkedHashMap` now. Upstream has the same bug.
- Floor 20 walled itself off after the boss.
- Custom levels shuffled with an unseeded RNG.
- Brawler's Stance never drained charge.
- Level-name messages formatted a string with `%d`.
- Eight conflicts between port branches that merged cleanly but behaved wrong. One example: a scroll of remove curse wasn't used up when it cleansed a tormented spirit.

**Tests**
- `core:test` now treats the asset tree and the root build script as inputs, so asset-only or version-only changes re-run it.
- New tests check placeholders in translated strings and reject message values split across lines.
- Mob behaviour is pinned with a stub sprite and turn pump, and boss floors are covered by golden level-generation digests.
- Core tests went from 181 to 545.

### 2026-08-12: content tools

- `services/tools/content-audit`: checks that every core Mob and Item is fully wired up (sprite, localization, registration).
- `services/tools/content-scaffold`: generates the boilerplate for a new Mob or Item.

### 2026-08-11

- `services/tools/asset-audit`: finds shipped assets that nothing references.
- deletion-audit now detects renames, and rejects pairings where the file name itself changed.

### 2026-08-10: audit tools, boot smoke, repo rename

**Added**
- `services/tools/deletion-audit`: catches removals api-diff can't see, like private members or statements cut from a method whose signature didn't change. It has an allowlist for removals I've already reviewed. Its first run over the v2.1.0 to v2.5.4 port found 35 deletions and 8 shrunken bodies across 1,021 files: 27 were fine, 16 stayed open for review.
- `services/tools/desktop-smoke`: boots the real game and confirms a frame renders. It replaces the Android emulator smoke, which never got as far as `adb install`. First green run rendered 120 frames.
- `services/tools/port-verify`: checks that an upstream commit's content actually landed.
- `BundleAliasRoundtripTest`: pins the `Bundle.addAlias` lookup that the `EntranceRoom`/`ExitRoom` package move relies on. A negative control proves an unregistered missing class doesn't resolve some other way.

**Changed**
- Renamed the GitHub repo from `CustomPixelDungeonUltimate` to `Lutherverse`. Old URLs redirect, and the links in README, CONTRIBUTING and the issue templates point at the new name.
- The Android runtime smoke is now a manual pre-release check instead of an automated gate. It had never actually run the APK here.

**Fixed**
- deletion-audit looked for its allowlist relative to the process working directory, which `gradle run` sets to the subproject. The file wasn't found, the allowlist loaded empty, and all 43 findings came back as unreviewed. Relative paths now resolve from the repo root, and a missing allowlist exits 2 instead of quietly allowing nothing.

### July 2026: first upstream sync (SPD v2.1.0 to v2.5.4)

The range is 1,209 upstream commits. I sorted them commit by commit (and hunk by hunk where engine work and later features overlap) instead of merging the tag or cherry-picking everything.

**Ported**
- Actor, Char, Hero, Dungeon, Level and level generation, and mob and combat tuning.
- The full entrance/exit room set: `EntranceRoom`/`ExitRoom` moved package (with `Bundle.addAlias` so old saves still load), six new region standard rooms, twenty entrance/exit variants, and the `canMerge(Level, Room, Point, int)` signature across fourteen definitions.
- New terrain: `CUSTOM_DECO` (reusing the old `SIGN` id 23, so saves stay compatible), `CUSTOM_DECO_EMPTY` and `ENTRANCE_SP`, with the tiles layer to match.
- 11 of the 34 tuning batches. The other 23 landed in October.
- Re-sorted 730 commits that had been parked as dead code: 541 belonged in this sync, 56 belong to the next one, 83 really are dead, and 50 needed a manual call. Another 60 quest-content commits were moved out, because the first pass only scanned added lines and missed commits that touch quest state without naming a marker class.

**Fixed**
- CorpseDust could be dropped. The `actions(Hero)` override that makes the quest item undroppable got lost during the port, which could soft-lock the Wandmaker quest. It's back.
- api-diff never audited anything. It ran `git ls-tree` with no working directory, so under Gradle it scanned the subproject, looked at zero files and printed PASS. Fixing that exposed a second bug: it only recognised one of git's two "path not found" messages and crashed on the first newly added file. Both fixed; it now scans 1,021 files.

**Known gaps at the time**
- `core:test` had no sources, so compiling was the only automatic check for core-only changes.
- The Android emulator never reached `sys.boot_completed`, so the runtime smoke couldn't run.
- There was no save round-trip test, even though this sync touched serialized state.
- The package-namespace transformer I'd planned turned out not to exist yet, so it became the first prerequisite for this sync.
- Pack smoke only checks the 29 manifests structurally, the Android smoke only proves the process stays alive (not that the title screen appears), and the save bridge only has synthetic tests.

### July 2026: groundwork for the upstream sync

**Added**
- `services/tools/api-diff`: compares the public and protected Java surface between two git refs and exits 1 on removals or signature changes. Run it with `./gradlew apiDiff --args="--base <ref> --head <ref>"`.
- `services/tools/pack-smoke`: validates every marketplace pack's manifest and asset references without booting the game. 29 of 29 pass at the fork base. `marketplace/Summary/` is storefront metadata with no `mod_info.json`, so it's skipped.
- `services/tools/smoke-boot`: headless Android scripts (pwsh and bash) that boot the emulator, install the debug APK and check the process is still alive 10 seconds later. Manual only, not in CI.
- `BundleBridge` save-compatibility scaffolding in `SPD-classes`: version detection, a translator chain (`PreV232Translator`, `PreV242Translator`, `PreV254Translator`, all stubs for now) and `BundleBridgeException` for formats it doesn't recognise.
- `BundleBridgeTest`: 5 JUnit 5 tests on a synthetic fixture. Real save fixtures come later.

**Changed**
- Removed the dangling `include ':ios'` from `settings.gradle`.
- Planned the full sync to SPD v3.3.8 in stages. The main calls: SPD's tilemap edits win and CPD's changes are reapplied on top, the Cleric is a full hero, the Vault is first-class content, iOS waits, the mod DSL is frozen until near the end, and every marketplace pack has to pass before a merge is tagged.

### 2026-07-21: fork created

**Added**
- Public fork of QuasiStellar/custom-pixel-dungeon at [luther-rotmg/CustomPixelDungeonUltimate](https://github.com/luther-rotmg/CustomPixelDungeonUltimate) (since renamed to Lutherverse).
- Three remotes: `origin` (this fork), `upstream-cpd` (QuasiStellar's `margarita` @ c97fb83) and `upstream-spd` (Evan's `master`, v3.3.8 @ 7b8b845a).
- Annotated tag `cpd-sync-base-2025-08-15` marking the fork base at c97fb83.
- `README.md` rewritten for Lutherverse: placeholder title card, badges, the vision (cosmic horror with FFX, KH2, DkS and Bloodborne influences, keyblades and a Keybearer class, co-op, save zones, towns, a sphere grid, a 200-floor descent), a roadmap, a call for alpha testers, a cameo list (O'aka, Patches, Sora, Doll and a few lighter nods) and the attribution chain. I later rewrote the prose to be plainer and less salesy.
- `THIRD_PARTY_NOTICES.md` covering the GPLv3 chain (Watabou, Evan, QuasiStellar, CPDU), libGDX (Apache 2.0), RoboVM/MobiVM (GPLv2 with Classpath Exception) and SPD-classes.
- `docs/assets/lutherverse-title.svg`: a placeholder purple and gold title card with spirals, with a `GPL-3.0-or-later` SPDX header.
- This changelog, `CONTRIBUTING.md`, and issue templates for bug reports, feature requests and cameo suggestions (blank issues turned off).

**Changed**
- Renamed the default branch from `margarita` (CPD's cocktail codename) to `main`, locally and on origin. The upstream tracking branches keep their own names: `margarita` for CPD, `master` for SPD.

**Fixed: the fork base didn't build**

CPD's `c97fb83` wouldn't build as-is. Four separate problems, fixed one at a time:

- `build.gradle`: pinned `gdxControllersVersion` from `2.2.4-SNAPSHOT` to `2.2.3`. The snapshot had expired from Google Maven, Maven Central and Sonatype snapshots, and 2.2.4 was never actually released.
- `android/build.gradle`: turned on `multiDexEnabled true` and added `androidx.multidex:multidex:2.0.1`, since the app went over the 64K DEX method limit once dependencies resolved.
- `AndroidManifest.xml`: set `android:name="androidx.multidex.MultiDexApplication"` on `<application>`. CPD had no custom Application class, so no Java was needed.
- `gradle.properties`: added `android.useAndroidX=true`. Jetifier isn't needed (no `com.android.support` references).
- The docs said to build desktop with `desktop:dist`, which doesn't exist. The task is `desktop:release`. README corrected.

Both builds now pass: `desktop-2.1.0-1.0.jar` (45.9 MB) and `android-debug.apk` (22.8 MB), on JDK 17, Android SDK 33, build-tools 33.0.2 and Gradle 8.5.

The multidex dependency can go once SPD v3.2 raises `minSdk` to 21. The gdx-controllers pin is temporary too: SPD v3.2 and v3.3 move libGDX to 1.13.6-SNAPSHOT and 1.14.0, which will pull a newer version.

**Fixed: README and notices cleanup**
- A README roadmap row leaked an internal name for Bonfire Mode. Removed.
- The attribution paragraph promised more library notices than the file had. Reworded to match. A full audit of the Apache/BSD/MIT dependencies (FreeType, LWJGL, Kotlin, Ktor, SLF4J, org.json, gdx-controllers, androidx.multidex) is due before the first alpha build.
- The iOS row implied dormant iOS code. There isn't any: QuasiStellar deleted `ios/` in May 2023 and the `settings.gradle` entry pointed at nothing.
- The modding section linked `docs/modding-api-v1.md`, which doesn't exist yet. Unlinked, with a note.
- RoboVM's copyright line credited RoboVM AB through 2026. Split it: RoboVM AB up to the 2015 Xamarin acquisition, the MobiVM community fork after.

**Still open**
- No CI yet.
- iOS builds are off the table for now.
- The full library-notice audit (above).
- A cameo disclaimer that reserves trademarks to their owners.
- Parts of the README vision describe planned features in the present tense.
- No trademark, domain or handle check on the name "Lutherverse" yet.
- Whether to keep or rename Keyblade and Aeons once they become in-game nouns.

---

## Before the fork

Everything before `c97fb832` is [Custom Pixel Dungeon](https://github.com/QuasiStellar/custom-pixel-dungeon) history (QuasiStellar) and, before that, [Shattered Pixel Dungeon](https://github.com/00-Evan/shattered-pixel-dungeon) history (Evan Debenham).
