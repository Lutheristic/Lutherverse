package com.qsr.customspd.items.armor;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.items.BrokenSeal;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.test.HeadlessGdx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port regression for upstream 1635c2a00 (v2.5.3, "fixed cursed armor not including seal info in
 * desc"). The seal line lived in the same else-if chain as the curse warnings, so a known-cursed
 * armor with the Warrior's seal attached described the curse and silently dropped the seal.
 * It is now appended after the chain.
 */
class ArmorSealInfoPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		Dungeon.hero = null;
	}

	private static ClothArmor sealedArmor() {
		Dungeon.hero = new Hero();
		ClothArmor armor = new ClothArmor();
		armor.seal = new BrokenSeal();
		return armor;
	}

	private static String sealLine(ClothArmor armor) {
		return Messages.get(Armor.class, "seal_attached", armor.seal.maxShield(armor.tier, armor.level()));
	}

	@Test
	void aKnownCursedArmorStillDescribesItsSeal() {
		ClothArmor armor = sealedArmor();
		armor.cursed = true;
		armor.cursedKnown = true;

		String info = armor.info();

		assertTrue(info.contains(Messages.get(Armor.class, "cursed")), "fixture: the curse warning shows");
		assertTrue(info.contains(sealLine(armor)),
				"upstream 1635c2a00: the seal line must follow the curse warning, not be dropped by it");
	}

	@Test
	void aPlainSealedArmorStillDescribesItsSeal() {
		ClothArmor armor = sealedArmor();
		assertTrue(armor.info().contains(sealLine(armor)));
	}

	@Test
	void anArmorWithoutASealHasNoSealLine() {
		ClothArmor armor = sealedArmor();
		armor.seal = null;
		assertFalse(armor.info().contains(Messages.get(Armor.class, "seal_attached", 0).substring(0, 20)));
	}
}
