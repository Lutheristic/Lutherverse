package com.qsr.customspd.actors.blobs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.journal.Notes;
import com.qsr.customspd.levels.Level;
import com.qsr.customspd.test.HeadlessRun;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Using up a well removes its landmark at once, unless a visited cell of that water remains
 * (upstream ef43f2458): an unvisited second cell must not keep the entry alive, since that would
 * hint at a well the hero has not found.
 */
class WellWaterLandmarkPortTest {

	/** Drinking without the scene effects; the landmark logic under test is WellWater's own. */
	public static class QuietHealth extends WaterOfHealth {
		@Override
		protected boolean affectHero(Hero hero) {
			return true;
		}
	}

	private HeadlessRun run;
	private Level level;
	private Hero hero;

	@BeforeEach
	void setUp() {
		run = HeadlessRun.start(20261118L);
		level = run.enterFirstLevel();
		hero = Dungeon.hero;
		Notes.reset();
	}

	@AfterEach
	void tearDown() {
		run.close();
	}

	@Test
	void usingAWellWhoseOtherWaterIsUnvisitedRemovesItsLandmark() {
		int a = hero.pos;
		int b = farCell(a);
		WaterOfHealth water = seedWater(a, b);
		level.visited[a] = true;
		level.visited[b] = false;
		Notes.add(Notes.Landmark.WELL_OF_HEALTH);

		WellWater.affectCell(a);

		assertEquals(0, water.cur[a], "fixture: the hero drank the well at A");
		assertTrue(water.volume > 0 && water.cur[b] > 0, "fixture: water remains at B");
		assertFalse(Notes.contains(Notes.Landmark.WELL_OF_HEALTH),
				"only unvisited water remains, so the landmark must go at once");
	}

	@Test
	void usingAWellWhoseOtherWaterIsVisitedKeepsItsLandmark() {
		int a = hero.pos;
		int b = farCell(a);
		WaterOfHealth water = seedWater(a, b);
		level.visited[a] = true;
		level.visited[b] = true;
		Notes.add(Notes.Landmark.WELL_OF_HEALTH);

		WellWater.affectCell(a);

		assertEquals(0, water.cur[a], "fixture: the hero drank the well at A");
		assertTrue(Notes.contains(Notes.Landmark.WELL_OF_HEALTH),
				"visited water remains, so the landmark stays");
	}

	@Test
	void usingTheLastWaterRemovesItsLandmark() {
		int a = hero.pos;
		seedWater(a, -1);
		level.visited[a] = true;
		Notes.add(Notes.Landmark.WELL_OF_HEALTH);

		WellWater.affectCell(a);

		assertFalse(Notes.contains(Notes.Landmark.WELL_OF_HEALTH), "an empty well keeps no landmark");
	}

	/** Seeds the quiet water at a (and b when b >= 0), filed under WaterOfHealth as affectCell looks it up. */
	private WaterOfHealth seedWater(int a, int b) {
		QuietHealth water = Blob.seed(a, 1, QuietHealth.class, level);
		if (b >= 0) Blob.seed(b, 1, QuietHealth.class, level);
		level.blobs.remove(QuietHealth.class);
		level.blobs.remove(WaterOfHealth.class);
		level.blobs.put(WaterOfHealth.class, water);
		return water;
	}

	private int farCell(int from) {
		for (int cell = level.length() - 1; cell >= 0; cell--) {
			if (level.insideMap(cell) && level.distance(cell, from) > 2) return cell;
		}
		throw new IllegalStateException("no far cell");
	}
}
