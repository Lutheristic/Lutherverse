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

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port regression for Mob.chooseEnemy. Upstream a49215ccc (v2.1.1), as refined by 74eb63739
 * (v2.5.0): a mob ranks the enemies it can see by path distance to a cell next to each one, not
 * by straight-line distance, so it goes for a target it can reach over one it can only see.
 * Upstream 49af7826e (v2.5.0): a sleeping mob keeps its current enemy.
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

	// From upstream 49af7826e: sleeping no longer counts as a reason to pick a new enemy. Waking
	// up already weighs every hostile in view, so a sleeper keeps the enemy it has.
	@Test
	void aSleepingMobKeepsTheEnemyItAlreadyHas() {
		HeadlessLevel.install(30, 9);
		Hero hero = placeHero(4, 4);
		Rat rat = placeMob(new Rat(), 3, 4);
		Rat ally = placeMob(new Rat(), 20, 4);
		ally.alignment = Char.Alignment.ALLY;
		ally.state = ally.PASSIVE;
		rat.state = rat.SLEEPING;
		rat.enemy = ally;
		rat.fieldOfView = new boolean[Dungeon.level.length()];
		Dungeon.level.updateFieldOfView(rat, rat.fieldOfView);
		assertTrue(rat.fieldOfView[hero.pos] && !rat.fieldOfView[ally.pos],
				"precondition: the hero is in view and the ally is not");

		assertSame(ally, rat.chooseEnemy(), "a sleeping rat must not swap its enemy for the hero next to it");
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
