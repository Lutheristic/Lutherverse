<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
# Lutherverse theme bible

This is the working theme for everything the project shows the public: README, title art, in-game text, issue templates, release notes. Anything shipped is marked as shipped. The rest is direction. If this document and the code disagree, the code is right and this document gets fixed.

---

## The pitch

Lutherverse is a roguelike about going further down than anyone meant you to.

Shattered Pixel Dungeon's twenty-five floors already tell a containment story. A prison was sealed when a miasma crept up from below. Its warden's last letter tells a guard to lock one cell, throw the key into an alcove and get out. Under the prison, the dwarves spent their city trying to keep an Old God called Yog-Dzewa contained. That god sits at floor 25, and it is the last thing in the dungeon with a name.

Lutherverse keeps all of that and plans 175 more floors underneath it. The first original creatures are what is left of a prison's staff and machinery: a jailer's ghost "still making rounds it no longer remembers", a tax-collector's shade, a carved stone "keeper of doors that no longer exist", and the stray magic of old wards and locks. You play the one carrying the keys. What you learn on the way down (Insight) stays with you through every death.

## Core fantasy

> I am opening something I should not, and I keep getting better at it.

The player should feel two things at once, and both should grow with depth.

- **Competence.** Every run you put together a different strong build: keyblades, a grid of nodes, elements that stack.
- **Complicity.** Every lock you open is one somebody meant to stay shut. Most of the dungeon never punishes you for it. It barely notices you.

The horror comes from scale and indifference, not from cruelty. Two hundred floors is about eight times what Shattered ships. Nobody has been to the bottom, including the people making the game.

## The core, as five rules

1. **The bottom is never shown.** Shattered names its god at floor 25. Past that, nothing gets a name, a portrait or a lore book that explains it.
2. **Containment is failing, quietly.** Wards, gaols, sigils, locks. A ward still glowing over an empty cell is scarier than a broken one. This is inherited canon as much as direction: the dwarves "have been trying to keep it contained ever since" (Yog-Dzewa's description).
3. **Knowledge is permanent.** What you learn (Insight) can't be unlearned. Of everything the Keybearer builds in a run, it's the part that comes back down the stairs with you.
4. **Indifference over malice.** The original residents are doing their jobs. The keywraith walks its rounds and the gaol shade collects. Some of the staff were cruel in life, and the gaol shade still is, out of habit rather than at you. The inherited exception is Yog-Dzewa, which does notice you ("I. SEE. YOU."). That's the point: the one thing that sees you is the last thing with a name.
5. **Repetition is the haunting.** The dead here repeat duties that lost their meaning long ago. A roguelike player dies and walks down the same stairs again. Let the player notice they are making rounds too.

---

## Pillars, and how each hangs off the core

### 1. Build-craft: Keybearer, keyblades, sphere grid, Insight
*Shipped on main as a prototype. The game labels it "Prototype" itself.*

What exists:
- **Keybearer**, a hero class that is always unlocked. It starts with the fire keyblade in hand, the frost keyblade in the off-hand and the storm keyblade in the pack.
- **True dual-wield** for the Keybearer only. The off-hand keyblade also strikes on every hit, at half its damage roll, with its own elemental effect.
- **Three keyblades.** Fire sets targets burning and frost chills and slows, from the first swing. Storm adds shock damage and arcs it into an adjacent enemy once its sphere-grid nodes are active. Before that it is a plain tier-2 weapon.
- **Sphere grid** of 20 nodes, rooted at Attunement. It has Ember, Frost and Storm branches, which end in Conflagration, Permafrost and Tempest, plus Might, Vigor and Ability branches. The keystone, Elemental Conflux, needs Ember II, Frost II and Storm II.
- **Keyblade nova.** One of three armor abilities offered when the Dwarf King's crown upgrades your armor (the other two are borrowed from the Warrior). It hits every enemy within 4 tiles and ignites, chills or shocks them depending on which element the build leans on.
- **Insight.** Each node starts locked. Spending 1 Insight unlocks it for good, and run points activate it each descent. Insight comes from Keybearer level-ups and from shattering an insight crystal.

How it hangs off the core: the Keybearer is the only character whose job is *opening* things, and every original creature is a lock-keeper of some kind. The keyblade's bright, borrowed-from-Kingdom-Hearts energy is deliberate: a cheerful tool in a place built to stay shut. The sphere grid is a chart of locked nodes that you open one at a time, and the openings are permanent.

### 2. The residents: original creatures and items
*Shipped on main, spawning in the default dungeon's Sewers and Prison floors, each with unit tests.*

| name | where | what it does | the line that sets the tone |
|---|---|---|---|
| tumbler wisp | floor 2 | flies, dodges, blinks away when wounded | "A flittering mote of locksmith's magic, escaped from some long-broken mechanism." |
| keywraith | floor 3 | its touch cripples ("locking" your joints) | "still making rounds it no longer remembers" |
| hexward moth | floor 4 | its touch causes vertigo | "A moth grown fat on warding-hex dust." |
| wardstone sentinel | floor 6 | slow, heavily armoured | "a crude keeper of doors that no longer exist" |
| gaol shade | floor 7 | steals gold on every hit; the keywraith's "crueler kin" | "the shade of a gaol tax-collector who never stopped collecting" |
| warden's sigil | runestone | cripples whatever it lands on (half movement speed) and briefly cripples enemies beside it | "a gaol-warden's binding sigil" |
| insight crystal | runestone | +1 Insight, permanently | "Shatter it and the insight lingers" |

How they hang off the core: two of them are dead staff (the jailer and the tax-collector), one is a carved doorkeeper, and two are what leaked out of the wards and locks. They are the prison's staff and equipment, not its prisoners. Shattered's own text already says who the prisoners were (convicts, and Tengu in the warden's letter), so don't pretend that's a mystery. The open question is the one the original text leaves open: the sentinel keeps doors that no longer exist, and the keywraith holds "the last one it ever turned". Neither says what was on the other side. Do not answer what the doors were for.

On the surface the text is folk-gothic: jailers, locksmiths, moths, lockstone. The cosmic horror is in what it implies. Don't go back and rewrite these lines to be more "eldritch". They already work.

### 3. The descent: 200 floors, stretches, labeled seeds, leaderboards
*Direction. The default dungeon ships 25 floors plus the Amulet floor below Yog-Dzewa.*

The public plan: a 200-floor descent in stretches that each feel distinct. Examples are "a hyper-utopian future with something wrong underneath, a version of Zanarkand you weren't supposed to see, calm plains that stop feeling calm". It also has a town every ten floors, one city that recurs for about thirty floors and changes with your choices, and save zones that can be set to heal or not, or switched off entirely for hardcore. A cosmic-horror biome pack is a planned layer, and it depends on the 200-floor generator existing first.

How it hangs off the core:
- **Depth is distance from the last thing anyone explained,** and the last explanation is at floor 25. Each stretch looks finished and is wrong in exactly one way, rather than in general gloom. (Illustration only, not planned content: the calm plains are calm until you count the cattle.)
- **The recurring city remembers you.** It is the one place besides Insight where your choices visibly stick.
- **Save zones are the only places the dungeon lets you stop.** The hardcore toggle takes even that away.
- **Labeled seeds are names you say to call up the same dungeon again.** The public example is `seed COSMICNIGHTMARE-1`. Copy can play with the idea that the dungeon already exists and you are only reading it. Seeded layouts reproduce, but full-run determinism is still being pinned down, so no copy should promise identical runs yet.
- **Leaderboards** (planned) are a list of how far other people got.

### 4. The platform: mod packs and the "-verse"
*The pack framework and the 29 community packs are inherited infrastructure. The broader modding API is direction.*

Custom Pixel Dungeon's JSON-manifest pack system ships in Lutherverse, and all 29 community packs mirrored under `marketplace/` pass the pack smoke test (`marketplace/Summary` is the listing index, not a pack). Each pack belongs to the third-party author credited in its `mod_info.json`. The planned modding API (biomes, mobs, weapons, story branches, cutscenes, dialogue, NPC insertion) is what turns one dungeon into a "-verse": tonally distinct places reachable from the same descent, the way Kingdom Hearts 2 hops between worlds.

How it hangs off the core: packs can replace sprites, tiles, music and even the dungeon layout. So **the theme has to live in names, text and mechanics first, and in pixels second.** A keywraith redrawn by a pack is still a jailer that forgot. Some packs already lean dark. Dark-Shadow's Exotic Dungeon has tentacles "undoubtedly connected to a greater power" and ends its description with "Something is definitely wrong with this dungeon." (Its "dark miasma" is Shattered's own phrase from the prison intro.) Credit packs like that. Never present them as Lutherverse content.

### 5. Coop
*Direction, after v0.1.*

The plan has two modes. Real-time rooms use a nickname and a room, and the lobby decides which addons count as cheats: God Mode does, Hard Mode and Bonfire Mode don't. A turn-based mode inspired by Nestalgia splits a four-unit party across one, two or four players, and you can play it async on a phone.

How it hangs off the core: coop is the warm part of the game. The dungeon takes almost everything, and company is the one comfort it leaves alone. One small, true image for copy: your friend's turn arriving while you were away, like a knock from a few floors down.

### 6. Story spine, cameos, addons
*Direction.*

The plan is a story that runs through all 200 floors, with cutscenes and dialogue. Cameos (the public list is in the README) are non-commercial fan references: fictional characters only, never real living people. The addons go by their public names only: God Mode, Hard Mode, Bonfire Mode.

How it hangs off the core: cameos are visitors who came in through a different door. They don't explain the dungeon, and they may not know where they are either. The cameo template already asks the right question: "Get haunted forever?" Story beats follow rule 1: they get closer to the answer and never reach it. Don't add new franchise names to public copy beyond what the README already lists.

---

## Tone and voice

**In-game text**
- Write like an inventory clerk describing something awful. The shipped creature text is the model: a plain noun phrase, then one quiet wrong detail, then the mechanic, highlighted with SPD's `_underscore_` markup.
- Get your irony from persistence: *no longer*, *never stopped*, *still*. Duty outlives purpose.
- Two sentences of flavour at most, then the mechanic.
- Use concrete objects instead of adjectives: a key, a coin pouch, ward-dust, a graven stone. Never "eldritch", "unspeakable", "nameless", "beyond the veil".
- Flavour stays in the third person. Mechanics can say "you" (shipped text does: "Its touch leaves you dizzied").
- New strings use a colon or full stop where older strings used a dash.
- The mechanic text must match the code. Two shipped strings don't yet (see the status list at the end).

**Public docs (README, CHANGELOG, templates)**
- First-person indie dev. CONTRIBUTING says enthusiasm is on-brand, and it is.
- Write like a person talking, not a press release. No promotional filler.
- The project's honest-engineering habit is part of the theme. Things that look fine while hollow underneath are the dread this project actually lived through: gates printing PASS while checking nothing. Tell that story plainly.
- Unshipped features are always "the plan", never present tense.

**Pixel art stays cute.** The contributor docs promise "cute pixel-art vibes" alongside the horror. The horror lives in the words and the implications. Friendly sprites make the text more unsettling, not less.

| context | do | don't |
|---|---|---|
| creature | "The ghost of a prison jailer, still making rounds it no longer remembers." *(shipped)* | "The Keywraith is an unspeakable horror from beyond the veil, a nightmare of eldritch dread!" |
| item | "Shatter it and the insight lingers." *(shipped)* | "You gain forbidden knowledge that will forever scar your soul." (also implies a cost Insight doesn't have) |
| README pitch | "Two hundred floors is about eight times what Shattered ships. Nobody has been to the bottom, including me." | "Descend into an epic odyssey of unimaginable cosmic terror!" |
| status / engineering | "The project's gates were passing without checking anything." | "Our robust, battle-tested pipeline guarantees quality." |
| unshipped feature | "The plan is a town every ten floors." | "Explore towns every ten floors!" |

---

## Naming conventions

Derived from shipped names.

- **In-game names are lower case and plain English:** a containment or lock word plus an old folk noun. *keywraith, wardstone sentinel, hexward moth, gaol shade, tumbler wisp* (a tumbler is a part of a lock), *warden's sigil*.
- **Word bank from shipped text:** key, ward, warden, gaol, hex, sigil, tumbler, lockstone, rounds, tribute, graven. Extend it with real words from locks, prisons and old bureaucracy (bolt, hasp, turnkey, warrant, ledger, tally, oubliette). Don't coin fantasy syllables.
- **One archaic spelling per name at most,** and only where it adds age (*gaol*, *graven*).
- **Class and ability:** class names are title case in UI prose (Keybearer). Abilities are lower case (keyblade nova).
- **Sphere grid:** a branch name plus a roman numeral for steps (Ember I, Ember II). Capstones are single nouns of escalation (Conflagration, Permafrost, Tempest), and the keystone is two words (Elemental Conflux). The UI currently shows raw enum names such as `EMBER_I`. Display text should read "Ember I".
- **No new named gods, and no Lovecraft names.** Lutherverse adds no gods of its own. Yog-Dzewa is inherited, stays exactly as Shattered wrote it, and is the last thing in the dungeon with a name. No "Eldritch X" or "Void Y".
- **Seeds:** UPPERCASE words, a hyphen and a number, e.g. `COSMICNIGHTMARE-1`.
- **Addons:** public names only (God Mode, Hard Mode, Bonfire Mode).
- **Code:** the class name matches the in-game name in CamelCase (`Keywraith`, `GaolShade`, `WardensSigil`). The inherited `com.qsr.customspd` package stays.

---

## Visual direction

**Palette.** Taken from the title card and the shipped sprites.

| role | hex | source |
|---|---|---|
| void (background gradient) | `#0f0322` → `#241147` → `#3a1a5c` | title card |
| key gold | `#f4c04a`; sprite golds `#d4ac3c`, `#ffe278` | title card keyhole; keywraith's key |
| ward-light | `#a1e5f5`; sprite teal `#8cecdc` | title card border and wordmark edge; keywraith eye-glint |
| spiral lilac | `#d4a5ff` | title card stars and reflection |
| bone cream (text) | `#e8dcc6` | title card wordmark and subtitle |
| dusk grey-violet (quiet text) | `#a790c6` | title card tagline |
| shadow teal / slate | `#385e6e` | keywraith hood |

What the colours mean:
- **Gold is a key**, or anything that locks or opens. Keep it rare so it reads.
- **Teal and cyan are ward-light:** containment that is still working.
- **Violet is the void.** Most of any frame should be violet.

**Sprites.** Original mob, item and icon sprites are 16x16 and generated by `docs/art/make_*.py`. The Keybearer hero sheet and splash are recolours of the Warrior's art, not script output. Design for readability at 16x16 first: one silhouette and one gold glint. Show horror through negative space (the keywraith's dark hood, with two points of light where a face should be), never gore.

**Pack compatibility.** Sprites, tiles and music come from pack config, so:
- never put a mechanic only in colour;
- a creature must still read correctly after a pack redraws it;
- the palette is Lutherverse's default for its own art, not a rule for packs.

**Title card.** `docs/assets/lutherverse-title.svg` is concept art, not final hero art, and says so in the image. A square stair winds inward toward a small gold keyhole. Its first arm is lit, standing for floors 001 to 025 (the dungeon inherited from Shattered). The rest of the way to floor 200 is dark. A dotted ward-light border runs round the edge and fails in one corner. All lettering is drawn from rectangles, so it renders the same on every machine. Direction for the final hero art:
- the spiral as the descent, with floor numbers as a quiet typographic device;
- a single small key or keyhole as the only warm element;
- cold ward-light at the edges;
- a lot of empty dark for scale.

Keep the file path. Never put unshipped features on it as a tag list. Keep the "concept art" marker until the art is final. Under CONTRIBUTING, art contributors get in touch first.

## Audio direction

*Entirely direction. No original audio ships, and music comes from pack config.*

- **Mechanical, close sounds over orchestral swells:** lock tumblers, a chain settling, air moving in a shaft.
- **Music thins out as you go deeper** instead of building. Long silences make deep floors feel big.
- The public reference is Bloodborne's gothic ambient horror.
- Towns, save zones and coop lobbies get the warmth.
- Everything has to stay swappable by packs.

---

## Cosmic horror in the mechanics

Read the shipped mechanics. Don't change them in copy.

| mechanic (shipped) | what it actually does | how to read it |
|---|---|---|
| Insight | persistent across runs; 1 Insight permanently unlocks a node | knowledge you can't give back; nothing in the game takes it away |
| insight crystal | +1 Insight for whoever shatters it ("whoever finds it") | understanding lying on the floor, indifferent to who picks it up |
| locked sphere nodes | locked until Insight opens them | your build is a set of locks you open yourself |
| keywraith's touch | cripples ("a phantom key in your joints") | the prison still works, and now it works on you |
| hexward moth's touch | vertigo | wards meant for something else, landing on you |
| gaol shade | steals gold on every hit | the descent charges you for passage |
| warden's sigil | cripples the target and, briefly, its neighbours | you pick up the jailers' tools |
| dying and starting again | the roguelike loop | you are making rounds too |
| Yog-Dzewa's spawners *(inherited)* | something left alive on the floors above feeds the god power | the dungeon is one system, and the floors you think you finished aren't finished |
| the warden's letter *(inherited)* | Kiana tells Thomas to lock Tengu's cell, throw the key into an alcove and get out | the keys the Keybearer carries are the kind the last staff threw away |

Guardrails:
- **Insight has no downside today.** Don't write copy that implies madness, cost or corruption. Any future sanity or cost mechanic is an open design decision and must not be described as existing.
- Only the Keybearer can spend Insight. Other classes can shatter crystals, and the Insight is banked for a future Keybearer run.
- Warden's sigils and insight crystals are uncommon runestones. Don't call them common or guaranteed.
- Ideas worth deciding later, all unshipped: deep stretches that reveal more to players with more Insight; wards you can choose to leave closed; save zones that are rarer the deeper you go.
- **Never** use jump scares, gore, or "sanity" effects that take your turns away. Shattered already has paralysis and vertigo; new content shouldn't add more control loss for flavour.

---

## What Lutherverse is not

- A gore game or a jump-scare game.
- A Lovecraft name-drop. No Cthulhu, no new named gods, no tentacles as wallpaper. (The README's public direction does mention "deep-ones, mythos creatures". How literally to take that is an open call, below.)
- Grim all the way down. The cute pixel art, the enthusiasm and the cameos stay.
- Affiliated with, endorsed by, or connected to any franchise it references. Cameos are non-commercial fan references.
- A commercial product. It's a GPL-3.0 fan project, forked from Custom Pixel Dungeon (QuasiStellar), which forked Shattered Pixel Dungeon (Evan Debenham), which came from Pixel Dungeon (Watabou).
- The owner of the community packs it mirrors.
- A lore dump. No codex ever explains the bottom.
- Finished. It's pre-alpha with no release.

---

## Status of the theme

**Shipped on main (original):**
- Keybearer prototype, with three keyblades and true dual-wield.
- 20-node sphere grid (shown as a plain list for now), keyblade nova, persistent Insight.
- insight crystal and warden's sigil.
- Five creatures on the default Sewers and Prison floors: tumbler wisp, keywraith, hexward moth, wardstone sentinel, gaol shade. The README names them.
- The concept title card with the stair, the keyhole and the violet/gold palette.

**Shipped on main (inherited):**
- Shattered's 25-floor dungeon and the Amulet floor, including Yog-Dzewa and the prison's journal and letter.
- Custom Pixel Dungeon's pack framework and 29 community packs by their credited authors.

**Direction only:**
- 200 floors and the tonal stretches; towns and the recurring city; save zones.
- The turn-based toggle; coop; story and cutscenes; cameos.
- Labeled seed sharing; leaderboards; the cosmic-horror biome pack.
- God Mode, Hard Mode and Bonfire Mode; the broad modding API.
- The sphere grid's web visual; final hero art; all original audio.

**Surfaces that don't match this theme yet** (each is a small separate fix):
- The warden's sigil item text says the target is "locked in place". The code applies Cripple, which halves movement speed. Either the text or the mechanic (to Roots) should change.
- The storm keyblade item text promises arcing shock unconditionally. The arc only fires once Storm nodes are active.
- The Keybearer's in-game text uses engineering jargon ("build-craft spine"), and its long description says it starts with a basic weapon, which no longer matches the kit.
- The sphere grid window shows raw enum names (`EMBER_I`).
- The issue templates list Dark Souls as a tone reference, which the README leaves out.
- CONTRIBUTING uses promotional phrasing ("cult-classic") and still calls the repo `CustomPixelDungeonUltimate`.
- THIRD_PARTY_NOTICES.md still uses the old name in places.

**Open calls for the maintainer:**
- whether Insight ever gets a cost;
- what the sphere grid's final visual looks like;
- how gameplay scales past floor 25;
- keyblade/Aeon trademark: rename or keep, now that "keyblade" is an in-game noun;
- how far to take the README's "deep-ones, mythos creatures" direction, given the no-Lovecraft-names rule;
- whether the binding promises (the bottom is never shown, no codex ever explains it, the story never reaches the answer) are ones to make publicly in the first person.

