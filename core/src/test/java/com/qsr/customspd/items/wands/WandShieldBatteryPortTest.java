package com.qsr.customspd.items.wands;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.buffs.Barrier;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.hero.HeroClass;
import com.qsr.customspd.actors.hero.Talent;
import com.qsr.customspd.items.Item;
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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 242e426b9 (v2.1.1): aiming a wand at yourself with the Shield Battery talent converts
 * its charges into a barrier; with zero charges it must fizzle instead of consuming the action
 * for a zero-point shield.
 */
class WandShieldBatteryPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() throws Exception {
		setItemStatic("curUser", null);
		setItemStatic("curItem", null);
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private static void setItemStatic(String name, Object value) throws Exception {
		Field f = Item.class.getDeclaredField(name);
		f.setAccessible(true);
		f.set(null, value);
	}

	private static Hero shieldBatteryMage() throws Exception {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		Random.pushGenerator(0x5B77L);
		try {
			HeroClass.MAGE.initHero(hero);
		} finally {
			Random.popGenerator();
		}
		hero.talents.get(0).put(Talent.SHIELD_BATTERY, 1);
		hero.pos = 3 * 8 + 3;
		StubSprite.attach(hero);
		setItemStatic("curUser", hero);
		return hero;
	}

	/** Aims the wand at the hero's own cell through the real zapper listener. */
	private static List<String> zapSelf(Hero hero, Wand wand) throws Exception {
		setItemStatic("curItem", wand);
		List<String> log = new ArrayList<>();
		Signal.Listener<String> listener = log::add;
		GLog.update.add(listener);
		try {
			Wand.zapper.onSelect(hero.pos);
		} finally {
			GLog.update.remove(listener);
		}
		return log;
	}

	@Test
	void anEmptyWandFizzlesInsteadOfGrantingAZeroShield() throws Exception {
		HeadlessLevel.install(8, 8);
		Hero hero = shieldBatteryMage();
		Wand wand = new WandOfMagicMissile();
		wand.curCharges = 0;

		List<String> log = zapSelf(hero, wand);

		assertNull(hero.buff(Barrier.class), "no barrier from an empty wand");
		assertTrue(log.contains(GLog.WARNING + Messages.get(Wand.class, "fizzles")),
				"the player is told the wand fizzles; log was " + log);
	}

	@Test
	void aChargedWandStillBecomesAShield() throws Exception {
		HeadlessLevel.install(8, 8);
		Hero hero = shieldBatteryMage();
		Wand wand = new WandOfMagicMissile();
		wand.curCharges = 2;

		zapSelf(hero, wand);

		assertNotNull(hero.buff(Barrier.class), "charges are converted into a barrier");
		assertTrue(hero.buff(Barrier.class).shielding() > 0);
		assertEquals(0, wand.curCharges, "and spent");
	}
}
