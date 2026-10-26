package com.qsr.customspd.actors.buffs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.items.weapon.melee.RoundShield;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Upstream 11a410f07 (v2.4.0) let the Duelist's guard block any number of attacks for its whole
 * duration, marking it once it has blocked, and ends a guard that has blocked when the hero
 * attacks, through Invisibility.dispel. Without that last part a guard that had blocked kept the
 * hero at infinite evasion while she attacked freely.
 */
class GuardDispelPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private static Hero guardingHero(boolean hasBlocked) {
		HeadlessLevel level = HeadlessLevel.install(10, 10);
		Hero hero = new Hero();
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, 4 * level.width() + 4);
		Buff.prolong(hero, RoundShield.GuardTracker.class, 5f).hasBlocked = hasBlocked;
		return hero;
	}

	@Test
	void attackingEndsAGuardThatHasBlocked() {
		Hero hero = guardingHero(true);

		Invisibility.dispel(hero);

		assertNull(hero.buff(RoundShield.GuardTracker.class));
	}

	@Test
	void attackingKeepsAGuardThatHasNotBlockedYet() {
		Hero hero = guardingHero(false);

		Invisibility.dispel(hero);

		assertNotNull(hero.buff(RoundShield.GuardTracker.class));
	}

	@Test
	void theHerosOwnDispelEndsAGuardThatHasBlocked() {
		Hero hero = guardingHero(true);

		Invisibility.dispel();

		assertNull(hero.buff(RoundShield.GuardTracker.class));
	}
}
