package com.qsr.customspd.levels.rooms.special;

import com.qsr.customspd.items.Ankh;
import com.qsr.customspd.items.artifacts.TimekeepersHourglass;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A shop's minimum size no longer depends on the hourglass the player holds (upstream f23962ec5,
 * v2.4.0: "fixed very rare cases of hourglass affecting levelgen"). Sandbags are stocked from the
 * hero's hourglass, so counting them made the room's min size, and with it the generated floor,
 * vary with an item the seed does not decide. spacesNeeded() ignores them, always reserves the 4
 * the shop can carry at most, and adds one for the shopkeeper.
 */
class ShopRoomSpacesNeededPortTest {

	private static ShopRoom shopWith(int ordinary, int sandbags) {
		ShopRoom room = new ShopRoom();
		room.itemsToSpawn = new ArrayList<>();
		for (int i = 0; i < ordinary; i++) room.itemsToSpawn.add(new Ankh());
		for (int i = 0; i < sandbags; i++) room.itemsToSpawn.add(new TimekeepersHourglass.sandBag());
		return room;
	}

	@Test
	void sandbagsDoNotChangeTheSpaceTheShopAsksFor() {
		int none = shopWith(20, 0).spacesNeeded();
		assertEquals(20 + 4 + 1, none, "ordinary items + the 4 reserved for sandbags + the shopkeeper");
		for (int sandbags = 1; sandbags <= 4; sandbags++) {
			assertEquals(none, shopWith(20, sandbags).spacesNeeded(),
					sandbags + " sandbags in the stock must not change the size asked for");
		}
	}

	@Test
	void theMinimumSizeFollowsSpacesNeeded() {
		// 20 items -> 25 spaces -> sqrt(25)+3 = 8; counting items alone would give sqrt(20)+3 = 7
		ShopRoom room = shopWith(20, 2);
		assertEquals(8, room.minWidth());
		assertEquals(8, room.minHeight());
		assertEquals(8, shopWith(20, 0).minWidth(), "and the same with no sandbags at all");
	}
}
