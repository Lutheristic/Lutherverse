package com.qsr.customspd.items;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.buffs.Roots;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.items.scrolls.ScrollOfTeleportation;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import com.watabou.noosa.Group;
import com.watabou.utils.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port regression test for upstream SPD a60a585b1 (v2.1.2): "teleportation effects now cleanse the
 * rooted debuff".
 *
 * A teleported character kept its Roots buff (and {@code rooted} flag), so it arrived at the new
 * cell still unable to move. Every teleport path (teleportToLocation, teleportChar,
 * teleportPreferringUnseen, teleportInNonRegularLevel) now detaches Roots after occupying the
 * destination.
 *
 * The HeadlessLevel fixture is not a RegularLevel, so teleportChar routes through
 * teleportInNonRegularLevel; teleportToLocation is the other directly reachable entry point.
 * Both are exercised here. (teleportPreferringUnseen needs a generated RegularLevel and shares
 * the same one-line change.)
 */
class TeleportClearsRootsPortTest {

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

	private Rat rootedRatAt(HeadlessLevel level, int cell) {
		Rat rat = HeadlessLevel.at(new Rat(), cell);
		StubSprite sprite = StubSprite.attach(rat);
		sprite.parent = new Group(); // appear() fades the sprite in via a tweener added to its parent
		Buff.affect(rat, Roots.class, Roots.DURATION);
		assertTrue(rat.rooted, "precondition: the rat is rooted");
		assertNotNull(rat.buff(Roots.class), "precondition: the rat carries Roots");
		return rat;
	}

	@Test
	void teleportToLocationCleansesRoots() {
		prevHero = Dungeon.hero;
		HeadlessLevel level = HeadlessLevel.install(8, 8);
		Dungeon.hero = new Hero();
		HeadlessLevel.at(Dungeon.hero, 1 * level.width() + 1);
		Rat rat = rootedRatAt(level, 3 * level.width() + 3);
		int dest = 5 * level.width() + 5;

		assertTrue(ScrollOfTeleportation.teleportToLocation(rat, dest));

		assertEquals(dest, rat.pos);
		assertNull(rat.buff(Roots.class), "teleporting must cleanse Roots");
		assertFalse(rat.rooted, "teleporting must clear the rooted flag");
	}

	@Test
	void teleportCharCleansesRoots() {
		prevHero = Dungeon.hero;
		HeadlessLevel level = HeadlessLevel.install(8, 8);
		Dungeon.hero = new Hero();
		HeadlessLevel.at(Dungeon.hero, 1 * level.width() + 1);
		int start = 3 * level.width() + 3;
		Rat rat = rootedRatAt(level, start);

		Random.pushGenerator(20261023L);
		try {
			assertTrue(ScrollOfTeleportation.teleportChar(rat));
		} finally {
			Random.popGenerator();
		}

		assertTrue(rat.pos != start, "the rat must actually have moved");
		assertNull(rat.buff(Roots.class), "teleporting must cleanse Roots");
		assertFalse(rat.rooted, "teleporting must clear the rooted flag");
	}
}
