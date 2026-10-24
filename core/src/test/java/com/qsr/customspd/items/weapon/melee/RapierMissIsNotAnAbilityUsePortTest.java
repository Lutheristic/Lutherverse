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
 * Port regression for upstream fa66229e9 (v2.5.0, "fixed rapier lunge misses counting as
 * ability uses"). A lunge into empty space still spends a charge, but it ran the full
 * before/after ability hooks, so it triggered every "when you use a weapon ability" talent
 * (Precise Assault, Varied Charge, Combined Lethality...) for an attack that never happened.
 */
class RapierMissIsNotAnAbilityUsePortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	@Test
	void lungingIntoNothingSpendsTheChargeButTriggersNoAbilityTalent() {
		HeadlessLevel level = HeadlessLevel.install(10, 10);
		int start = 4 * level.width() + 4;
		int target = 4 * level.width() + 6; // two cells away, nothing there

		// Hero.speed() casts its sprite to HeroSprite (needs GL), so it is pinned here as in
		// RapierLungePortTest.
		Hero hero = new Hero() {
			@Override
			public float speed() {
				return 1f;
			}
		};
		hero.heroClass = HeroClass.DUELIST;
		Talent.initClassTalents(hero);
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, start);
		// PRECISE_ASSAULT is a tier-2 talent.
		hero.talents.get(1).put(Talent.PRECISE_ASSAULT, 1);
		Rapier rapier = new Rapier();
		hero.belongings.weapon = rapier;

		MeleeWeapon.Charger charger = Buff.affect(hero, MeleeWeapon.Charger.class);
		int chargesBefore = charger.charges;

		rapier.duelistAbility(hero, target);

		assertEquals(start + 1, hero.pos, "fixture: the lunge landed one step toward the target");
		assertEquals(chargesBefore - 1, charger.charges, "the miss still spends one charge");
		assertNull(hero.buff(Talent.PreciseAssaultTracker.class),
				"upstream fa66229e9: a lunge miss is not an ability use, so no talent tracker starts");
	}

	/**
	 * CPDU keeps separate charge pools for the primary weapon and a Champion's second weapon
	 * (upstream 11a410f07's shared pool is not ported). A missed lunge with the second-weapon
	 * Rapier must pay from the second pool, as MeleeWeapon.beforeAbilityUsed would.
	 */
	@Test
	void lungeMissWithTheSecondWeaponPaysFromTheSecondPool() {
		HeadlessLevel level = HeadlessLevel.install(10, 10);
		int start = 4 * level.width() + 4;
		int target = 4 * level.width() + 6;

		Hero hero = new Hero() {
			@Override
			public float speed() {
				return 1f;
			}
		};
		hero.heroClass = HeroClass.DUELIST;
		Talent.initClassTalents(hero);
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, start);
		hero.belongings.weapon = new Sword();
		Rapier rapier = new Rapier();
		hero.belongings.secondWep = rapier;

		MeleeWeapon.Charger charger = Buff.affect(hero, MeleeWeapon.Charger.class);
		charger.charges = 3;
		charger.partialCharge = 0;
		charger.secondCharges = 2;
		charger.secondPartialCharge = 0;

		rapier.duelistAbility(hero, target);

		assertEquals(start + 1, hero.pos, "fixture: the lunge landed one step toward the target");
		assertEquals(3, charger.charges, "the primary pool is not touched by the second weapon's miss");
		assertEquals(0f, charger.partialCharge, 0f, "the primary pool is not put into debt");
		assertEquals(1, charger.secondCharges, "the miss spends one charge from the second pool");
	}

	@Test
	void lungeMissWithTheSecondWeaponAndAnEmptyPrimaryPoolDoesNotGoIntoDebt() {
		HeadlessLevel level = HeadlessLevel.install(10, 10);
		int start = 4 * level.width() + 4;
		int target = 4 * level.width() + 6;

		Hero hero = new Hero() {
			@Override
			public float speed() {
				return 1f;
			}
		};
		hero.heroClass = HeroClass.DUELIST;
		Talent.initClassTalents(hero);
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, start);
		hero.belongings.weapon = new Sword();
		Rapier rapier = new Rapier();
		hero.belongings.secondWep = rapier;

		MeleeWeapon.Charger charger = Buff.affect(hero, MeleeWeapon.Charger.class);
		charger.charges = 0;
		charger.partialCharge = 0;
		charger.secondCharges = 2;
		charger.secondPartialCharge = 0;

		rapier.duelistAbility(hero, target);

		assertEquals(0, charger.charges);
		assertEquals(0f, charger.partialCharge, 0f, "primary pool must not go negative");
		assertEquals(1, charger.secondCharges, "the second pool pays");
	}
}
