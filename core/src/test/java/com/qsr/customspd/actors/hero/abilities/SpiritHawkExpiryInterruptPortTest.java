package com.qsr.customspd.actors.hero.abilities;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.hero.abilities.huntress.SpiritHawk;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Upstream 076e13ee0 (v2.3.0): when the spirit hawk's time runs out it interrupts the hero, so a
 * resting or travelling hero stops instead of carrying on as if nothing happened.
 */
class SpiritHawkExpiryInterruptPortTest {

	/**
	 * Hero.interrupt() ends in GameScene.resetKeyHold(), which has no scene to talk to headless
	 * (TurnPump documents the gap), so the hero only counts the calls.
	 */
	private static final class CountingHero extends Hero {
		int interrupts = 0;

		@Override
		public void interrupt() {
			interrupts++;
		}
	}

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	@Test
	void anExpiringHawkInterruptsTheHero() throws Exception {
		HeadlessLevel level = HeadlessLevel.install(8, 8);
		CountingHero hero = HeadlessLevel.at(new CountingHero(), 2 * level.width() + 2);
		Dungeon.hero = hero;
		StubSprite.attach(hero);

		SpiritHawk.HawkAlly hawk = HeadlessLevel.at(new SpiritHawk.HawkAlly(), 3 * level.width() + 3);
		StubSprite.attach(hawk);
		Field time = SpiritHawk.HawkAlly.class.getDeclaredField("timeRemaining");
		time.setAccessible(true);
		time.setFloat(hawk, 0f);

		Method act = SpiritHawk.HawkAlly.class.getDeclaredMethod("act");
		act.setAccessible(true);
		act.invoke(hawk);

		assertFalse(hawk.isAlive(), "fixture sanity: the expired hawk must have died");
		assertEquals(1, hero.interrupts, "the hawk expiring must interrupt the hero exactly once");
	}
}
