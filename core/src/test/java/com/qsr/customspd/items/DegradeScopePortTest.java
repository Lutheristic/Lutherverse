package com.qsr.customspd.items;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.QuickSlot;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.buffs.Degrade;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.items.armor.LeatherArmor;
import com.qsr.customspd.items.rings.RingOfAccuracy;
import com.qsr.customspd.items.weapon.melee.Sword;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Port regression test for upstream SPD 1ec5434c1 (v2.1.3): "cleaned up logic for checking the
 * degradation debuff slightly".
 *
 * Degradation is a hero-only debuff. Item.buffedLvl used to apply it to ANY item and only Armor
 * and Weapon overrode it to ask "is this the hero's?"; every other item kind (rings, wands,
 * artifacts) read the hero's Degrade buff even when a mob or the floor held the item. The check
 * now lives in Item.buffedLvl, so the two overrides are gone and every item kind agrees.
 *
 * Degrade.reduceLevel(7) == 4, so a +7 item is distinguishable from a degraded one.
 */
class DegradeScopePortTest {

	private Hero prevHero;
	private QuickSlot prevQuickslot;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@BeforeEach
	void setUp() {
		prevHero = Dungeon.hero;
		prevQuickslot = Dungeon.quickslot;
		Dungeon.quickslot = new QuickSlot();
	}

	@AfterEach
	void tearDown() {
		Dungeon.hero = prevHero;
		Dungeon.quickslot = prevQuickslot;
	}

	private static Hero degradedHero() {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		Buff.affect(hero, Degrade.class, Degrade.DURATION);
		assertEquals(4, Degrade.reduceLevel(7), "test premise: degradation visibly lowers +7");
		return hero;
	}

	@Test
	void anUnownedRingIsNotDegraded() {
		degradedHero();
		RingOfAccuracy ring = new RingOfAccuracy();
		ring.level(7);
		assertEquals(7, ring.buffedLvl(), "a ring on the floor or in a mob's hand ignores the hero's Degrade");
	}

	@Test
	void anOwnedRingIsStillDegraded() {
		Hero hero = degradedHero();
		RingOfAccuracy ring = new RingOfAccuracy();
		ring.level(7);
		hero.belongings.backpack.items.add(ring);
		assertEquals(4, ring.buffedLvl(), "a ring in the hero's pack still feels the debuff");
	}

	@Test
	void unownedArmorAndWeaponAreNotDegraded() {
		degradedHero();
		LeatherArmor armor = new LeatherArmor();
		armor.level(7);
		Sword sword = new Sword();
		sword.level(7);
		assertEquals(7, armor.buffedLvl());
		assertEquals(7, sword.buffedLvl());
	}

	@Test
	void ownedArmorAndWeaponAreStillDegraded() {
		Hero hero = degradedHero();
		LeatherArmor armor = new LeatherArmor();
		armor.level(7);
		Sword sword = new Sword();
		sword.level(7);
		hero.belongings.backpack.items.add(armor);
		hero.belongings.backpack.items.add(sword);
		assertEquals(4, armor.buffedLvl());
		assertEquals(4, sword.buffedLvl());
	}
}
