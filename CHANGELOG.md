# Changelog

Notable changes to Lutherverse, newest first.

The format loosely follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Release tags will use [Semantic Versioning](https://semver.org/spec/v2.0.0.html). Merge checkpoints are tagged `cpd-sync-base-*` (and later `spd-sync-*`).

---

## [Unreleased]

Everything on `main` since the fork base (`cpd-sync-base-2025-08-15`, CPD commit `c97fb83`).

### 2026-11-14: pack music

**Added**
- A pack can ship its own copy of any music file, and that copy plays in place of the base track, the same way pack sprites replace the base art. With several packs, the first enabled one wins. A layout's music block can also name a file only its pack ships. Track picks draw the same random numbers as before, so seeded runs are unchanged.

### 2026-11-14: the loading screen, statues and mimics, the sad ghost, music, the catalog and the update check

The fourth batch from the check of Slice 1 commits against v2.5.4.

**Fixed**
- The wandmaker's quest room and the wandmaker always arrive together, on whichever regular floor the layout names. Before, a pack that named a floor outside the prison got a quest room with no wandmaker, and the next prison floor got a wandmaker with no room.
- Packs with their own start floor get the right loading screen. A new game shows the start floor's region, continuing from the title shows the region of the floor you saved on, and the region story shows when the layout enters a new region instead of on every fifth floor. A pack that starts above the Sewers shows the Sewers story once, not twice.
- The missing libraries screen on Android names this game and no longer crashes when Android reopens it.

**Ported**
- The loading screen shows each region's splash art and pans it to suit the window. Walking down to the first floor of a new region shows that region's story there, every run, with a button to continue and a button to hide the text. Any key continues. Falling skips the story, and the story window on arrival is gone. The amulet floor counts as its own region, so it shows no Halls story.
- The run's seed is set when you start from hero select, as upstream does.
- Statues and mimics draw from the item decks only while a floor is generated. Loading a save, a distortion trap's statue or a cursed wand's mimic no longer shifts what later floors hold. Armored statues roll their armour once. Seeded floors with a mimic lay out differently.
- The sad ghost appears in the exit room and stays there. It no longer teleports when you talk to it or follows you afterwards, and it never wanders onto items or the exit. Seeded sewer floors lay out differently.
- Each region's music has its third track.
- The missing libraries screen shows the app version, device and Android details for a bug report.
- Empty files left by an interrupted save are cleaned up at startup.
- The hero remains items have upstream's redrawn art, and the seal shard reads as plural.
- The victory scene's daytime runs from 8:00 to 19:59.
- Fewer allocations every frame: the lost inventory check is cached, and the status bar, arcs, cell selector and game log do less work when nothing changed. The alchemy screen reads the same cached check.
- The catalog records enchantments and glyphs in their own sections and counts how often each item is used. The journal popup shows the count. Cleansing an enchantment or glyph no longer crashes, stones used from the inventory always count, sandals upgrades count and upgrade scrolls count once. Opening a ranking no longer marks that run's enchantments and glyphs as seen.
- The update check skips releases this device or build can't run.
- The display settings have a screen shake slider, from off to twice the default. At the default, shakes are twice as strong as before, which is how upstream has them.
- The new and changed lines take v2.5.4's translations, and where upstream's line is only the English, the English shows.

### 2026-11-12: missed upstream fixes for rooms, the corpse dust quest, traps, bombs, hero abilities and text, plus Belarusian

The third batch from the check of Slice 1 commits against v2.5.4.

**Fixed**
- The wandmaker appears on every prison floor that has his quest room. Since the entrance room variants arrived he only showed up next to the plain entrance room, so about four floors in five had his quest room and no wandmaker.
- The imp asks for monks on floor 17 and golems on floor 19, and picks either on 18, as upstream does. The floors were in a different order before.
- Special entrance stairs have their own tile art in every region. Statue entrance rooms used them already and drew an empty tile.
- Packs that restyle the stairs but have no special stairs art use their own stairs for both.
- A seed now stocks the secret laboratory and the secret library the same way on every run, and a cursing trap, allies following the hero to a new floor, and a dead hero's scattered belongings pick their cells from the seed too.
- Mimics a pack adds to a floor as extra mobs get their stats for that floor, then any hp the pack sets. They used to keep empty stats.
- Packs that still name the old flashbang or shock bomb get the renamed bombs instead of crashing when a hero starts with one or an enemy drops one.
- On a save from before the staff imbue rework, the imbue window promises the old wand back when its preservation is still unused, as it should.
- The Jowiro's Class Reskins pack text is up to date with the current talents and abilities.

**Ported**
- On cave floors the corner fill no longer walls up a corner next to a door, or leaves a visible trap in a hallway.
- Demon spawner rooms can't connect to an exit room, as in v2.5.4.
- Circle basin and hallway entrance rooms use special stairs, and a dewdrop on special stairs is picked up even at full health, as one on the normal stairs is.
- Bridge rooms and cave fissure rooms no longer put items or characters in the gap or on the bridge, so seeded floors with one lay out differently.
- The corpse dust quest raises its own kind of wraith. They spawn at least a third of the hero's view distance away, the summon cost stops growing at 49 turns, and only these wraiths count toward it or die when the curse is lifted. Before, a third wraith could never spawn. Mass graves can't connect to the entrance and need three rooms between them and it.
- Mass grave and ritual site floors use a tile nothing can change, so fire no longer leaves embers on them.
- A pitfall trap's delayed collapse belongs to the floor it was set off on, not just its depth.
- Geysers and aqua brew put out burning targets.
- Trampled high grass drops dew at the intended rate, and grassy floors drop half as much.
- Throwing an aqua brew at your own cell no longer aims it up and to the left.
- Knockback, the wand of blast wave's follow-up and the amulet scene act at effect priority, and a floor change rounds partial turns for every actor, not just characters.
- The ninja log presses the cell it lands on, a cursed wand of warding aims like other cursed wands, and elemental strike reaches 4 tiles before talents. Retribution and psionic blast pick their targets before they deal damage.
- During hourglass stasis the hero looks frozen in place, and ending it clears each sprite effect on its own, so invisibility no longer sticks.
- Conjured explosions deal physical damage.
- The v2.5.3 alchemy bomb rework: each bomb hits everything in its own range, arcane, holy and shrapnel bombs hit harder, and the regrowth bomb reaches 3 tiles. The flashbang becomes the smoke bomb, and the shock bomb becomes the flashbang, which deals electricity damage and paralyses. Old saves keep their bombs under the new names.
- Several splash colours can play at once. Warning text is orange and every damage number is red.
- The imp is easier to see, and his quest opens with its own intro line. His floor tile now sets the tile's flags too.
- A dying demon spawner's splash covers the whole sprite.
- Belarusian is the 19th language, with v2.5.4's translations and a later upstream fix to its berserk rage text. Text only Lutherverse has shows in English for now.
- The new and changed lines take v2.5.4's translations, and where upstream's line is only the English, the English shows.

### 2026-11-12: missed upstream fixes for rooms, combat, mob behaviour, the shop and the desktop launcher

The second batch from the same check of Slice 1 commits against v2.5.4.

**Fixed**
- Magical fire no longer burns items lying on water next to it, and frost or a blizzard next to the fire puts it out.
- Toxic gas rooms no longer get extra random traps inside them.
- Traps rooms can no longer be so large that their chest can't be reached. They are 6 to 8 cells each way.
- When anti-magic on an ally, a prismatic image or an armored statue absorbs a whole magic hit, the hit now lands as 0 damage instead of being skipped. The hero already worked this way.
- Rock armor and earthroot armor now reduce a skeleton's bone explosion. Before, it ignored them.
- Yog-Dzewa no longer turns its minions on each other when a stone of aggression hits one, and minions that changed sides are no longer pulled into its aggro.
- Mimics that spawn mid-run, and mimics a custom level places, now use the scaling depth like other enemies, so they keep up on the ascent.
- On desktop the game always starts in a window and switches to fullscreen afterwards, since going fullscreen at launch could pick the wrong size and monitor. The crash handler spots a failure to create the window again.
- The crash messages on desktop and Android no longer ask players to email upstream's developer. They ask for a report with the error message.
- Sentries no longer turn to face what they shoot at.
- A pack layout's hp for a mimic is kept. It used to be reset when the mimic was scaled to the floor.

**Ported**
- Cave, chasm and ruins rooms stop growing their fill once a room passes 18x18, and only force a path through when they have a door. Cave rooms are at least 5x5.
- Circle basin rooms keep their water as rolled instead of forcing a clear path through it, so seeded sewer floors with one lay out differently.
- A laboratory always drops exactly 5 alchemy energy instead of 4 to 6. A secret laboratory's two piles are 3 to 5 each instead of 4 to 6, so it gives less. The only queued item a laboratory takes as its prize is a potion of strength.
- Secret rooms are chosen the way special rooms are: the front of the queue 60% of the time, the next 30% and the one after 10%.
- Pool, sentry, traps and secret maze rooms strip the curse from their weapon or armor prize instead of rolling a new one.
- DM-300's pylons, the rot heart, demon spawners, Yog-Dzewa and the regrowth lotus share one set of immunities for things that never move, so all of them ignore charm, slowing and freezing.
- The Rogue's shadow clone is inorganic.
- A ripper demon's leap wind-up follows its own attack speed, so it lasts half a turn to one and a half instead of one to three.
- Levitation kills a piranha, even over water, and a flying phantom piranha no longer teleports away.
- Bats, bees, evil eyes, swarms, elementals and the spirit hawk stop flying as they die, so they fall into chasms.
- A stone of aggression lasts its full duration on regular enemies. Bosses and minibosses keep the short duration, and while one carries the aggression its own allies deal it half damage with melee, an evil eye's beam or a warlock's bolt. Yog-Dzewa takes a quarter.
- Enemies pick the target they can reach soonest, by walking distance to a cell next to it, instead of the nearest in a straight line.
- A fleeing enemy that loses its pursuer keeps running, and one that is cornered turns to fight. A cornered thief or crystal mimic out of the hero's sight wanders off instead of getting away. Hunting enemies that lose their target pick a wander target, and sleeping enemies keep the one they had. A mob a pack spawns as fleeing now behaves the same way and stops fleeing once it has nothing to run from.
- A shop steal with the master thieves' armband that could fail now asks first, since a failed steal closes the shop.
- Energizing a potion or scroll identifies it. At the alchemy pot the old name fades into the new one, also when a scroll becomes runestones.
- The crash dialog for a window that can't be created has new wording and shows the error.
- The new and changed lines take v2.5.4's translations, and where upstream's line is only the English, the English shows.

### 2026-11-12: missed upstream fixes for levels, the Warrior, bosses, input and settings

A check of every Slice 1 commit against v2.5.4 found upstream code that earlier batches had missed. This entry covers the most serious of it.

**Fixed**
- Level generation no longer loops forever when a floor's branch rooms can't be placed. After more than 100 failed tries the builder gives up and the floor is built again, as upstream has done since v2.2.0, so a seed that used to need that many tries now lays out differently. The line and branches builders also place their main path rooms now, though no floor in the shipped dungeons uses them.
- The Warrior can pick Endure again as their third armor ability. A leftover of upstream debug code had put Ratmogrify in its place. A save that already picked Ratmogrify keeps it.
- Using Endure again starts a fresh 12-turn buff, and its icon is tinted red.
- The Warrior's seal shield no longer regrows while regeneration is switched off, for example when a boss fight goes on too long without the boss taking damage.
- A huge hit that takes DM-300 to zero health or below while pylons remain no longer leaves it stuck alive. Its health stops at the threshold and the next pylon fires.
- Loading a save in Yog's last phase no longer drops the view distance to zero and blinds the hero.
- Toolbar keys no longer crash when pressed while there is no hero, and a necromancer finishing a summon no longer crashes when its summoning effect never started.
- Rebinding a key no longer lets the action's old default key creep back in as an extra binding, on keyboard or controller.
- The betas checkbox saves the beta channel choice. It used to switch the update check instead.
- A new run can no longer start stuck in the tutorial with the toolbar hidden: floor 1 now removes the intro guide page when it drops the guidebook.

**Ported**
- DM-300's pylon sparks move faster and are denser close to the charging pylon, and the fight no longer logs a warning every time it vents gas or drops rocks.
- Pressing the hero info or journal key again closes that window.
- Players who already have finished runs skip the intro, and on desktop the tutorial lines name the keys for hero info, the inventory and the journal.
- The game options panel always shows the seed, daily and challenge buttons, and before a first win it explains that a win unlocks them. The earliest playable daily moves to January 30, 2024.
- The settings window has a Vibration checkbox that buzzes once when ticked, and long presses only vibrate when it is on. The power saver option only shows when it is already on or the device can't go fullscreen.
- The new lines take v2.5.4's translations wherever upstream has one, and the removed DM-300 and guidebook lines lose theirs.

### 2026-10-26: tier-2 talent buffs, text catch-up and translations

**Ported**
- Wand Preservation no longer rolls a chance and never gives resin, as in upstream v2.5.0. Each time it works, the old wand comes back at +0. At +1 it works once, and at +2 once per hero level.
- Rogue's Foresight notices that a floor has a secret room 75% of the time at +1 and every time at +2.
- Swift Equip's second instant swap at +2 can be used any time in the 20-turn cooldown, instead of only in the first few turns after the first swap.
- About sixty English texts take v2.5.4's wording. Most catch up with code that already worked the way v2.5.4 describes it, and the rest are typo and punctuation fixes. The gladiator and monk descriptions list their abilities, the basic bomb shows its damage, the empower buffs and meal talents count hits instead of attacks, heavy blow says it always hits, the katana blocks 0-3, and a run of buff, well, talent, scroll and guide texts follow upstream.
- All 18 translations take v2.5.4's translated lines wherever our English now matches v2.5.4's: the tier-2 talents, the Champion and the Duelist abilities, the alchemy items, the journal catalog and every text above. A few lines are left out, either because v2.5.4's version drops a placeholder or because upstream left it in English, and those show the English. Translated lines for keys that no longer exist are gone.

**Fixed**
- The loading texts showed four to six dots after "Descending" and the rest, because the scene adds one to three dots of its own to a text that already ended in three. The texts no longer carry dots of their own, and falling reads "Falling!" as upstream has it. Turkish "returning" keeps its ellipsis, because v2.5.4's Turkish line has one.

### 2026-10-26: talent chances, Duelist retunes and artifact charging

**Ported**
- Spells now trigger the scroll talents, the way brews and elixirs already triggered the potion talents (upstream v2.4.0). Items that a recipe makes several of only trigger them some of the time: one use in eight for aqua brew, one in five for a beacon of returning. A whole batch counts about the same as a single potion or scroll. Elixir of might, magical infusion and recycle count double.
- The unstable brew and unstable spell now keep rerolling until the effect suits the moment, as in v2.5.0. Before, they rerolled only once.
- Aggressive Barrier now kicks in at half health or lower and gives 3 shielding, or 5 with both points. Provoked Anger adds a flat 2 or 3 damage.
- The Duelist's charge pool grows every 3 levels instead of every 4.
- Duelist abilities take upstream's v2.4.2 and v2.5.0 numbers. Flail spins, runic slash (now +300% enchantment power), harvest on the sickle and war scythe, lunge, cleave, spike and the greataxe's retribution all hit harder. The whip's lash loses its flat bonus.
- The combo strike, spin and charged shot texts now say "regular attack". The longsword, studded gloves and whip texts match v2.5.4.

**Fixed**
- The cloak of shadows, dried rose, ethereal chains, talisman of foresight, timekeeper's hourglass and unstable spellbook can gain more than one charge in a single turn, and the ethereal chains and horn of plenty more than one from a single recharge. Before, anything past one charge waited for later turns.
- When the talisman of foresight fills up on its own, it now says so and drops its leftover partial charge. Before, only a recharge effect that filled it did that.
- Combo strike no longer writes to the log when no combo weapon is held.
- The Champion's weapon swap tag refreshes whenever the item displays do, so it no longer shows the old weapon.

### 2026-10-26: the Duelist's shared charge pool

**Ported**
- Both of the Duelist's weapons now draw on one charge pool, as in upstream v2.4.0. It starts at 2 charges, grows with the Duelist's level, and refills faster the emptier it is; the Champion gets a bigger pool that refills half again as fast. Every weapon ability costs 1 charge.
- The weapon abilities are retuned to match. Sai's combo strike counts recent hits on a timer, the sickle and war scythe harvest deals its damage as bleed, sword and greataxe kills don't use up the turn, and the whip lashes every enemy in reach without missing.
- Round shield and greatshield guards and runic slash have new numbers, and a charged crossbow shot now hits a 7x7 area and gives tipped darts 3 uses plus the crossbow's level.
- Secondary Charge is replaced by Varied Charge, which gives back part of a charge when an ability is used with a different weapon than the last one. Old saves keep their points.
- The changes, news, rankings, start and supporter screens have icon titles.

**Notes**
- A save from before this change keeps whatever charges it had, even above the new cap, until they're spent. Upstream does the same.

### 2026-10-26: Duelist damage, the Monk rework and the alchemy screen

**Ported**
- Duelist weapon abilities that deal damage now show their damage range, and they add a flat amount of damage instead of multiplying it, as in upstream v2.4.0. That covers heavy blow, spike and lunge, cleave, flail spins, combo strike, harvest and the pickaxe's pierce.
- The Monk works as it does in v2.5.0. Abilities no longer share one cooldown; each decides for itself when it's usable. Flurry has its own cooldown, Focus is a normal buff, Dash reaches 4 tiles (4 more when empowered), Dragon Kick's damage is retuned, and Combined Energy gives back 1 energy when an ability and a weapon ability land within 5 turns of each other. Old saves keep their Monk state.
- Rooted heroes can't use movement abilities (leap, Challenge, Feint, smoke bomb, Heroic Leap), and flying heroes can land on tiles they would normally avoid.
- The alchemy screen uses the space on larger screens: the guide sits beside the bench when there's room, and the scene has a proper title with an icon.

**Fixed**
- The alchemy cancel button stays usable while ingredients are still on the bench, and the split guide refreshes when the bench changes.
- The bundled Jowiro's Class Reskins pack had weapon ability text that no longer matched the new numbers. Its overrides are updated, and a test now checks pack overrides against the English placeholders.

### 2026-10-26: alchemy items and charge caps

**Ported**
- Aqua blast is now aqua brew and feather fall is now an elixir, as in upstream v2.4.0. Old saves convert them. Feather fall now lasts through the landing, and one recipe gives a single elixir.
- The alchemical and arcane catalysts are now the unstable brew and unstable spell. They pick their effect from an ordered table so a seed always rolls the same one, and they reroll once when the first pick doesn't suit the moment.
- Brew, elixir and spell costs and output amounts match v2.5.4. Frost imbue chills for 3 turns, toxic imbue spreads 54 gas over 9 cells and lingers briefly, wild energy now takes a scroll of recharging, and wild energy and reclaim trap have upstream's new art.
- Wands and artifacts drop their leftover partial charge once they're full, and the sandals of nature recharge directly instead of through grass charging.

**Fixed**
- A thrown aqua brew no longer sets off the trap it lands on, so its water can disarm it.

### 2026-10-25: tier-2 talents, monk and guard fixes

**Ported**
- The tier-2 potion and scroll talents now work the way upstream's do: they trigger from any potion or scroll, rare ones (strength, experience, upgrade, transmutation and a few others) count double, and five of them take upstream's names (Inscribed Power, Inscribed Stealth, Liquid Willpower, Liquid Nature, Liquid Agility). Old saves keep their points.
- Empowering Scrolls is replaced by Desperate Power. Exotic potions that skip the normal drink step still trigger the potion talents.
- A rooted monk can't dash, and a dash aimed at an occupied cell is cancelled instead of landing on the enemy.
- Ability warnings now say whether a target is out of range or the cell is occupied, instead of one vague message.
- Swift Equip lets any hero with the talent quickslot a weapon, not just the Duelist.

**Fixed**
- Once a Duelist's guard had blocked a hit, it kept its evasion even while they attacked. Attacking now ends it, as it does upstream.
- The combined force ring description words its weapon bonus as an increase.
- Every talent now has its title and description text, with a test to keep it that way.

### 2026-10-25: Slice 1 loose ends

**Ported**
- The journal catalog now uses upstream's categories, and the identification badges are redesigned around it. The pickaxe is back in the weapon tables without being needed for any badge. The fourth researcher badge stays out of reach until there's a bestiary to count.
- Statues uncurse their weapon instead of re-rolling it, and guardians, transmuted wands and the cursed wand no longer pull cards from the item decks.
- Combined lethality loses its old trigger buff. Burn and ooze end in water once they've done their damage. Dart traps log a death message, and targeted traps stop at view distance and pass over the hero on a tie.
- Two later upstream fixes brought forward early: a missed lunge no longer leaves the ability weapon set, and the enchant and glyph windows clear each other's saved item.

**Fixed**
- Same seed, same run: mobs choose between aggression targets in actor order, seeded and daily runs no longer change their first doors depending on whether you've finished the tutorial, and the stone of disarming breaks distance ties with the game's own random.
- The spectral necromancer can't summon onto a tile that just became impassable.
- Indonesian translations load again on newer Java versions, which map the old "in" code to "id".
- Keybearer, keyblade and warden's sigil text now describes what they actually do, and the sphere grid shows node names instead of code names.
- Fury's description showed a literal "50%%".
- Translations of text that only exists in Lutherverse were stuck on old wording. Those lines now fall back to English until someone translates them.

**Tests and CI**
- content-audit now fails CI on any creature or item that isn't fully wired up.
- A hit on the hero can run in headless tests.
- Core tests went from 545 to 605.

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
