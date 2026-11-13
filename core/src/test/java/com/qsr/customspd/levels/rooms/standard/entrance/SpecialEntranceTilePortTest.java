package com.qsr.customspd.levels.rooms.standard.entrance;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.levels.Terrain;
import com.qsr.customspd.levels.features.LevelTransition;
import com.qsr.customspd.levels.rooms.Room;
import com.qsr.customspd.levels.rooms.standard.EmptyRoom;
import com.qsr.customspd.levels.rooms.standard.StandardRoom;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.HeadlessRun;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Upstream 345b6fe79: the circle basin and hallway entrance rooms put their stairs on the
 * special-floor entrance tile, as the statues entrance room already did.
 */
class SpecialEntranceTilePortTest {

	private static final int ORIGIN = 2;
	private static final int SIZE = 13;

	/**
	 * Paints the room on floor 2 of a real run (a transition reads the layout) with one door on
	 * its left wall, and returns the terrain under its entrance transition.
	 */
	private static int entranceTerrain(StandardRoom room, long seed) {
		try (HeadlessRun run = HeadlessRun.start(seed)) {
			Dungeon.levelName = "2";
			Dungeon.depth = 2;
			HeadlessLevel level = HeadlessLevel.install(SIZE + 2 * ORIGIN, SIZE + 2 * ORIGIN);
			room.set(ORIGIN, ORIGIN, ORIGIN + SIZE - 1, ORIGIN + SIZE - 1);
			room.connected.put(new EmptyRoom(), new Room.Door(room.left, room.top + SIZE / 2));
			Random.pushGenerator(seed);
			try {
				room.paint(level);
			} finally {
				Random.popGenerator();
				PathFinder.setMapSize(level.width(), level.height());
			}
			assertEquals(1, level.transitions.size(), "the room adds one transition");
			LevelTransition entrance = level.transitions.get(0);
			assertEquals(LevelTransition.Type.REGULAR_ENTRANCE, entrance.type);
			int terrain = level.map[entrance.cell()];
			HeadlessLevel.uninstall();
			return terrain;
		}
	}

	@Test
	void theCircleBasinEntranceUsesTheSpecialEntranceTile() {
		for (long seed = 1; seed <= 5; seed++) {
			assertEquals(Terrain.ENTRANCE_SP, entranceTerrain(new CircleBasinEntranceRoom(), seed), "seed " + seed);
		}
	}

	@Test
	void theHallwayEntranceUsesTheSpecialEntranceTile() {
		for (long seed = 1; seed <= 5; seed++) {
			assertEquals(Terrain.ENTRANCE_SP, entranceTerrain(new HallwayEntranceRoom(), seed), "seed " + seed);
		}
	}
}
