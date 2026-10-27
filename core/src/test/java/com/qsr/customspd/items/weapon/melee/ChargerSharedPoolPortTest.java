package com.qsr.customspd.items.weapon.melee;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Char;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.buffs.Daze;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.hero.HeroClass;
import com.qsr.customspd.actors.hero.HeroSubClass;
import com.qsr.customspd.actors.hero.Talent;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.IdleCellSelector;
import com.qsr.customspd.test.SaveRoundtrip;
import com.qsr.customspd.test.StubSprite;
import com.watabou.utils.Bundle;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 11a410f07 (v2.4.0, "Big Duelist charge speed changes"). A Champion's two weapons share
 * one charge pool instead of the second weapon keeping its own smaller one. The pool starts at 2,
 * caps at min(8, 2 + (lvl-1)/3) (Champion: min(10, 4 + (lvl-1)/3)), so both reach their cap at
 * level 19 since 753b340f0 (v2.4.2) where they used to at 25, and regains a charge every 60 to 45
 * turns, 1.5x as fast for the Champion. Every ability costs 1 charge: a heavy blow that does
 * not surprise its target now deals regular damage instead of costing 2.
 */
class ChargerSharedPoolPortTest {

	private static final int W = 10;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
		IdleCellSelector.install();
	}

	@AfterAll
	static void restoreSelector() {
		IdleCellSelector.uninstall();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	/** A rat that has already seen the hero, so it cannot be surprised. */
	public static class AwareRat extends Rat {
		{
			HP = HT = 1000;
			enemySeen = true;
		}

		@Override
		public int drRoll() {
			return 0;
		}
	}

	public static class FixedRollMace extends Mace {
		@Override
		public int damageRoll(Char owner) {
			return 5;
		}
	}

	private static Hero duelist(int lvl, HeroSubClass subClass) {
		HeadlessLevel.install(W, 10);
		Hero hero = new Hero();
		hero.heroClass = HeroClass.DUELIST;
		hero.subClass = subClass;
		hero.lvl = lvl;
		Talent.initClassTalents(hero);
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, 4 * W + 4);
		return hero;
	}

	private static MeleeWeapon.Charger pool(Hero hero, int charges) {
		MeleeWeapon.Charger charger = Buff.affect(hero, MeleeWeapon.Charger.class);
		charger.charges = charges;
		charger.partialCharge = 0f;
		return charger;
	}

	@Test
	void aNewPoolStartsWithTwoCharges() {
		assertEquals(2, new MeleeWeapon.Charger().charges);
	}

	@Test
	void theCapGainsAChargeEveryThreeLevels() {
		Hero hero = duelist(1, HeroSubClass.NONE);
		MeleeWeapon.Charger charger = pool(hero, 0);
		int[][] levelToCap = {{1, 2}, {3, 2}, {4, 3}, {7, 4}, {16, 7}, {18, 7}, {19, 8}, {25, 8}, {30, 8}};
		for (int[] row : levelToCap) {
			hero.lvl = row[0];
			assertEquals(row[1], charger.chargeCap(), "level " + row[0]);
		}
	}

	@Test
	void theChampionsCapIsTwoHigher() {
		Hero hero = duelist(1, HeroSubClass.CHAMPION);
		MeleeWeapon.Charger charger = pool(hero, 0);
		int[][] levelToCap = {{1, 4}, {3, 4}, {4, 5}, {16, 9}, {18, 9}, {19, 10}, {25, 10}, {30, 10}};
		for (int[] row : levelToCap) {
			hero.lvl = row[0];
			assertEquals(row[1], charger.chargeCap(), "level " + row[0]);
		}
	}

	@Test
	void aChargeTakesSixtyToFortyFiveTurns() {
		Hero hero = duelist(25, HeroSubClass.NONE);
		MeleeWeapon.Charger charger = pool(hero, 0);
		charger.act();
		assertEquals(1f / 48f, charger.partialCharge, 1e-6f, "empty, cap 8: 1/(60-1.5*8)");

		charger = pool(hero, 7);
		charger.act();
		assertEquals(1f / 58.5f, charger.partialCharge, 1e-6f, "one short of the cap: 1/(60-1.5)");
	}

	@Test
	void theChampionChargesHalfAgainAsFast() {
		Hero hero = duelist(25, HeroSubClass.CHAMPION);
		MeleeWeapon.Charger charger = pool(hero, 0);
		charger.act();
		assertEquals(1.5f / 45f, charger.partialCharge, 1e-6f, "empty, cap 10: 1.5/(60-1.5*10)");
	}

	private static Hero championWithTwoWeapons(MeleeWeapon primary, MeleeWeapon second) {
		Hero hero = duelist(1, HeroSubClass.CHAMPION);
		hero.STR = 20; // strong enough for either weapon's ability
		hero.belongings.weapon = primary;
		hero.belongings.secondWep = second;
		return hero;
	}

	@Test
	void theSecondWeaponsAbilitySpendsTheSharedPool() {
		Sword sword = new Sword();
		Spear spear = new Spear();
		Hero hero = championWithTwoWeapons(sword, spear);
		MeleeWeapon.Charger charger = pool(hero, 2);

		spear.beforeAbilityUsed(hero, null);
		assertEquals(1, charger.charges, "the second weapon pays from the one pool");
		assertEquals(0f, charger.partialCharge, 0f);

		sword.beforeAbilityUsed(hero, null);
		assertEquals(0, charger.charges, "and so does the primary");
	}

	@Test
	void bothWeaponsShowTheSharedPool() {
		Sword sword = new Sword();
		Spear spear = new Spear();
		Hero hero = championWithTwoWeapons(sword, spear);
		pool(hero, 3);

		assertEquals("3/4", sword.status());
		assertEquals("3/4", spear.status(), "the second weapon shows the same count and cap");
	}

	@Test
	void anEmptyPoolRefusesTheSecondWeaponsAbility() {
		Quarterstaff staff = new Quarterstaff();
		Hero hero = championWithTwoWeapons(new Sword(), staff);
		MeleeWeapon.Charger charger = pool(hero, 0);

		staff.execute(hero, MeleeWeapon.AC_ABILITY);
		assertNull(hero.buff(Quarterstaff.DefensiveStance.class), "no charge in the shared pool, no stance");

		charger.charges = 1;
		staff.execute(hero, MeleeWeapon.AC_ABILITY);
		assertNotNull(hero.buff(Quarterstaff.DefensiveStance.class), "one shared charge is enough");
		assertEquals(0, charger.charges);
	}

	@Test
	void everyAbilityCostsOneCharge() {
		Hero hero = duelist(1, HeroSubClass.NONE);
		AwareRat aware = HeadlessLevel.at(new AwareRat(), 4 * W + 5);
		assertFalse(aware.surprisedBy(hero), "fixture: the rat cannot be surprised");

		MeleeWeapon[] weapons = {new AssassinsBlade(), new Dagger(), new Dirk(), new Quarterstaff(),
				new Scimitar(), new Sickle(), new WarScythe(), new Flail(),
				new HandAxe(), new Mace(), new BattleAxe(), new WarHammer()};
		List<String> wrong = new ArrayList<>();
		for (MeleeWeapon weapon : weapons) {
			if (weapon.abilityChargeUse(hero, null) != 1f) wrong.add(weapon.getClass().getSimpleName());
			if (weapon.abilityChargeUse(hero, aware) != 1f) wrong.add(weapon.getClass().getSimpleName() + " vs an aware enemy");
		}
		assertTrue(wrong.isEmpty(), "abilities not costing 1: " + wrong);
	}

	@Test
	void spinningAFlailOnlyCostsAChargeToStart() {
		Hero hero = duelist(1, HeroSubClass.NONE);
		Flail flail = new Flail();
		assertEquals(1f, flail.abilityChargeUse(hero, null), 0f, "starting the spin, down from 2");
		Buff.affect(hero, Flail.SpinAbilityTracker.class, 3f);
		assertEquals(0f, flail.abilityChargeUse(hero, null), 0f, "further spins are free");
	}

	@Test
	void aHeavyBlowThatDoesNotSurpriseDealsRegularDamageForOneCharge() {
		Hero hero = duelist(1, HeroSubClass.NONE);
		hero.STR = 20;
		FixedRollMace mace = new FixedRollMace();
		mace.level(3);
		mace.identify();
		hero.belongings.weapon = mace;
		MeleeWeapon.Charger charger = pool(hero, 1);
		int cell = 4 * W + 5;
		AwareRat rat = HeadlessLevel.at(new AwareRat(), cell);
		StubSprite.attach(rat);
		Dungeon.level.heroFOV[cell] = true;

		mace.duelistAbility(hero, cell);

		assertEquals(5, rat.HT - rat.HP, "the roll of 5 with no boost; it used to need 2 charges for this");
		assertEquals(0, charger.charges, "one charge spent");
		assertNotNull(rat.buff(Daze.class), "it still dazes");
	}

	@Test
	void aTwoPoolSaveLoadsWithTheFirstPoolOnly() throws Exception {
		Bundle old = new Bundle();
		old.put("charges", 3);
		old.put("partialCharge", 0.5f);
		old.put("second_charges", 2);
		old.put("second_partialCharge", 0.25f);
		MeleeWeapon.Charger charger = new MeleeWeapon.Charger();
		charger.restoreFromBundle(SaveRoundtrip.writeRead(old));

		assertEquals(3, charger.charges);
		assertEquals(0.5f, charger.partialCharge, 1e-6f);

		Bundle saved = new Bundle();
		charger.storeInBundle(saved);
		assertFalse(saved.contains("second_charges"), "the second pool is gone from saves");
		assertFalse(saved.contains("second_partialCharge"));
	}

	/** Upstream accepts this: a save holding more charges than the lower cap keeps them until spent. */
	@Test
	void chargesAboveTheNewCapAreNotClampedOnLoad() throws Exception {
		Hero hero = duelist(30, HeroSubClass.NONE);
		Sword sword = new Sword();
		hero.belongings.weapon = sword;
		MeleeWeapon.Charger charger = pool(hero, 10); // the old level-30 cap
		charger.partialCharge = 0.5f;

		MeleeWeapon.Charger restored = SaveRoundtrip.of(charger);
		charger.detach();
		restored.attachTo(hero);
		assertEquals(10, restored.charges, "not clamped to the new cap of 8");

		restored.act();
		assertEquals(10, restored.charges, "regen leaves an over-cap pool alone");
		assertEquals(0f, restored.partialCharge, 0f, "and only clears its partial charge");
		assertEquals("10/8", sword.status());
	}
}
