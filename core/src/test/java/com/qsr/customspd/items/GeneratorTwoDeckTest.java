package com.qsr.customspd.items;

import com.qsr.customspd.test.HeadlessGdx;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream v2.3.0's two-deck consumable system (27312460f), cpdu-5h0: potions and scrolls deal
 * from two half-size decks that alternate on every reshuffle.
 *
 * What this pins and why:
 *  - Each deck is dealt exactly, and an exhausted deck reshuffles into the OTHER one, so two
 *    cycles together deal defaultProbs + defaultProbs2.
 *  - Non-deck generation (randomUsingDefaults) rolls both decks' cards together (9b7344958).
 *  - Which deck is live is save state ("_using_probs2"): it must round-trip, and a save written
 *    before the key existed must still load, onto the first deck, with its saved probs intact.
 *
 * Statics discipline: as in GeneratorParityTest, Generator state is snapshotted in @BeforeEach and
 * put back in @AfterEach, now including using2ndProbs.
 */
class GeneratorTwoDeckTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	private Map<Generator.Category, float[]> savedProbs;
	private Map<Generator.Category, Long> savedSeeds;
	private Map<Generator.Category, Integer> savedDropped;
	private Map<Generator.Category, Boolean> savedUsing2nd;
	private Bundle savedGeneratorBundle;

	@BeforeEach
	void snapshotGeneratorState() {
		savedProbs = new LinkedHashMap<>();
		savedSeeds = new LinkedHashMap<>();
		savedDropped = new LinkedHashMap<>();
		savedUsing2nd = new LinkedHashMap<>();
		for (Generator.Category cat : Generator.Category.values()) {
			savedProbs.put(cat, cat.probs == null ? null : cat.probs.clone());
			savedSeeds.put(cat, cat.seed);
			savedDropped.put(cat, cat.dropped);
			savedUsing2nd.put(cat, cat.using2ndProbs);
		}
		savedGeneratorBundle = new Bundle();
		Generator.storeInBundle(savedGeneratorBundle);
	}

	@AfterEach
	void restoreGeneratorState() {
		Random.pushGenerator(0xC0FFEEL);
		try {
			Generator.restoreFromBundle(savedGeneratorBundle);
		} finally {
			Random.popGenerator();
		}
		for (Generator.Category cat : Generator.Category.values()) {
			float[] probs = savedProbs.get(cat);
			cat.probs = probs == null ? null : probs.clone();
			cat.seed = savedSeeds.get(cat);
			cat.dropped = savedDropped.get(cat);
			cat.using2ndProbs = savedUsing2nd.get(cat);
		}
	}

	private static int sum(float[] deck) {
		int n = 0;
		for (float p : deck) n += (int) p;
		return n;
	}

	/** Draws n items from the live deck and counts them per class index. */
	private static float[] deal(Generator.Category cat, int n) {
		float[] counts = new float[cat.classes.length];
		for (int d = 0; d < n; d++) {
			Class<?> got = Generator.random(cat).getClass();
			int i = Arrays.asList(cat.classes).indexOf(got);
			assertTrue(i >= 0, cat + " dealt " + got);
			counts[i]++;
		}
		return counts;
	}

	@Test
	void potionsAndScrollsAlternateBetweenTwoDecks() {
		for (Generator.Category cat : new Generator.Category[]{Generator.Category.POTION, Generator.Category.SCROLL}) {
			assertNotNull(cat.defaultProbs2, cat + " must have a second deck");
			for (long seed = 1; seed <= 4; seed++) {
				Random.pushGenerator(seed);
				try {
					Generator.fullReset();
					boolean startedOnSecond = cat.using2ndProbs;
					float[] first = startedOnSecond ? cat.defaultProbs2 : cat.defaultProbs;
					float[] second = startedOnSecond ? cat.defaultProbs : cat.defaultProbs2;
					assertArrayEquals(first, cat.probs, cat + ": fullReset must deal the deck it reports");

					assertArrayEquals(first, deal(cat, sum(first)),
							cat + ": the first cycle must deal its deck exactly (seed " + seed + ")");
					assertArrayEquals(second, deal(cat, sum(second)),
							cat + ": the reshuffle must switch to the other deck (seed " + seed + ")");
					assertEquals(!startedOnSecond, cat.using2ndProbs);
				} finally {
					Random.popGenerator();
				}
			}
		}
	}

	@Test
	void nonDeckGenerationRollsFromBothDecksTogether() {
		// upstream 9b7344958: randomUsingDefaults must roll the whole 30-card table
		// (defaultProbsTotal), not just the first deck, and must not touch the live deck.
		// Frost and liquid flame are 1+2 and 2+1 cards: equal overall, but 1:2 in the first deck.
		Generator.Category potion = Generator.Category.POTION;
		int frost = Arrays.asList(potion.classes).indexOf(com.qsr.customspd.items.potions.PotionOfFrost.class);
		int flame = Arrays.asList(potion.classes).indexOf(com.qsr.customspd.items.potions.PotionOfLiquidFlame.class);
		Random.pushGenerator(0x70741L);
		try {
			Generator.fullReset();
			float[] live = potion.probs.clone();
			float[] counts = new float[potion.classes.length];
			for (int n = 0; n < 3000; n++) {
				counts[Arrays.asList(potion.classes).indexOf(Generator.randomUsingDefaults(potion).getClass())]++;
			}
			assertTrue(Math.abs(counts[frost] - counts[flame]) < 60,
					"frost and liquid flame must roll equally often: " + counts[frost] + " vs " + counts[flame]);
			assertArrayEquals(live, potion.probs, "non-deck generation must not draw from the live deck");
		} finally {
			Random.popGenerator();
		}
	}

	@Test
	void liveDeckChoiceSurvivesASave() {
		Generator.Category potion = Generator.Category.POTION;
		Random.pushGenerator(77L);
		try {
			Generator.fullReset();
			potion.using2ndProbs = true;
			potion.probs = potion.defaultProbs2.clone();
			potion.probs[1]--;
			float[] atSave = potion.probs.clone();

			Bundle saved = new Bundle();
			Generator.storeInBundle(saved);
			assertTrue(saved.contains("potion_using_probs2"));
			potion.using2ndProbs = false;

			Generator.restoreFromBundle(saved);
			assertTrue(potion.using2ndProbs, "the live deck must come back as saved");
			assertArrayEquals(atSave, potion.probs);
		} finally {
			Random.popGenerator();
		}
	}

	@Test
	void saveWrittenBeforeTheSecondDeckLoadsOntoTheFirst() {
		// A save from before this port: no "_using_probs2" keys, and potion/scroll probs from the
		// old single 30-card deck, partially drained. Same length as today, so they are kept.
		float[] oldDeck = { 0, 5, 4, 3, 3, 3, 2, 2, 2, 2, 2, 1 };
		Bundle old = new Bundle();
		Generator.storeInBundle(old);
		old.remove("potion_using_probs2");
		old.remove("scroll_using_probs2");
		old.put("potion_probs", oldDeck);

		Random.pushGenerator(0xBADL);
		try {
			Generator.restoreFromBundle(old);
			Generator.Category potion = Generator.Category.POTION;
			assertFalse(potion.using2ndProbs, "an old save must count as being on the first deck");
			assertFalse(Generator.Category.SCROLL.using2ndProbs);
			assertArrayEquals(oldDeck, potion.probs, "the saved old-deck probs must be kept");

			// it finishes the old deck, then reshuffles into the second deck like any other
			assertArrayEquals(oldDeck, deal(potion, sum(oldDeck)));
			deal(potion, 1);
			assertTrue(potion.using2ndProbs, "after the old deck, the next deck is the second one");
			assertEquals(sum(potion.defaultProbs2) - 1, sum(potion.probs));
		} finally {
			Random.popGenerator();
		}
	}
}
