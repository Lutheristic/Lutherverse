package com.qsr.customspd.levels;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.mobs.Mob;
import com.qsr.customspd.actors.mobs.npcs.Ghost;
import com.qsr.customspd.levels.rooms.Room;
import com.qsr.customspd.modding.DungeonLayout;
import com.qsr.customspd.test.HeadlessRun;
import com.watabou.utils.Point;
import kotlinx.serialization.json.Json;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 4bbd69a47, 97b8edd4d and 6217f64f1 (v2.2.0, v2.5.0): the sad ghost spawns on a random
 * open cell inside the level's exit room, never on the exit itself. A floor without rooms (a
 * pack's custom level) has no exit room, so the ghost spawns there as it did before.
 */
class GhostExitRoomSpawnTest {

	@Test
	void theGhostSpawnsInsideTheExitRoom() {
		for (long seed = 1; seed <= 15; seed++) {
			try (HeadlessRun run = HeadlessRun.start(seed)) {
				String ghostLevel = Dungeon.ghostLevel;
				assertNotNull(ghostLevel, "the shipped layout has a ghost floor");
				Level level = run.generate(ghostLevel);
				assertTrue(level instanceof RegularLevel, "seed " + seed + ": the ghost floor is a regular level");

				Ghost ghost = onlyGhost(level, "seed " + seed);
				Point p = level.cellToPoint(ghost.pos);
				Room exitRoom = ((RegularLevel) level).roomExit;
				assertTrue(exitRoom.inside(p), "seed " + seed + ": the ghost at " + p.x + "," + p.y + " is inside the exit room ("
						+ exitRoom.left + "," + exitRoom.top + ")-(" + exitRoom.right + "," + exitRoom.bottom + ")");
				assertFalse(level.solid[ghost.pos], "seed " + seed + ": not inside a wall");
				assertNotEquals(level.exit(), ghost.pos, "seed " + seed + ": not on the exit");
			}
		}
	}

	@Test
	void onACustomLevelTheGhostStillSpawns() {
		DungeonLayout layout = Json.Default.decodeFromString(DungeonLayout.Companion.serializer(), resource("custom-level-mimic-layout.json"));
		for (long seed = 1; seed <= 3; seed++) {
			try (HeadlessRun run = HeadlessRun.start(seed)) {
				Dungeon.layout = layout;
				Dungeon.levelName = layout.getStart();
				Dungeon.depth = layout.getDungeon().get(layout.getStart()).getDepth();
				Dungeon.ghostLevel = layout.getStart();

				Level level = run.enterFirstLevel();
				assertTrue(level instanceof CustomLevel, "test setup: a custom level");
				Ghost ghost = onlyGhost(level, "seed " + seed);
				assertTrue(level.passable[ghost.pos], "seed " + seed + ": the ghost stands on a passable cell");
			}
		}
	}

	private static Ghost onlyGhost(Level level, String where) {
		Ghost ghost = null;
		int ghosts = 0;
		for (Mob m : level.mobs) {
			if (m instanceof Ghost) {
				ghost = (Ghost) m;
				ghosts++;
			}
		}
		assertEquals(1, ghosts, where + ": one ghost on its floor");
		return ghost;
	}

	private static String resource(String name) {
		try (InputStream in = GhostExitRoomSpawnTest.class.getResourceAsStream(name)) {
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
