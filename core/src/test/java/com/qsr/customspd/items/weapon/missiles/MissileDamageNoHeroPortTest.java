package com.qsr.customspd.items.weapon.missiles;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.test.HeadlessGdx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Port regression for the MissileWeapon.min()/max() hunks of upstream b6f7e7462 (v2.5.0, "made a
 * bunch of code tweaks so catalogs can appear out of game"). Both asked RingOfSharpshooting for
 * the hero's damage bonus, which dereferences its target, so a missile's damage range could not
 * be read with no hero (catalog/description outside a run). With no hero there is no ring bonus,
 * so the range is the plain level-scaled one.
 *
 * The expected values come from the level-scaled min(lvl)/max(lvl) at the item's own level (2).
 */
class MissileDamageNoHeroPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		Dungeon.hero = null;
	}

	@Test
	void damageRangeWithNoHeroIsThePlainLevelScaledRange() {
		Dungeon.hero = null;

		Shuriken shuriken = new Shuriken();
		shuriken.level(2);

		assertEquals(shuriken.min(2), shuriken.min(), "upstream b6f7e7462: no hero, no Sharpshooting bonus");
		assertEquals(shuriken.max(2), shuriken.max(), "upstream b6f7e7462: no hero, no Sharpshooting bonus");
	}
}
