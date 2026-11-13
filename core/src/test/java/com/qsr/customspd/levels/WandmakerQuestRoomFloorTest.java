package com.qsr.customspd.levels;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.mobs.Mob;
import com.qsr.customspd.actors.mobs.npcs.Wandmaker;
import com.qsr.customspd.levels.rooms.Room;
import com.qsr.customspd.levels.rooms.special.MassGraveRoom;
import com.qsr.customspd.levels.rooms.special.RotGardenRoom;
import com.qsr.customspd.levels.rooms.standard.RitualSiteRoom;
import com.qsr.customspd.modding.DungeonLayout;
import com.qsr.customspd.test.HeadlessRun;
import kotlinx.serialization.json.Json;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The wandmaker's quest room and the wandmaker himself come from the same prison floor build,
 * as in v2.5.4 where PrisonLevel both adds the room and places him. A pack layout that names a
 * sewer floor as the wandmaker floor must not leave the quest armed for the next prison floor,
 * which would then get a wandmaker with no quest room.
 */
class WandmakerQuestRoomFloorTest {

	private static final String LAYOUT = "sewer-wandmaker-layout.json";

	@Test
	void aSewerWandmakerFloorLeavesNothingArmedForTheNextPrisonFloor() throws Exception {
		DungeonLayout layout = Json.Default.decodeFromString(DungeonLayout.Companion.serializer(), resource(LAYOUT));
		for (long seed = 1; seed <= 3; seed++) {
			try (HeadlessRun run = HeadlessRun.start(seed)) {
				Dungeon.layout = layout;
				Dungeon.levelName = layout.getStart();
				Dungeon.depth = layout.getDungeon().get(layout.getStart()).getDepth();
				Dungeon.wandmakerLevel = layout.getStart();

				Level sewer = run.enterFirstLevel();
				assertTrue(sewer instanceof SewerLevel, "test setup: the wandmaker floor is a sewer floor");
				assertQuestRoomOnlyWithTheWandmaker((RegularLevel) sewer, "seed " + seed + " sewer floor");
				assertFalse(questRoomSpawned(), "seed " + seed + ": the quest room flag outlived the sewer floor");

				Level prison = run.descend();
				assertTrue(prison instanceof PrisonLevel, "test setup: the next floor is a prison floor");
				assertEquals(0, wandmakers(prison), "seed " + seed + ": the next prison floor is not the wandmaker floor");
				assertQuestRoomOnlyWithTheWandmaker((RegularLevel) prison, "seed " + seed + " prison floor");
				assertFalse(questRoomSpawned(), "seed " + seed + ": the quest room flag outlived the prison floor");
			}
		}
	}

	private static void assertQuestRoomOnlyWithTheWandmaker(RegularLevel level, String where) {
		boolean questRoom = false;
		for (Room r : level.rooms()) {
			if (r instanceof MassGraveRoom || r instanceof RitualSiteRoom || r instanceof RotGardenRoom) questRoom = true;
		}
		assertEquals(questRoom, wandmakers(level) == 1,
				where + ": the quest room and the wandmaker come together (quest room " + questRoom + ", wandmakers " + wandmakers(level) + ")");
	}

	private static int wandmakers(Level level) {
		int n = 0;
		for (Mob m : level.mobs) {
			if (m instanceof Wandmaker) n++;
		}
		return n;
	}

	private static boolean questRoomSpawned() throws Exception {
		Field f = Wandmaker.Quest.class.getDeclaredField("questRoomSpawned");
		f.setAccessible(true);
		return f.getBoolean(null);
	}

	private static String resource(String name) {
		try (InputStream in = WandmakerQuestRoomFloorTest.class.getResourceAsStream(name)) {
			assertNotNull(in, "test resource " + name);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buf = new byte[4096];
			for (int n; (n = in.read(buf)) > 0; ) out.write(buf, 0, n);
			return new String(out.toByteArray(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
