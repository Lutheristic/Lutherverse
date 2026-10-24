package com.qsr.customspd.items.weapon;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.items.weapon.melee.Sword;
import com.qsr.customspd.items.weapon.missiles.Shuriken;
import com.qsr.customspd.items.weapon.missiles.darts.HealingDart;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.test.HeadlessGdx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port regression for the weapon hunks of upstream b6f7e7462 (v2.5.0, "made a bunch of code
 * tweaks so catalogs can appear out of game"; unchanged in v2.5.4) that
 * ItemBuffedLvlNoHeroPortTest and MissileDamageNoHeroPortTest do not cover: MeleeWeapon.buffedLvl
 * and info(), MissileWeapon.durabilityPerUse and info(), and TippedDart.durabilityPerUse. All of
 * them dereferenced Dungeon.hero, so a weapon could not be described or have its durability read
 * with no hero (a catalog shown outside a run).
 */
class WeaponNoHeroPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		Dungeon.hero = null;
	}

	@Test
	void meleeWeaponBuffedLvlWithNoHeroIsThePlainLevel() {
		Dungeon.hero = null;
		Sword sword = new Sword();
		sword.level(2);
		assertEquals(2, sword.buffedLvl(), "isEquipped(hero) / TWIN_UPGRADES are hero questions");
	}

	@Test
	void meleeWeaponInfoWithNoHeroSkipsTheHeroSpecificNotes() {
		Dungeon.hero = null;
		Sword sword = new Sword();

		String unknown = assertDoesNotThrow(sword::info);
		assertFalse(unknown.contains(Messages.get(com.qsr.customspd.items.weapon.melee.MeleeWeapon.class, "probably_too_heavy")));

		sword.identify();
		String known = assertDoesNotThrow(sword::info);
		assertTrue(known.contains(sword.desc()), "the description itself is still produced");
		assertFalse(known.contains(Messages.get(Weapon.class, "too_heavy")), "no hero, so no strength comparison");
	}

	@Test
	void missileWeaponDurabilityAndInfoWithNoHero() {
		Dungeon.hero = null;
		Shuriken shuriken = new Shuriken();

		// 5 base uses, level 0, no talent / ring modifiers: 100/5 plus the tiny rounding allowance
		assertEquals(20f, assertDoesNotThrow(shuriken::durabilityPerUse), 0.01f,
				"durabilityPerUse asks for DURABLE_PROJECTILES and the Sharpshooting ring");

		String info = assertDoesNotThrow(shuriken::info);
		assertFalse(info.contains(Messages.get(Weapon.class, "too_heavy")), "no hero, so no strength comparison");
	}

	@Test
	void tippedDartDurabilityWithNoHero() {
		Dungeon.hero = null;
		HealingDart dart = new HealingDart();
		float perUse = assertDoesNotThrow(dart::durabilityPerUse,
				"DURABLE_TIPS, Lotus preservation and the charged-shot bonus all read the hero");
		assertTrue(perUse >= 0f);
	}
}
