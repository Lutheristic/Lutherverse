package com.qsr.customspd.levels.traps;

import com.qsr.customspd.Badges;
import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Actor;
import com.qsr.customspd.actors.buffs.AscensionChallenge;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.mobs.Mob;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.windows.WndResurrect;
import com.watabou.utils.Bundle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A trap spawned by Reclaim Trap is "reclaimed": it scales by the ascension depth even if its cell
 * happens to hold a level trap, and killing the hero with it awards DEATH_FROM_FRIENDLY_MAGIC
 * (upstream 7d7743153, v2.5.0). The hero is a stand-in whose die() is a no-op, because the real one
 * needs the game scene; the trap's own "the hero is dead" check is all this exercises.
 *
 * Only RockfallTrap is driven end to end; the other five traps carrying the same one-line badge
 * hook (Disintegration, Explosive, Grim, PoisonDart, WornDart) need beams, bombs or thrown darts
 * and so a scene.
 */
class ReclaimedTrapPortTest {

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
		Dungeon.customSeedText = ""; // seeded runs never unlock badges
		Dungeon.depth = 3;
		// A hero whose death skips Hero.die's scene work (interrupt, resurrection window, bones, fog):
		// the trap only asks whether the hero is dead afterwards.
		Hero real = Dungeon.hero;
		Hero hero = new Hero() {
			@Override
			public void interrupt() {
			}

			@Override
			public void die(Object cause) {
			}
		};
		Actor.remove(real);
		Dungeon.hero = HeadlessLevel.at(hero, real.pos);
		// Dungeon.fail would submit the stand-in to the rankings; a pending resurrection window skips that
		WndResurrect.instance = new Object();
		Buff.affect(Dungeon.hero, AscensionChallenge.class);
		assertEquals(26, Dungeon.scalingDepth(), "test setup: the ascension challenge must be live");
		for (Mob m : new ArrayList<>(Dungeon.level.mobs)) {
			Dungeon.level.mobs.remove(m);
			Actor.remove(m);
		}
		Arrays.fill(Dungeon.level.heroFOV, false); // no particles or shake headless
	}

	@AfterEach
	void closeRun() {
		WndResurrect.instance = null;
		run.close();
	}

	@Test
	void aReclaimedTrapScalesByTheAscensionEvenOnALevelTrapCell() {
		ProbeTrap trap = new ProbeTrap();
		trap.set(Dungeon.hero.pos);
		Dungeon.level.traps.put(trap.pos, trap);
		assertEquals(3, trap.depthSeen(), "control: a level trap uses the true depth");

		trap.reclaimed = true;
		assertEquals(26, trap.depthSeen());
	}

	/** Drops a fatal rockfall on the hero and reports whether the friendly-magic badge came of it. */
	private boolean friendlyMagicBadgeAfterRockfall(boolean reclaimed) {
		Badges.reset(); // this run's badge list only: the global set outlives a test and would carry over
		Dungeon.hero.HP = 1;

		RockfallTrap trap = new RockfallTrap();
		trap.reclaimed = reclaimed;
		trap.set(Dungeon.hero.pos);
		trap.activate();

		assertFalse(Dungeon.hero.isAlive(), "test setup: the rockfall must kill the hero");
		Bundle local = new Bundle();
		Badges.saveLocal(local);
		return Arrays.asList(local.getStringArray("badges")).contains("DEATH_FROM_FRIENDLY_MAGIC");
	}

	@Test
	void aReclaimedRockfallThatKillsTheHeroAwardsDeathFromFriendlyMagic() {
		assertTrue(friendlyMagicBadgeAfterRockfall(true));
	}

	@Test
	void anOrdinaryRockfallThatKillsTheHeroDoesNot() {
		assertFalse(friendlyMagicBadgeAfterRockfall(false));
	}
}
