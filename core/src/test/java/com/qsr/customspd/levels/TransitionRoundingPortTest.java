package com.qsr.customspd.levels;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Actor;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Upstream a94186e01 (v2.3.1): before a floor transition the hero's partial turn is spent, and
 * every actor left ahead of the hero by that rounding is rounded up too, not only characters.
 * A buff or blob left a fraction of a turn ahead used to keep that fraction on the next floor.
 */
class TransitionRoundingPortTest {

	private static class Ticker extends Actor {
		@Override
		protected boolean act() {
			spend(TICK);
			return true;
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
	void actorsAheadOfTheHeroRoundUpWithThem() {
		HeadlessLevel level = HeadlessLevel.install(8, 8);
		Hero hero = new Hero();
		Dungeon.hero = hero;
		hero.pos = 3 * level.width() + 3;
		Actor.addDelayed(hero, 0.5f);

		Rat rat = new Rat();
		rat.pos = 3 * level.width() + 5;
		Actor.addDelayed(rat, 0.25f);
		Ticker ticker = new Ticker();
		Actor.addDelayed(ticker, 0.25f);
		Ticker behind = new Ticker();
		Actor.addDelayed(behind, 1.5f);

		Level.beforeTransition();

		assertEquals(1f, hero.cooldown(), 0.0001f, "the hero's partial turn is spent");
		assertEquals(1f, rat.cooldown(), 0.0001f, "a char ahead of the hero rounds up with them");
		assertEquals(1f, ticker.cooldown(), 0.0001f, "and so does any other actor ahead of them");
		assertEquals(1.5f, behind.cooldown(), 0.0001f, "an actor already behind the hero is left alone");
	}
}
