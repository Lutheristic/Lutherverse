package com.qsr.customspd.items.weapon.melee;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Port regression for upstream 0c27cc8b6 (v2.3.0, "fixed lunge to empty space spending speed()
 * instead of 1/speed()").
 *
 * A lunge that finds no enemy where the hero lands still spends the hero's turn. The time cost
 * of a move-like action is 1/speed(): slow heroes spend more, fast heroes less. The code spent
 * speed() itself, so a half-speed hero (a crippled one) paid half a turn for a lunge into nothing.
 */
class RapierLungePortTest {

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
	void lungeIntoEmptySpaceSpendsOneOverSpeed() {
		HeadlessLevel level = HeadlessLevel.install(10, 10);
		int start = 4 * level.width() + 4;
		int target = 4 * level.width() + 6; // two cells away, nothing there

		// Hero.speed() casts its sprite to HeroSprite (needs GL textures), which the headless
		// fixture cannot build, so the half speed a Cripple would give is pinned directly.
		Hero hero = new Hero() {
			@Override
			public float speed() {
				return 0.5f;
			}
		};
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, start);
		Rapier rapier = new Rapier();
		hero.belongings.weapon = rapier;

		float before = hero.cooldown();
		rapier.duelistAbility(hero, target);

		assertEquals(start + 1, hero.pos, "fixture: the lunge landed one step toward the target");
		assertEquals(2f, hero.cooldown() - before, 0.0001f,
				"upstream 0c27cc8b6: the empty lunge costs 1/speed() = 2 turns for a half-speed hero, not 0.5");
	}
}
