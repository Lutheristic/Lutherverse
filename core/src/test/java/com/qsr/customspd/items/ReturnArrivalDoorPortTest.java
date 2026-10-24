package com.qsr.customspd.items;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.items.scrolls.ScrollOfTeleportation;
import com.qsr.customspd.levels.Terrain;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import com.watabou.noosa.Group;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Port regression test for upstream SPD 175a344fb (v2.5.4): "fixed beacon of returning placing
 * hero into closed doors".
 *
 * Arriving by beacon of returning (or resurrection) places the hero with the scene's
 * ScrollOfTeleportation.appearVFX, which only played effects. A hero standing on a door cell
 * left it as a closed DOOR tile, because nothing "pressed" the cell the way a normal move or
 * teleport (appear + occupyCell) does. appearVFX now re-occupies the cell.
 */
class ReturnArrivalDoorPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	private Hero prevHero;

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = prevHero;
	}

	@Test
	void arrivingOnADoorOpensIt() {
		prevHero = Dungeon.hero;
		HeadlessLevel level = HeadlessLevel.install(8, 8);
		int door = 3 * level.width() + 3;
		setTerrain(level, door, Terrain.DOOR);

		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeadlessLevel.at(hero, door);
		StubSprite sprite = StubSprite.attach(hero);
		sprite.parent = new Group(); // appearVFX fades the sprite in via a tweener added to its parent

		assertEquals(Terrain.DOOR, level.map[door], "precondition: the arrival cell is a closed door");

		ScrollOfTeleportation.appearVFX(hero);

		assertEquals(Terrain.OPEN_DOOR, level.map[door],
				"a hero placed on a door by beacon of returning must open it, not stand in a closed door");
	}

	private static void setTerrain(HeadlessLevel level, int cell, int terrain) {
		level.map[cell] = terrain;
		level.buildFlagMaps();
	}
}
