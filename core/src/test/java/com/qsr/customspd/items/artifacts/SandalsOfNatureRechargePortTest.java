package com.qsr.customspd.items.artifacts;

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
 * Upstream 4ec2c80e1 (v2.4.0): recharging the sandals (scroll of recharging, recharging talents)
 * gives 2 charge per unit instead of going through the grass rate, and a recharge that fills them
 * leaves no partial charge behind. Upstream eeca547cc keeps a recharge smaller than one charge as
 * partial charge instead of rounding it.
 */
class SandalsOfNatureRechargePortTest {

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

	private static SandalsOfNature equipped(Hero hero) {
		HeadlessLevel.install(8, 8);
		Dungeon.hero = hero;
		SandalsOfNature sandals = new SandalsOfNature();
		hero.belongings.artifact = sandals;
		sandals.activate(hero);
		return sandals;
	}

	@Test
	void rechargeGivesTwoChargePerUnit() {
		Hero hero = new Hero();
		SandalsOfNature sandals = equipped(hero);
		sandals.charge = 0;
		sandals.partialCharge = 0;
		sandals.charge(hero, 1f);
		assertEquals(2, sandals.charge);
		assertEquals(0f, sandals.partialCharge, 0f);
	}

	@Test
	void smallRechargeAccumulates() {
		Hero hero = new Hero();
		SandalsOfNature sandals = equipped(hero);
		sandals.charge = 0;
		sandals.partialCharge = 0.75f;
		//0.25 charge, less than one
		sandals.charge(hero, 0.125f);
		assertEquals(1, sandals.charge);
		assertEquals(0f, sandals.partialCharge, 0f);
	}

	@Test
	void rechargeClearsPartialChargeWhenItFills() {
		Hero hero = new Hero();
		SandalsOfNature sandals = equipped(hero);
		sandals.charge = sandals.chargeCap - 1;
		sandals.partialCharge = 0.75f;
		sandals.charge(hero, 1f);
		assertEquals(sandals.chargeCap, sandals.charge);
		assertEquals(0f, sandals.partialCharge, 0f);
	}
}
