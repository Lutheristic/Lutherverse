package com.qsr.customspd.items.artifacts;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.assets.GeneralAsset;
import com.qsr.customspd.test.HeadlessGdx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Upstream ab67766c6 (v2.2.0): the horn's sprite thresholds were 15/10/5 on eat and on
 * charge() but 8/5/2 when restoring and on the recharge buff, so the same charge showed
 * different sprites depending on the last code path to touch it.
 */
class HornOfPlentySpritePortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		Dungeon.hero = null;
	}

	@Test
	void chargingUsesTheSameSpriteThresholdsAsTheRechargeBuff() {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HornOfPlenty horn = new HornOfPlenty();
		horn.chargeCap = 10; //a fresh horn caps at 7, below the top sprite's threshold

		//(charge after the tick, expected sprite) at the 2 / 5 / 8 boundaries
		Object[][] cases = {
				{2, GeneralAsset.ARTIFACT_HORN2},
				{5, GeneralAsset.ARTIFACT_HORN3},
				{8, GeneralAsset.ARTIFACT_HORN4},
		};
		for (Object[] c : cases) {
			int target = (Integer) c[0];
			horn.charge = target - 1;
			horn.charge(hero, 4f); //0.25 partial charge per unit: 4 units = exactly 1 charge
			assertEquals(target, horn.charge);
			assertEquals(c[1], horn.image, "sprite at charge " + target);
		}
	}
}
