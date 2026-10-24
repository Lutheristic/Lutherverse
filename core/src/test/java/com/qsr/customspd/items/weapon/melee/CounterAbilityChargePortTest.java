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

/**
 * Port regression for upstream 9067a486c (v2.1.4, "fixed Counter Ability talent incorrectly
 * applying 0 charge at +2"). The talent's discount was applied to the base cost of 1 before the
 * double-cost weapons multiplied it by 2, so at +2 a Dagger ability cost 2 * max(0, 1 - 1) = 0
 * charge. Weapons now only state their base cost (baseChargeUse) and MeleeWeapon applies the
 * discount to that, so the same Dagger costs max(0, 2 - 1) = 1.
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
	void theDiscountIsAppliedToTheDoubledBaseCost() {
		Hero hero = duelist(2, true);
		assertEquals(1f, new Dagger().abilityChargeUse(hero, null), 0f,
				"upstream 9067a486c: 2 - 1, not 2 * (1 - 1)");
		assertEquals(0f, new Sword().abilityChargeUse(hero, null), 0f, "1 - 1, as before");
	}

	@Test
	void theHalfPointDiscountAlsoSubtractsFromTheDoubledCost() {
		Hero hero = duelist(1, true);
		assertEquals(1.5f, new Dagger().abilityChargeUse(hero, null), 0f,
				"upstream 9067a486c: 2 - 0.5, not 2 * (1 - 0.5)");
		assertEquals(0.5f, new Sword().abilityChargeUse(hero, null), 0f);
	}
}
