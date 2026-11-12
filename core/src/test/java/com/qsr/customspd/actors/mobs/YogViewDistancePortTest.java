package com.qsr.customspd.actors.mobs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.levels.HallsBossLevel;
import com.qsr.customspd.levels.Level;
import com.qsr.customspd.test.HeadlessRun;
import com.watabou.utils.Bundle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port regression for upstream 849fc81a9: on load, HallsBossLevel asks Yog to update the
 * level's view distance, which was 4 - (phase - 1) with no floor. A save made in the last
 * phase (phase 5, Yog still alive) therefore loaded with view distance 0 for the level and
 * the hero. It is now clamped to at least 1.
 */
class YogViewDistancePortTest {

	private static final long SEED = 20261112L;

	private static YogDzewa yogInPhase(int phase) {
		YogDzewa yog = new YogDzewa();
		Bundle bundle = new Bundle();
		yog.storeInBundle(bundle);
		bundle.put("phase", phase);
		YogDzewa restored = new YogDzewa();
		restored.restoreFromBundle(bundle);
		return restored;
	}

	private static Level saveAndLoadWithYog(HeadlessRun run, int phase) {
		Level halls = run.generate("25");
		assertTrue(halls instanceof HallsBossLevel, "precondition: floor 25 is the Yog arena");
		Dungeon.level = halls;
		Dungeon.hero.pos = halls.entrance();

		YogDzewa yog = yogInPhase(phase);
		yog.pos = halls.exit() + 3 * halls.width();
		halls.mobs.add(yog);

		run.saveLevel();
		return run.loadLevel();
	}

	@Test
	void loadingInTheLastPhaseKeepsTheHeroAbleToSee() {
		try (HeadlessRun run = HeadlessRun.start(SEED)) {
			run.enterFirstLevel();

			Level loaded = saveAndLoadWithYog(run, 5);

			assertEquals(1, loaded.viewDistance, "level view distance after loading in phase 5");
			assertEquals(1, Dungeon.hero.viewDistance, "hero view distance after loading in phase 5");
		}
	}

	@Test
	void loadingInPhaseFourStillNarrowsTheViewToOne() {
		// unchanged by the fix: shows the load path reaches Yog's visibility update at all
		try (HeadlessRun run = HeadlessRun.start(SEED)) {
			run.enterFirstLevel();

			Level loaded = saveAndLoadWithYog(run, 4);

			assertEquals(1, loaded.viewDistance);
			assertEquals(1, Dungeon.hero.viewDistance);
		}
	}

	@Test
	void loadingInPhaseTwoNarrowsTheViewToThree() {
		try (HeadlessRun run = HeadlessRun.start(SEED)) {
			run.enterFirstLevel();

			Level loaded = saveAndLoadWithYog(run, 2);

			assertEquals(3, loaded.viewDistance);
			assertEquals(3, Dungeon.hero.viewDistance);
		}
	}
}
