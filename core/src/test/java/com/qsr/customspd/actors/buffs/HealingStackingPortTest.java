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
 * A weaker healing effect used to overwrite a stronger one already running (upstream cebe05d08).
 * A second source whose total is not larger than what is left is now ignored.
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
	void aWeakerHealDoesNotOverrideAStrongerRunningOne() {
		Healing healing = Buff.affect(hero, Healing.class);
		healing.setHeal(20, 0, 2);
		healing.setHeal(5, 0, 5);

		healing.act();

		assertEquals(3, hero.HP, "still the original 2 per tick, not the weaker heal's 5");
		assertEquals("18", healing.iconTextDisplay(), "the original 20 left, minus one 2-point tick");
	}

	@Test
	void aLargerHealReplacesTheRunningOne() {
		Healing healing = Buff.affect(hero, Healing.class);
		healing.setHeal(5, 0, 5);
		healing.setHeal(20, 0, 2);

		healing.act();

		assertEquals(3, hero.HP, "the larger heal's 2 per tick won");
		assertEquals("18", healing.iconTextDisplay());
	}
}
