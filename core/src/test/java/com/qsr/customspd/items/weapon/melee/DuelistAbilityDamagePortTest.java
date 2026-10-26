package com.qsr.customspd.items.weapon.melee;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Char;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.buffs.Degrade;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins upstream 998cc211c, dafbb6f85 and 729a86295 (v2.4.0): damage-dealing weapon abilities
 * state their damage range, and the boost is a flat amount that grows with the weapon's level
 * instead of a damage multiplier. The expected numbers are written out by hand so that a wrong
 * boost formula cannot agree with itself.
 */
class DuelistAbilityDamagePortTest {

	private static final int W = 10;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	/** A rat with no armor and plenty of health, so the damage it takes is the attack's damage. */
	public static class SturdyRat extends Rat {
		{
			HP = HT = 1000;
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

	public static class FixedRollGreataxe extends Greataxe {
		@Override
		public int damageRoll(Char owner) {
			return 10;
		}
	}

	public static class FixedRollSpear extends Spear {
		@Override
		public int damageRoll(Char owner) {
			return 5;
		}
	}

	public static class FixedRollRapier extends Rapier {
		@Override
		public int damageRoll(Char owner) {
			return 5;
		}
	}

	public static class FixedRollKatana extends Katana {
		@Override
		public int damageRoll(Char owner) {
			return 5;
		}
	}

	public static class FixedRollSword extends Sword {
		@Override
		public int damageRoll(Char owner) {
			return 5;
		}
	}

	/** Its own damage roll stays in place, since the spin boost is added there; min and max are pinned instead. */
	public static class FixedRangeFlail extends Flail {
		@Override
		public int min(int lvl) {
			return 10;
		}

		@Override
		public int max(int lvl) {
			return 10;
		}
	}

	private static Hero heroOnFixture() {
		HeadlessLevel.install(W, 10);
		Hero hero = new Hero();
		hero.STR = 20; // meets every requirement, so surprise attacks are allowed
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, 4 * W + 4);
		return hero;
	}

	private static <T extends MeleeWeapon> T identifiedAt(T weapon, int level) {
		weapon.level(level);
		weapon.identify();
		return weapon;
	}

	private static SturdyRat ratAt(int cell) {
		SturdyRat rat = HeadlessLevel.at(new SturdyRat(), cell);
		StubSprite.attach(rat);
		Dungeon.level.heroFOV[cell] = true;
		return rat;
	}

	private static void assertTypicalRange(MeleeWeapon unidentified, int min, int max) {
		String info = unidentified.abilityInfo();
		assertTrue(info.contains("typically"), info);
		assertTrue(info.contains("_" + min + "-" + max), info);
	}

	// not checked for "typically": upstream's greataxe ability_desc says it too
	private static void assertKnownRange(MeleeWeapon identified, int min, int max) {
		String info = identified.abilityInfo();
		assertTrue(info.contains("_" + min + "-" + max), info);
	}

	@Test
	void heavyBlowWeaponsDescribeTheirBoostedRange() {
		heroOnFixture();
		// +0 unidentified: base range plus the level 0 boost
		assertTypicalRange(new Mace(), 3 + 4, 16 + 4);
		assertTypicalRange(new HandAxe(), 2 + 3, 12 + 3);
		assertTypicalRange(new BattleAxe(), 4 + 4, 20 + 4);
		assertTypicalRange(new WarHammer(), 5 + 4, 24 + 4);
		assertTypicalRange(new Greataxe(), 5 + 13, 45 + 13);

		// +3 identified: upgraded range plus the boost at +3
		assertKnownRange(identifiedAt(new Mace(), 3), 6 + 7, 28 + 7);
		assertKnownRange(identifiedAt(new HandAxe(), 3), 5 + 6, 21 + 6);
		assertKnownRange(identifiedAt(new BattleAxe(), 3), 7 + 7, 35 + 7);
		assertKnownRange(identifiedAt(new WarHammer(), 3), 8 + 7, 42 + 7);
		assertKnownRange(identifiedAt(new Greataxe(), 3), 8 + 18, 63 + 18);
	}

	@Test
	void heavyBlowOnASurprisedRatAddsAFlatBoost() {
		Hero hero = heroOnFixture();
		FixedRollMace mace = identifiedAt(new FixedRollMace(), 3);
		hero.belongings.weapon = mace;
		int cell = 4 * W + 5;
		SturdyRat rat = ratAt(cell);
		assertTrue(rat.surprisedBy(hero), "fixture: the rat has not noticed the hero");

		mace.duelistAbility(hero, cell);

		// a roll of 5 plus 4+3 at +3; the old 1.4x multiplier made it 7
		assertEquals(12, rat.HT - rat.HP);
	}

	@Test
	void retributionAddsAFlatBoost() {
		Hero hero = heroOnFixture();
		hero.HP = hero.HT / 4; // retribution needs the hero below half health
		FixedRollGreataxe axe = new FixedRollGreataxe();
		hero.belongings.weapon = axe;
		int cell = 4 * W + 5;
		SturdyRat rat = ratAt(cell);

		axe.duelistAbility(hero, cell);

		// a roll of 10 plus 13 at +0; the old 1.5x multiplier made it 15
		assertEquals(23, rat.HT - rat.HP);
	}

	@Test
	void spikeAndLungeWeaponsDescribeTheirBoostedRange() {
		heroOnFixture();
		assertTypicalRange(new Spear(), 2 + 5, 20 + 5);
		assertTypicalRange(new Glaive(), 5 + 7, 40 + 7);
		assertTypicalRange(new Rapier(), 1 + 3, 8 + 3);
		assertTypicalRange(new Katana(), 4 + 4, 20 + 4);

		assertKnownRange(identifiedAt(new Spear(), 3), 5 + 8, 32 + 8);
		assertKnownRange(identifiedAt(new Glaive(), 3), 8 + 11, 64 + 11);
		assertKnownRange(identifiedAt(new Rapier(), 3), 4 + 6, 14 + 6);
		assertKnownRange(identifiedAt(new Katana(), 3), 7 + 7, 35 + 7);
	}

	@Test
	void spikeAddsAFlatBoost() {
		Hero hero = heroOnFixture();
		FixedRollSpear spear = new FixedRollSpear();
		hero.belongings.weapon = spear;
		int cell = 4 * W + 6; // in reach, not adjacent
		SturdyRat rat = ratAt(cell);

		spear.duelistAbility(hero, cell);

		// a roll of 5 plus 5 at +0; the old 1.45x multiplier made it 7
		assertEquals(10, rat.HT - rat.HP);
	}

	@Test
	void katanaLungeAddsAFlatBoost() {
		Hero hero = heroOnFixture();
		FixedRollKatana katana = new FixedRollKatana();
		hero.belongings.weapon = katana;
		int cell = 4 * W + 6; // one tile between them
		SturdyRat rat = ratAt(cell);

		katana.duelistAbility(hero, cell);

		assertEquals(4 * W + 5, hero.pos, "fixture: the hero lunged next to the rat");
		// a roll of 5 plus 4 at +0; the old 1.35x multiplier made it 7
		assertEquals(9, rat.HT - rat.HP);
	}

	@Test
	void rapierLungeBoostUsesTheBuffedLevel() {
		Hero hero = heroOnFixture();
		FixedRollRapier rapier = identifiedAt(new FixedRollRapier(), 4);
		hero.belongings.weapon = rapier;
		Buff.affect(hero, Degrade.class, 30f);
		assertEquals(3, rapier.buffedLvl(), "fixture: degrade lowers a +4 rapier to +3");
		int cell = 4 * W + 6;
		SturdyRat rat = ratAt(cell);

		rapier.duelistAbility(hero, cell);

		// a roll of 5 plus 3+3; the boost read the unbuffed level and made it 12
		assertEquals(11, rat.HT - rat.HP);
	}

	@Test
	void cleaveWeaponsDescribeTheirBoostedRange() {
		heroOnFixture();
		assertTypicalRange(new Sword(), 3 + 3, 20 + 3);
		assertTypicalRange(new Shortsword(), 2 + 3, 15 + 3);
		assertTypicalRange(new WornShortsword(), 1 + 2, 10 + 2);
		assertTypicalRange(new Longsword(), 4 + 4, 25 + 4);
		assertTypicalRange(new Greatsword(), 5 + 4, 30 + 4);

		assertKnownRange(identifiedAt(new Sword(), 3), 6 + 5, 32 + 5);
		assertKnownRange(identifiedAt(new Shortsword(), 3), 5 + 4, 24 + 4);
		assertKnownRange(identifiedAt(new WornShortsword(), 3), 4 + 4, 16 + 4);
		assertKnownRange(identifiedAt(new Longsword(), 3), 7 + 6, 40 + 6);
		assertKnownRange(identifiedAt(new Greatsword(), 3), 8 + 6, 48 + 6);
	}

	@Test
	void flailDescribesItsBoostPerSpin() {
		heroOnFixture();
		String typical = new Flail().abilityInfo();
		assertTrue(typical.contains("typically deal _+7 damage_"), typical);
		String known = identifiedAt(new Flail(), 3).abilityInfo();
		assertTrue(known.contains("deal _+11 damage_"), known);
	}

	@Test
	void cleaveAddsAFlatBoost() {
		Hero hero = heroOnFixture();
		FixedRollSword sword = new FixedRollSword();
		hero.belongings.weapon = sword;
		int cell = 4 * W + 5;
		SturdyRat rat = ratAt(cell);

		sword.duelistAbility(hero, cell);

		// a roll of 5 plus 3 at +0; the old 1.27x multiplier made it 6
		assertEquals(8, rat.HT - rat.HP);
	}

	@Test
	void flailSpinsAddAFlatBoostToTheNextAttack() {
		Hero hero = heroOnFixture();
		FixedRangeFlail flail = new FixedRangeFlail();
		hero.belongings.weapon = flail;
		hero.STR = flail.STRReq(); // no bonus damage from excess strength
		int cell = 4 * W + 5;
		SturdyRat rat = ratAt(cell);

		flail.duelistAbility(hero, null);
		flail.duelistAbility(hero, null);
		assertEquals(2, hero.buff(Flail.SpinAbilityTracker.class).spins, "fixture: two spins");
		hero.attack(rat);

		// 10 plus 7 per spin at +0; the old +33% per spin made it 17
		assertEquals(24, rat.HT - rat.HP);
	}
}
