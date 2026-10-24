package com.qsr.customspd.items;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.items.armor.LeatherArmor;
import com.qsr.customspd.items.weapon.missiles.Shuriken;
import com.qsr.customspd.test.HeadlessGdx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Port regression for the Item.buffedLvl hunk of upstream b6f7e7462 (v2.5.0, "made a bunch of
 * code tweaks so catalogs can appear out of game"). Item.buffedLvl asked Dungeon.hero for its
 * Degrade buff unconditionally, so reading the level of any item while no hero exists (a catalog
 * or item description shown outside a run) was a NullPointerException. With no hero there is
 * nobody to be degraded, so the plain level is the answer.
 *
 * Sword/ring are deliberately not used: upstream's same commit also guards MeleeWeapon.buffedLvl and
 * Ring.buffedLvl, which are separate overrides CPDU has not ported (tracked as follow-ups).
 */
class ItemBuffedLvlNoHeroPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		Dungeon.hero = null;
	}

	@Test
	void buffedLvlWithNoHeroIsThePlainLevel() {
		Dungeon.hero = null;

		LeatherArmor armor = new LeatherArmor();
		armor.level(3);
		assertEquals(3, armor.buffedLvl(), "upstream b6f7e7462: no hero, so no Degrade to apply");

		Shuriken shuriken = new Shuriken();
		shuriken.level(2);
		assertEquals(2, shuriken.buffedLvl(), "the guard lives in Item.buffedLvl, so every item kind benefits");
	}
}
