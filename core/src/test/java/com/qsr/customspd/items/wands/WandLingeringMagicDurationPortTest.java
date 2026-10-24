package com.qsr.customspd.items.wands;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.hero.HeroClass;
import com.qsr.customspd.actors.hero.Talent;
import com.qsr.customspd.items.Item;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import com.watabou.utils.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 70e0f5d7f (v2.4.2): "fixed duration on lingering magic going past 5". Each wand use
 * refreshes the lingering-magic window to 5 turns; it used to add 5 more turns to whatever was
 * left, so quick successive zaps stacked the window past its cap.
 */
class WandLingeringMagicDurationPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() throws Exception {
		setItemStatic("curUser", null);
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private static void setItemStatic(String name, Object value) throws Exception {
		Field f = Item.class.getDeclaredField(name);
		f.setAccessible(true);
		f.set(null, value);
	}

	@Test
	void repeatedZapsRefreshTheWindowInsteadOfStackingIt() throws Exception {
		HeadlessLevel.install(8, 8);
		Hero hero = new Hero();
		Dungeon.hero = hero;
		Random.pushGenerator(0x11C7L);
		try {
			HeroClass.MAGE.initHero(hero);
		} finally {
			Random.popGenerator();
		}
		hero.talents.get(0).put(Talent.LINGERING_MAGIC, 1);
		hero.pos = 3 * 8 + 3;
		StubSprite.attach(hero);
		setItemStatic("curUser", hero);

		Wand wand = new WandOfMagicMissile();
		wand.charge(hero); // a wand held by the hero's charger, as in the staff
		wand.curCharges = 3;

		wand.wandUsed();
		wand.wandUsed();
		wand.wandUsed();

		Talent.LingeringMagicTracker tracker = hero.buff(Talent.LingeringMagicTracker.class);
		assertNotNull(tracker, "using a wand starts the lingering-magic window");
		assertTrue(tracker.cooldown() <= 5f,
				"the window is capped at 5 turns, but is " + tracker.cooldown());
	}
}
