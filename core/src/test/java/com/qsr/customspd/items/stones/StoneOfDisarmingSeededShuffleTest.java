package com.qsr.customspd.items.stones;

import com.qsr.customspd.levels.Terrain;
import com.qsr.customspd.levels.traps.Trap;
import com.qsr.customspd.levels.traps.WornDartTrap;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.watabou.utils.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * StoneOfDisarming disarms the nearest traps first and breaks ties at equal distance with a
 * shuffle. That shuffle draws from Random's generator stack like every other in-run roll, so a
 * pushed seeded generator decides which of the tied traps goes first.
 */
class StoneOfDisarmingSeededShuffleTest {

	/** The twelve cells exactly 5 from the centre: one distance, so only the shuffle orders them. */
	private static final int[][] RING = {
			{5, 0}, {-5, 0}, {0, 5}, {0, -5},
			{3, 4}, {3, -4}, {-3, 4}, {-3, -4},
			{4, 3}, {4, -3}, {-4, 3}, {-4, -3}};

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
	}

	@Test
	void theSameSeedDisarmsTheSameTrapFirst() {
		Set<Integer> picks = new LinkedHashSet<>();
		for (long seed = 1; seed <= 3; seed++) {
			int first = firstDisarmed(seed);
			for (int again = 0; again < 2; again++) {
				assertEquals(first, firstDisarmed(seed), "seed " + seed + " disarmed a different trap first on a rerun");
			}
			picks.add(first);
		}
		assertTrue(picks.size() > 1, "every seed disarmed the same trap first, so the pick may not follow the seed: " + picks);
	}

	/** A fresh room with the ring of traps; throws the stone at its centre and returns the first trap disarmed. */
	private static int firstDisarmed(long seed) {
		HeadlessLevel level = HeadlessLevel.install(15, 15);
		int centre = 7 * level.width() + 7;
		for (int[] d : RING) {
			int cell = centre + d[0] + d[1] * level.width();
			level.map[cell] = Terrain.TRAP;
			level.setTrap(new WornDartTrap(), cell);
		}
		level.buildFlagMaps();

		Random.pushGenerator(seed);
		try {
			new StoneOfDisarming().activate(centre);
		} catch (NullPointerException e) {
			// Each disarm puffs steam through CellEmitter.get, which needs a live GameScene, so
			// headless only the first trap in the stone's order gets disarmed. That is enough.
			if (!e.getStackTrace()[0].getClassName().endsWith(".CellEmitter")) {
				throw e;
			}
		} finally {
			Random.popGenerator();
		}

		int disarmed = -1;
		for (Trap t : level.traps.valueList()) {
			if (!t.active) {
				assertEquals(-1, disarmed, "only one trap disarms before the stone needs a scene");
				disarmed = t.pos;
			}
		}
		assertTrue(disarmed != -1, "the stone disarmed nothing");
		return disarmed;
	}
}
