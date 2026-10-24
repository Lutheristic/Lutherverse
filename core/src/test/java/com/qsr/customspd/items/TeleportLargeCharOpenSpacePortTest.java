package com.qsr.customspd.items;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Char;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.items.scrolls.ScrollOfTeleportation;
import com.qsr.customspd.levels.Terrain;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import com.watabou.noosa.Group;
import com.watabou.utils.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port regression test for upstream SPD f542c7438 (v2.4.0): "fixed tele not properly checking
 * openSpace on boss floors".
 *
 * On non-regular levels (boss floors) teleportInNonRegularLevel floods outward from the char over
 * passable cells. For a LARGE char the passable set is meant to be narrowed to open space, but the
 * code OR-ed passable with openSpace (a no-op) instead of AND-ing them, so a large char could be
 * teleported into a one-wide corridor it cannot stand in.
 *
 * The fixture is an 8x8 room whose top row is a one-wide corridor (walled below, joined to the
 * open area by a single gap). HeadlessLevel is not a RegularLevel, so teleportChar takes the
 * non-regular path. Many seeds are tried so the corridor cells would otherwise be picked.
 */
class TeleportLargeCharOpenSpacePortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	private Hero prevHero;

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = prevHero;
	}

	/** A rat that counts as LARGE. */
	private static final class BigRat extends Rat {
		{
			properties.add(Char.Property.LARGE);
		}
	}

	private static HeadlessLevel corridorLevel() {
		HeadlessLevel level = HeadlessLevel.install(8, 8);
		int w = level.width();
		// wall off row 2 except a one-cell gap at x=1: row 1 becomes a one-wide corridor
		for (int x = 1; x < w - 1; x++) {
			if (x != 1) level.map[2 * w + x] = Terrain.WALL;
		}
		level.buildFlagMaps();
		return level;
	}

	private BigRat bigRatAt(int cell) {
		BigRat rat = HeadlessLevel.at(new BigRat(), cell);
		StubSprite sprite = StubSprite.attach(rat);
		sprite.parent = new Group(); // appear() fades the sprite in via a tweener added to its parent
		return rat;
	}

	@Test
	void aLargeCharIsNeverTeleportedOutOfOpenSpace() {
		prevHero = Dungeon.hero;
		HeadlessLevel level = corridorLevel();
		Dungeon.hero = new Hero();
		HeadlessLevel.at(Dungeon.hero, 6 * level.width() + 6);
		int w = level.width();
		int start = 4 * w + 4;
		assertTrue(level.openSpace[start], "fixture: the start cell is open space");
		assertFalse(level.openSpace[1 * w + 3], "fixture: the corridor is not open space");
		BigRat rat = bigRatAt(start);

		for (long seed = 1; seed <= 40; seed++) {
			Random.pushGenerator(seed);
			try {
				assertTrue(ScrollOfTeleportation.teleportChar(rat));
			} finally {
				Random.popGenerator();
			}
			assertTrue(level.openSpace[rat.pos],
					"seed " + seed + ": a large char landed on non-open cell " + rat.pos);
		}
	}
}
