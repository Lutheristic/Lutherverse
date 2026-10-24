package com.qsr.customspd.actors.buffs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Hunger pushed past STARVING by something other than the hunger tick (affectHunger with a big
 * negative value) used to bank the excess as partialDamage and only pay
 * it out on the next hunger tick; upstream pays any whole damage out immediately (5ba584da5).
 */
class HungerOverfeedDamagePortTest {

	private HeadlessRun run;
	private Hero hero;

	@BeforeEach
	void startRun() {
		run = HeadlessRun.start(20230925L);
		run.enterFirstLevel();
		hero = Dungeon.hero;
		StubSprite.attach(hero);
	}

	@AfterEach
	void closeRun() {
		run.close();
	}

	@Test
	void hungerPastStarvingDamagesImmediately() {
		Hunger hunger = hero.buff(Hunger.class);
		assertNotNull(hunger, "test setup: a hero starts with Hunger");
		hero.HP = hero.HT = 20;

		// 100 over STARVING at HT/1000 per point = 100 * 20/1000 = 2 damage
		hunger.affectHunger(-(Hunger.STARVING + 100f));

		assertEquals(18, hero.HP, "the whole damage in the excess is dealt at once, not on the next tick");
		assertEquals((int) Hunger.STARVING, hunger.hunger(), "and hunger is clamped at STARVING");
	}

	@Test
	void subWholeExcessStaysBankedAsPartialDamage() {
		Hunger hunger = hero.buff(Hunger.class);
		hero.HP = hero.HT = 20;

		// 20 over STARVING = 0.4 damage: below one point, so nothing is dealt yet
		hunger.affectHunger(-(Hunger.STARVING + 20f));

		assertEquals(20, hero.HP);
	}
}
