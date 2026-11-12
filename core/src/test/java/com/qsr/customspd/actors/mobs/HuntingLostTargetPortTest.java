package com.qsr.customspd.actors.mobs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.levels.Terrain;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Port regression for upstream 649d89bd3 and ab8ca6b6f (v2.3.0): a hunting mob that has no
 * enemy left, or has lost sight of it and cannot reach its target, picks its next destination
 * through its own Wandering state's randomDestination(), not straight from the level. That is
 * what lets FetidRat, GnollTrickster and GreatCrab wander toward the hero: of two random cells
 * they keep the one nearer the hero.
 *
 * The seed is chosen so the second of the two draws is the nearer one; the level's own pick
 * would be the first.
 */
class HuntingLostTargetPortTest {

	private static final long SEED = 20261112L;

	private Hero prevHero;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@BeforeEach
	void saveHero() {
		prevHero = Dungeon.hero;
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = prevHero;
	}

	@Test
	void aHuntingMobWithNoEnemyLeftWandersByItsOwnRules() {
		HeadlessLevel.install(30, 9);
		Hero hero = placeHero(2, 4);
		FetidRat rat = placeMob(new FetidRat(), 20, 4);
		rat.state = rat.HUNTING;

		long seed = seedWhereTheSecondDrawIsNearer(rat, hero);
		int[] draws = draws(seed, rat);

		act(rat, seed);

		assertSame(rat.WANDERING, rat.state, stateName(rat));
		assertEquals(draws[1], rat.target, "the rat must wander toward the hero, not to the level's first random cell " + draws[0]);
	}

	@Test
	void aHuntingMobThatCannotReachItsTargetWandersByItsOwnRules() {
		HeadlessLevel level = HeadlessLevel.install(30, 9);
		// A floor cell walled in on all sides: there is no path to it.
		for (int x = 24; x <= 26; x++) {
			for (int y = 3; y <= 5; y++) {
				if (x != 25 || y != 4) level.map[cell(x, y)] = Terrain.WALL;
			}
		}
		level.buildFlagMaps();
		Hero hero = placeHero(2, 4);
		FetidRat rat = placeMob(new FetidRat(), 20, 4);
		rat.state = rat.HUNTING;
		rat.enemy = hero;
		rat.target = cell(25, 4);

		long seed = seedWhereTheSecondDrawIsNearer(rat, hero);
		int[] draws = draws(seed, rat);

		act(rat, seed);

		assertSame(rat.WANDERING, rat.state, stateName(rat));
		assertEquals(draws[1], rat.target, "the rat must wander toward the hero, not to the level's first random cell " + draws[0]);
	}

	// ------------------------------------------------------------------------------ helpers

	private static Hero placeHero(int x, int y) {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeadlessLevel.at(hero, cell(x, y));
		StubSprite.attach(hero);
		return hero;
	}

	private static <T extends Mob> T placeMob(T mob, int x, int y) {
		HeadlessLevel.at(mob, cell(x, y));
		Dungeon.level.mobs.add(mob);
		StubSprite.attach(mob);
		return mob;
	}

	/** The first two random destinations the level hands this mob under the seed. */
	private static int[] draws(long seed, Mob mob) {
		Random.pushGenerator(seed);
		try {
			return new int[]{Dungeon.level.randomDestination(mob), Dungeon.level.randomDestination(mob)};
		} finally {
			Random.popGenerator();
		}
	}

	private static long seedWhereTheSecondDrawIsNearer(Mob mob, Hero hero) {
		PathFinder.buildDistanceMap(hero.pos, Dungeon.level.passable);
		int[] distance = PathFinder.distance.clone();
		for (long seed = SEED; seed < SEED + 1000; seed++) {
			int[] d = draws(seed, mob);
			if (distance[d[1]] < distance[d[0]]) return seed;
		}
		throw new IllegalStateException("no seed puts the second draw nearer the hero");
	}

	private static void act(Mob mob, long seed) {
		Random.pushGenerator(seed);
		try {
			mob.act();
		} finally {
			Random.popGenerator();
		}
	}

	private static int cell(int x, int y) {
		return y * Dungeon.level.width() + x;
	}

	private static String stateName(Mob mob) {
		return mob.state.getClass().getSimpleName();
	}
}
