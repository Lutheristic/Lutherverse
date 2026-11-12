package com.qsr.customspd.levels.rooms.special;

import com.watabou.utils.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 6a6e049f4: a traps room is 6-8 cells each way rather than the 5-10 every special room
 * gets, so it never holds too few traps or grows too big to cross to the chest.
 */
class TrapsRoomPortTest {

	@Test
	void sizeLimitsAreSixToEight() {
		TrapsRoom room = new TrapsRoom();
		assertEquals(6, room.minWidth());
		assertEquals(8, room.maxWidth());
		assertEquals(6, room.minHeight());
		assertEquals(8, room.maxHeight());
	}

	@Test
	void rolledSizesStayInsideTheLimits() {
		boolean sawSix = false, sawEight = false;
		Random.pushGenerator(66L);
		try {
			for (int i = 0; i < 500; i++) {
				TrapsRoom room = new TrapsRoom();
				assertTrue(room.setSize(), "the room rolls a size");
				for (int d : new int[]{room.width(), room.height()}) {
					assertTrue(d >= 6 && d <= 8, "a traps room " + room.width() + "x" + room.height());
					sawSix |= d == 6;
					sawEight |= d == 8;
				}
			}
		} finally {
			Random.popGenerator();
		}
		assertTrue(sawSix && sawEight, "both ends of the range come up");
	}
}
