package com.qsr.customspd.items.rings;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 90d14838a + 83643fd2f (v2.5.0): ring of haste is 17.5% per level (was 20%) and ring of
 * energy is 17.5% per level (was 15%). 90d14838a briefly shipped haste's speedMultiplier as 1.75
 * (75%); 83643fd2f corrected it. The haste test below is what pins the correction.
 */
class RingHasteEnergyTuningPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private static Hero wear(Ring ring) {
		HeadlessLevel.install(8, 8);
		Hero hero = new Hero();
		Dungeon.hero = hero;
		hero.pos = 3 * 8 + 2;
		hero.belongings.ring = ring;
		ring.identify();
		ring.activate(hero);
		return hero;
	}

	@Test
	void aPlainRingOfHasteQuickensMovementBySeventeenAndAHalfPercent() {
		Hero hero = wear(new RingOfHaste());
		assertEquals(1.175f, RingOfHaste.speedMultiplier(hero), 0.0001f);
	}

	@Test
	void aPlainRingOfEnergyRechargesSeventeenAndAHalfPercentFaster() {
		Hero hero = wear(new RingOfEnergy());
		assertEquals(1.175f, RingOfEnergy.wandChargeMultiplier(hero), 0.0001f);
		assertEquals(1.175f, RingOfEnergy.armorChargeMultiplier(hero), 0.0001f);
		assertEquals(1.175f, RingOfEnergy.artifactChargeMultiplier(hero), 0.0001f);
	}

	@Test
	void theRingDescriptionsQuoteTheNewTypicalBonus() {
		assertTrue(new RingOfHaste().statsInfo().contains("17.5"), new RingOfHaste().statsInfo());
		assertTrue(new RingOfEnergy().statsInfo().contains("17.5"), new RingOfEnergy().statsInfo());
	}
}
