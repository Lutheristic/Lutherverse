package com.qsr.customspd.items;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.hero.HeroClass;
import com.qsr.customspd.actors.hero.Talent;
import com.qsr.customspd.items.weapon.melee.Dagger;
import com.qsr.customspd.items.weapon.melee.Sword;
import com.qsr.customspd.items.weapon.melee.WornShortsword;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port regression for upstream c9a3819d5 (v2.4.0, "improved code relating to swift equip and
 * equip times generally"). Swift Equip used to refund an equip with a negative spend
 * (spendAndNext(-time2equip)) after the normal one, so equipping into an empty hand, where
 * there is no unequip to cancel against, handed the hero a whole turn of credit. Swift equipping
 * now just makes the equip cost nothing (timeToEquip is 0 while the flag is up), unequip of the
 * old weapon included.
 */
class SwiftEquipTimePortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private static Hero duelist(int swiftEquipPoints) {
		HeadlessLevel level = HeadlessLevel.install(10, 10);
		Hero hero = new Hero();
		hero.heroClass = HeroClass.DUELIST;
		Talent.initClassTalents(hero);
		if (swiftEquipPoints > 0) {
			hero.talents.get(0).put(Talent.SWIFT_EQUIP, swiftEquipPoints);
		}
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, 4 * level.width() + 4);
		return hero;
	}

	/** Puts the weapon in the pack, equips it from there, returns the time the hero spent. */
	private static float equipFromPack(Hero hero, KindOfWeapon weapon) {
		weapon.collect(hero.belongings.backpack);
		float before = hero.cooldown();
		assertTrue(weapon.doEquip(hero), "fixture: equip succeeds");
		return hero.cooldown() - before;
	}

	@Test
	void withoutTheTalentAnEquipIntoAnEmptyHandCostsOneTurn() {
		Hero hero = duelist(0);
		assertEquals(1f, equipFromPack(hero, new Sword()), 0.0001f);
	}

	@Test
	void swiftEquipIntoAnEmptyHandIsFreeNotACreditOfATurn() {
		Hero hero = duelist(2);
		assertEquals(0f, equipFromPack(hero, new Sword()), 0.0001f,
				"upstream c9a3819d5: swift equip costs nothing; it no longer spends -1");
		assertNotNull(hero.buff(Talent.SwiftEquipCooldown.class));
	}

	@Test
	void swiftEquipSwappingAWieldedWeaponIsFreeEndToEnd() {
		Hero hero = duelist(2);
		hero.belongings.weapon = new WornShortsword();
		assertEquals(0f, equipFromPack(hero, new Sword()), 0.0001f,
				"the unequip of the old weapon and the equip of the new one are both free");
		assertTrue(hero.belongings.weapon instanceof Sword);
	}

	@Test
	void theSecondSwiftEquipNeedsTwoPointsAndTheThirdIsPaidInFull() {
		Hero hero = duelist(2);
		assertEquals(0f, equipFromPack(hero, new Sword()), 0.0001f, "first, free");
		assertEquals(0f, equipFromPack(hero, new Dagger()), 0.0001f, "second use, free");
		assertEquals(2f, equipFromPack(hero, new WornShortsword()), 0.0001f,
				"cooldown spent: unequip 1 + equip 1, as without the talent");
	}

	@Test
	void oneTalentPointGivesOnlyOneSwiftEquip() {
		Hero hero = duelist(1);
		assertEquals(0f, equipFromPack(hero, new Sword()), 0.0001f);
		assertEquals(2f, equipFromPack(hero, new Dagger()), 0.0001f);
	}
}
