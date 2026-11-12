package com.qsr.customspd.levels.rooms.standard;

import com.qsr.customspd.levels.Terrain;
import com.qsr.customspd.levels.rooms.Room;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.HeadlessRun;
import com.watabou.utils.Point;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Upstream e45206016: the ritual site's 3x3 floor under its marker overlay is the unmodifiable
 * empty tile, so fire no longer leaves embers showing through the marker.
 */
class RitualSiteRoomPortTest {

	@Test
	void theRitualMarkerFloorIsTheUnmodifiableEmptyTile() {
		try (HeadlessRun run = HeadlessRun.start(20230919L)) {
			HeadlessLevel level = HeadlessLevel.install(11, 11);
			RitualSiteRoom site = new RitualSiteRoom();
			site.set(1, 1, 9, 9);
			site.connected.put(new EmptyRoom(), new Room.Door(site.left, site.top + 4));
			site.paint(level);

			Point c = site.center();
			for (int x = c.x - 1; x <= c.x + 1; x++) {
				for (int y = c.y - 1; y <= c.y + 1; y++) {
					int cell = x + y * level.width();
					assertEquals(Terrain.CUSTOM_DECO_EMPTY, level.map[cell], "marker floor at " + x + "," + y);
					level.destroy(cell);
					assertEquals(Terrain.CUSTOM_DECO_EMPTY, level.map[cell], "fire changed the marker floor at " + x + "," + y);
				}
			}
			int plain = (c.x - 2) + c.y * level.width();
			level.destroy(plain);
			assertEquals(Terrain.EMBERS, level.map[plain], "test setup: fire does burn the floor beside the marker");
			HeadlessLevel.uninstall();
		}
	}
}
