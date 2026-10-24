package com.qsr.customspd.items.stones;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Char;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.levels.Terrain;
import com.qsr.customspd.levels.traps.Trap;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port regression test for upstream SPD bfb460215 (v2.4.0): "fixed cases of runestones
 * affecting terrain when they shouldn't".
 *
 * Runestone.onThrow used to hard-press the landing cell unconditionally (except for stones that
 * opted out with pressesCell), so a stone thrown at a creature standing on a trap set the trap
 * off underneath it. It now presses the cell only when nobody stands there, and a stone aimed at
 * a creature over a pit still triggers instead of just falling in.
 *
 * The control case (an empty trap cell) proves the trap fixture does fire when pressed.
 */
class RunestoneThrowPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	private Hero prevHero;

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = prevHero;
	}

	private static final class CountingTrap extends Trap {
		int activations = 0;

		@Override
		public void activate() {
			activations++;
		}
	}

	/** A stone that only records that it was activated. */
	private static final class ProbeStone extends Runestone {
		int activations = 0;

		@Override
		protected void activate(int cell) {
			activations++;
		}
	}

	private void setUpHero(HeadlessLevel level) {
		prevHero = Dungeon.hero;
		Dungeon.hero = new Hero();
		HeadlessLevel.at(Dungeon.hero, 1 * level.width() + 1);
	}

	private CountingTrap armTrap(HeadlessLevel level, int cell) {
		CountingTrap trap = new CountingTrap();
		trap.set(cell);
		level.map[cell] = Terrain.TRAP;
		level.traps.put(cell, trap);
		level.buildFlagMaps();
		return trap;
	}

	private Rat ratAt(int cell) {
		Rat rat = HeadlessLevel.at(new Rat(), cell);
		rat.alignment = Char.Alignment.ENEMY;
		StubSprite.attach(rat);
		return rat;
	}

	@Test
	void aStoneThrownAtACreatureDoesNotSetOffTheTrapUnderIt() {
		HeadlessLevel level = HeadlessLevel.install(8, 8);
		setUpHero(level);
		int cell = 5 * level.width() + 5;
		CountingTrap trap = armTrap(level, cell);
		ratAt(cell);
		ProbeStone stone = new ProbeStone();

		stone.onThrow(cell);

		assertEquals(1, stone.activations, "the stone still takes effect");
		assertEquals(0, trap.activations, "a creature stands on the cell, so the stone must not press it");
	}

	@Test
	void controlAStoneThrownAtAnEmptyTrapCellStillSetsItOff() {
		HeadlessLevel level = HeadlessLevel.install(8, 8);
		setUpHero(level);
		int cell = 5 * level.width() + 5;
		CountingTrap trap = armTrap(level, cell);
		ProbeStone stone = new ProbeStone();

		stone.onThrow(cell);

		assertEquals(1, stone.activations);
		assertEquals(1, trap.activations, "nobody on the cell, so it is pressed");
	}

	@Test
	void aStoneThrownAtACreatureOverAPitStillTakesEffect() {
		HeadlessLevel level = HeadlessLevel.install(8, 8);
		setUpHero(level);
		int cell = 5 * level.width() + 5;
		level.pit[cell] = true;
		ratAt(cell);
		ProbeStone stone = new ProbeStone();

		stone.onThrow(cell);

		assertTrue(stone.activations == 1, "a stone aimed at a creature over a pit must activate, not fall in");
	}
}
