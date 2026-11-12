package com.qsr.customspd.levels.rooms.special;

import com.qsr.customspd.levels.rooms.Room;
import com.qsr.customspd.levels.rooms.standard.EmptyRoom;
import com.qsr.customspd.levels.rooms.standard.entrance.EntranceRoom;
import com.qsr.customspd.test.HeadlessGdx;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 123e20b66: the mass grave keeps at least 3 rooms between itself and the entrance
 * room, so the corpse dust is never picked up right at the start of the floor.
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
}
