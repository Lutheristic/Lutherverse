package com.qsr.customspd.actors.mobs;

import com.qsr.customspd.items.Generator;
import com.qsr.customspd.items.Gold;
import com.qsr.customspd.items.Item;
import com.qsr.customspd.items.armor.Armor;
import com.qsr.customspd.items.weapon.Weapon;
import com.qsr.customspd.levels.Level;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.test.SaveRoundtrip;
import com.watabou.utils.Random;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream e9e71ca26, e7bd3857e and a86e20c79 (v2.2.0): a statue gets its weapon once, when the
 * level is built, instead of in its constructor, which also ran on every save load and so moved
 * the item decks of a seeded run. Statues and mimics made outside levelgen (distortion traps,
 * the cursed wand) roll from the default probabilities and leave the decks alone.
 */
class StatueDeckDrawsPortTest {

	private static final long RUN_SEED = 4242L;

	@Test
	void loadingAStatueFromASaveLeavesTheDecksAlone() throws IOException {
		try (HeadlessRun run = HeadlessRun.start(RUN_SEED)) {
			int plain = 0, armored = 0;
			for (long seed = 1; seed <= 200 && (plain < 10 || armored < 3); seed++) {
				Statue statue;
				Random.pushGenerator(seed);
				try {
					statue = Statue.random();
				} finally {
					Random.popGenerator();
				}
				if (statue instanceof ArmoredStatue) {
					if (armored >= 3) continue;
					armored++;
				} else {
					if (plain >= 10) continue;
					plain++;
				}

				String before = decks();
				Statue loaded = SaveRoundtrip.of(statue);
				assertEquals(before, decks(), "seed " + seed + ": loading the " + statue.getClass().getSimpleName() + " drew from the decks");
				assertEquals(statue.getClass(), loaded.getClass());
				assertEquals(weaponOf(statue).getClass(), weaponOf(loaded).getClass(), "seed " + seed + ": the saved weapon comes back");
			}
			assertEquals(10, plain, "plain statues checked");
			assertEquals(3, armored, "armored statues checked");
		}
	}

	@Test
	void aLevelgenStatueDrawsItsWeaponFromTheDecks() {
		try (HeadlessRun run = HeadlessRun.start(RUN_SEED)) {
			for (long seed = 1; seed <= 20; seed++) {
				float before;
				Statue statue;
				Random.pushGenerator(seed);
				try {
					Generator.fullReset();
					before = weaponCards();
					statue = Statue.random(true);
				} finally {
					Random.popGenerator();
				}
				assertEquals(1f, before - weaponCards(), "seed " + seed + ": one card from the weapon decks");
				assertTrue(statue.levelGenStatue, "seed " + seed + ": a levelgen statue is a landmark");
				assertFalse(weaponOf(statue).cursed);
			}
		}
	}

	@Test
	void aStatueMadeOutsideLevelgenLeavesTheDecksAlone() {
		try (HeadlessRun run = HeadlessRun.start(RUN_SEED)) {
			for (long seed = 1; seed <= 20; seed++) {
				String before = decks();
				Statue statue;
				Random.pushGenerator(seed);
				try {
					statue = Statue.random(false);
				} finally {
					Random.popGenerator();
				}
				assertEquals(before, decks(), "seed " + seed + ": a distortion trap statue must not draw from the decks");
				assertFalse(statue.levelGenStatue, "seed " + seed + ": not a levelgen statue, so no landmark");
				assertNotNull(weaponOf(statue));
				assertFalse(weaponOf(statue).cursed);
			}
		}
	}

	@Test
	void aMimicMadeOutsideLevelgenLeavesTheDecksAlone() {
		try (HeadlessRun run = HeadlessRun.start(RUN_SEED)) {
			Level floor = run.enterFirstLevel();
			for (long seed = 1; seed <= 30; seed++) {
				String before = decks();
				Mimic mimic, golden;
				Random.pushGenerator(seed);
				try {
					mimic = Mimic.spawnAt(floor.entrance(), false);
					golden = Mimic.spawnAt(floor.entrance(), GoldenMimic.class, false);
				} finally {
					Random.popGenerator();
				}
				assertEquals(before, decks(), "seed " + seed + ": a distortion trap or cursed wand mimic must not draw from the decks");
				assertEquals(1, mimic.items.size(), "seed " + seed + ": the mimic still gets its prize");
				assertTrue(golden instanceof GoldenMimic);
				assertEquals(1, golden.items.size());
			}
		}
	}

	@Test
	void aLevelgenMimicDrawsItsPrizeFromTheDecks() {
		try (HeadlessRun run = HeadlessRun.start(RUN_SEED)) {
			Level floor = run.enterFirstLevel();
			int fromDecks = 0;
			for (long seed = 1; seed <= 30; seed++) {
				String before = decks();
				Mimic mimic;
				Random.pushGenerator(seed);
				try {
					mimic = Mimic.spawnAt(floor.entrance());
				} finally {
					Random.popGenerator();
				}
				Item prize = mimic.items.get(0);
				if (!before.equals(decks())) fromDecks++;
				// gold and armor never come from a deck; missiles, weapons and rings do
				boolean deckCategory = !(prize instanceof Gold) && !(prize instanceof Armor);
				assertEquals(deckCategory, !before.equals(decks()), "seed " + seed + ": " + prize.getClass().getSimpleName());
			}
			assertTrue(fromDecks > 0, "some levelgen mimic prizes came from the decks");
		}
	}

	// ------------------------------------------------------------------------------ helpers

	/** Every deck's cards, seed, draw count and active half. */
	private static String decks() {
		StringBuilder sb = new StringBuilder();
		for (Generator.Category cat : Generator.Category.values()) {
			sb.append(cat).append(Arrays.toString(cat.probs)).append(' ').append(cat.seed)
					.append('/').append(cat.dropped).append('/').append(cat.using2ndProbs).append('\n');
		}
		return sb.toString();
	}

	/** Cards left across the melee weapon tier decks. */
	private static float weaponCards() {
		float sum = 0;
		for (Generator.Category tier : Generator.wepTiers) {
			for (float p : tier.probs) sum += p;
		}
		return sum;
	}

	static Weapon weaponOf(Statue statue) {
		try {
			Field f = Statue.class.getDeclaredField("weapon");
			f.setAccessible(true);
			return (Weapon) f.get(statue);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}
}
