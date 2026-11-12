package com.qsr.customspd.levels.painters;

import com.qsr.customspd.levels.Terrain;
import com.qsr.customspd.levels.rooms.Room;
import com.qsr.customspd.levels.rooms.standard.EmptyRoom;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.watabou.utils.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Upstream e0f7f2d62: the caves painter's corner wall fill skips a corner when either wall
 * beside it holds a door connection, so the fill cannot wall off a door's approach.
 */
class CavesPainterCornerPortTest {

	private static final int SIZE = 16;
	private static final int LEFT = 2, TOP = 2, RIGHT = 13, BOTTOM = 13;

	/** Corner order: top left, top right, bottom left, bottom right. */
	private static final int[] SIDE_WALL = {-1, +1, -1, +1};
	private static final int[] END_WALL = {-SIZE, -SIZE, +SIZE, +SIZE};

	private interface Setup {
		void apply(HeadlessLevel level, EmptyRoom room, int[] corners);
	}

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
	}

	private static int[] corners() {
		return new int[]{
				(LEFT + 1) + (TOP + 1) * SIZE,
				(RIGHT - 1) + (TOP + 1) * SIZE,
				(LEFT + 1) + (BOTTOM - 1) * SIZE,
				(RIGHT - 1) + (BOTTOM - 1) * SIZE};
	}

	/** Paints a bare 12x12 standard room, applies the setup, decorates it with the seed, and reports which corners became wall. */
	private static boolean[] filledCorners(long seed, Setup setup) {
		HeadlessLevel level = HeadlessLevel.install(SIZE, SIZE);
		EmptyRoom room = new EmptyRoom();
		room.set(LEFT, TOP, RIGHT, BOTTOM);
		Painter.fill(level, room, Terrain.WALL);
		Painter.fill(level, room, 1, Terrain.EMPTY);
		int[] corners = corners();
		if (setup != null) setup.apply(level, room, corners);

		ArrayList<Room> rooms = new ArrayList<>();
		rooms.add(room);
		Random.pushGenerator(seed);
		try {
			new CavesPainter().decorate(level, rooms);
		} finally {
			Random.popGenerator();
		}

		boolean[] filled = new boolean[4];
		for (int i = 0; i < 4; i++) {
			filled[i] = (Terrain.flags[level.map[corners[i]]] & Terrain.SOLID) != 0;
		}
		HeadlessLevel.uninstall();
		return filled;
	}

	/** Seeds whose bare room gets all four corners walled, so each obstruction below has something to stop. */
	private static List<Long> seedsThatFillEveryCorner() {
		List<Long> seeds = new ArrayList<>();
		for (long seed = 1; seed <= 200 && seeds.size() < 5; seed++) {
			boolean[] filled = filledCorners(seed, null);
			if (filled[0] && filled[1] && filled[2] && filled[3]) seeds.add(seed);
		}
		assertFalse(seeds.isEmpty(), "test setup: no seed walled all four corners of a bare room");
		return seeds;
	}

	private static boolean[] allButOne(int corner) {
		boolean[] expected = {true, true, true, true};
		expected[corner] = false;
		return expected;
	}

	@Test
	void aDoorInTheSideWallKeepsItsCornerOpen() {
		for (long seed : seedsThatFillEveryCorner()) {
			for (int i = 0; i < 4; i++) {
				final int corner = i;
				boolean[] filled = filledCorners(seed, (level, room, c) -> {
					int door = c[corner] + SIDE_WALL[corner];
					room.connected.put(new EmptyRoom(), new Room.Door(level.cellToPoint(door)));
				});
				assertArrayEquals(allButOne(corner), filled, "seed " + seed + ", door beside corner " + corner);
			}
		}
	}

	@Test
	void aDoorInTheEndWallKeepsItsCornerOpen() {
		for (long seed : seedsThatFillEveryCorner()) {
			for (int i = 0; i < 4; i++) {
				final int corner = i;
				boolean[] filled = filledCorners(seed, (level, room, c) -> {
					int door = c[corner] + END_WALL[corner];
					room.connected.put(new EmptyRoom(), new Room.Door(level.cellToPoint(door)));
				});
				assertArrayEquals(allButOne(corner), filled, "seed " + seed + ", door above or below corner " + corner);
			}
		}
	}
}
