package com.qsr.customspd.items.artifacts;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.plants.Swiftthistle;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Upstream c9c638034 (v2.4.1): the time freeze / swiftthistle buffs count down in floats, so a
 * value a hair under a whole number (2.9999) used to show as one turn too few, and a value a hair
 * under zero (-0.0005) used to print "-0" in the description.
 */
class TimeFreezeDisplayRoundingPortTest {

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
	void theHourglassBuffRoundsItsIconTextAndNeverPrintsNegativeZero() {
		HeadlessLevel.install(8, 8);
		Dungeon.hero = new Hero();
		TimekeepersHourglass.timeFreeze freeze = new TimekeepersHourglass().new timeFreeze();

		freeze.turnsToCost = 1.9999f;
		assertEquals("2", freeze.iconTextDisplay());

		freeze.turnsToCost = -0.0005f;
		assertFalse(freeze.desc().contains("-0"), "desc was: " + freeze.desc());
	}

	@Test
	void theSwiftthistleBubbleRoundsItsIconTextAndNeverPrintsNegativeZero() throws Exception {
		HeadlessLevel.install(8, 8);
		Dungeon.hero = new Hero();
		Swiftthistle.TimeBubble bubble = new Swiftthistle.TimeBubble();
		Field left = Swiftthistle.TimeBubble.class.getDeclaredField("left");
		left.setAccessible(true);

		left.setFloat(bubble, 2.9999f);
		assertEquals("3", bubble.iconTextDisplay());

		left.setFloat(bubble, -0.0005f);
		assertFalse(bubble.desc().contains("-0"), "desc was: " + bubble.desc());
	}
}
