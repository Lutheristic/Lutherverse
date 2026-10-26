package com.qsr.customspd.actors.buffs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.hero.HeroClass;
import com.qsr.customspd.actors.hero.Talent;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.actors.mobs.Succubus;
import com.qsr.customspd.items.artifacts.EtherealChains;
import com.qsr.customspd.levels.Level;
import com.qsr.customspd.levels.Terrain;
import com.qsr.customspd.mechanics.Ballistica;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.scenes.CellSelector;
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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 70f5b796e (v2.1.1, "big consistency pass on rooted"): a rooted hero cannot leap with
 * the gladiator's combo, a rooted succubus cannot blink, and the assassin's blink and ethereal
 * chains shake the screen when roots refuse them. Upstream 00c45dc7f lets a flying gladiator leap,
 * and a flying assassin blink, onto a hazard.
 */
class RootedMovementPortTest {

	private static final int W = 16;
	private static final int H = 10;
	private static final int START = 4 * W + 2;

	private HeadlessLevel level;
	private Hero hero;
	private Camera prevCamera;
	private int shakes;
	private final List<String> log = new ArrayList<>();
	private final Signal.Listener<String> logListener = log::add;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@BeforeEach
	void setUp() {
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

		level = HeadlessLevel.install(W, H);
		hero = new Hero();
		Talent.initClassTalents(hero);
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, START);
		Arrays.fill(level.heroFOV, true);
		// as Hero.act sets it
		hero.fieldOfView = level.heroFOV;
	}

	@AfterEach
	void tearDown() throws Exception {
		GLog.update.remove(logListener);
		Camera.main = prevCamera;
		moveBeingUsed().set(null, null);
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private Rat ratAt(int cell) {
		return placed(new Rat(), cell);
	}

	/** A rat whose armor blocks nothing, so any hit deals at least 1. */
	private Rat unarmoredRatAt(int cell) {
		return placed(new Rat() {
			@Override
			public int drRoll() {
				return 0;
			}
		}, cell);
	}

	private Rat placed(Rat rat, int cell) {
		HeadlessLevel.at(rat, cell);
		rat.HP = rat.HT = 1000;
		StubSprite.attach(rat);
		level.mobs.add(rat);
		return rat;
	}

	private static Field field(Class<?> cls, String name) throws Exception {
		Field f = cls.getDeclaredField(name);
		f.setAccessible(true);
		return f;
	}

	private static Field moveBeingUsed() throws Exception {
		return field(Combo.class, "moveBeingUsed");
	}

	/** A gladiator with Enhanced Combo +3 and a 3 hit combo, picking a slam's target. */
	private CellSelector.Listener comboLeap() throws Exception {
		hero.heroClass = HeroClass.WARRIOR;
		hero.talents.get(2).put(Talent.ENHANCED_COMBO, 3);
		Combo combo = Buff.affect(hero, Combo.class);
		field(Combo.class, "count").setInt(combo, 3);
		moveBeingUsed().set(null, Combo.ComboMove.SLAM);
		return (CellSelector.Listener) field(Combo.class, "listener").get(combo);
	}

	@Test
	void aRootedGladiatorCannotLeapAtItsTarget() throws Exception {
		hero.rooted = true;
		Rat rat = ratAt(START + 2);

		comboLeap().onSelect(rat.pos);

		assertEquals(START, hero.pos);
		assertEquals(rat.HT, rat.HP, "no slam lands");
		assertEquals(1, shakes);
		assertTrue(log.contains(GLog.WARNING + Messages.get(Combo.class, "bad_target")), "log was " + log);
	}

	@Test
	void aGladiatorWhoCannotFlyDoesNotLeapIntoAChasm() throws Exception {
		Level.set(START + 1, Terrain.CHASM, level);
		Rat rat = ratAt(START + 2);

		comboLeap().onSelect(rat.pos);

		assertEquals(START, hero.pos);
		assertTrue(log.contains(GLog.WARNING + Messages.get(Combo.class, "bad_target")), "log was " + log);
	}

	@Test
	void aFlyingGladiatorLeapsOverAChasm() throws Exception {
		Level.set(START + 1, Terrain.CHASM, level);
		hero.flying = true;
		Rat rat = unarmoredRatAt(START + 2);

		comboLeap().onSelect(rat.pos);

		assertEquals(START + 1, hero.pos, "the leap lands above the chasm");
		assertTrue(rat.HP < rat.HT, "and the slam hits");
		assertNull(hero.buff(Combo.class), "the slam spends the combo");
		assertEquals(0, shakes);
	}

	@Test
	void aRootedAssassinsBlinkShakesTheScreen() throws Exception {
		hero.heroClass = HeroClass.ROGUE;
		hero.rooted = true;
		Preparation prep = Buff.affect(hero, Preparation.class);
		field(Preparation.class, "turnsInvis").setInt(prep, 9);
		Rat rat = ratAt(START + 2);

		((CellSelector.Listener) field(Preparation.class, "attack").get(prep)).onSelect(rat.pos);

		assertEquals(START, hero.pos);
		assertTrue(log.contains(GLog.WARNING + Messages.get(Preparation.class, "out_of_reach")), "log was " + log);
		assertEquals(1, shakes);
	}

	/**
	 * Runs a blink that goes through. Headless, it stops at the puff of smoke on landing:
	 * CellEmitter.get needs the game scene, and everything the blink decides happens before it.
	 * Returns whether it got there.
	 */
	private static boolean upToThePuff(Runnable blink) {
		try {
			blink.run();
			return false;
		} catch (NullPointerException e) {
			for (StackTraceElement el : e.getStackTrace()) {
				if (el.getClassName().endsWith(".CellEmitter") && el.getMethodName().equals("get")) {
					return true;
				}
			}
			throw e;
		}
	}

	@Test
	void aFlyingAssassinBlinksOntoAChasm() throws Exception {
		hero.heroClass = HeroClass.ROGUE;
		hero.flying = true;
		Preparation prep = Buff.affect(hero, Preparation.class);
		field(Preparation.class, "turnsInvis").setInt(prep, 9);
		for (int y = 1; y < H - 1; y++) {
			Level.set(y * W + 5, Terrain.CHASM, level);
		}
		Rat rat = ratAt(START + 4);
		CellSelector.Listener attack = (CellSelector.Listener) field(Preparation.class, "attack").get(prep);

		boolean blinked = upToThePuff(() -> attack.onSelect(rat.pos));

		assertEquals(START + 3, hero.pos, "a flier lands on the chasm beside the target, the nearest cell to her");
		assertTrue(log.isEmpty(), "log was " + log);
		assertTrue(blinked, "the blink goes on to its puff of smoke");
	}

	@Test
	void rootedEtherealChainsShakeTheScreen() throws Exception {
		hero.rooted = true;
		EtherealChains chains = new EtherealChains();
		Ballistica chain = new Ballistica(hero.pos, START + 5, Ballistica.STOP_TARGET);
		Method pull = EtherealChains.class.getDeclaredMethod("chainLocation", Ballistica.class, Hero.class);
		pull.setAccessible(true);

		pull.invoke(chains, chain, hero);

		assertEquals(START, hero.pos);
		assertTrue(log.contains(GLog.WARNING + Messages.get(EtherealChains.class, "rooted")), "log was " + log);
		assertEquals(1, shakes);
	}

	@Test
	void aRootedSuccubusDoesNotBlink() throws Exception {
		Succubus succubus = HeadlessLevel.at(new Succubus(), START + 8);
		StubSprite.attach(succubus);
		level.mobs.add(succubus);
		succubus.fieldOfView = new boolean[level.length()];
		Arrays.fill(succubus.fieldOfView, true);
		succubus.rooted = true;
		Method getCloser = Succubus.class.getDeclaredMethod("getCloser", int.class);
		getCloser.setAccessible(true);

		boolean moved = (Boolean) getCloser.invoke(succubus, hero.pos);

		assertFalse(moved);
		assertEquals(START + 8, succubus.pos, "roots hold the succubus in place");
	}
}
