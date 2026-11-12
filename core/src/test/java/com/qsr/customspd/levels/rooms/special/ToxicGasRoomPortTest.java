package com.qsr.customspd.levels.rooms.special;

import com.qsr.customspd.levels.rooms.standard.EmptyRoom;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 86910fb5e: the toxic gas room already is a trap hazard, so the painter's random level
 * traps are not placed inside it. RegularPainter.paintTraps only picks cells from
 * trapPlaceablePoints(), and the room's floor is plain EMPTY, which that pass would otherwise take.
 */
class ToxicGasRoomPortTest {

	@Test
	void noRandomTrapsGoInsideTheGasRoom() {
		ToxicGasRoom room = new ToxicGasRoom();
		room.set(0, 0, 8, 8);
		assertTrue(room.trapPlaceablePoints().isEmpty(), "trap cells offered: " + room.trapPlaceablePoints().size());
	}

	@Test
	void otherRoomsStillTakeTraps() {
		EmptyRoom room = new EmptyRoom();
		room.set(0, 0, 8, 8);
		assertFalse(room.trapPlaceablePoints().isEmpty());
	}
}
