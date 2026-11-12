package com.qsr.customspd.actors.hero;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.Statistics;
import com.qsr.customspd.items.Heap;
import com.qsr.customspd.items.food.Food;
import com.qsr.customspd.test.GameClasses;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StaticSnapshot;
import com.qsr.customspd.test.StubSprite;
import com.watabou.noosa.Game;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * When the hero dies, their belongings scatter onto the free cells around them in a shuffled
 * order. That shuffle draws from Random's generator stack like every other in-run roll, so a
 * pushed seeded generator decides where things land. Upstream uses Collections.shuffle, which
 * has its own generator that no seed reaches; this differs on purpose so seeded runs repeat.
 */
class DeathScatterSeededShuffleTest {

	private static final int W = 9;
	private static final int CENTRE = 4 * W + 4;

	private StaticSnapshot saved;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@BeforeEach
	void setUp() {
		saved = StaticSnapshot.capture(GameClasses.all());
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		saved.restore();
	}

	@Test
	void theSameSeedDropsTheHerosBelongingsInTheSameCell() {
		Set<Integer> cells = new LinkedHashSet<>();
		for (long seed = 1; seed <= 5; seed++) {
			int first = firstDropCell(seed);
			for (int again = 0; again < 2; again++) {
				assertEquals(first, firstDropCell(seed), "seed " + seed + " dropped the hero's food somewhere else on a rerun");
			}
			cells.add(first);
		}
		assertTrue(cells.size() > 1, "every seed dropped the food in the same cell, so the drop may not follow the seed: " + cells);
	}

	/** Kills a hero carrying one ration in the middle of an open room; returns the cell it lands in. */
	private static int firstDropCell(long seed) {
		HeadlessLevel level = HeadlessLevel.install(W, W);
		// a dying hero's level is revealed, which reads what is discoverable; the fixture has no secrets
		level.discoverable = new boolean[level.length()];
		// dropping an item asks for the current scene; a bare Game has none
		new Game(null, Game.platform);
		// daily runs leave no remains, so no bones file is written
		Dungeon.daily = true;
		Dungeon.depth = 1;
		Dungeon.levelName = "1";
		Statistics.deepestFloor = 1;
		Hero hero = new Hero();
		StubSprite.attach(hero);
		Dungeon.hero = HeadlessLevel.at(hero, CENTRE);
		hero.belongings.backpack.items.add(new Food());

		Random.pushGenerator(seed);
		try {
			Hero.reallyDie(new Object());
		} catch (NullPointerException e) {
			// a dropped heap gets its sprite from the scene, so headless the first drop is as
			// far as it goes: reallyDie itself calls drop on the missing sprite. That heap is
			// already on the level
			StackTraceElement[] trace = e.getStackTrace();
			if (trace.length == 0
					|| !trace[0].getClassName().equals(Hero.class.getName())
					|| !trace[0].getMethodName().equals("reallyDie")) {
				throw e;
			}
		} finally {
			Random.popGenerator();
		}

		int dropped = -1;
		for (Heap heap : level.heaps.valueList()) {
			assertEquals(-1, dropped, "only one heap drops before the hero's death needs a scene");
			assertNull(heap.sprite, "the heap the hero dropped was expected to have no sprite headless");
			dropped = heap.pos;
		}
		assertTrue(isNeighbour(dropped), "the food landed at " + dropped + ", not next to the hero");
		return dropped;
	}

	private static boolean isNeighbour(int cell) {
		for (int i : PathFinder.NEIGHBOURS8) {
			if (CENTRE + i == cell) return true;
		}
		return false;
	}
}
