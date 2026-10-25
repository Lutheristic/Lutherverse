package com.qsr.customspd.items.weapon.melee;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.hero.HeroClass;
import com.qsr.customspd.actors.hero.Talent;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Port regression for upstream b34c0af73 (v4.0.0): a lunge that lands with nothing to hit
 * wastes the charge, and must also clear belongings.abilityWeapon, which it set early to check
 * canAttack. Left set, the next ordinary attack is treated as an ability attack.
 * Katana lunges through the same Rapier.lungeAbility.
 */
class LungeAbilityWeaponPortTest {

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
	void rapierLungeWithNoTargetClearsAbilityWeapon() {
		lungeAtEmptyCell(new Rapier());
	}

	@Test
	void katanaLungeWithNoTargetClearsAbilityWeapon() {
		lungeAtEmptyCell(new Katana());
	}

	private static void lungeAtEmptyCell(MeleeWeapon wep) {
		int w = 10;
		HeadlessLevel.install(w, 10);
		// Hero.speed() casts its sprite to HeroSprite (needs GL), so it is pinned as in
		// RapierLungePortTest.
		Hero hero = new Hero() {
			@Override
			public float speed() {
				return 1f;
			}
		};
		hero.heroClass = HeroClass.DUELIST;
		Talent.initClassTalents(hero);
		Dungeon.hero = hero;
		int start = 4 * w + 2;
		HeadlessLevel.at(hero, start);
		StubSprite.attach(hero);
		hero.belongings.weapon = wep;

		// Out of view and empty: the duelist may lunge there, but finds nothing to attack.
		int target = 4 * w + 4;
		Rapier.lungeAbility(hero, target, 1, 0, wep);

		assertEquals(4 * w + 3, hero.pos, "the hero lunges next to the target cell");
		assertNull(hero.belongings.abilityWeapon, "a lunge with no attack must not leave abilityWeapon set");
	}
}
