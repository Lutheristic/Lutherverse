package com.qsr.customspd.actors.mobs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Char;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Allies that follow the hero to a new floor are placed on the free cells around the hero in a
 * shuffled order, unless the arrival names a cell for them to gather near. That shuffle draws
 * from Random's generator stack like every other in-run roll, so a pushed seeded generator
 * decides where an ally appears. Upstream uses Collections.shuffle, which has its own generator
 * that no seed reaches; this differs on purpose so seeded runs repeat.
 */
class RestoreAlliesSeededShuffleTest {

	private static final int W = 9;
	private static final int CENTRE = 4 * W + 4;

	private Hero prevHero;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@BeforeEach
	void setUp() {
		prevHero = Dungeon.hero;
	}

	@AfterEach
	void tearDown() {
		Mob.clearHeldAllies();
		HeadlessLevel.uninstall();
		Dungeon.hero = prevHero;
	}

	@Test
	void theSameSeedPlacesAnAllyOnTheSameCell() {
		Set<Integer> cells = new LinkedHashSet<>();
		for (long seed = 1; seed <= 5; seed++) {
			int first = allyCell(seed);
			for (int again = 0; again < 2; again++) {
				assertEquals(first, allyCell(seed), "seed " + seed + " placed the ally somewhere else on a rerun");
			}
			cells.add(first);
		}
		assertTrue(cells.size() > 1, "every seed placed the ally on the same cell, so the placement may not follow the seed: " + cells);
	}

	/** Holds an ally next to the hero, then restores it around the room's centre; returns its cell. */
	private static int allyCell(long seed) {
		HeadlessLevel level = HeadlessLevel.install(W, W);
		Dungeon.hero = HeadlessLevel.at(new Hero(), W + 1);
		Rat ally = new Rat();
		ally.alignment = Char.Alignment.ALLY;
		ally.intelligentAlly = true;
		ally.pos = CENTRE + 1;
		level.mobs.add(ally);
		Mob.holdAllies(level, CENTRE);
		assertTrue(level.mobs.isEmpty(), "fixture: the ally is held for the trip");

		Random.pushGenerator(seed);
		try {
			Mob.restoreAllies(level, CENTRE);
		} finally {
			Random.popGenerator();
		}

		assertTrue(level.mobs.contains(ally), "the ally is back on the level");
		boolean beside = false;
		for (int i : PathFinder.NEIGHBOURS8) {
			if (CENTRE + i == ally.pos) beside = true;
		}
		assertTrue(beside, "the ally appears next to the arrival cell, not at " + ally.pos);
		return ally.pos;
	}
}
