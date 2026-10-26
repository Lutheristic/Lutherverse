package com.qsr.customspd.items.rings;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 650d05bbc (v2.2.0): an equipped ring whose bonus is boosted by a second equipped ring of
 * the same type appends a "your equipped rings are combining their power" paragraph. A lone ring,
 * or one that is not equipped, describes only itself.
 */
class RingCombinedStatsPortTest {

	private static final String COMBINED = "combining their power";

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private static final Class<?>[] RINGS = {
			RingOfAccuracy.class, RingOfArcana.class, RingOfElements.class, RingOfEnergy.class,
			RingOfEvasion.class, RingOfForce.class, RingOfFuror.class, RingOfHaste.class,
			RingOfMight.class, RingOfSharpshooting.class, RingOfTenacity.class, RingOfWealth.class
	};

	private static Hero wear(Ring first, Ring second) {
		HeadlessLevel.install(8, 8);
		Hero hero = new Hero();
		Dungeon.hero = hero;
		hero.pos = 3 * 8 + 2;
		hero.belongings.ring = first;
		first.identify();
		first.activate(hero);
		if (second != null) {
			hero.belongings.misc = second;
			second.identify();
			second.activate(hero);
		}
		return hero;
	}

	@Test
	void everyRingDescribesTheCombinedEffectOfASecondEquippedRingOfItsType() throws Exception {
		for (Class<?> type : RINGS) {
			HeadlessLevel.uninstall();
			Ring a = (Ring) type.getDeclaredConstructor().newInstance();
			Ring b = (Ring) type.getDeclaredConstructor().newInstance();
			wear(a, b);
			assertTrue(a.statsInfo().contains(COMBINED), type.getSimpleName() + " should describe the combined effect");

			HeadlessLevel.uninstall();
			Ring solo = (Ring) type.getDeclaredConstructor().newInstance();
			wear(solo, null);
			assertFalse(solo.statsInfo().contains(COMBINED), type.getSimpleName() + " alone has nothing to combine");
		}
	}

	/** Upstream 120c7dc24 (v2.2.0): the combined force rings word their weapon bonus as the single ring does. */
	@Test
	void combinedForceRingsDescribeTheirWeaponBonusAsADamageIncrease() {
		RingOfForce a = new RingOfForce();
		wear(a, new RingOfForce());
		String info = a.statsInfo();
		assertTrue(info.contains("without a weapon, or increasing damage by _"), info);
	}
}
