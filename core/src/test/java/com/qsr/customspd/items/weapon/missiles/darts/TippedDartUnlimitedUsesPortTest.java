package com.qsr.customspd.items.weapon.missiles.darts;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.hero.HeroClass;
import com.qsr.customspd.actors.hero.Talent;
import com.qsr.customspd.items.rings.RingOfSharpshooting;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Port regression for the TippedDart hunk of upstream c1fabfbd8 (v2.4.0, "fixed a few bugs when
 * tipped darts get high durability"). Once a sharpshooting ring pushes a dart past 100 uses,
 * MissileWeapon says "lasts forever" (0 durability per use). TippedDart, which since f9e6fbc94
 * re-derives the use count from that figure, turned the 0 into an infinite use count and
 * returned a tiny non-zero cost instead of 0, so the dart was no longer unlimited. It now keeps
 * the same "100 uses is forever" rule at the end.
 */
class TippedDartUnlimitedUsesPortTest {

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
	void aTippedDartPast100UsesCostsNothingToThrow() {
		HeadlessLevel level = HeadlessLevel.install(10, 10);
		Hero hero = new Hero();
		hero.heroClass = HeroClass.HUNTRESS;
		Talent.initClassTalents(hero);
		Dungeon.hero = hero;
		HeadlessLevel.at(hero, 4 * level.width() + 4);

		RingOfSharpshooting ring = new RingOfSharpshooting();
		ring.level(25); // +26 bonus: 1.2^26 is about 114 uses
		ring.activate(hero);

		assertEquals(0f, new PoisonDart().durabilityPerUse(), 0f,
				"upstream c1fabfbd8: at 100 uses a tipped dart lasts forever");
		assertEquals(0f, new Dart().durabilityPerUse(), 0f, "control: a plain dart already did");
	}
}
