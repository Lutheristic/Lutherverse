package com.qsr.customspd.actors.mobs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.hero.abilities.huntress.SpiritHawk;
import com.qsr.customspd.levels.Level;
import com.qsr.customspd.levels.Terrain;
import com.qsr.customspd.sprites.MobSprite;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Port regression for upstream bc4a36890: since flying chars stopped falling into chasms on
 * death, the ones that are flying only by wing or by magic (bats, bees, evil eyes, swarms,
 * elementals and the spirit hawk) drop their flight as they die, so they visibly fall into a
 * chasm when killed over one.
 */
class FlyingDeathFallPortTest {

	private static final int SIZE = 10;
	private static final int CHASM = 5 * SIZE + 5;

	/** A mob sprite that draws nothing and counts its falls. */
	static class FallSprite extends MobSprite {
		int falls = 0;

		@Override
		public void fall() {
			falls++;
		}

		@Override public void die() {}
		@Override public void play(Animation anim) {}
		@Override public void showStatus(int color, String text, Object... args) {}
		@Override public void showStatusWithIcon(int color, String text, int icon, Object... args) {}
		@Override public void update() {}
		@Override public void draw() {}
	}

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@BeforeEach
	void setUp() {
		HeadlessLevel level = HeadlessLevel.install(SIZE, SIZE);
		Level.set(CHASM, Terrain.CHASM, level);
		Hero hero = new Hero();
		hero.lvl = 100; // far past every loot level here, so the deaths drop nothing
		Dungeon.hero = hero;
		HeadlessLevel.at(hero, SIZE + 1);
		StubSprite.attach(hero);
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	/** Kills the mob over the chasm and returns how many times its sprite fell. */
	private static int fallsWhenKilledOverAChasm(Mob mob) {
		HeadlessLevel.at(mob, CHASM);
		FallSprite sprite = new FallSprite();
		sprite.ch = mob;
		mob.sprite = sprite;
		mob.die(null);
		return sprite.falls;
	}

	@Test
	void aBatFallsIntoTheChasm() {
		assertEquals(1, fallsWhenKilledOverAChasm(new Bat()));
	}

	@Test
	void aBeeFallsIntoTheChasm() {
		Bee bee = new Bee();
		bee.spawn(5);
		assertEquals(1, fallsWhenKilledOverAChasm(bee));
	}

	@Test
	void anEvilEyeFallsIntoTheChasm() {
		assertEquals(1, fallsWhenKilledOverAChasm(new Eye()));
	}

	@Test
	void aSwarmFallsIntoTheChasm() {
		assertEquals(1, fallsWhenKilledOverAChasm(new Swarm()));
	}

	@Test
	void anElementalFallsIntoTheChasm() {
		assertEquals(1, fallsWhenKilledOverAChasm(new Elemental.FrostElemental()));
	}

	@Test
	void theSpiritHawkFallsIntoTheChasm() {
		assertEquals(1, fallsWhenKilledOverAChasm(new SpiritHawk.HawkAlly()));
	}

	@Test
	void aWraithStillDoesNotFall() {
		// upstream leaves a wraith flying as it dies, so it never falls
		assertEquals(0, fallsWhenKilledOverAChasm(new Wraith()));
	}
}
