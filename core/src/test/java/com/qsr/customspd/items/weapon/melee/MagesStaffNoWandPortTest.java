package com.qsr.customspd.items.weapon.melee;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.items.wands.WandOfMagicMissile;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Port regression for upstream b4ba9c9b9 (v2.1.1, "added a few safety checks to the mage's
 * staff"). A staff with no imbued wand (the no-argument constructor, or the state a staff is in
 * while a new wand is being imbued) NPE'd in targetingPos and in imbueWand's read of the old
 * wand's charges. targetingPos now falls back to the plain item behaviour and imbueWand counts
 * a missing wand as 0 carried-over charges.
 */
class MagesStaffNoWandPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private Hero hero() {
		HeadlessLevel level = HeadlessLevel.install(10, 10);
		Hero hero = new Hero();
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, 4 * level.width() + 4);
		return hero;
	}

	@Test
	void targetingAWandlessStaffFallsBackToTheItemBehaviour() {
		Hero hero = hero();
		MagesStaff staff = new MagesStaff();
		int dst = 4 * 10 + 7;
		assertEquals(dst, staff.targetingPos(hero, dst),
				"upstream b4ba9c9b9: no wand to ask, so the staff aims straight at the target");
	}

	@Test
	void imbuingAWandlessStaffCarriesOverZeroCharges() {
		Hero hero = hero();
		MagesStaff staff = new MagesStaff();
		WandOfMagicMissile wand = new WandOfMagicMissile();
		int before = wand.curCharges;

		assertSame(staff, staff.imbueWand(wand, null),
				"upstream b4ba9c9b9: a staff with no wand can be imbued");
		assertEquals(before, wand.curCharges,
				"no old wand, so no charges carried over on top of the wand's own");
	}
}
