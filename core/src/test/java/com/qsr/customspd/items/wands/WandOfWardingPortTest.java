package com.qsr.customspd.items.wands;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.Rankings;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.hero.HeroClass;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import com.qsr.customspd.utils.GLog;
import com.watabou.utils.Random;
import com.watabou.utils.Signal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Wand of warding fixes ported from upstream:
 *  - 0ed96a3c1 (v2.3.1): a ward that kills the hero logs a death message naming the ward, and the
 *    run is ranked as killed by the wand, not by the transient Ward actor.
 */
class WandOfWardingPortTest {

	/** A hero whose death and interrupt machinery (scene, ankh, bones, save deletion) is inert. */
	private static final class MortalHero extends Hero {
		@Override
		public void interrupt() {
		}

		@Override
		public void die(Object cause) {
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

	private static void set(Object target, Class<?> declaring, String name, Object value) throws Exception {
		Field f = declaring.getDeclaredField(name);
		f.setAccessible(true);
		f.set(target, value);
	}

	@Test
	void aWardThatKillsTheHeroIsNamedInTheDeathMessageAndRankedAsTheWand() throws Exception {
		HeadlessLevel level = HeadlessLevel.install(8, 8);
		Hero hero = new MortalHero();
		Dungeon.hero = hero;
		Random.pushGenerator(0x3A8DL);
		try {
			HeroClass.WARRIOR.initHero(hero); // talents etc.; Rankings serialises the hero
		} finally {
			Random.popGenerator();
		}
		hero.HP = hero.HT = 1;
		hero.pos = 2 * level.width() + 2;
		StubSprite.attach(hero);

		WandOfWarding.Ward ward = new WandOfWarding.Ward();
		ward.pos = 2 * level.width() + 3;
		set(ward, WandOfWarding.Ward.class, "wandLevel", 3);
		// a sentry (tier 4+) pays HP per zap instead of dissipating, so the test never reaches
		// Ward.destroy, which re-renders the scene fog
		ward.tier = 4;
		ward.HP = ward.HT = 1000;
		StubSprite.attach(ward);
		set(ward, com.qsr.customspd.actors.mobs.Mob.class, "enemy", hero);

		List<String> log = new ArrayList<>();
		Signal.Listener<String> listener = log::add;
		GLog.update.add(listener);
		try {
			Method zap = WandOfWarding.Ward.class.getDeclaredMethod("zap");
			zap.setAccessible(true);
			zap.invoke(ward);
		} finally {
			GLog.update.remove(listener);
		}

		assertTrue(hero.HP <= 0, "the zap must kill the one-hit-point hero");
		String expected = GLog.NEGATIVE
				+ Messages.capitalize(Messages.get(ward, "kill", ward.name()));
		assertTrue(log.contains(expected), "a death message naming the ward must be logged; log was " + log);

		Rankings rankings = Rankings.INSTANCE;
		Rankings.Record rec = rankings.records.get(rankings.lastRecord);
		assertEquals(WandOfWarding.class, rec.cause, "the run is ranked as killed by the wand of warding");
	}
}
