package com.qsr.customspd.items.weapon.melee;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import com.watabou.noosa.Camera;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Upstream 70f5b796e (v2.1.1): a lunge refused because the hero is rooted shakes the screen, like
 * the other movement abilities a rooted hero cannot use. A lunge refused for range does not.
 */
class RapierRootedShakePortTest {

	private static final int W = 10;

	private Camera prevCamera;
	private int shakes;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@BeforeEach
	void countShakes() {
		prevCamera = Camera.main;
		shakes = 0;
		Camera.main = new Camera(0, 0, 100, 100, 1f) {
			@Override
			public void shake(float magnitude, float duration) {
				shakes++;
			}
		};
	}

	@AfterEach
	void tearDown() {
		Camera.main = prevCamera;
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private static Hero duelistAt(int cell, Rapier rapier) {
		HeadlessLevel.install(W, 10);
		Hero hero = new Hero();
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, cell);
		hero.belongings.weapon = rapier;
		Buff.affect(hero, MeleeWeapon.Charger.class);
		return hero;
	}

	private static Rat ratAt(int cell) {
		Rat rat = HeadlessLevel.at(new Rat(), cell);
		StubSprite.attach(rat);
		Dungeon.level.mobs.add(rat);
		Dungeon.level.heroFOV[cell] = true;
		return rat;
	}

	@Test
	void aRootedLungeIsRefusedWithAShake() {
		int start = 4 * W + 2;
		Rapier rapier = new Rapier();
		Hero hero = duelistAt(start, rapier);
		hero.rooted = true;
		Rat rat = ratAt(start + 2);

		rapier.duelistAbility(hero, rat.pos);

		assertEquals(start, hero.pos, "a rooted hero cannot lunge");
		assertEquals(1, shakes, "the rooted refusal shakes the screen");
	}

	@Test
	void aLungeOutOfReachIsRefusedWithoutAShake() {
		int start = 4 * W + 2;
		Rapier rapier = new Rapier();
		Hero hero = duelistAt(start, rapier);
		Rat rat = ratAt(start + 4);

		rapier.duelistAbility(hero, rat.pos);

		assertEquals(start, hero.pos, "fixture: four cells is past the rapier's lunge");
		assertEquals(0, shakes, "only the rooted refusal shakes");
	}
}
