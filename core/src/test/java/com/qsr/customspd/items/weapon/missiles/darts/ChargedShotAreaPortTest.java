package com.qsr.customspd.items.weapon.missiles.darts;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.buffs.Paralysis;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.items.weapon.melee.Crossbow;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Upstream 11a410f07 (v2.4.0): a charged shot spreads a tipped dart's effect over a 7x7 area
 * (3 tiles from where it lands), up from 5x5.
 */
class ChargedShotAreaPortTest {

	private static final int W = 12;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private static Rat ratAt(int cell) {
		Rat rat = HeadlessLevel.at(new Rat(), cell);
		StubSprite.attach(rat);
		return rat;
	}

	@Test
	void aChargedTippedDartReachesThreeTilesFromWhereItLands() throws Exception {
		HeadlessLevel.install(W, W);
		Hero hero = new Hero();
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, W + 1);
		hero.belongings.weapon = new Crossbow();
		Buff.affect(hero, Crossbow.ChargedShot.class);

		ParalyticDart dart = new ParalyticDart();
		dart.throwSound(); // refreshes Dart's static crossbow from the hero

		int impact = 6 * W + 5;
		Rat target = ratAt(impact);
		Rat threeAway = ratAt(impact + 3);
		Rat fourAway = ratAt(impact + 4);

		// onThrow records where the dart landed; the rest of a throw needs a scene
		Field landing = Dart.class.getDeclaredField("chargedShotPos");
		landing.setAccessible(true);
		landing.setInt(dart, impact);
		dart.processChargedShot(target, 1);

		assertNotNull(threeAway.buff(Paralysis.class), "3 tiles away is inside the 7x7 area");
		assertNull(fourAway.buff(Paralysis.class), "4 tiles away is outside it");
	}
}
