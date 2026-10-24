package com.qsr.customspd.actors.hero.abilities.rogue;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.hero.HeroClass;
import com.qsr.customspd.actors.hero.Talent;
import com.qsr.customspd.items.armor.ClassArmor;
import com.qsr.customspd.items.armor.ClothArmor;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Port regression for upstream e4b704e6a (v2.2.1, "fixed various blink effects working over
 * magical fire"). Smoke bomb measured its blink range over every non-solid cell, but a cell
 * held by eternal fire is non-solid and also not passable and not avoid-flagged (the fire clears
 * Level.passable, see MagicalFireRoom.EternalFire), so the rogue could blink straight across a
 * wall of magical fire. The range map now walks passable-or-avoid cells only.
 */
class SmokeBombBlinkPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	@Test
	void smokeBombCannotBlinkAcrossAWallOfMagicalFire() {
		int w = 10;
		HeadlessLevel level = HeadlessLevel.install(w, 10);
		Hero hero = new Hero();
		hero.heroClass = HeroClass.ROGUE;
		Talent.initClassTalents(hero);
		Dungeon.hero = hero;
		int start = 4 * w + 2;
		HeadlessLevel.at(hero, start);

		// A full-height column of eternal-fire cells: terrain stays EMPTY (not solid, not avoid)
		// but the blob has cleared passable, exactly as EternalFire.evolve does.
		for (int y = 1; y < 9; y++) {
			level.passable[y * w + 4] = false;
		}
		int target = 4 * w + 6;
		level.heroFOV[target] = true;

		ClassArmor armor = ClassArmor.upgrade(hero, new ClothArmor());
		float chargeBefore = armor.charge;

		new SmokeBomb().activate(armor, hero, target);

		assertEquals(start, hero.pos, "the blink is refused: the target is only reachable across the fire");
		assertEquals(chargeBefore, armor.charge, 0f, "a refused blink costs no armor charge");
	}
}
