package com.qsr.customspd.items.wands;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.buffs.ScrollEmpower;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.hero.HeroClass;
import com.qsr.customspd.actors.hero.Talent;
import com.qsr.customspd.actors.hero.abilities.mage.WildMagic;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Upstream 935b831af (v2.2.0): desperate power gives the mage's wands +1/+2/+3 levels while they
 * are down to their last charge, and the scroll empower bonus moves ahead of wild magic in
 * Wand.buffedLvl, so it now counts toward wild magic's level cap instead of stacking past it.
 */
class DesperatePowerWandPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private static Hero mage() {
		HeadlessLevel level = HeadlessLevel.install(8, 8);
		Hero hero = new Hero();
		hero.heroClass = HeroClass.MAGE;
		Talent.initClassTalents(hero);
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, 3 * level.width() + 3);
		return hero;
	}

	@Test
	void desperatePowerBoostsAWandOnItsLastCharge() {
		Hero hero = mage();
		hero.talents.get(2).put(Talent.DESPERATE_POWER, 2);
		WandOfMagicMissile wand = new WandOfMagicMissile();
		wand.charge(hero);

		wand.curCharges = 2;
		assertEquals(0, wand.buffedLvl(), "no boost while charges remain");

		wand.curCharges = 1;
		assertEquals(2, wand.buffedLvl(), "+2 levels at +2 on the last charge");
	}

	@Test
	void scrollEmpowerCountsTowardWildMagicsCap() {
		Hero hero = mage();
		WandOfMagicMissile wand = new WandOfMagicMissile();
		wand.charge(hero);
		Buff.affect(hero, ScrollEmpower.class);
		Buff.affect(hero, WildMagic.WildMagicTracker.class, 5f);

		// +2 from scroll empower first, then wild magic's +2 stops at its cap of 3
		assertEquals(3, wand.buffedLvl());
	}
}
