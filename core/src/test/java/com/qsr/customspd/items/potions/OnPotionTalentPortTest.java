package com.qsr.customspd.items.potions;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.buffs.Barrier;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.hero.HeroClass;
import com.qsr.customspd.actors.hero.Talent;
import com.qsr.customspd.items.Item;
import com.qsr.customspd.items.potions.exotic.PotionOfDivineInspiration;
import com.qsr.customspd.items.potions.exotic.PotionOfMastery;
import com.qsr.customspd.levels.Terrain;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import com.watabou.utils.PathFinder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Upstream 33b065e69 (v2.2.0) reworked the T2 on-potion talents. They used to fire only from
 * potions of healing and the items built on them; now Potion.drink and Potion.onThrow fire them
 * for any potion that is not anonymous, scaled by the potion's talentFactor, which is 2 for
 * strength, experience, mastery and divine inspiration. A thrown potion works around the cell it
 * lands on. b7051a219 made liquid agility's accuracy half follow the same factor.
 */
class OnPotionTalentPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() throws Exception {
		curUser().set(null, null);
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private static Field curUser() throws Exception {
		Field f = Item.class.getDeclaredField("curUser");
		f.setAccessible(true);
		return f;
	}

	private HeadlessLevel level;

	/** A mage (so liquid willpower takes its barrier branch) with 100 HP, standing at (2,2). */
	private Hero mage(int size) throws Exception {
		level = HeadlessLevel.install(size, size);
		Hero hero = new Hero();
		hero.heroClass = HeroClass.MAGE;
		Talent.initClassTalents(hero);
		hero.HP = hero.HT = 100;
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, 2 * level.width() + 2);
		curUser().set(null, hero);
		Potion.initColors();
		return hero;
	}

	private static int shieldingOf(Hero hero) {
		Barrier barrier = hero.buff(Barrier.class);
		return barrier == null ? 0 : barrier.shielding();
	}

	@Test
	void drinkingAPotionOtherThanHealingTriggersTheTalents() throws Exception {
		Hero hero = mage(10);
		hero.talents.get(1).put(Talent.LIQUID_WILLPOWER, 2);

		PotionOfLevitation potion = new PotionOfLevitation();
		potion.collect(hero.belongings.backpack);
		potion.drink(hero);

		// 7.5% of max HP at +2 for a hero other than the warrior
		assertEquals(8, shieldingOf(hero));
	}

	@Test
	void drinkingAPotionOfHealingTriggersThemOnce() throws Exception {
		Hero hero = mage(10);
		hero.talents.get(1).put(Talent.LIQUID_WILLPOWER, 2);
		StubSprite sprite = StubSprite.of(hero);

		PotionOfHealing potion = new PotionOfHealing();
		potion.collect(hero.belongings.backpack);
		potion.drink(hero);

		int shieldTexts = 0;
		for (StubSprite.Status status : sprite.statuses) {
			if (status.text.equals("8")) shieldTexts++;
		}
		assertEquals(1, shieldTexts, "healing's own trigger went away with the move to drink: " + sprite.statuses);
	}

	@Test
	void anAnonymousPotionTriggersNothing() throws Exception {
		Hero hero = mage(10);
		hero.talents.get(1).put(Talent.LIQUID_WILLPOWER, 2);

		PotionOfLevitation potion = new PotionOfLevitation();
		potion.anonymize();
		potion.collect(hero.belongings.backpack);
		potion.drink(hero);

		assertNull(hero.buff(Barrier.class));
	}

	@Test
	void strengthExperienceMasteryAndDivineInspirationCountDouble() {
		assertEquals(2f, new PotionOfStrength().talentFactor);
		assertEquals(2f, new PotionOfExperience().talentFactor);
		assertEquals(2f, new PotionOfMastery().talentFactor);
		assertEquals(2f, new PotionOfDivineInspiration().talentFactor);
		assertEquals(1f, new PotionOfLevitation().talentFactor, "control: other potions count once");
		assertEquals(1f, new PotionOfHealing().talentFactor, "control: other potions count once");
	}

	@Test
	void theTalentFactorScalesTheShield() throws Exception {
		Hero hero = mage(10);
		hero.talents.get(1).put(Talent.LIQUID_WILLPOWER, 1);

		Talent.onPotionUsed(hero, hero.pos, 2f);

		// 5% of max HP at +1, doubled
		assertEquals(10, shieldingOf(hero));
	}

	@Test
	void liquidAgilityFollowsTheTalentFactor() throws Exception {
		Hero hero = mage(10);
		hero.talents.get(1).put(Talent.LIQUID_AGILITY, 1);

		Talent.onPotionUsed(hero, hero.pos, 2f);

		assertNotNull(hero.buff(Talent.LiquidAgilEVATracker.class));
		assertEquals(1f, hero.buff(Talent.LiquidAgilEVATracker.class).cooldown(), 0.0001f,
				"the evasion bonus lasts one extra turn");
		assertEquals(2, hero.buff(Talent.LiquidAgilACCTracker.class).uses,
				"and the accuracy bonus covers one extra attack");
	}

	@Test
	void aThrownPotionTriggersAroundTheCellItLandsOn() throws Exception {
		Hero hero = mage(12);
		hero.talents.get(1).put(Talent.LIQUID_NATURE, 2);
		int target = 8 * level.width() + 8;

		try {
			new PotionOfToxicGas().onThrow(target);
		} catch (NullPointerException e) {
			// each grass tile bursts leaves through CellEmitter.get, which needs a scene; the first
			// tile is already set by then, and the tile is all this test reads
			StackTraceElement top = e.getStackTrace()[0];
			if (!top.getClassName().endsWith(".CellEmitter") || !"get".equals(top.getMethodName())) {
				throw e;
			}
		}

		int nearTarget = 0;
		int nearHero = 0;
		for (int i : PathFinder.NEIGHBOURS9) {
			if (level.map[target + i] == Terrain.GRASS) nearTarget++;
			if (level.map[hero.pos + i] == Terrain.GRASS) nearHero++;
		}
		assertEquals(1, nearTarget, "liquid nature sprouts grass where the potion landed");
		assertEquals(0, nearHero, "not around the thrower");
	}
}
