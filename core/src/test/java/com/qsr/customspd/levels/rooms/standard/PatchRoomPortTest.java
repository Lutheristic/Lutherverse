package com.qsr.customspd.levels.rooms.standard;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.levels.Level;
import com.qsr.customspd.levels.Patch;
import com.qsr.customspd.levels.Terrain;
import com.qsr.customspd.levels.rooms.Room;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayDeque;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cave, chasm and ruins rooms painted on a headless level.
 *
 * Upstream 1e82b73c0 made patch rooms more flexible: the fill rate stops growing past 18x18, so a
 * bigger room is not simply filled solid, and a path through the room is only forced when the
 * room has a door to reach. Without the cap, any of these rooms past about 27x27 asked Patch for
 * a fill above 100%, which it can never reach, and levelgen hung.
 *
 * Upstream 7d595da27: cave rooms are at least 5x5, so a small one no longer looks like a tunnel.
 */
class PatchRoomPortTest {

	private static final int ORIGIN = 2;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
	}

	private interface RoomMaker {
		PatchRoom make();
	}

	/** Builds and paints a w x h room at ORIGIN on a fresh level, all under one seed. */
	private static PatchRoom paint(RoomMaker maker, int w, int h, long seed, boolean withDoor) {
		HeadlessLevel level = HeadlessLevel.install(w + 2 * ORIGIN, h + 2 * ORIGIN);
		Random.pushGenerator(seed);
		try {
			PatchRoom room = maker.make();
			room.set(ORIGIN, ORIGIN, ORIGIN + w - 1, ORIGIN + h - 1);
			if (withDoor) {
				room.connected.put(new EmptyRoom(), new Room.Door(room.left, room.top + h / 2));
			}
			room.paint(level);
			assertEquals(w, room.width(), "test setup");
			assertEquals(h, room.height(), "test setup");
			return room;
		} finally {
			Random.popGenerator();
			PathFinder.setMapSize(level.width(), level.height());
		}
	}

	private static float interiorShare(Level level, Room room, int terrain) {
		int hits = 0, cells = 0;
		for (int y = room.top + 1; y < room.bottom; y++) {
			for (int x = room.left + 1; x < room.right; x++) {
				cells++;
				if (level.map[x + y * level.width()] == terrain) hits++;
			}
		}
		return hits / (float) cells;
	}

	/** Groups of open interior cells, joined the way PathFinder walks (8 directions). */
	private static int openRegions(Level level, Room room) {
		int w = level.width();
		boolean[] seen = new boolean[level.length()];
		int regions = 0;
		for (int y = room.top + 1; y < room.bottom; y++) {
			for (int x = room.left + 1; x < room.right; x++) {
				int start = x + y * w;
				if (seen[start] || level.map[start] != Terrain.EMPTY) continue;
				regions++;
				ArrayDeque<Integer> queue = new ArrayDeque<>();
				queue.add(start);
				seen[start] = true;
				while (!queue.isEmpty()) {
					int c = queue.poll();
					for (int n : PathFinder.NEIGHBOURS8) {
						int next = c + n;
						int nx = next % w, ny = next / w;
						if (nx <= room.left || nx >= room.right || ny <= room.top || ny >= room.bottom) continue;
						if (seen[next] || level.map[next] != Terrain.EMPTY) continue;
						seen[next] = true;
						queue.add(next);
					}
				}
			}
		}
		return regions;
	}

	@Test
	void roomsPastEighteenByEighteenAreNotFilledSolid() {
		// 30x30: the old 0.30 + area/1024 fill came to 1.18, more than the interior holds;
		// capped at 18x18 it is 0.30 + 324/1024 = 0.62 (ruins 0.52)
		assertTimeoutPreemptively(Duration.ofSeconds(20), () -> {
			PatchRoom cave = paint(CaveRoom::new, 30, 30, 1L, false);
			float caveWalls = interiorShare(Dungeon.level, cave, Terrain.WALL);
			assertTrue(caveWalls > 0.5f && caveWalls < 0.7f, "cave wall share " + caveWalls);

			PatchRoom chasm = paint(ChasmRoom::new, 30, 30, 2L, false);
			float chasmCells = interiorShare(Dungeon.level, chasm, Terrain.CHASM);
			assertTrue(chasmCells > 0.5f && chasmCells < 0.7f, "chasm share " + chasmCells);

			PatchRoom ruins = paint(RuinsRoom::new, 30, 30, 3L, false);
			float ruinWalls = interiorShare(Dungeon.level, ruins, Terrain.WALL);
			assertTrue(ruinWalls > 0.4f && ruinWalls < 0.6f, "ruins wall share " + ruinWalls);
		}, "painting a 30x30 patch room did not finish");
	}

	@Test
	void aPathIsForcedOnlyWhenTheRoomHasADoor() {
		int[] splitWithoutDoor = {0};
		assertTimeoutPreemptively(Duration.ofSeconds(60), () -> {
			for (long seed = 1; seed <= 20; seed++) {
				PatchRoom open = paint(RuinsRoom::new, 16, 16, seed, false);
				if (openRegions(Dungeon.level, open) > 1) splitWithoutDoor[0]++;

				PatchRoom joined = paint(RuinsRoom::new, 16, 16, seed, true);
				assertEquals(1, openRegions(Dungeon.level, joined),
						"seed " + seed + ": a room with a door keeps every open cell reachable");
			}
		});
		assertTrue(splitWithoutDoor[0] > 0, "no seed left a room without doors in more than one piece,"
				+ " so a path is still being forced through it");
	}

	@Test
	void thePathCheckStartsFromTheRoomCentre() {
		// center() flips a coin on each even axis, so a 16x16 room with a door draws two more
		// values per attempt than Patch.generate alone, even though the door then sets the start.
		HeadlessLevel level = HeadlessLevel.install(16 + 2 * ORIGIN, 16 + 2 * ORIGIN);
		RuinsRoom room = new RuinsRoom();
		room.set(ORIGIN, ORIGIN, ORIGIN + 15, ORIGIN + 15);
		room.connected.put(new EmptyRoom(), new Room.Door(room.left, room.top + 8));

		long expected;
		Random.pushGenerator(5L);
		try {
			Patch.generate(room.width() - 2, room.height() - 2, 0.1f, 0, true);
			room.center();
			expected = Random.Long();
		} finally {
			Random.popGenerator();
		}

		long actual;
		Random.pushGenerator(5L);
		try {
			room.setupPatch(level, 0.1f, 0, true);
			actual = Random.Long();
		} finally {
			Random.popGenerator();
			PathFinder.setMapSize(level.width(), level.height());
		}
		assertEquals(expected, actual, "one Patch.generate attempt plus center() before the door start point");
	}

	@Test
	void caveRoomsAreAtLeastFiveByFive() {
		CaveRoom cave = new CaveRoom();
		for (StandardRoom.SizeCategory cat : StandardRoom.SizeCategory.values()) {
			cave.sizeCat = cat;
			assertTrue(cave.minWidth() >= 5, cat + " cave min width " + cave.minWidth());
			assertTrue(cave.minHeight() >= 5, cat + " cave min height " + cave.minHeight());
			assertEquals(Math.max(5, cat.minDim), cave.minWidth());
			assertEquals(cat.maxDim, cave.maxWidth(), "the maximum size is unchanged");
		}
	}
}
