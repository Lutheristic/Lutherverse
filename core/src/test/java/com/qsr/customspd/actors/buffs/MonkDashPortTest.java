package com.qsr.customspd.actors.buffs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
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
 * The monk's dash. Upstream 70f5b796e (v2.1.1, "big consistency pass on rooted") stops a rooted
 * hero from dashing, with a screen shake, the same way it stops lunges, blinks and leaps.
 */
class MonkDashPortTest {

	private static final int W = 12;

	private HeadlessLevel level;
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

	private Hero monkAt(int cell) {
		level = HeadlessLevel.install(W, 10);
		Hero hero = new Hero();
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, cell);
		// enough for a dash, never empowered (that needs 120% of the cap with no Monastic Vigor)
		Buff.affect(hero, MonkEnergy.class).energy = 5;
		return hero;
	}

	private static void dash(Hero hero, int target) {
		new MonkEnergy.MonkAbility.Dash().doAbility(hero, target);
	}

	@Test
	void aRootedMonkCannotDash() {
		int start = 4 * W + 2;
		Hero hero = monkAt(start);
		hero.rooted = true;

		dash(hero, start + 2);

		assertEquals(start, hero.pos, "a rooted hero stays where she is");
		assertEquals(5f, hero.buff(MonkEnergy.class).energy, 0.0001f, "and the refused dash costs no energy");
		assertEquals(1, shakes, "the refusal shakes the screen");
	}

	@Test
	void anUnrootedMonkDashesToTheSameCell() {
		int start = 4 * W + 2;
		Hero hero = monkAt(start);

		dash(hero, start + 2);

		assertEquals(start + 2, hero.pos, "control: the same dash goes through without roots");
		assertEquals(2f, hero.buff(MonkEnergy.class).energy, 0.0001f);
		assertEquals(0, shakes);
	}
}
