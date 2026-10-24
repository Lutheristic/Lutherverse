package com.qsr.customspd.levels.traps;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Actor;
import com.qsr.customspd.actors.buffs.AscensionChallenge;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.mobs.Mob;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.levels.Level;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.HeadlessRun;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Traps use the true depth when they are part of the level and {@code Dungeon.scalingDepth()}
 * (26 while the Amulet-ascension challenge is on) when they are not, e.g. an effect spawned from
 * a reclaimed trap (upstream 5a1d6e5d9, v2.2.0). CPDU read {@code Dungeon.depth} directly in nine
 * traps, so a lone trap never scaled with the ascension, and RockfallTrap diverged from v2.5.4.
 *
 * The fixture is a real run so the hero (whose AscensionChallenge buff drives scalingDepth) and
 * the level are the genuine objects.
 */
class TrapScalingDepthPortTest {

	/** Exposes the protected scalingDepth() and does nothing when triggered. */
	private static final class ProbeTrap extends Trap {
		int depthSeen() {
			return scalingDepth();
		}

		@Override
		public void activate() {
		}
	}

	private HeadlessRun run;

	@BeforeEach
	void startRun() {
		run = HeadlessRun.start(20261024L);
		run.enterFirstLevel();
		Dungeon.depth = 3;
		Buff.affect(Dungeon.hero, AscensionChallenge.class);
		assertEquals(26, Dungeon.scalingDepth(), "test setup: the ascension challenge must be live");
	}

	@AfterEach
	void closeRun() {
		run.close();
	}

	@Test
	void aTrapPartOfTheLevelUsesTheTrueDepth() {
		ProbeTrap trap = new ProbeTrap();
		trap.set(Dungeon.hero.pos);
		Dungeon.level.traps.put(trap.pos, trap);

		assertEquals(3, trap.depthSeen());
	}

	@Test
	void aTrapNotPartOfTheLevelUsesTheScalingDepth() {
		ProbeTrap trap = new ProbeTrap();
		trap.set(Dungeon.hero.pos);

		assertEquals(26, trap.depthSeen());
	}

	/** Damage a lone RockfallTrap deals to a fresh rat standing on it. */
	private int rockfallDamage() {
		Level level = Dungeon.level;
		// the floor's own mobs must not stand in the blast (no sprites headless)
		for (Mob m : new ArrayList<>(level.mobs)) {
			level.mobs.remove(m);
			Actor.remove(m);
		}
		// far enough from the hero that the 5x5 blast cannot reach them (no hero sprite/scene headless)
		int cell = -1;
		for (int c = 0; c < level.length() && cell == -1; c++) {
			if (level.passable[c] && Actor.findChar(c) == null && Dungeon.level.distance(c, Dungeon.hero.pos) > 4) {
				cell = c;
			}
		}
		assertTrue(cell != -1, "test setup: need a free cell away from the hero");
		Rat rat = HeadlessLevel.at(new Rat(), cell);
		rat.HP = rat.HT = 1000;
		Dungeon.hero.HP = Dungeon.hero.HT = 1000;

		// nothing in view, so the trap skips its rock particles and screen shake (no scene headless)
		Arrays.fill(level.heroFOV, false);

		RockfallTrap trap = new RockfallTrap();
		trap.set(cell);
		trap.activate();
		Actor.remove(rat);
		return 1000 - rat.HP;
	}

	@Test
	void aLoneRockfallTrapHitsHarderWhileAscending() {
		// 5+26 .. 10+52 under the ascension, minus the rat's 0..1 armour
		int ascending = rockfallDamage();
		assertTrue(ascending >= 30, "ascension depth 26 must scale the rockfall, took " + ascending);

		Dungeon.hero.remove(Dungeon.hero.buff(AscensionChallenge.class));
		Dungeon.depth = 1;
		// 5+1 .. 10+2 at depth 1
		int plain = rockfallDamage();
		assertTrue(plain <= 12, "without the ascension the rockfall scales with depth 1, took " + plain);
	}
}
