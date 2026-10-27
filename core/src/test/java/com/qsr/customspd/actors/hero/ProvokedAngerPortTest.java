package com.qsr.customspd.actors.hero;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import com.watabou.utils.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Upstream 753b340f0 (v2.4.2): provoked anger adds a flat 2 or 3 damage (1 + points) to the
 * warrior's next attack after his shield breaks, where it used to roll 1-2 at +1 and 2 at +2.
 */
class ProvokedAngerPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private static Hero warrior(int points) {
		HeadlessLevel level = HeadlessLevel.install(8, 8);
		Hero hero = new Hero();
		hero.heroClass = HeroClass.WARRIOR;
		Talent.initClassTalents(hero);
		hero.talents.get(0).put(Talent.PROVOKED_ANGER, points);
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, 3 * level.width() + 3);
		return hero;
	}

	/** The damage of one attack of 10 made right after the shield broke. */
	private static int provokedAttack(Hero hero) {
		Buff.affect(hero, Talent.ProvokedAngerTracker.class, 5f);
		int dmg = Talent.onAttackProc(hero, new Rat(), 10);
		assertNull(hero.buff(Talent.ProvokedAngerTracker.class), "the bonus is spent by the attack");
		return dmg;
	}

	@Test
	void plusOneAlwaysAddsTwo() {
		Hero hero = warrior(1);
		Random.pushGenerator(753);
		try {
			for (int i = 0; i < 50; i++) {
				assertEquals(12, provokedAttack(hero), "attack " + i);
			}
		} finally {
			Random.popGenerator();
		}
	}

	@Test
	void plusTwoAddsThree() {
		assertEquals(13, provokedAttack(warrior(2)));
	}

	@Test
	void withoutTheBuffNothingIsAdded() {
		assertEquals(10, Talent.onAttackProc(warrior(2), new Rat(), 10));
	}
}
