package com.qsr.customspd.items.wands;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.items.weapon.missiles.darts.BlindingDart;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream c1fabfbd8 (v2.4.0), the wand of regrowth half: a lotus from a high-level wand
 * (0.40 + 0.04 per level passes 100% at level 15) must not preserve more than every seed-based
 * item, which showed up as tipped darts with NEGATIVE durability use. The tipped-dart half of the
 * commit depends on f9e6fbc94 and is not ported here.
 */
class LotusSeedPreservationPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private static WandOfRegrowth.Lotus lotus(int wandLevel) throws Exception {
		WandOfRegrowth.Lotus lotus = new WandOfRegrowth.Lotus();
		Method setLevel = WandOfRegrowth.Lotus.class.getDeclaredMethod("setLevel", int.class);
		setLevel.setAccessible(true);
		setLevel.invoke(lotus, wandLevel);
		return lotus;
	}

	@Test
	void preservationIsCappedAtOneHundredPercent() throws Exception {
		assertEquals(0.40f + 0.04f * 3, lotus(3).seedPreservation(), 1e-6f, "low levels are untouched");
		assertEquals(1f, lotus(15).seedPreservation(), 1e-6f);
		assertEquals(1f, lotus(25).seedPreservation(), 0f, "level 25 would be 140% uncapped");
	}

	@Test
	void aMaxedLotusNeverGivesATippedDartNegativeDurabilityUse() throws Exception {
		HeadlessLevel level = HeadlessLevel.install(8, 8);
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeadlessLevel.at(hero, 3 * level.width() + 3);
		HeadlessLevel.at(lotus(25), 3 * level.width() + 4);

		float use = new BlindingDart().durabilityPerUse();

		assertTrue(use >= 0f, "durability use must not go negative; was " + use);
	}
}
