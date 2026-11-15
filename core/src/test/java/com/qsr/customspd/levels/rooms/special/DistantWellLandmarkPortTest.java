package com.qsr.customspd.levels.rooms.special;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.blobs.Blob;
import com.qsr.customspd.journal.Notes;
import com.qsr.customspd.levels.Level;
import com.qsr.customspd.levels.RegularLevel;
import com.qsr.customspd.levels.features.Chasm;
import com.qsr.customspd.levels.rooms.Room;
import com.qsr.customspd.scenes.InterlevelScene;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.tiles.CustomTilemap;
import com.watabou.noosa.Game;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 6b9940e88 and eab0cd3a3: a weak floor room seeds a one-cell WellID blob at its hidden
 * well, so the hero records a distant well when that cell comes into view. Once recorded the blob
 * is cleared, and falling into the room's chasm removes the entry.
 */
class DistantWellLandmarkPortTest {

	private static final String[] FLOORS = {"2", "3", "4"};

	private HeadlessRun run;

	@AfterEach
	void tearDown() {
		if (run != null) run.close();
	}

	/** A generated level holding a WeakFloorRoom, with the seed and floor that made it. */
	private static final class Found {
		final long seed;
		final String floor;
		final RegularLevel level;
		final WeakFloorRoom room;

		Found(long seed, String floor, RegularLevel level, WeakFloorRoom room) {
			this.seed = seed;
			this.floor = floor;
			this.level = level;
			this.room = room;
		}
	}

	private Found findWeakFloorRoom() {
		for (long seed = 20261115L; seed < 20261115L + 200; seed++) {
			for (String floor : FLOORS) {
				if (run != null) run.close();
				run = HeadlessRun.start(seed);
				Level level = run.generate(floor);
				if (!(level instanceof RegularLevel)) continue;
				for (Room r : ((RegularLevel) level).rooms()) {
					if (r instanceof WeakFloorRoom) {
						return new Found(seed, floor, (RegularLevel) level, (WeakFloorRoom) r);
					}
				}
			}
		}
		throw new AssertionError("no seed in range generated a weak floor room on floors 2-4");
	}

	private static int hiddenWellCell(Level level) {
		for (CustomTilemap t : level.customTiles) {
			if (t instanceof WeakFloorRoom.HiddenWell) {
				return t.tileX + level.width() * t.tileY;
			}
		}
		throw new AssertionError("weak floor room painted no hidden well tile");
	}

	private static int wellIdCell(Level level) {
		Blob b = level.blobs.get(WeakFloorRoom.WellID.class);
		assertNotNull(b, "the level must hold a WellID blob");
		int found = -1;
		for (int cell = 0; cell < b.cur.length; cell++) {
			if (b.cur[cell] != 0) {
				assertEquals(-1, found, "WellID must cover exactly one cell");
				assertEquals(1, b.cur[cell], "WellID's one cell must hold 1");
				found = cell;
			}
		}
		assertTrue(found >= 0, "WellID must cover a cell");
		return found;
	}

	@Test
	void weakFloorRoomSeedsAWellIdAtTheHiddenWell() {
		Found f = findWeakFloorRoom();
		int well = hiddenWellCell(f.level);
		assertEquals(well, wellIdCell(f.level), "WellID must sit at the hidden well");
		assertEquals(1, f.level.blobs.get(WeakFloorRoom.WellID.class).volume);

		Dungeon.level = f.level;
		run.saveLevel();
		Level loaded = run.loadLevel();
		assertEquals(well, wellIdCell(loaded), "WellID must survive a level save and load");
	}

	@Test
	void sameSeedPutsTheWellIdOnTheSameCell() {
		Found f = findWeakFloorRoom();
		int first = wellIdCell(f.level);
		run.close();
		run = HeadlessRun.start(f.seed);
		Level again = run.generate(f.floor);
		assertEquals(first, wellIdCell(again), "the same seed must seed WellID at the same cell");
	}

	@Test
	void seeingTheWellRecordsItAndClearsTheBlob() {
		Found f = findWeakFloorRoom();
		int well = wellIdCell(f.level);
		Dungeon.level = f.level;
		Notes.reset();
		Dungeon.hero.fieldOfView = new boolean[f.level.length()];

		Dungeon.hero.checkVisibleMobs();
		assertFalse(Notes.contains(Notes.Landmark.DISTANT_WELL), "an unseen well must not be recorded");

		Dungeon.hero.fieldOfView[well] = true;
		Dungeon.hero.checkVisibleMobs();
		assertTrue(Notes.contains(Notes.Landmark.DISTANT_WELL), "a seen well must be recorded");
		assertEquals(0, f.level.blobs.get(WeakFloorRoom.WellID.class).volume,
				"a recorded well's WellID must be fully cleared");
	}

	@Test
	void fallingIntoTheRoomRemovesTheDistantWell() {
		Found f = findWeakFloorRoom();
		Dungeon.level = f.level;
		Notes.reset();
		Notes.add(Notes.Landmark.DISTANT_WELL);
		int cell = f.room.left + 1 + f.level.width() * (f.room.top + 1);

		Game priorInstance = Game.instance;
		InterlevelScene.Mode priorMode = InterlevelScene.mode;
		boolean priorFallIntoPit = InterlevelScene.fallIntoPit;
		boolean fellIntoPit;
		new Game(null, null);
		try {
			Chasm.heroFall(cell);
			fellIntoPit = InterlevelScene.fallIntoPit;
		} finally {
			Game.instance = priorInstance;
			InterlevelScene.mode = priorMode;
			InterlevelScene.fallIntoPit = priorFallIntoPit;
		}

		assertTrue(fellIntoPit, "a fall inside the room is a fall into the pit");
		assertFalse(Notes.contains(Notes.Landmark.DISTANT_WELL), "falling into the room removes the distant well");
	}
}
