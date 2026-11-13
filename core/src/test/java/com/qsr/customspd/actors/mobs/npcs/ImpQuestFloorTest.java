package com.qsr.customspd.actors.mobs.npcs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.items.Generator;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.HeadlessRun;
import com.watabou.utils.Random;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The imp's quest follows upstream's floors: monks on 17, golems on 19, and on 18 a
 * Random.Int(2) pick between them (v2.5.4 Imp.Quest.spawn, switch (Dungeon.depth)).
 * CPDU keeps a modulo over the depth so layouts that put the imp elsewhere still get a type.
 */
class ImpQuestFloorTest {

	private static final long[] SEEDS = {1L, 2L, 3L, 5L, 8L, 13L, 21L, 34L, 55L, 89L, 144L, 233L};

	/** What one spawn left behind: the quest type, and every Random.Int(2) it drew. */
	private static final class Spawned {
		final boolean monks;
		final List<Integer> coinFlips;

		Spawned(boolean monks, List<Integer> coinFlips) {
			this.monks = monks;
			this.coinFlips = coinFlips;
		}
	}

	@Test
	void floor17GivesTheMonkQuest() throws Exception {
		try (HeadlessRun run = HeadlessRun.start(1717L)) {
			for (long seed : SEEDS) {
				Spawned s = spawnAt(17, seed);
				assertTrue(s.monks, "seed " + seed + ": floor 17 is the monk quest");
				assertEquals(0, s.coinFlips.size(), "seed " + seed + ": floor 17 draws nothing for the type");
			}
		}
	}

	@Test
	void floor19GivesTheGolemQuest() throws Exception {
		try (HeadlessRun run = HeadlessRun.start(1919L)) {
			for (long seed : SEEDS) {
				Spawned s = spawnAt(19, seed);
				assertTrue(!s.monks, "seed " + seed + ": floor 19 is the golem quest");
				assertEquals(0, s.coinFlips.size(), "seed " + seed + ": floor 19 draws nothing for the type");
			}
		}
	}

	@Test
	void floor18PicksEitherQuestWithRandomInt2() throws Exception {
		try (HeadlessRun run = HeadlessRun.start(1818L)) {
			Set<Boolean> types = new LinkedHashSet<>();
			for (long seed : SEEDS) {
				Spawned s = spawnAt(18, seed);
				assertEquals(1, s.coinFlips.size(), "seed " + seed + ": floor 18 draws one Random.Int(2) for the type");
				assertEquals(s.coinFlips.get(0) == 0, s.monks, "seed " + seed + ": that draw picks the quest (0 is monks)");
				types.add(s.monks);
			}
			assertEquals(2, types.size(), "floor 18 gives both quests across seeds, got only " + types);
		}
	}

	/**
	 * Spawns the quest on a fresh open level at the given depth, with a fresh ring deck, under a
	 * seeded generator that records each Int(2) drawn from it. The ring deck draws from its own
	 * generator, and nothing else in the spawn asks for Int(2).
	 */
	private static Spawned spawnAt(int depth, long seed) throws Exception {
		Imp.Quest.reset();
		Generator.fullReset();
		HeadlessLevel level = HeadlessLevel.install(12, 12);
		Dungeon.depth = depth;
		CoinFlipRecorder recorder = new CoinFlipRecorder(seed);
		generators().push(recorder);
		try {
			Imp.Quest.spawn(level);
		} finally {
			Random.popGenerator();
		}
		return new Spawned(alternative(), recorder.flips);
	}

	private static final class CoinFlipRecorder extends java.util.Random {
		final List<Integer> flips = new ArrayList<>();

		CoinFlipRecorder(long seed) {
			super(seed);
		}

		@Override
		public int nextInt(int bound) {
			int r = super.nextInt(bound);
			if (bound == 2) flips.add(r);
			return r;
		}
	}

	@SuppressWarnings("unchecked")
	private static ArrayDeque<java.util.Random> generators() throws Exception {
		Field f = Random.class.getDeclaredField("generators");
		f.setAccessible(true);
		return (ArrayDeque<java.util.Random>) f.get(null);
	}

	private static boolean alternative() throws Exception {
		Field f = Imp.Quest.class.getDeclaredField("alternative");
		f.setAccessible(true);
		return f.getBoolean(null);
	}
}
