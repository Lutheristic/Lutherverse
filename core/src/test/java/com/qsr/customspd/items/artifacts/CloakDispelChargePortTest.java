package com.qsr.customspd.items.artifacts;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.buffs.Invisibility;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Upstream 822cf82b6 (v2.3.0): the cloak charges its first charge on the turn after activation,
 * so dispelling the stealth (attacking, reading a scroll...) before that tick used to hand the
 * charge back for free, letting the cloak be toggled indefinitely. Dispelling now pays for the
 * turn that was about to be charged.
 */
class CloakDispelChargePortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private static CloakOfShadows wearActiveCloak() {
		HeadlessLevel.install(8, 8);
		Hero hero = new Hero();
		Dungeon.hero = hero;
		hero.pos = 3 * 8 + 2;
		CloakOfShadows cloak = new CloakOfShadows();
		hero.belongings.artifact = cloak;
		cloak.identify();
		cloak.activate(hero);
		cloak.activeBuff = cloak.activeBuff();
		cloak.activeBuff.attachTo(hero);
		return cloak;
	}

	@Test
	void dispellingBeforeTheFirstChargeTickStillCostsACharge() {
		CloakOfShadows cloak = wearActiveCloak();
		int before = cloak.charge;
		Invisibility.dispel();
		assertEquals(before - 1, cloak.charge);
		assertNull(Dungeon.hero.buff(CloakOfShadows.cloakStealth.class));
	}

	@Test
	void dispellingMidwayThroughAChargeDoesNotChargeAgain() {
		CloakOfShadows cloak = wearActiveCloak();
		((CloakOfShadows.cloakStealth) cloak.activeBuff).turnsToCost = 3; //a charge was already paid this cycle
		int before = cloak.charge;
		Invisibility.dispel();
		assertEquals(before, cloak.charge);
	}
}
