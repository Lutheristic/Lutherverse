package com.qsr.customspd.actors.hero;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.abilities.ArmorAbility;
import com.qsr.customspd.actors.hero.abilities.Ratmogrify;
import com.qsr.customspd.actors.hero.abilities.warrior.Endure;
import com.qsr.customspd.actors.hero.abilities.warrior.HeroicLeap;
import com.qsr.customspd.actors.hero.abilities.warrior.Shockwave;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessRun;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream f5531fd65 (v2.5.4) took out debug code that offered the Warrior Ratmogrify in place of
 * Endure. A Warrior who already picked Ratmogrify keeps it: the save stores the chosen ability
 * itself, not a slot in the class's list.
 */
class WarriorArmorAbilitiesPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@Test
	void theWarriorChoosesFromHeroicLeapShockwaveAndEndure() {
		List<Class<?>> offered = new ArrayList<>();
		for (ArmorAbility ability : HeroClass.WARRIOR.armorAbilities()) offered.add(ability.getClass());
		assertEquals(Arrays.<Class<?>>asList(HeroicLeap.class, Shockwave.class, Endure.class), offered);
	}

	/**
	 * Guards save loading, not the list change: it passes with the old list too, and fails if
	 * restoring a hero re-derives the armor ability from the class list.
	 */
	@Test
	void aWarriorWhoPickedRatmogrifyKeepsItThroughASave() {
		try (HeadlessRun run = HeadlessRun.start(20241009L)) {
			run.enterFirstLevel();
			Hero hero = Dungeon.hero;
			assertEquals(HeroClass.WARRIOR, hero.heroClass, "test setup");
			hero.armorAbility = new Ratmogrify();
			Talent.initArmorTalents(hero);
			hero.upgradeTalent(Talent.RATSISTANCE);
			assertEquals(1, hero.pointsInTalent(Talent.RATSISTANCE), "test setup");

			run.saveAll();
			Dungeon.hero = null;
			run.loadGame();

			assertNotNull(Dungeon.hero, "the hero is rebuilt from the save");
			assertTrue(Dungeon.hero.armorAbility instanceof Ratmogrify,
					"the saved ability comes back, got " + Dungeon.hero.armorAbility);
			assertTrue(Dungeon.hero.talents.get(3).containsKey(Talent.RATFORCEMENTS), "Ratmogrify's talents stay in tier 4");
			assertEquals(1, Dungeon.hero.pointsInTalent(Talent.RATSISTANCE), "and keep their points");
		}
	}
}
