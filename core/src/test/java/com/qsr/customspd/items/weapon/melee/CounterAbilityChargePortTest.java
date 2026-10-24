package com.qsr.customspd.items.weapon.melee;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.hero.HeroClass;
import com.qsr.customspd.actors.hero.Talent;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Counter Ability history, as upstream shipped it:
 *  - 9067a486c (v2.1.4) fixed the talent's per-ability discount being applied before double-cost
 *    weapons multiplied it (a +2 Dagger ability cost 0), by moving the discount onto the doubled
 *    base cost via baseChargeUse().
 *  - 20cd08c32 (v2.4.0, "adjusted counter ability talent now that all abilities cost 1")
 *    replaced the discount entirely: abilityChargeUse() is just baseChargeUse(), and using an
 *    ability within 3 turns of the afterimage being attacked refunds charge instead.
 * Weapons keep stating only their base cost, which is what the first two tests pin; the rest pin
 * the refund.
 */
class CounterAbilityChargePortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private static Hero duelist(int counterAbilityPoints, boolean primed) {
		HeadlessLevel level = HeadlessLevel.install(10, 10);
		Hero hero = new Hero();
		hero.heroClass = HeroClass.DUELIST;
		Talent.initClassTalents(hero);
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, 4 * level.width() + 4);
		// COUNTER_ABILITY is a tier-4 talent; talents.get(3) is the tier-4 map once initialised.
		hero.talents.get(3).put(Talent.COUNTER_ABILITY, counterAbilityPoints);
		if (primed) {
			Buff.affect(hero, Talent.CounterAbilityTacker.class);
		}
		return hero;
	}

	@Test
	void withoutTheTalentBuffAWeaponCostsItsBase() {
		Hero hero = duelist(2, false);
		assertEquals(2f, new Dagger().abilityChargeUse(hero, null), 0f);
		assertEquals(1f, new Sword().abilityChargeUse(hero, null), 0f);
	}

	@Test
	void theTalentNoLongerDiscountsTheCost() {
		Hero hero = duelist(4, true);
		assertEquals(2f, new Dagger().abilityChargeUse(hero, null), 0f,
				"upstream 20cd08c32: the cost is the weapon's base cost, primed or not");
		assertEquals(1f, new Sword().abilityChargeUse(hero, null), 0f);
	}

	/** Charges after using a weapon ability, starting from 1 full charge and nothing partial. */
	private static float chargeAfterAbility(int points, boolean primed) {
		Hero hero = duelist(points, primed);
		MeleeWeapon.Charger charger = Buff.affect(hero, MeleeWeapon.Charger.class);
		charger.charges = 1;
		charger.partialCharge = 0f;
		new Sword().afterAbilityUsed(hero);
		assertNull(hero.buff(Talent.CounterAbilityTacker.class), "using an ability consumes the window");
		return charger.charges + charger.partialCharge;
	}

	@Test
	void usingAnAbilityInsideTheWindowRefundsOnlyAtFourPoints() {
		assertEquals(1f, chargeAfterAbility(3, true), 0f, "upstream 20cd08c32: nothing below +4");
		assertEquals(1f + 4f / 3f, chargeAfterAbility(4, true), 0.0001f, "+4: 1.33 charges");
	}

	@Test
	void usingAnAbilityOutsideTheWindowRefundsNothing() {
		assertEquals(1f, chargeAfterAbility(4, false), 0f);
	}
}
