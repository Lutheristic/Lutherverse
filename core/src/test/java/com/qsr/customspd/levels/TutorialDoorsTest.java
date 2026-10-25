package com.qsr.customspd.levels;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.SPDSettings;
import com.qsr.customspd.journal.Document;
import com.qsr.customspd.levels.rooms.Room;
import com.qsr.customspd.test.HeadlessRun;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RegularPainter hides the entrance doors on floor 1 while the tutorial runs, and on floor 2
 * until the searching guide page is found. A custom seed or daily run builds them as a returning
 * player would, so one seed gives everyone the same doors; an unseeded run keeps the tutorial
 * doors.
 */
class TutorialDoorsTest {

	private static final long SEED = 31337L;
	private static final String[] FLOORS = {"1", "2"};

	@Test
	void aSeededRunGivesANewPlayerTheSameDoorsAsAReturningOne() {
		for (String floor : FLOORS) {
			Doors returning = doors(floor, false, true);
			Doors fresh = doors(floor, true, true);
			assertEquals(returning.all, fresh.all, "floor " + floor + ": every door of a seeded run, new player vs returning");
			assertTrue(returning.entrance.contains(Terrain.DOOR), "precondition: floor " + floor + " has a plain entrance door");
		}
	}

	@Test
	void anUnseededRunStillHidesTheEntranceDoorsFromANewPlayer() {
		for (String floor : FLOORS) {
			Doors returning = doors(floor, false, false);
			Doors fresh = doors(floor, true, false);
			assertTrue(returning.entrance.contains(Terrain.DOOR), "precondition: floor " + floor + " has a plain entrance door");
			for (int i = 0; i < returning.entrance.size(); i++) {
				if (returning.entrance.get(i) == Terrain.DOOR) {
					assertEquals(Terrain.SECRET_DOOR, (int) fresh.entrance.get(i),
							"floor " + floor + ": entrance door " + i + " must be hidden from a new player");
				}
			}
		}
	}

	/** The terrain at every room door of a floor, and at the entrance room's doors, in room order. */
	private static final class Doors {
		final List<Integer> all = new ArrayList<>();
		final List<Integer> entrance = new ArrayList<>();
	}

	private static Doors doors(String floor, boolean newPlayer, boolean seeded) {
		try (HeadlessRun run = HeadlessRun.start(SEED)) {
			if (newPlayer) {
				SPDSettings.intro(true);
				Document.ADVENTURERS_GUIDE.deletePage(Document.GUIDE_SEARCHING);
			}
			if (!seeded) {
				// What Dungeon.init leaves for a run with no custom seed; the seed itself stays fixed.
				Dungeon.customSeedText = "";
			}
			RegularLevel level = (RegularLevel) run.generate(floor);
			Doors doors = new Doors();
			for (Room room : level.rooms) {
				for (Room.Door door : room.connected.values()) {
					int terrain = level.map[level.pointToCell(door)];
					doors.all.add(terrain);
					if (room.isEntrance()) doors.entrance.add(terrain);
				}
			}
			return doors;
		}
	}
}
