package com.qsr.customspd.items.potions.brews;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Actor;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.effects.Pushing;
import com.qsr.customspd.items.Item;
import com.qsr.customspd.levels.Terrain;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import com.watabou.noosa.Camera;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Upstream 0d9746b59 (v2.5.0): an aqua brew thrown onto the thrower's own cell no longer aims
 * its knockback from the thrower to that same cell, which always pointed up and to the left.
 * With no aim the geyser treats the hero as it would on a trap, and picks a direction that
 * does not throw them into a hazard.
 */
class AquaBrewSelfThrowPortTest {

	private static final int W = 9;
	private static final int CENTRE = 4 * W + 4;

	private Hero prevHero;
	private Camera prevCamera;
	private HeadlessLevel level;
	private Hero hero;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@BeforeEach
	void setUp() throws Exception {
		prevHero = Dungeon.hero;
		prevCamera = Camera.main;
		// pushing the hero pans the camera after them
		Camera.main = new Camera(0, 0, 100, 100, 1f);
		level = HeadlessLevel.install(W, W);
		hero = new Hero();
		StubSprite.attach(hero);
		Dungeon.hero = HeadlessLevel.at(hero, CENTRE);
		curUser().set(null, hero);
	}

	@AfterEach
	void tearDown() throws Exception {
		curUser().set(null, null);
		HeadlessLevel.uninstall();
		Dungeon.hero = prevHero;
		Camera.main = prevCamera;
	}

	private static Field curUser() throws Exception {
		Field f = Item.class.getDeclaredField("curUser");
		f.setAccessible(true);
		return f;
	}

	@Test
	void aBrewThrownAtYourOwnFeetPushesYouAwayFromHazards() throws Exception {
		// chasms on every side but the right, so the only safe way out is two cells right
		for (int i : PathFinder.NEIGHBOURS8) {
			if (i != 1) level.map[CENTRE + i] = Terrain.CHASM;
		}
		level.buildFlagMaps();

		Random.pushGenerator(5L);
		try {
			new AquaBrew().shatter(CENTRE);
		} finally {
			Random.popGenerator();
		}

		assertEquals(CENTRE + 2, pushedTo(hero), "a self-thrown brew must not always aim up and to the left");
	}

	private static int pushedTo(Hero hero) throws Exception {
		Field ch = Pushing.class.getDeclaredField("ch");
		Field to = Pushing.class.getDeclaredField("to");
		ch.setAccessible(true);
		to.setAccessible(true);
		Integer cell = null;
		for (Actor a : Actor.all()) {
			if (a instanceof Pushing && ch.get(a) == hero) {
				cell = to.getInt(a);
			}
		}
		assertNotNull(cell, "the geyser should have knocked the hero back");
		return cell;
	}
}
