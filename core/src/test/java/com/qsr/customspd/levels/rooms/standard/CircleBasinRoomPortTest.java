package com.qsr.customspd.levels.rooms.standard;

import com.qsr.customspd.levels.Patch;
import com.qsr.customspd.levels.rooms.Room;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream c48b372bf: a circle basin room's water patch does not force a path through the room.
 * Water never blocks the way, so the room takes the first patch it rolls, door cells included.
 */
class CircleBasinRoomPortTest {

	private static final int ORIGIN = 2;
	private static final int SIZE = 13;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
	}

	@Test
	void aConnectedRoomTakesTheFirstPatchItRolls() {
		int doorCellsInWater = 0;
		for (long seed = 1; seed <= 20; seed++) {
			boolean[] expected;
			long expectedNext;
			Random.pushGenerator(seed);
			try {
				expected = Patch.generate(SIZE - 2, SIZE - 2, 0.5f, 5, true);
				expectedNext = Random.Long();
			} finally {
				Random.popGenerator();
			}

			HeadlessLevel level = HeadlessLevel.install(SIZE + 2 * ORIGIN, SIZE + 2 * ORIGIN);
			CircleBasinRoom room = new CircleBasinRoom();
			room.set(ORIGIN, ORIGIN, ORIGIN + SIZE - 1, ORIGIN + SIZE - 1);
			Room.Door door = new Room.Door(room.left, room.top + SIZE / 2);
			room.connected.put(new EmptyRoom(), door);

			long actualNext;
			Random.pushGenerator(seed);
			try {
				room.paint(level);
				actualNext = Random.Long();
			} finally {
				Random.popGenerator();
				PathFinder.setMapSize(level.width(), level.height());
			}

			assertTrue(Arrays.equals(expected, room.patch),
					"seed " + seed + ": the patch is exactly one Patch.generate roll, with nothing cleared at the door");
			assertEquals(expectedNext, actualNext,
					"seed " + seed + ": painting draws one patch and no retries");
			if (room.patch[room.xyToPatchCoords(door.x + 1, door.y)]) doorCellsInWater++;
		}
		assertTrue(doorCellsInWater > 0, "test setup: no seed rolled water beside the door, so nothing was checked");
	}
}
