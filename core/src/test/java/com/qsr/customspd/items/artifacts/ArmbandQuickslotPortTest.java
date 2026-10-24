package com.qsr.customspd.items.artifacts;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.items.Item;
import com.qsr.customspd.items.weapon.missiles.darts.Dart;
import com.qsr.customspd.scenes.GameScene;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream ec165ecbe (v2.4.0): a successful steal spends charges, so the armband's quickslot
 * display has to be refreshed.
 */
class ArmbandQuickslotPortTest {

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

	@Test
	void aSuccessfulStealRefreshesTheQuickslot() {
		HeadlessLevel.install(8, 8);
		Hero hero = new Hero();
		Dungeon.hero = hero;
		StubSprite.attach(hero);

		MasterThievesArmband armband = new MasterThievesArmband();
		armband.charge = 5;
		MasterThievesArmband.Thievery thievery = armband.new Thievery();
		Item loot = new Dart();
		assertEquals(1f, thievery.stealChance(loot), 0f, "fixture sanity: the steal cannot fail");

		GameScene.updateItemDisplays = false;
		assertTrue(thievery.steal(loot));

		assertTrue(armband.charge < 5, "the steal spent charges");
		assertTrue(GameScene.updateItemDisplays, "so the quickslot display must be refreshed");
	}
}
