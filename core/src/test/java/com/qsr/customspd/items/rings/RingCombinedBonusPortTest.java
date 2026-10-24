package com.qsr.customspd.items.rings;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.buffs.MagicImmune;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 878e24975 (v2.3.0): ring descriptions sum the equipped rings of their own type from the
 * worn items, not from the live ring buffs. The buff-based sum reads 0 while the hero is magic
 * immune, so a lone worn ring wrongly claimed "your equipped rings are combining their power" with
 * a total of 0.
 */
class RingCombinedBonusPortTest {

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
	void aLoneWornRingDoesNotClaimToCombineWhileTheHeroIsMagicImmune() throws Exception {
		for (Class<?> type : RINGS) {
			HeadlessLevel.uninstall();
			Ring solo = (Ring) type.getDeclaredConstructor().newInstance();
			Hero hero = wear(solo, null);
			Buff.affect(hero, MagicImmune.class);
			assertFalse(solo.statsInfo().contains(COMBINED),
					type.getSimpleName() + " alone has nothing to combine, magic immune or not");
		}
	}

	@Test
	void theCombinedBonusSumsTheWornRingsOfTheSameTypeOnly() {
		Ring a = new RingOfAccuracy();
		Ring b = new RingOfAccuracy();
		Hero hero = wear(a, b);
		assertEquals(a.soloBonus() + b.soloBonus(), a.combinedBonus(hero));
		assertEquals(a.soloBuffedBonus() + b.soloBuffedBonus(), a.combinedBuffedBonus(hero));
		//a ring of another type contributes nothing to this ring's total
		assertEquals(0, new RingOfHaste().combinedBonus(hero));
		assertTrue(a.statsInfo().contains(COMBINED));
	}
}
