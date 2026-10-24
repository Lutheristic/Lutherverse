package com.qsr.customspd.items.armor;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Char;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.items.armor.glyphs.Swiftness;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Port regression for upstream 2f5531339 (v2.4.1, "fixed swiftness glyph deactivating around
 * neutral mobs"). Swiftness only speeds the wearer while no enemy is close; a NEUTRAL mob (a
 * hidden mimic, a sleeping-alignment NPC) is not an enemy, but any alignment different from the
 * wearer's counted, so a bystander next to the hero switched the glyph off.
 *
 * Upstream's hunk edits the distance check that 5b959f315 introduced; CPDU's loop was the
 * older PathFinder-based one when this was first ported, so the alignment clause was added to
 * it, and 5b959f315 (see SwiftnessAdjacentPortTest) has since replaced that loop.
 */
class SwiftnessNeutralPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private float speedWithMobNextToHero(Char.Alignment alignment) {
		HeadlessLevel level = HeadlessLevel.install(10, 10);
		int center = 4 * level.width() + 4;

		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeadlessLevel.at(hero, center);

		ClothArmor armor = new ClothArmor();
		armor.inscribe(new Swiftness());
		hero.belongings.armor = armor;

		Rat rat = HeadlessLevel.at(new Rat(), center + 1);
		rat.alignment = alignment;

		return armor.speedFactor(hero, 1f);
	}

	@Test
	void anAdjacentEnemyStillSwitchesSwiftnessOff() {
		assertEquals(1f, speedWithMobNextToHero(Char.Alignment.ENEMY), 0.0001f);
	}

	@Test
	void anAdjacentNeutralMobDoesNot() {
		// +0 swiftness: 1.2x speed
		assertEquals(1.2f, speedWithMobNextToHero(Char.Alignment.NEUTRAL), 0.0001f);
	}
}
