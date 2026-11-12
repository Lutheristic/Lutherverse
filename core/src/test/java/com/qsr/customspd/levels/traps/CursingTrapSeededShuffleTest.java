package com.qsr.customspd.levels.traps;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.items.armor.ClothArmor;
import com.qsr.customspd.items.weapon.melee.WornShortsword;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import com.watabou.utils.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A cursing trap curses one of the hero's worn items, shuffling the candidates to pick it. That
 * shuffle draws from Random's generator stack like every other in-run roll, so a pushed seeded
 * generator decides which item is cursed. Upstream uses Collections.shuffle, which has its own
 * generator that no seed reaches; this differs on purpose so seeded runs repeat.
 */
class CursingTrapSeededShuffleTest {

	private Hero prevHero;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@BeforeEach
	void setUp() {
		prevHero = Dungeon.hero;
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = prevHero;
	}

	@Test
	void theSameSeedCursesTheSameItem() {
		Set<String> picks = new LinkedHashSet<>();
		for (long seed = 1; seed <= 12; seed++) {
			String first = cursedItem(seed);
			for (int again = 0; again < 2; again++) {
				assertEquals(first, cursedItem(seed), "seed " + seed + " cursed a different item on a rerun");
			}
			picks.add(first);
		}
		assertTrue(picks.size() > 1, "every seed cursed the same item, so the pick may not follow the seed: " + picks);
	}

	/** A hero in an uncursed, unenchanted weapon and armour steps on the trap; returns what got cursed. */
	private static String cursedItem(long seed) {
		HeadlessLevel level = HeadlessLevel.install(8, 8);
		Hero hero = new Hero();
		StubSprite.attach(hero);
		Dungeon.hero = HeadlessLevel.at(hero, 3 * level.width() + 3);
		WornShortsword weapon = new WornShortsword();
		ClothArmor armor = new ClothArmor();
		hero.belongings.weapon = weapon;
		hero.belongings.armor = armor;

		Random.pushGenerator(seed);
		try {
			CursingTrap.curse(hero);
		} finally {
			Random.popGenerator();
		}

		assertTrue(weapon.cursed != armor.cursed, "the trap curses exactly one of the two");
		return weapon.cursed ? "weapon" : "armor";
	}
}
