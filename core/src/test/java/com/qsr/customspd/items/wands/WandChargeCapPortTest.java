package com.qsr.customspd.items.wands;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.scenes.GameScene;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Upstream eeca547cc (v2.4.0): recharging a wand up to its maximum leaves no partial charge
 * behind, so the next charge after a zap is not already part way there.
 */
class WandChargeCapPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
		GameScene.updateItemDisplays = false;
	}

	private static Wand chargingWand() {
		HeadlessLevel.install(8, 8);
		Hero hero = new Hero();
		Dungeon.hero = hero;
		Wand wand = new WandOfMagicMissile();
		wand.charge(hero);
		return wand;
	}

	@Test
	void reachingMaxChargesClearsPartialCharge() {
		Wand wand = chargingWand();
		wand.curCharges = wand.maxCharges - 1;
		wand.partialCharge = 0.5f;
		wand.charger.gainCharge(1f);
		assertEquals(wand.maxCharges, wand.curCharges);
		assertEquals(0f, wand.partialCharge, 0f);
	}

	@Test
	void overshootingMaxChargesClampsAndClears() {
		Wand wand = chargingWand();
		wand.curCharges = wand.maxCharges - 1;
		wand.partialCharge = 0.25f;
		wand.charger.gainCharge(3f);
		assertEquals(wand.maxCharges, wand.curCharges);
		assertEquals(0f, wand.partialCharge, 0f);
	}

	@Test
	void belowMaxKeepsPartialCharge() {
		Wand wand = chargingWand();
		wand.curCharges = 0;
		wand.partialCharge = 0.25f;
		wand.charger.gainCharge(1f);
		assertEquals(1, wand.curCharges);
		assertEquals(0.25f, wand.partialCharge, 0f);
	}
}
