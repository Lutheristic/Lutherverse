package com.qsr.customspd.items.potions;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.items.potions.exotic.PotionOfStormClouds;
import com.qsr.customspd.levels.Terrain;
import com.qsr.customspd.levels.traps.Trap;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Port regression test for upstream SPD bb90b1640 (v2.5.0): "storm clouds now no longer presses
 * cells when thrown".
 *
 * Potion.onThrow hard-presses the landing cell before shattering. A thrown potion of storm clouds
 * therefore set off a trap it landed on, instead of letting the cloud's water disarm the trap
 * (StormCloud.evolve turns trap cells to water). Upstream excludes storm clouds, and aqua brew,
 * from the press. CPDU has no AquaBrew yet (upstream e67d25967), so only the storm clouds term
 * is exercised here.
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

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = prevHero;
	}

	private static final class CountingTrap extends Trap {
		int activations = 0;

		@Override
		public void activate() {
			activations++;
		}
	}

	private CountingTrap armTrapAndGetHeroSetUp(HeadlessLevel level, int cell) {
		prevHero = Dungeon.hero;
		Dungeon.hero = new Hero();
		HeadlessLevel.at(Dungeon.hero, 1 * level.width() + 1);
		CountingTrap trap = new CountingTrap();
		trap.set(cell);
		level.map[cell] = Terrain.TRAP;
		level.traps.put(cell, trap);
		level.buildFlagMaps();
		return trap;
	}

	@Test
	void aThrownPotionOfStormCloudsDoesNotSetOffTheTrapItLandsOn() {
		HeadlessLevel level = HeadlessLevel.install(8, 8);
		int cell = 5 * level.width() + 5;
		CountingTrap trap = armTrapAndGetHeroSetUp(level, cell);

		new PotionOfStormClouds().onThrow(cell);

		assertEquals(0, trap.activations, "storm clouds must not press the cell, so the trap can be disarmed by the water");
	}

	@Test
	void controlAnOrdinaryThrownPotionStillSetsOffTheTrap() {
		HeadlessLevel level = HeadlessLevel.install(8, 8);
		int cell = 5 * level.width() + 5;
		CountingTrap trap = armTrapAndGetHeroSetUp(level, cell);

		new PotionOfToxicGas().onThrow(cell);

		assertEquals(1, trap.activations, "a normally thrown potion presses the cell");
	}
}
