package com.qsr.customspd.actors.buffs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Meditate spends its five turns up front and then hands control back with {@code hero.next()};
 * without {@code hero.busy()} the hero stayed input-ready, so the player could keep acting
 * during the meditation (upstream 936d6a014).
 */
class MeditateBusyPortTest {

	private HeadlessRun run;
	private Hero hero;

	@BeforeEach
	void startRun() {
		run = HeadlessRun.start(20240602L);
		run.enterFirstLevel();
		hero = Dungeon.hero;
		StubSprite.attach(hero);
	}

	@AfterEach
	void closeRun() {
		run.close();
	}

	@Test
	void usingMeditateLeavesTheHeroNotReadyForInput() {
		Buff.affect(hero, MonkEnergy.class).energy = 5;
		hero.ready = true;

		new MonkEnergy.MonkAbility.Meditate().doAbility(hero, null);

		assertFalse(hero.ready, "the hero must not accept input while the meditation turns are spent");
		assertTrue(hero.buff(MonkEnergy.class).energy < 5, "test setup: the ability must have run to completion");
	}
}
