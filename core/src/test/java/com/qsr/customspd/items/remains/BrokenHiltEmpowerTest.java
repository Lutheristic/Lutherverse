package com.qsr.customspd.items.remains;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.buffs.PhysicalEmpower;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.test.HeadlessGdx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Upstream cd2a70aea (v2.2.0): PhysicalEmpower.set keeps whichever boost is worth more in total
 * (dmg * hits) instead of overwriting the damage and keeping the longer duration. The Duelist's
 * BrokenHilt remains item and the Strengthening/Focused Meal talents all feed the same buff, so a
 * weak meal boost used to downgrade a strong hilt boost while keeping its hit count.
 */
class BrokenHiltEmpowerTest {

	private Hero prevHero;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@BeforeEach
	void saveStatics() {
		prevHero = Dungeon.hero;
	}

	@AfterEach
	void restoreStatics() {
		Dungeon.hero = prevHero;
	}

	private static Hero heroAtLevel(int lvl) {
		Hero hero = new Hero();
		hero.lvl = lvl;
		Dungeon.hero = hero;
		return hero;
	}

	@Test
	void brokenHiltGrantsLevelScaledBoostForTwoHits() {
		Hero hero = heroAtLevel(30);
		new BrokenHilt().doEffect(hero);

		PhysicalEmpower emp = hero.buff(PhysicalEmpower.class);
		assertEquals(10, emp.dmgBoost, "max(2, lvl/3) damage at level 30");
		assertEquals(2, emp.left);
	}

	@Test
	void weakerBoostDoesNotDowngradeAStrongerOne() {
		Hero hero = heroAtLevel(30);
		new BrokenHilt().doEffect(hero); // 10 dmg x 2 hits

		// a Strengthening Meal style boost: 3 dmg x 1 hit
		Buff.affect(hero, PhysicalEmpower.class).set(3, 1);

		PhysicalEmpower emp = hero.buff(PhysicalEmpower.class);
		assertEquals(10, emp.dmgBoost, "the weaker boost must not replace the stronger one");
		assertEquals(2, emp.left);
	}

	@Test
	void strongerBoostReplacesAWeakerOneWholesale() {
		Hero hero = heroAtLevel(1);
		Buff.affect(hero, PhysicalEmpower.class).set(3, 1); // 3 total

		new BrokenHilt().doEffect(hero); // max(2, 0) = 2 dmg x 2 hits = 4 total

		PhysicalEmpower emp = hero.buff(PhysicalEmpower.class);
		assertEquals(2, emp.dmgBoost);
		assertEquals(2, emp.left);

		// a 1-hit boost worth more in total replaces both fields, not just the damage
		Buff.affect(hero, PhysicalEmpower.class).set(5, 1);
		assertEquals(5, emp.dmgBoost);
		assertEquals(1, emp.left, "the duration comes from the winning boost, not the longer one");
	}
}
