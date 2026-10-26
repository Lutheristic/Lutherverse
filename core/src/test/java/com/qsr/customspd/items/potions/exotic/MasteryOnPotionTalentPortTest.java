package com.qsr.customspd.items.potions.exotic;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.buffs.Barrier;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.hero.HeroClass;
import com.qsr.customspd.actors.hero.Talent;
import com.qsr.customspd.items.Item;
import com.qsr.customspd.items.potions.Potion;
import com.qsr.customspd.items.weapon.melee.Sword;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 061d59c61 (v2.2.0): potions of mastery, divine inspiration and dragon's breath override
 * drink() and never reach Potion.drink, so they did not trigger the on-potion talents. Each now
 * calls Talent.onPotionUsed itself once the potion takes effect. Mastery counts double
 * (talentFactor 2). Its effect lands in the item selector, which this drives directly; the
 * selector window itself needs a scene. Divine inspiration's tier window is built inside drink()
 * and handed to GameScene.show, which drops it when there is no scene, and dragon's breath calls
 * the talents from a bolt animation callback, so neither call is covered here.
 */
class MasteryOnPotionTalentPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() throws Exception {
		curUser().set(null, null);
		staticField("curItem").set(null, null);
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private static Field curUser() throws Exception {
		return staticField("curUser");
	}

	private static Field staticField(String name) throws Exception {
		Field f = Item.class.getDeclaredField(name);
		f.setAccessible(true);
		return f;
	}

	/** As drink() leaves things for a known potion: the potion still in the pack, chosen as curItem. */
	private static PotionOfMastery carried(Hero hero, PotionOfMastery potion) throws Exception {
		potion.collect(hero.belongings.backpack);
		staticField("curItem").set(null, potion);
		return potion;
	}

	private static Hero mage() throws Exception {
		HeadlessLevel level = HeadlessLevel.install(8, 8);
		Hero hero = new Hero();
		hero.heroClass = HeroClass.MAGE;
		Talent.initClassTalents(hero);
		hero.HP = hero.HT = 100;
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, 3 * level.width() + 3);
		curUser().set(null, hero);
		Potion.initColors();
		return hero;
	}

	@Test
	void masteryTriggersTheOnPotionTalentsAtDoubleStrength() throws Exception {
		Hero hero = mage();
		hero.talents.get(1).put(Talent.LIQUID_WILLPOWER, 1);
		Sword sword = new Sword();

		carried(hero, new PotionOfMastery()).itemSelector.onSelect(sword);

		assertTrue(sword.masteryPotionBonus, "fixture: the potion took effect");
		Barrier barrier = hero.buff(Barrier.class);
		// 5% of max HP at +1, doubled for mastery
		assertEquals(10, barrier == null ? 0 : barrier.shielding());
	}

	@Test
	void anAnonymousMasteryTriggersNothing() throws Exception {
		Hero hero = mage();
		hero.talents.get(1).put(Talent.LIQUID_WILLPOWER, 1);

		PotionOfMastery potion = new PotionOfMastery();
		potion.anonymize();
		carried(hero, potion).itemSelector.onSelect(new Sword());

		assertNull(hero.buff(Barrier.class));
	}
}
