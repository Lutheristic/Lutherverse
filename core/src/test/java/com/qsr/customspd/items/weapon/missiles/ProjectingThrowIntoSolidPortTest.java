package com.qsr.customspd.items.weapon.missiles;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.items.weapon.enchantments.Projecting;
import com.qsr.customspd.items.weapon.missiles.darts.Dart;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port regression for upstream 3d7b25e7e (v2.3.0, "fixed missile projecting not working on chars
 * in solid terrain"). Projecting lets a missile ignore obstacles and land on its target, but
 * only when the target cell was passable or avoidable, so a char standing inside a wall (a
 * burrowed or phased enemy) could not be hit through the enchantment. A char in the cell now
 * counts as a valid projecting destination.
 */
class ProjectingThrowIntoSolidPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	@Test
	void projectingLandsOnACharInsideSolidTerrain() {
		HeadlessLevel level = HeadlessLevel.install(8, 8);
		int wall = 2 * level.width();            // the room's west wall, x = 0
		int heroCell = 2 * level.width() + 2;    // two cells away
		assertTrue(level.solid[wall], "fixture: the target cell is solid");

		Hero hero = new Hero();
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, heroCell);

		Dart dart = new Dart();
		dart.enchant(new Projecting());

		assertNotEquals(wall, dart.throwPos(hero, wall),
				"fixture: with nobody in the wall the throw is stopped short of it");

		Rat rat = new Rat();
		StubSprite.attach(rat);
		HeadlessLevel.at(rat, wall);

		assertEquals(wall, dart.throwPos(hero, wall),
				"upstream 3d7b25e7e: projecting reaches a char embedded in solid terrain");
	}
}
