package com.qsr.customspd.levels.rooms.special;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.mobs.Mob;
import com.qsr.customspd.items.Heap;
import com.qsr.customspd.items.Item;
import com.qsr.customspd.items.quest.CorpseDust;
import com.qsr.customspd.levels.Terrain;
import com.qsr.customspd.levels.rooms.Room;
import com.qsr.customspd.levels.rooms.standard.EmptyRoom;
import com.qsr.customspd.levels.rooms.standard.entrance.EntranceRoom;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.HeadlessRun;
import com.watabou.utils.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 123e20b66: the mass grave keeps at least 3 rooms between itself and the entrance
 * room, so the corpse dust is never picked up right at the start of the floor.
 * Upstream e45206016: its floor under the bones overlay is the unmodifiable empty tile, and the
 * skeletons and remains are placed on it.
 */
class MassGraveRoomPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	private static void link(Room a, Room b) {
		a.connected.put(b, null);
		b.connected.put(a, null);
	}

	/**
	 * Whether a fresh mass grave at (0,0)-(6,6) may connect to a room on its right edge that sits
	 * {@code hops} connections away from an entrance room; -1 means no entrance at all.
	 */
	private static boolean canConnectAt(int hops) {
		MassGraveRoom grave = new MassGraveRoom();
		grave.set(0, 0, 6, 6);
		Room candidate = hops == 0 ? new EntranceRoom() : new EmptyRoom();
		candidate.set(6, 0, 12, 6);
		Room last = candidate;
		for (int i = 1; i <= hops; i++) {
			Room next = i == hops ? new EntranceRoom() : new EmptyRoom();
			link(last, next);
			last = next;
		}
		if (hops < 0) link(candidate, new EmptyRoom());
		return grave.canConnect(candidate);
	}

	@Test
	void aMassGraveConnectsAwayFromTheEntrance() {
		assertTrue(canConnectAt(-1), "test setup: the two rooms share an edge and can connect");
		assertTrue(canConnectAt(4));
	}

	@Test
	void aMassGraveKeepsThreeRoomsFromTheEntrance() {
		for (int hops = 0; hops <= 3; hops++) {
			assertFalse(canConnectAt(hops), "a room " + hops + " connections from the entrance");
		}
	}

	@Test
	void aMassGraveFloorIsTheUnmodifiableEmptyTile() {
		for (long seed = 1; seed <= 5; seed++) {
			try (HeadlessRun run = HeadlessRun.start(seed)) {
				HeadlessLevel level = HeadlessLevel.install(11, 11);
				MassGraveRoom grave = new MassGraveRoom();
				grave.set(1, 1, 9, 9);
				grave.connected.put(new EmptyRoom(), new Room.Door(grave.left, grave.top + 4));
				Dungeon.level = null; // as during levelgen (Dungeon.newLevel)
				Random.pushGenerator(seed);
				try {
					grave.paint(level);
				} finally {
					Random.popGenerator();
				}

				for (int x = grave.left + 1; x < grave.right; x++) {
					for (int y = grave.top + 1; y < grave.bottom; y++) {
						assertEquals(Terrain.CUSTOM_DECO_EMPTY, level.map[x + y * level.width()],
								"seed " + seed + " at " + x + "," + y);
					}
				}
				boolean dust = false;
				for (Heap h : level.heaps.valueList()) {
					assertEquals(Terrain.CUSTOM_DECO_EMPTY, level.map[h.pos], "seed " + seed + ": remains off the grave floor");
					for (Item item : h.items) dust |= item instanceof CorpseDust;
				}
				assertTrue(dust, "seed " + seed + ": the corpse dust was placed");
				assertFalse(level.mobs.isEmpty(), "seed " + seed + ": skeletons were placed");
				for (Mob m : level.mobs) {
					assertEquals(Terrain.CUSTOM_DECO_EMPTY, level.map[m.pos], "seed " + seed + ": a skeleton off the grave floor");
				}
				HeadlessLevel.uninstall();
			}
		}
	}
}
