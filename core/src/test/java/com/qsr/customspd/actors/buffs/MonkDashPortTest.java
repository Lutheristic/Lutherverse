package com.qsr.customspd.actors.buffs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.levels.Level;
import com.qsr.customspd.levels.Terrain;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import com.qsr.customspd.utils.GLog;
import com.watabou.noosa.Camera;
import com.watabou.utils.Signal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The monk's dash, and the reach messages of the monk's other targeted abilities.
 *
 * Upstream 70f5b796e (v2.1.1, "big consistency pass on rooted") stops a rooted hero from dashing,
 * with a screen shake, the same way it stops lunges, blinks and leaps.
 *
 * Upstream 969610dfd (v2.4.0, "adjusted some weapon ability error messages") splits the one "can't
 * be reached" warning into "isn't in range" and "is occupied".
 */
class MonkDashPortTest {

	private static final int W = 12;

	private static final String OUT_OF_RANGE = GLog.WARNING + "That target isn't in range.";
	private static final String OCCUPIED = GLog.WARNING + "That location is occupied.";

	private HeadlessLevel level;
	private Camera prevCamera;
	private int shakes;
	private final List<String> log = new ArrayList<>();
	private final Signal.Listener<String> logListener = log::add;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@BeforeEach
	void listen() {
		prevCamera = Camera.main;
		shakes = 0;
		Camera.main = new Camera(0, 0, 100, 100, 1f) {
			@Override
			public void shake(float magnitude, float duration) {
				shakes++;
			}
		};
		log.clear();
		GLog.update.add(logListener);
	}

	@AfterEach
	void tearDown() {
		GLog.update.remove(logListener);
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

	private Rat ratAt(int cell) {
		Rat rat = HeadlessLevel.at(new Rat(), cell);
		rat.HP = rat.HT = 1000;
		StubSprite.attach(rat);
		level.mobs.add(rat);
		level.heroFOV[cell] = true;
		return rat;
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
		assertTrue(log.contains(OUT_OF_RANGE), "log was " + log);
	}

	@Test
	void anUnrootedMonkDashesToTheSameCell() {
		int start = 4 * W + 2;
		Hero hero = monkAt(start);

		dash(hero, start + 2);

		assertEquals(start + 2, hero.pos, "control: the same dash goes through without roots");
		assertEquals(2f, hero.buff(MonkEnergy.class).energy, 0.0001f);
		assertEquals(0, shakes);
		assertTrue(log.isEmpty(), "log was " + log);
	}

	@Test
	void aDashPastItsRangeSaysTheTargetIsOutOfRange() {
		int start = 4 * W + 2;
		Hero hero = monkAt(start);

		dash(hero, start + 5);

		assertEquals(start, hero.pos);
		assertEquals(5f, hero.buff(MonkEnergy.class).energy, 0.0001f);
		assertEquals(0, shakes, "only the rooted refusal shakes");
		assertTrue(log.contains(OUT_OF_RANGE), "log was " + log);
	}

	@Test
	void aDashThroughAWallSaysTheTargetIsOutOfRange() {
		int start = 4 * W + 2;
		Hero hero = monkAt(start);
		Level.set(start + 1, Terrain.WALL, level);

		dash(hero, start + 2);

		assertEquals(start, hero.pos);
		assertTrue(log.contains(OUT_OF_RANGE), "log was " + log);
	}

	@Test
	void aDashOntoAnOccupiedCellSaysTheCellIsOccupied() {
		int start = 4 * W + 2;
		Hero hero = monkAt(start);
		ratAt(start + 2);

		dash(hero, start + 2);

		assertTrue(log.contains(OCCUPIED), "log was " + log);
	}

	/** Upstream 1e59e2b42 (v2.4.0, "fixed dash unintentionally not cancelling when targeting chars"). */
	@Test
	void aDashOntoAnOccupiedCellIsCancelled() {
		int start = 4 * W + 2;
		Hero hero = monkAt(start);
		Rat rat = ratAt(start + 2);

		dash(hero, start + 2);

		assertEquals(start, hero.pos, "the hero must not land on the rat");
		assertEquals(start + 2, rat.pos);
		assertEquals(5f, hero.buff(MonkEnergy.class).energy, 0.0001f, "a cancelled dash costs no energy");
	}

	@Test
	void flurryOutOfReachSaysTheTargetIsOutOfRange() {
		int start = 4 * W + 2;
		Hero hero = monkAt(start);
		Rat rat = ratAt(start + 2);

		new MonkEnergy.MonkAbility.Flurry().doAbility(hero, rat.pos);

		assertEquals(rat.HT, rat.HP, "an unarmed hero cannot reach two cells");
		assertTrue(log.contains(OUT_OF_RANGE), "log was " + log);
	}

	@Test
	void dragonKickOutOfReachSaysTheTargetIsOutOfRange() {
		int start = 4 * W + 2;
		Hero hero = monkAt(start);
		Rat rat = ratAt(start + 2);

		new MonkEnergy.MonkAbility.DragonKick().doAbility(hero, rat.pos);

		assertEquals(rat.HT, rat.HP, "an unarmed hero cannot reach two cells");
		assertTrue(log.contains(OUT_OF_RANGE), "log was " + log);
	}
}
