package com.qsr.customspd.levels.rooms.standard;

import com.qsr.customspd.levels.Terrain;
import com.qsr.customspd.levels.rooms.Room;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.SaveRoundtrip;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Point;
import com.watabou.utils.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 28628a31b: bridge rooms and the caves fissure room place no items or characters in
 * their gap or on the bridge across it. Upstream 354219135: a bridge room loaded from a save has
 * no gap rect (it is not saved) and still answers placement checks.
 */
class BridgeRoomPlacementPortTest {

	private static final int ORIGIN = 2;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
	}

	private static HeadlessLevel paint(StandardRoom room, int size, long seed) {
		HeadlessLevel level = HeadlessLevel.install(size + 2 * ORIGIN, size + 2 * ORIGIN);
		room.set(ORIGIN, ORIGIN, ORIGIN + size - 1, ORIGIN + size - 1);
		room.connected.put(new EmptyRoom(), new Room.Door(room.left, room.top + size / 2));
		room.connected.put(new EmptyRoom(), new Room.Door(room.right, room.top + size / 2));
		Random.pushGenerator(seed);
		try {
			room.paint(level);
		} finally {
			Random.popGenerator();
			PathFinder.setMapSize(level.width(), level.height());
		}
		return level;
	}

	/** Only plain floor takes items and characters; the gap and the bridge take neither. */
	private static void assertOnlyFloorIsPlaceable(StandardBridgeRoom room, int gapTile) {
		int gap = 0, bridge = 0, floor = 0;
		for (long seed = 1; seed <= 10; seed++) {
			HeadlessLevel level = paint(room, 9, seed);
			for (int x = room.left + 1; x < room.right; x++) {
				for (int y = room.top + 1; y < room.bottom; y++) {
					Point p = new Point(x, y);
					int terrain = level.map[level.pointToCell(p)];
					String where = "seed " + seed + " at " + x + "," + y + " on terrain " + terrain;
					boolean item = room.canPlaceItem(p, level);
					boolean ch = room.canPlaceCharacter(p, level);
					if (terrain == gapTile || terrain == Terrain.EMPTY_SP) {
						if (terrain == gapTile) gap++; else bridge++;
						assertFalse(item, where + ": an item can be placed in the gap or on the bridge");
						assertFalse(ch, where + ": a character can be placed in the gap or on the bridge");
					} else {
						assertEquals(Terrain.EMPTY, terrain, where);
						floor++;
						assertTrue(item, where + ": floor must still take items");
						assertTrue(ch, where + ": floor must still take characters");
					}
				}
			}
			room.connected.clear();
			HeadlessLevel.uninstall();
		}
		assertTrue(gap > 0 && bridge > 0 && floor > 0, "test setup: the rooms painted a gap, a bridge and floor");
	}

	@Test
	void aChasmBridgeRoomPlacesNothingInItsChasmOrOnItsBridge() {
		assertOnlyFloorIsPlaceable(new ChasmBridgeRoom(), Terrain.CHASM);
	}

	@Test
	void aWaterBridgeRoomPlacesNothingInItsWaterOrOnItsBridge() {
		assertOnlyFloorIsPlaceable(new WaterBridgeRoom(), Terrain.WATER);
	}

	@Test
	void aCavesFissureRoomPlacesNothingOnItsBridges() {
		int bridge = 0, floor = 0;
		for (long seed = 1; seed <= 20; seed++) {
			CavesFissureRoom room = new CavesFissureRoom();
			HeadlessLevel level = paint(room, 11, seed);
			for (int x = room.left + 1; x < room.right; x++) {
				for (int y = room.top + 1; y < room.bottom; y++) {
					Point p = new Point(x, y);
					int terrain = level.map[level.pointToCell(p)];
					String where = "seed " + seed + " at " + x + "," + y;
					if (terrain == Terrain.EMPTY_SP) {
						bridge++;
						assertFalse(room.canPlaceItem(p, level), where + ": an item can be placed on a bridge");
						assertFalse(room.canPlaceCharacter(p, level), where + ": a character can be placed on a bridge");
					} else if (terrain == Terrain.EMPTY) {
						floor++;
						assertTrue(room.canPlaceItem(p, level), where);
						assertTrue(room.canPlaceCharacter(p, level), where);
					}
				}
			}
			HeadlessLevel.uninstall();
		}
		assertTrue(bridge > 0 && floor > 0, "test setup: some fissure room painted a bridge");
	}

	@Test
	void aBridgeRoomLoadedFromASaveStillAnswersPlacementChecks() throws Exception {
		ChasmBridgeRoom painted = new ChasmBridgeRoom();
		HeadlessLevel level = paint(painted, 9, 1);
		ChasmBridgeRoom loaded = SaveRoundtrip.of(painted);
		Point inside = new Point(loaded.left + 1, loaded.top + 1);
		assertTrue(loaded.canPlaceItem(inside, level));
		assertTrue(loaded.canPlaceCharacter(inside, level));
		assertFalse(loaded.canPlaceItem(new Point(loaded.left, loaded.top), level), "a wall cell is still refused");
	}
}
