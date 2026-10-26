package com.qsr.customspd.items.weapon.melee;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Char;
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
}
