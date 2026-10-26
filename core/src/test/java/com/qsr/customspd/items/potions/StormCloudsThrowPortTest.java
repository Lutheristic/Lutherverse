package com.qsr.customspd.items.potions;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.items.Item;
import com.qsr.customspd.items.potions.brews.AquaBrew;
import com.qsr.customspd.items.potions.exotic.PotionOfStormClouds;
import com.qsr.customspd.levels.Terrain;
import com.qsr.customspd.levels.traps.Trap;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Port regression test for upstream SPD bb90b1640 (v2.5.0): "storm clouds now no longer presses
 * cells when thrown", and 466ca870d (v2.4.0): "fixed aqua brew triggering traps when thrown".
 *
 * Potion.onThrow hard-presses the landing cell before shattering. A thrown potion of storm clouds
 * therefore set off a trap it landed on, instead of letting the cloud's water disarm the trap
 * (StormCloud.evolve turns trap cells to water). Aqua brew had the same problem. Upstream
 * excludes both from the press.
 *
 * The control case throws a potion that must still press the cell, proving the trap fixture
 * really does fire when pressed.
 */
class StormCloudsThrowPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	private Hero prevHero;
	private Object prevUser;

	@AfterEach
	void tearDown() throws Exception {
		HeadlessLevel.uninstall();
		Dungeon.hero = prevHero;
		curUser().set(null, prevUser);
	}

	private static Field curUser() throws Exception {
		Field f = Item.class.getDeclaredField("curUser");
		f.setAccessible(true);
		return f;
	}

	private static final class CountingTrap extends Trap {
		int activations = 0;

		@Override
		public void activate() {
			activations++;
		}
	}

	private CountingTrap armTrapAndGetHeroSetUp(HeadlessLevel level, int cell) throws Exception {
		prevHero = Dungeon.hero;
		prevUser = curUser().get(null);
		Dungeon.hero = new Hero();
		// a thrown potion triggers the thrower's on-potion talents
		curUser().set(null, Dungeon.hero);
		HeadlessLevel.at(Dungeon.hero, 1 * level.width() + 1);
		CountingTrap trap = new CountingTrap();
		trap.set(cell);
		level.map[cell] = Terrain.TRAP;
		level.traps.put(cell, trap);
		level.buildFlagMaps();
		return trap;
	}

	@Test
	void aThrownPotionOfStormCloudsDoesNotSetOffTheTrapItLandsOn() throws Exception {
		HeadlessLevel level = HeadlessLevel.install(8, 8);
		int cell = 5 * level.width() + 5;
		CountingTrap trap = armTrapAndGetHeroSetUp(level, cell);

		new PotionOfStormClouds().onThrow(cell);

		assertEquals(0, trap.activations, "storm clouds must not press the cell, so the trap can be disarmed by the water");
	}

	@Test
	void aThrownAquaBrewDoesNotSetOffTheTrapItLandsOn() throws Exception {
		HeadlessLevel level = HeadlessLevel.install(8, 8);
		int cell = 5 * level.width() + 5;
		CountingTrap trap = armTrapAndGetHeroSetUp(level, cell);

		new AquaBrew().onThrow(cell);

		assertEquals(0, trap.activations, "aqua brew must not press the cell, so the trap can be disarmed by the water");
	}

	@Test
	void controlAnOrdinaryThrownPotionStillSetsOffTheTrap() throws Exception {
		HeadlessLevel level = HeadlessLevel.install(8, 8);
		int cell = 5 * level.width() + 5;
		CountingTrap trap = armTrapAndGetHeroSetUp(level, cell);

		new PotionOfToxicGas().onThrow(cell);

		assertEquals(1, trap.activations, "a normally thrown potion presses the cell");
	}
}
