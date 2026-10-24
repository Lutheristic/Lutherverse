package com.qsr.customspd.items.wands;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.buffs.Barrier;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.hero.HeroClass;
import com.qsr.customspd.actors.hero.Talent;
import com.qsr.customspd.items.Item;
import com.qsr.customspd.items.weapon.melee.MagesStaff;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import com.watabou.utils.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Upstream ef51aede8 (v2.3.0): the Excess Charge talent's barrier, granted when a staffed wand is
 * used at full charge, now floats its amount as shielding text over the hero.
 */
class ExcessChargeShieldTextPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() throws Exception {
		Field f = Item.class.getDeclaredField("curUser");
		f.setAccessible(true);
		f.set(null, null);
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	@Test
	void usingAFullyChargedStaffWandFloatsTheShieldAmount() throws Exception {
		HeadlessLevel.install(8, 8);
		Hero hero = new Hero();
		Dungeon.hero = hero;
		Random.pushGenerator(0xE5CL);
		try {
			HeroClass.MAGE.initHero(hero);
		} finally {
			Random.popGenerator();
		}
		hero.talents.get(0).put(Talent.EXCESS_CHARGE, 2);
		hero.pos = 3 * 8 + 3;
		StubSprite sprite = StubSprite.attach(hero);
		Field f = Item.class.getDeclaredField("curUser");
		f.setAccessible(true);
		f.set(null, hero);

		Field wf = MagesStaff.class.getDeclaredField("wand");
		wf.setAccessible(true);
		Wand wand = (Wand) wf.get(hero.belongings.getItem(MagesStaff.class));
		wand.curCharges = wand.maxCharges;
		int expected = Math.round(wand.buffedLvl() * 0.67f * 2);

		wand.wandUsed();

		Barrier barrier = hero.buff(Barrier.class);
		assertNotNull(barrier, "excess charge grants a barrier");
		assertEquals(expected, barrier.shielding());
		assertEquals(1, sprite.statuses.size(), "and floats it once; got " + sprite.statuses);
		assertEquals(Integer.toString(expected), sprite.statuses.get(0).text);
	}
}
