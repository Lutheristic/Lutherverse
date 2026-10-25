package com.qsr.customspd.levels.traps;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import com.qsr.customspd.test.TurnPump;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port regression for upstream afcafa5e4, 0c889a8a9 and f4aa26bef (v2.5.0): targeted traps
 * cannot aim past the level's view distance (at least 6), count an invisible char as standing
 * at that range, and on a tie in distance pick another char over the hero.
 */
class TrapTargetingPortTest {

	private static final int W = 14;
	private static final int H = 12;
	private static final int TRAP = 6 * W + 2;

	@SuppressWarnings("unchecked")
	private static final Supplier<Trap>[] TRAPS = new Supplier[]{
			WornDartTrap::new, PoisonDartTrap::new, DisintegrationTrap::new, GrimTrap::new
	};

	/** A hero that only counts the hits it takes: the real damage path needs a game scene. */
	static class CountingHero extends Hero {
		int hits = 0;

		@Override
		public void damage(int dmg, Object src) {
			hits++;
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

	private static HeadlessLevel level() {
		HeadlessLevel level = HeadlessLevel.install(W, H);
		assertEquals(8, level.viewDistance, "fixture: trap range is the view distance of 8");
		return level;
	}

	private static CountingHero hero(int cell) {
		CountingHero hero = HeadlessLevel.at(new CountingHero(), cell);
		StubSprite.attach(hero); // a kill shows the hero's experience gain
		Dungeon.hero = hero;
		return hero;
	}

	private static Rat rat(int cell, Trap trap) {
		Rat rat = HeadlessLevel.at(new Rat(), cell);
		rat.HP = rat.HT = 100;
		// a dying rat needs a sprite, but the poison splash needs a game scene when there is one
		if (!(trap instanceof PoisonDartTrap)) StubSprite.attach(rat);
		return rat;
	}

	/** Triggers the trap and runs whatever it queued, unseen by the hero so it resolves at once. */
	private static void spring(Trap trap) {
		trap.set(TRAP);
		trap.activate();
		if (!(trap instanceof DisintegrationTrap)) {
			new TurnPump().step();
		}
	}

	@Test
	void targetsPastViewDistanceAreIgnored() {
		for (Supplier<Trap> kind : TRAPS) {
			level();
			Trap trap = kind.get();
			// a grim trap with no target bursts shadow at its own cell, which needs a game scene
			if (trap instanceof GrimTrap) {
				tearDown();
				continue;
			}
			hero(10 * W + 12); // about 10.8 tiles away
			Rat far = rat(6 * W + 11, trap); // 9 tiles away

			spring(trap);

			assertEquals(100, far.HP, trap.getClass().getSimpleName() + " must not reach 9 tiles");
			tearDown();
		}
	}

	@Test
	void targetsWithinViewDistanceAreHit() {
		for (Supplier<Trap> kind : TRAPS) {
			level();
			Trap trap = kind.get();
			hero(10 * W + 12);
			Rat near = rat(6 * W + 8, trap); // 6 tiles away

			spring(trap);

			assertTrue(near.HP < 100, trap.getClass().getSimpleName() + " hits a target 6 tiles away");
			tearDown();
		}
	}

	@Test
	void anInvisibleTargetCountsAsStandingAtMaxRange() {
		for (Supplier<Trap> kind : TRAPS) {
			level();
			Trap trap = kind.get();
			hero(10 * W + 12);
			Rat hidden = rat(9 * W + 2, trap); // 3 tiles away, so it counts as 8
			hidden.invisible = 1;
			Rat far = rat(6 * W + 11, trap); // 9 tiles away, out of range

			spring(trap);

			String name = trap.getClass().getSimpleName();
			assertTrue(hidden.HP < 100, name + " still finds an invisible target inside its range");
			assertEquals(100, far.HP, name + " does not pass over it for a visible one out of range");
			tearDown();
		}
	}

	@Test
	void onATieTheHeroIsNotTheOneHit() {
		// Actor.chars() keeps actor order, so both orders are spelled out
		for (boolean heroFirst : new boolean[]{true, false}) {
			for (Supplier<Trap> kind : TRAPS) {
				level();
				Trap trap = kind.get();
				CountingHero hero;
				Rat rat;
				if (heroFirst) {
					hero = hero(6 * W + 6); // 4 tiles away
					rat = rat(2 * W + 2, trap); // also 4 tiles away
				} else {
					rat = rat(2 * W + 2, trap);
					hero = hero(6 * W + 6);
				}

				spring(trap);

				String name = trap.getClass().getSimpleName() + (heroFirst ? ", hero added first" : ", rat added first");
				assertEquals(0, hero.hits, name + ": must not pick the hero on a tie");
				assertTrue(rat.HP < 100, name + ": hits the other char instead");
				tearDown();
			}
		}
	}
}
