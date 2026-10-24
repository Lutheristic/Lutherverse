package com.qsr.customspd.items.weapon.melee;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Duelist's sneak (Dagger, Dirk, Assassin's Blade all call Dagger.sneakAbility), as v2.5.4
 * has it on top of the targeted blink of 288f0ea18.
 *
 * A blink that goes through runs headlessly up to the puff of smoke at the landing cell:
 * CellEmitter.get needs GameScene's emitter pool, so that NPE is the boundary these tests stop
 * at. Everything the ability decides happens before it (charge, invisibility, time, position).
 */
class DaggerSneakPortTest {

	private static final int W = 10;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private static HeadlessLevel level;

	private static Hero heroAt(int cell, MeleeWeapon weapon) {
		level = HeadlessLevel.install(W, 10);
		Hero hero = new Hero();
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, cell);
		hero.belongings.weapon = weapon;
		Buff.affect(hero, MeleeWeapon.Charger.class);
		return hero;
	}

	/** Runs the ability; a blink that goes through ends at CellEmitter.get, which needs a scene. */
	private static void sneak(MeleeWeapon weapon, Hero hero, int target) {
		try {
			weapon.duelistAbility(hero, target);
		} catch (NullPointerException e) {
			boolean fromPuff = false;
			for (StackTraceElement el : e.getStackTrace()) {
				if (el.getClassName().endsWith("CellEmitter") && el.getMethodName().equals("get")) {
					fromPuff = true;
					break;
				}
			}
			assertTrue(fromPuff, "the only acceptable failure is the missing scene at the landing puff: " + e);
		}
	}

	/**
	 * Upstream e4b704e6a (v2.2.1, "fixed various blink effects working over magical fire"): a cell
	 * held by eternal fire is not solid, but it is neither passable nor avoid, so measuring the
	 * range over non-solid cells let the blink cross a wall of magical fire.
	 */
	@Test
	void sneakCannotBlinkAcrossAWallOfMagicalFire() {
		int start = 4 * W + 2;
		Dagger dagger = new Dagger();
		Hero hero = heroAt(start, dagger);
		// A full-height column of eternal fire: terrain stays EMPTY, the blob clears passable.
		for (int y = 1; y < 9; y++) {
			level.passable[y * W + 4] = false;
		}
		int target = 4 * W + 6;
		level.heroFOV[target] = true;
		MeleeWeapon.Charger charger = hero.buff(MeleeWeapon.Charger.class);
		int chargesBefore = charger.charges;

		sneak(dagger, hero, target);

		assertEquals(start, hero.pos, "the target is only reachable across the fire, so the blink is refused");
		assertEquals(chargesBefore, charger.charges, "a refused blink costs no charge");
	}

	@Test
	void sneakStillBlinksOverOpenFloor() {
		int start = 4 * W + 2;
		Dagger dagger = new Dagger();
		Hero hero = heroAt(start, dagger);
		int target = 4 * W + 6;
		level.heroFOV[target] = true;

		sneak(dagger, hero, target);

		assertEquals(target, hero.pos, "control: with no fire in the way the blink lands");
	}
}
