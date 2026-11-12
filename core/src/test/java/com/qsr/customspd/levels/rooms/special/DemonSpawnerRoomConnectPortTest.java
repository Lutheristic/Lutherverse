package com.qsr.customspd.levels.rooms.special;

import com.qsr.customspd.levels.rooms.Room;
import com.qsr.customspd.levels.rooms.standard.EmptyRoom;
import com.qsr.customspd.levels.rooms.standard.entrance.EntranceRoom;
import com.qsr.customspd.levels.rooms.standard.exit.ExitRoom;
import com.qsr.customspd.test.HeadlessGdx;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 769c459aa: a demon spawner room refuses to connect to an exit room (the isExit form
 * upstream's refactor gave it), and connects to an entrance room like any other room.
 */
class DemonSpawnerRoomConnectPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	/** Connects a fresh spawner room at (0,0)-(6,6) to the given room placed on its right edge. */
	private static boolean connects(Room other) {
		DemonSpawnerRoom spawner = new DemonSpawnerRoom();
		spawner.set(0, 0, 6, 6);
		other.set(6, 0, 12, 6);
		return spawner.connect(other);
	}

	@Test
	void aDemonSpawnerRoomRefusesAnExitRoom() {
		assertTrue(connects(new EmptyRoom()), "test setup: the two rooms share an edge and can connect");
		assertFalse(connects(new ExitRoom()));
	}

	@Test
	void aDemonSpawnerRoomConnectsToAnEntranceRoom() {
		assertTrue(connects(new EntranceRoom()));
	}
}
