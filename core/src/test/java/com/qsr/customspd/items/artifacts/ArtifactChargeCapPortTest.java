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
 * Upstream eeca547cc (v2.4.0): recharging an artifact (scroll of recharging, recharging talents)
 * can add more than one charge at once, and a charge that reaches the cap leaves no partial charge
 * behind. The rose and talisman also stop rounding small recharge amounts away.
 */
class ArtifactChargeCapPortTest {

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

	private static Hero hero() {
		HeadlessLevel.install(8, 8);
		Hero hero = new Hero();
		Dungeon.hero = hero;
		return hero;
	}

	@Test
	void toolkitGainsSeveralChargesInOneCall() {
		Hero hero = hero();
		AlchemistsToolkit kit = new AlchemistsToolkit();
		kit.charge(hero, 8f);
		assertEquals(2, kit.charge);
		assertEquals(0f, kit.partialCharge, 0f);
	}

	@Test
	void cloakClearsPartialChargeWhenItFills() {
		Hero hero = hero();
		CloakOfShadows cloak = new CloakOfShadows();
		hero.belongings.artifact = cloak;
		cloak.charge = cloak.chargeCap - 1;
		cloak.partialCharge = 0.5f;
		cloak.charge(hero, 4f);
		assertEquals(cloak.chargeCap, cloak.charge);
		assertEquals(0f, cloak.partialCharge, 0f);
	}

	@Test
	void cloakGainsSeveralChargesInOneCall() {
		Hero hero = hero();
		CloakOfShadows cloak = new CloakOfShadows();
		hero.belongings.artifact = cloak;
		cloak.charge = 0;
		cloak.charge(hero, 8f);
		assertEquals(2, cloak.charge);
		assertEquals(0f, cloak.partialCharge, 0f);
	}

	@Test
	void roseSmallRechargeAccumulatesAndReachesTheCap() {
		Hero hero = hero();
		DriedRose rose = new DriedRose();
		rose.charge = rose.chargeCap - 1;
		rose.partialCharge = 0.875f;
		//0.25 charge, which used to round to nothing
		rose.charge(hero, 0.0625f);
		assertEquals(rose.chargeCap, rose.charge);
		assertEquals(0f, rose.partialCharge, 0f);
	}

	@Test
	void talismanSmallRechargeAccumulatesAndReachesTheCap() {
		Hero hero = hero();
		TalismanOfForesight talisman = new TalismanOfForesight();
		talisman.charge = talisman.chargeCap - 1;
		talisman.partialCharge = 0.875f;
		//0.25 charge, which used to round to nothing
		talisman.charge(hero, 0.125f);
		assertEquals(talisman.chargeCap, talisman.charge);
		assertEquals(0f, talisman.partialCharge, 0f);
	}

	@Test
	void hourglassClearsPartialChargeWhenItFills() {
		Hero hero = hero();
		TimekeepersHourglass hourglass = new TimekeepersHourglass();
		hourglass.charge = hourglass.chargeCap - 1;
		hourglass.partialCharge = 0.5f;
		hourglass.charge(hero, 4f);
		assertEquals(hourglass.chargeCap, hourglass.charge);
		assertEquals(0f, hourglass.partialCharge, 0f);
	}

	@Test
	void hourglassGainsSeveralChargesInOneCall() {
		Hero hero = hero();
		TimekeepersHourglass hourglass = new TimekeepersHourglass();
		hourglass.charge = 0;
		hourglass.charge(hero, 8f);
		assertEquals(2, hourglass.charge);
		assertEquals(0f, hourglass.partialCharge, 0f);
	}

	@Test
	void spellbookClearsPartialChargeWhenItFills() {
		Hero hero = hero();
		UnstableSpellbook book = new UnstableSpellbook();
		book.charge = book.chargeCap - 1;
		book.partialCharge = 0.5f;
		book.charge(hero, 10f);
		assertEquals(book.chargeCap, book.charge);
		assertEquals(0f, book.partialCharge, 0f);
	}

	@Test
	void spellbookGainsSeveralChargesInOneCall() {
		Hero hero = hero();
		UnstableSpellbook book = new UnstableSpellbook();
		book.charge = 0;
		book.charge(hero, 20f);
		assertEquals(2, book.charge);
		assertEquals(0f, book.partialCharge, 0f);
	}
}
