package com.qsr.customspd.actors.mobs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Char;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.levels.Terrain;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import com.watabou.utils.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port regression for Mob.chooseEnemy's target ranking, upstream a49215ccc (v2.1.1) as refined
 * by 74eb63739 (v2.5.0): a mob ranks the enemies it can see by path distance to a cell next to
 * each one, not by straight-line distance, so it goes for a target it can reach over one it
 * can only see.
 */
class MobTargetChoicePortTest {

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
	void aMobGoesForTheAllyItCanReachOverTheHeroBehindAWall() {
		HeadlessLevel level = HeadlessLevel.install(16, 10);
		// A wall of statues down column 6, open only at the bottom row. Statues block movement
		// but not sight, so the rat sees the hero two cells away and has to walk around to him.
		for (int y = 1; y <= 7; y++) {
			level.map[cell(6, y)] = Terrain.STATUE;
		}
		level.buildFlagMaps();
		Hero hero = placeHero(7, 4);
		Rat rat = placeMob(new Rat(), 5, 4);
		Rat ally = placeMob(new Rat(), 1, 4);
		ally.alignment = Char.Alignment.ALLY;
		ally.state = ally.PASSIVE;
		rat.state = rat.HUNTING;

		act(rat);

		assertTrue(rat.isTargeting(ally), "the ally four cells away in the open is nearer by path than the hero behind the statues");
		assertTrue(!rat.isTargeting(hero));
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

	private static void act(Mob mob) {
		Random.pushGenerator(SEED);
		try {
			mob.act();
		} finally {
			Random.popGenerator();
		}
	}

	private static int cell(int x, int y) {
		return y * Dungeon.level.width() + x;
	}
}
