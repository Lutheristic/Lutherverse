package com.qsr.customspd.items.armor;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Char;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.items.armor.glyphs.Swiftness;
import com.qsr.customspd.levels.Terrain;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Port regression for upstream 5b959f315 (v2.4.0, "improved 'adjacent enemy' logic for
 * swiftness"). Swiftness switches off while an enemy is close. The old test asked "is the enemy
 * reachable by a walk of two steps over passable cells", which ignores an enemy standing on
 * terrain the hero cannot walk on (a flier over a chasm) even when it is right next to the hero.
 * The new test is geometric: adjacent counts, and two tiles away counts when an open cell
 * adjacent to both lets the enemy close in.
 */
class SwiftnessAdjacentPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private HeadlessLevel level;
	private ClothArmor armor;
	private Hero hero;
	private int center;

	private void setUp() {
		level = HeadlessLevel.install(10, 10);
		center = 4 * level.width() + 4;
		hero = new Hero();
		Dungeon.hero = hero;
		HeadlessLevel.at(hero, center);
		armor = new ClothArmor();
		armor.inscribe(new Swiftness());
		hero.belongings.armor = armor;
	}

	private Rat enemyAt(int cell) {
		Rat rat = HeadlessLevel.at(new Rat(), cell);
		rat.alignment = Char.Alignment.ENEMY;
		return rat;
	}

	@Test
	void anAdjacentEnemyOverAChasmSwitchesSwiftnessOff() {
		setUp();
		level.map[center + 1] = Terrain.CHASM;
		level.buildFlagMaps();
		enemyAt(center + 1);
		assertEquals(1f, armor.speedFactor(hero, 1f), 0.0001f,
				"upstream 5b959f315: adjacency is geometric, not 'reachable over passable cells'");
	}

	@Test
	void anEnemyTwoTilesAwayWithAnOpenCellBetweenSwitchesSwiftnessOff() {
		setUp();
		enemyAt(center + 2);
		assertEquals(1f, armor.speedFactor(hero, 1f), 0.0001f);
	}

	@Test
	void anEnemyTwoTilesAwayBehindSolidCellsDoesNot() {
		setUp();
		level.map[center + 1 - level.width()] = Terrain.WALL;
		level.map[center + 1] = Terrain.WALL;
		level.map[center + 1 + level.width()] = Terrain.WALL;
		level.buildFlagMaps();
		enemyAt(center + 2);
		assertEquals(1.2f, armor.speedFactor(hero, 1f), 0.0001f);
	}

	@Test
	void anEnemyThreeTilesAwayDoesNot() {
		setUp();
		enemyAt(center + 3);
		assertEquals(1.2f, armor.speedFactor(hero, 1f), 0.0001f);
	}
}
