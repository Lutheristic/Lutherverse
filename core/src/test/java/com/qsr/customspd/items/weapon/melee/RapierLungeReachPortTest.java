package com.qsr.customspd.items.weapon.melee;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.items.weapon.enchantments.Projecting;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port regression for upstream b9d54e3f6 (v2.3.0, "fixed rapier lunge not always using rapier
 * when checking range"). After landing, the lunge asks hero.canAttack(enemy), which measures
 * reach with belongings.attackingWeapon(). That is the weapon in the hero's hands unless an
 * ability weapon is set, and the lunge only set it later, inside the attack callback. So a lunge
 * made with a weapon other than the wielded one (a secondary weapon; here a Rapier with extra
 * reach while a plain sword is wielded) landed within the lunging weapon's reach but was judged
 * against the wielded weapon's, found the enemy unreachable and wasted the ability.
 */
class RapierLungeReachPortTest {

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
	void theLungeJudgesTheEnemyReachableWithTheLungingWeapon() {
		HeadlessLevel level = HeadlessLevel.install(10, 10);
		// Hero.speed() casts its sprite to HeroSprite, which the headless fixture cannot build; a
		// lunge that fails to attack spends 1/speed(), so pin it.
		Hero hero = new Hero() {
			@Override
			public float speed() {
				return 1f;
			}
		};
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		int start = 4 * level.width() + 2;
		HeadlessLevel.at(hero, start);
		int enemyCell = 4 * level.width() + 5; // 3 away: 2 after the lunge, out of a sword's reach
		Rat rat = HeadlessLevel.at(new Rat(), enemyCell);
		StubSprite.attach(rat);
		rat.HP = rat.HT = 1000;

		hero.belongings.weapon = new WornShortsword(); // the wielded weapon: reach 1
		Rapier rapier = new Rapier();
		rapier.enchant(new Projecting()); // the lunging weapon: reach 2
		assertEquals(1, hero.belongings.weapon.reachFactor(hero), "fixture: wielded reach");
		assertEquals(2, rapier.reachFactor(hero), "fixture: lunging weapon reach");

		try {
			Rapier.lungeAbility(hero, enemyCell, 1f, 0, rapier);
		} catch (NullPointerException e) {
			// A lunge that attacks reaches AttackIndicator.target, a HUD element the headless run
			// does not have. Only that NPE is tolerated; one thrown anywhere else is a real failure.
			StackTraceElement top = e.getStackTrace()[0];
			if (!top.getClassName().endsWith(".AttackIndicator") || !"target".equals(top.getMethodName())) {
				throw e;
			}
		}

		assertEquals(start + 1, hero.pos, "fixture: the hero lunged one cell towards the rat");
		assertTrue(StubSprite.of(hero).attacks.contains(enemyCell),
				"upstream b9d54e3f6: the hero attacks the rat, reach measured with the rapier");
	}
}
