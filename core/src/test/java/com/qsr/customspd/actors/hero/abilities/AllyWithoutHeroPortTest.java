package com.qsr.customspd.actors.hero.abilities;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Char;
import com.qsr.customspd.actors.hero.abilities.huntress.SpiritHawk;
import com.qsr.customspd.actors.hero.abilities.rogue.SmokeBomb;
import com.qsr.customspd.test.HeadlessGdx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Port regression for the SpiritHawk and SmokeBomb hunks of upstream b6f7e7462 (v2.5.0, "made a
 * bunch of code tweaks so catalogs can appear out of game"; unchanged in v2.5.4). Both ability
 * allies sized themselves from the hero's talents in their constructors, so building one with no
 * hero (the bestiary catalog creating a mob just to describe it) was a NullPointerException.
 * Without a hero they take their talent-less base stats.
 */
class AllyWithoutHeroPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		Dungeon.hero = null;
	}

	@Test
	void spiritHawkWithNoHeroHasBaseSightAndSpeed() throws Exception {
		Dungeon.hero = null;
		SpiritHawk.HawkAlly hawk = assertDoesNotThrow(() -> new SpiritHawk.HawkAlly());
		assertEquals(6, hawk.viewDistance, "no EAGLE_EYE points");
		// baseSpeed is protected; speed() itself consults the hero (AscensionChallenge), which is outside this hunk
		Field baseSpeed = Char.class.getDeclaredField("baseSpeed");
		baseSpeed.setAccessible(true);
		assertEquals(2f, baseSpeed.getFloat(hawk), 0f, "no SWIFT_SPIRIT points: base speed 2");
	}

	@Test
	void ninjaLogWithNoHeroKeepsTheBaseTwentyHealth() {
		Dungeon.hero = null;
		SmokeBomb.NinjaLog log = assertDoesNotThrow(() -> new SmokeBomb.NinjaLog());
		assertEquals(20, log.HT, "upstream b6f7e7462: base 20 HT, scaled by BODY_REPLACEMENT only with a hero");
		assertEquals(20, log.HP);
	}
}
