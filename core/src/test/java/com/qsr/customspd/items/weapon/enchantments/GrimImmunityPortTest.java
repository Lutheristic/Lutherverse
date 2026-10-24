package com.qsr.customspd.items.weapon.enchantments;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.items.weapon.melee.WornShortsword;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Port regression for upstream a6221113c (v2.1.1, "fixed grim immunity not applying"). Grim's
 * proc deferred an execute-chance roll through a GrimTracker on the defender without asking
 * whether the defender is immune to Grim (bosses are, via Property.BOSS), so the tracker was
 * attached to an immune target. The proc now returns early for an immune defender.
 */
class GrimImmunityPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private Hero hero;
	private int cellNextToHero;

	private void setup() {
		HeadlessLevel level = HeadlessLevel.install(10, 10);
		hero = new Hero();
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, 4 * level.width() + 4);
		cellNextToHero = 4 * level.width() + 5;
	}

	@Test
	void aVulnerableDefenderGetsTheGrimTracker() {
		setup();
		Rat rat = HeadlessLevel.at(new Rat(), cellNextToHero);
		StubSprite.attach(rat);

		int out = new Grim().proc(new WornShortsword(), hero, rat, 7);

		assertEquals(7, out);
		assertNotNull(rat.buff(Grim.GrimTracker.class), "fixture: a normal defender is tracked");
	}

	@Test
	void anImmuneDefenderIsLeftAloneAndTheDamageUntouched() {
		setup();
		Rat rat = new Rat() {
			{
				immunities.add(Grim.class);
			}
		};
		HeadlessLevel.at(rat, cellNextToHero);
		StubSprite.attach(rat);

		int out = new Grim().proc(new WornShortsword(), hero, rat, 7);

		assertEquals(7, out);
		assertNull(rat.buff(Grim.GrimTracker.class),
				"upstream a6221113c: no execute roll is queued against a Grim-immune defender");
	}
}
