package com.qsr.customspd.actors.buffs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Stacked healing sources do not add up, but they no longer discard each other either: the total
 * left is the larger of the two, and the percent and flat per-tick rates are each the larger of
 * the two (upstream cebe05d08 stopped a weaker heal overriding a stronger one; d007df8e7 made the
 * combination a per-property maximum).
 */
class HealingStackingPortTest {

	private HeadlessRun run;
	private Hero hero;

	@BeforeEach
	void startRun() {
		run = HeadlessRun.start(20230619L);
		run.enterFirstLevel();
		hero = Dungeon.hero;
		StubSprite.attach(hero);
		hero.HP = 1;
	}

	@AfterEach
	void closeRun() {
		run.close();
	}

	@Test
	void aSmallerHealStillContributesItsFasterFlatRate() {
		Healing healing = Buff.affect(hero, Healing.class);
		healing.setHeal(20, 0, 2);
		healing.setHeal(5, 0, 5);

		healing.act();

		assertEquals(6, hero.HP, "the 20 total is kept and the faster 5 per tick is taken");
		assertEquals("15", healing.iconTextDisplay());
	}

	@Test
	void aLargerHealKeepsTheFasterRateOfTheRunningOne() {
		Healing healing = Buff.affect(hero, Healing.class);
		healing.setHeal(5, 0, 5);
		healing.setHeal(20, 0, 2);

		healing.act();

		assertEquals(6, hero.HP, "the 20 total from the new heal, the 5 per tick from the old");
		assertEquals("15", healing.iconTextDisplay());
	}

	@Test
	void percentAndFlatRatesAreMaximisedIndependently() {
		Healing healing = Buff.affect(hero, Healing.class);
		healing.setHeal(20, 0.5f, 0);
		healing.setHeal(10, 0f, 3);

		healing.act();

		//round(20 * 0.5) + 3 = 13
		assertEquals(14, hero.HP);
		assertEquals("7", healing.iconTextDisplay());
	}
}
