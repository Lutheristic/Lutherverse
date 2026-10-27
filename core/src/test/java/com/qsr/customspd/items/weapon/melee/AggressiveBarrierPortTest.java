package com.qsr.customspd.items.weapon.melee;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.buffs.Barrier;
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

/**
 * Upstream 059e11cfa (v2.4.0): aggressive barrier works at or below half health at both talent
 * levels and its shield grows instead, 3 at +1 and 5 at +2 (1 + 2 per point). It used to need
 * less than 40% or 60% health and always gave 3.
 */
class AggressiveBarrierPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private static Hero duelist(int hp, int points) {
		HeadlessLevel level = HeadlessLevel.install(8, 8);
		Hero hero = new Hero();
		hero.heroClass = HeroClass.DUELIST;
		Talent.initClassTalents(hero);
		hero.HT = 100;
		hero.HP = hp;
		if (points > 0) hero.talents.get(0).put(Talent.AGGRESSIVE_BARRIER, points);
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, 3 * level.width() + 3);
		return hero;
	}

	/** Uses a weapon ability and returns the shield it left. */
	private static int shieldFromAnAbility(Hero hero) {
		new Sword().beforeAbilityUsed(hero, null);
		Barrier barrier = hero.buff(Barrier.class);
		return barrier == null ? 0 : barrier.shielding();
	}

	@Test
	void atHalfHealthPlusOneGivesThree() {
		Hero hero = duelist(50, 1);
		assertEquals(3, shieldFromAnAbility(hero));
		assertEquals("3", StubSprite.of(hero).statuses.get(0).text);
	}

	@Test
	void atHalfHealthPlusTwoGivesFive() {
		Hero hero = duelist(50, 2);
		assertEquals(5, shieldFromAnAbility(hero));
		assertEquals("5", StubSprite.of(hero).statuses.get(0).text);
	}

	@Test
	void aboveHalfHealthGivesNothingEvenAtPlusTwo() {
		assertEquals(0, shieldFromAnAbility(duelist(51, 2)));
	}

	@Test
	void lowHealthPlusOneStillGivesThree() {
		assertEquals(3, shieldFromAnAbility(duelist(10, 1)));
	}

	@Test
	void withoutTheTalentNothingHappens() {
		assertEquals(0, shieldFromAnAbility(duelist(10, 0)));
	}
}
