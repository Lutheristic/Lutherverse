package com.qsr.customspd.actors.buffs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A healing tick at full HP healed nothing but still floated a "+N" status over the hero; the
 * status is now only shown when HP was actually below max (upstream a2fac80a4).
 */
class HealingFullHpStatusPortTest {

	private HeadlessRun run;
	private Hero hero;
	private StubSprite sprite;

	@BeforeEach
	void startRun() {
		run = HeadlessRun.start(20231217L);
		run.enterFirstLevel();
		hero = Dungeon.hero;
		sprite = StubSprite.attach(hero);
	}

	@AfterEach
	void closeRun() {
		run.close();
	}

	@Test
	void aHealingTickAtFullHpShowsNoStatusText() {
		hero.HP = hero.HT;
		Healing healing = Buff.affect(hero, Healing.class);
		healing.setHeal(10, 0, 1);

		healing.act();

		assertTrue(sprite.statuses.isEmpty(), "nothing was healed, so no status text: " + sprite.statuses);
	}

	@Test
	void aHealingTickBelowFullHpStillShowsTheAmountHealed() {
		hero.HP = hero.HT - 5;
		Healing healing = Buff.affect(hero, Healing.class);
		healing.setHeal(10, 0, 1);

		healing.act();

		assertEquals(hero.HT - 4, hero.HP, "test setup: one point healed");
		assertEquals(1, sprite.statuses.size(), "one status for the heal: " + sprite.statuses);
		assertEquals("1", sprite.statuses.get(0).text);
	}
}
