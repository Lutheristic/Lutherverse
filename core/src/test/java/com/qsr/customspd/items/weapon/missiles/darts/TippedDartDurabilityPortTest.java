package com.qsr.customspd.items.weapon.missiles.darts;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.hero.HeroClass;
import com.qsr.customspd.actors.hero.Talent;
import com.qsr.customspd.items.weapon.melee.Crossbow;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Port regression for upstream f9e6fbc94 (v2.3.0, "fixed rounding errors in tipped dart
 * durability"). MissileWeapon rounded the base use count to a whole number before the tipped
 * dart's talent / lotus / charged-shot modifiers were applied, so the modifiers acted on an
 * already-rounded number. TippedDart now asks for the unrounded figure, applies every modifier,
 * and only then rounds to a whole number of uses (charged shot adds 4 on top of that).
 */
class TippedDartDurabilityPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private static Hero huntress(int durableProjectiles, int durableTips) {
		HeadlessLevel level = HeadlessLevel.install(10, 10);
		Hero hero = new Hero();
		hero.heroClass = HeroClass.HUNTRESS;
		Talent.initClassTalents(hero);
		Dungeon.hero = hero;
		HeadlessLevel.at(hero, 4 * level.width() + 4);
		// DURABLE_PROJECTILES is a tier-2 talent, DURABLE_TIPS a tier-3 one.
		hero.talents.get(1).put(Talent.DURABLE_PROJECTILES, durableProjectiles);
		hero.talents.get(2).put(Talent.DURABLE_TIPS, durableTips);
		return hero;
	}

	@Test
	void talentsAreAppliedBeforeTheUseCountIsRounded() {
		huntress(1, 1);
		// 1 base use * 1.5 (durable projectiles) = 1.5 uses, halved by durable tips' 1/(1+1)
		// reading as 3 uses. Rounding 1.5 up to 2 first (the old order) gave 4 uses instead.
		float perUse = new PoisonDart().durabilityPerUse();
		assertEquals(100f / 3f + 0.001f, perUse, 0.0005f, "upstream f9e6fbc94: 3 uses, not 4");
	}

	@Test
	void chargedShotAddsFourWholeUsesAfterRounding() {
		Hero hero = huntress(1, 1);
		Buff.affect(hero, Crossbow.ChargedShot.class);
		float perUse = new PoisonDart().durabilityPerUse();
		assertEquals(100f / 7f + 0.001f, perUse, 0.0005f, "3 rounded uses + 4, not 4 + 4");
	}

	@Test
	void anUntalentedTippedDartStillLastsOneUse() {
		huntress(0, 0);
		assertEquals(100f + 0.001f, new PoisonDart().durabilityPerUse(), 0.0005f);
	}
}
