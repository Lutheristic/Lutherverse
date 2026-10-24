package com.qsr.customspd.items.armor;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.hero.HeroClass;
import com.qsr.customspd.actors.hero.Talent;
import com.qsr.customspd.test.HeadlessGdx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port regression for upstream 8f1fc51a5 (v2.4.0, "fixed king's crown automatically IDing armor").
 * Upgrading armor into class armor identified it unconditionally (and ClassArmor.isIdentified()
 * always answered true), so the crown leaked the level and curse state of armor the player had
 * never identified. The class armor now inherits what the source armor's owner knew.
 * The transfer branch of ClassArmor.execute lives in an anonymous WndBag selector and is not
 * headlessly drivable; upgrade() carries the same logic and is what is pinned here.
 */
class ClassArmorIdentifyPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		Dungeon.hero = null;
	}

	private static Hero warrior() {
		Hero hero = new Hero();
		hero.heroClass = HeroClass.WARRIOR;
		Talent.initClassTalents(hero);
		Dungeon.hero = hero;
		return hero;
	}

	@Test
	void upgradingUnidentifiedArmorDoesNotIdentifyIt() {
		Hero hero = warrior();
		ClothArmor armor = new ClothArmor();
		armor.levelKnown = false;
		armor.cursedKnown = false;

		ClassArmor classArmor = ClassArmor.upgrade(hero, armor);

		assertFalse(classArmor.levelKnown, "upstream 8f1fc51a5: the crown must not reveal the level");
		assertFalse(classArmor.isIdentified());
		assertTrue(classArmor.cursedKnown, "curse state stays known: class armor is never worn blind");
	}

	@Test
	void upgradingArmorWithAKnownLevelButUnknownCurseKeepsTheLevelKnown() {
		Hero hero = warrior();
		ClothArmor armor = new ClothArmor();
		armor.levelKnown = true;
		armor.cursedKnown = false;

		ClassArmor classArmor = ClassArmor.upgrade(hero, armor);

		assertTrue(classArmor.levelKnown);
		assertTrue(classArmor.cursedKnown);
	}

	@Test
	void upgradingIdentifiedArmorIdentifiesTheClassArmor() {
		Hero hero = warrior();
		ClothArmor armor = new ClothArmor();
		armor.identify();

		ClassArmor classArmor = ClassArmor.upgrade(hero, armor);

		assertTrue(classArmor.isIdentified(), "control: known armor stays known");
	}
}
