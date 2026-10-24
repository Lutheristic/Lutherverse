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

	private static <T extends MeleeWeapon> T identifiedAt(T weapon, int level) {
		weapon.level(level);
		weapon.identify();
		return weapon;
	}

	/** Upstream 59fadf083 (v2.2.0): the sneak is instantaneous; it no longer spends a turn. */
	@Test
	void sneakIsInstant() {
		int start = 4 * W + 2;
		Dagger dagger = new Dagger();
		Hero hero = heroAt(start, dagger);
		int target = 4 * W + 6;
		level.heroFOV[target] = true;
		float before = hero.cooldown();

		sneak(dagger, hero, target);

		assertEquals(target, hero.pos, "fixture: the blink landed");
		assertEquals(before, hero.cooldown(), 0.0001f, "the blink costs no time");
	}

	/**
	 * Upstream aa507c7ef (v2.4.0) and 289a43def (v2.4.0, "added scaling to all non-damage based
	 * duelist abilities"): the sneak grants 2+level turns of invisibility, one of which is the
	 * instant ability's own turn, so the buff itself lasts 1+level.
	 */
	@Test
	void sneakInvisibilityScalesWithLevel() {
		int start = 4 * W + 2;
		Dagger dagger = identifiedAt(new Dagger(), 2);
		Hero hero = heroAt(start, dagger);
		int target = 4 * W + 6;
		level.heroFOV[target] = true;

		sneak(dagger, hero, target);

		assertEquals(3f, hero.buff(com.qsr.customspd.actors.buffs.Invisibility.class).cooldown(), 0.0001f,
				"a +2 dagger grants 4 turns of invisibility: 3 on the buff plus the instant blink's turn");
	}

	/** Upstream eb7a86451 (v2.3.0, "fixed duelist's sneak ability working while rooted"). */
	@Test
	void aRootedDuelistCannotSneak() {
		int start = 4 * W + 2;
		Dagger dagger = new Dagger();
		Hero hero = heroAt(start, dagger);
		hero.rooted = true;
		int target = 4 * W + 6;
		level.heroFOV[target] = true;
		MeleeWeapon.Charger charger = hero.buff(MeleeWeapon.Charger.class);
		int chargesBefore = charger.charges;
		// The refusal shakes the screen; headless there is no main camera, so give it one.
		com.watabou.noosa.Camera prevCamera = com.watabou.noosa.Camera.main;
		com.watabou.noosa.Camera.main = new com.watabou.noosa.Camera(0, 0, 100, 100, 1f);
		try {
			sneak(dagger, hero, target);
		} finally {
			com.watabou.noosa.Camera.main = prevCamera;
		}

		assertEquals(start, hero.pos, "a rooted Duelist cannot blink away");
		assertEquals(chargesBefore, charger.charges, "and the refused sneak costs no charge");
	}

	/** 289a43def: Dirk and Assassin's Blade keep their 4 / 3 tile range and scale like the dagger. */
	@Test
	void dirkAndAssassinsBladeKeepTheirRangeAndScale() {
		int start = 4 * W + 2;
		AssassinsBlade blade = identifiedAt(new AssassinsBlade(), 1);
		Hero hero = heroAt(start, blade);
		level.heroFOV[start + 4] = true;
		level.heroFOV[start + 3] = true;
		sneak(blade, hero, start + 4);
		assertEquals(start, hero.pos, "4 tiles is past the assassin's blade's range of 3");

		sneak(blade, hero, start + 3);
		assertEquals(start + 3, hero.pos);
		assertEquals(2f, hero.buff(com.qsr.customspd.actors.buffs.Invisibility.class).cooldown(), 0.0001f,
				"a +1 blade grants 3 turns of invisibility");

		Dirk dirk = identifiedAt(new Dirk(), 0);
		hero = heroAt(start, dirk);
		level.heroFOV[start + 5] = true;
		level.heroFOV[start + 4] = true;
		sneak(dirk, hero, start + 5);
		assertEquals(start, hero.pos, "5 tiles is past the dirk's range of 4");
		sneak(dirk, hero, start + 4);
		assertEquals(start + 4, hero.pos);
	}

	/** The descriptions are format strings, so they are only proven by rendering them. */
	@Test
	void sneakDescriptionsStateTheScaledInvisibility() {
		heroAt(4 * W + 2, null); // buffedLvl asks whether the hero holds the weapon
		assertTrue(identifiedAt(new Dagger(), 3).abilityInfo().contains("_5 turns of invisibility_"),
				new Dagger().abilityInfo());
		assertTrue(identifiedAt(new Dirk(), 1).abilityInfo().contains("_3 turns of invisibility_"));
		assertTrue(identifiedAt(new AssassinsBlade(), 0).abilityInfo().contains("_2 turns of invisibility,_"));
		String typical = new Dagger().abilityInfo();
		assertTrue(typical.contains("typically grants her _2 turns of invisibility_"), typical);
		assertTrue(typical.contains("instantly blink up to 5 tiles"), typical);
	}
}
