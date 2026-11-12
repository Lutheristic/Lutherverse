package com.qsr.customspd.actors.hero.abilities.rogue;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Char;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.hero.HeroClass;
import com.qsr.customspd.actors.hero.Talent;
import com.qsr.customspd.items.armor.ClassArmor;
import com.qsr.customspd.items.armor.RogueArmor;
import com.qsr.customspd.levels.Terrain;
import com.qsr.customspd.levels.traps.Trap;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * upstream c09174918 (v2.2.0): the body replacement log left behind by smoke bomb presses the
 * cell it is placed on, so a trap there goes off.
 */
class NinjaLogPressesCellPortTest {

	private static final int W = 10;

	/** A visible trap that only counts how often it goes off. */
	public static class CountingTrap extends Trap {
		int activations = 0;

		@Override
		public void activate() {
			activations++;
		}
	}

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
	void theLogSetsOffATrapOnTheCellTheRogueLeaves() {
		HeadlessLevel level = HeadlessLevel.install(W, W);
		Hero hero = new Hero();
		hero.heroClass = HeroClass.ROGUE;
		Talent.initClassTalents(hero);
		hero.talents.get(3).put(Talent.BODY_REPLACEMENT, 1);
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		int start = 4 * W + 2;
		HeadlessLevel.at(hero, start);
		Arrays.fill(level.heroFOV, true);

		CountingTrap trap = new CountingTrap();
		trap.visible = true;
		level.setTrap(trap, start);
		level.map[start] = Terrain.TRAP;
		level.buildFlagMaps();

		ClassArmor armor = ClassArmor.upgrade(hero, new RogueArmor());
		armor.charge = 100;

		boolean reachedTheScene = activateUpToTheScene(armor, hero, 4 * W + 6);
		assertTrue(reachedTheScene, "fixture: the blink goes through");

		SmokeBomb.NinjaLog log = null;
		for (Char ch : Dungeon.level.mobs) {
			if (ch instanceof SmokeBomb.NinjaLog) log = (SmokeBomb.NinjaLog) ch;
		}
		assertTrue(log != null && log.pos == start, "fixture: the log is left on the starting cell");
		assertEquals(1, trap.activations, "the log presses its cell, so the trap under it goes off");
	}

	/**
	 * Headless, the ability stops at the smoke puff (CellEmitter.get needs the game scene); the
	 * log is placed before that.
	 */
	private static boolean activateUpToTheScene(ClassArmor armor, Hero hero, int target) {
		try {
			new SmokeBomb().activate(armor, hero, target);
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
}
