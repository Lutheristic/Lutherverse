package com.qsr.customspd.items.armor;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.test.HeadlessGdx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port regression for upstream 3d93c615b (v2.1.2, "Armors now include tier in their desc").
 * curr_absorb / avg_absorb gained a leading tier argument, so the damage range and strength moved
 * from %1-%3 to %2-%4 in every locale; a missed locale or call site would show the tier where the
 * minimum damage belongs. This pins the rendered English text for the known and unknown branches.
 */
class ArmorTierInfoPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		Dungeon.hero = null;
	}

	@Test
	void anIdentifiedArmorStatesItsTierRangeAndStrength() {
		Dungeon.hero = new Hero();
		PlateArmor armor = new PlateArmor();
		armor.identify();

		String info = armor.info();

		assertTrue(armor.levelKnown, "fixture: the known-level branch");
		assertTrue(info.contains("This _tier-5_ armor blocks _" + armor.DRMin() + "-" + armor.DRMax()
				+ " damage_ and requires _" + armor.STRReq() + " strength_"),
				"upstream 3d93c615b: tier first, then range, then strength; was: " + info);
	}

	@Test
	void anUnidentifiedArmorStatesItsTypicalTierRangeAndStrength() {
		Dungeon.hero = new Hero();
		ScaleArmor armor = new ScaleArmor();

		String info = armor.info();

		assertFalse(armor.levelKnown, "fixture: the unknown-level branch");
		assertTrue(info.contains("Typically this _tier-4_ armor blocks _" + armor.DRMin(0) + "-" + armor.DRMax(0)
				+ " damage_ and requires _" + armor.STRReq(0) + " strength_"),
				"upstream 3d93c615b: tier first, then range, then strength; was: " + info);
	}
}
