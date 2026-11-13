package com.qsr.customspd.levels;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.mobs.Mob;
import com.qsr.customspd.actors.mobs.npcs.Wandmaker;
import com.qsr.customspd.levels.rooms.Room;
import com.qsr.customspd.levels.rooms.special.MassGraveRoom;
import com.qsr.customspd.levels.rooms.special.RotGardenRoom;
import com.qsr.customspd.levels.rooms.standard.RitualSiteRoom;
import com.qsr.customspd.levels.rooms.standard.entrance.ChasmBridgeEntranceRoom;
import com.qsr.customspd.levels.rooms.standard.entrance.EntranceRoom;
import com.qsr.customspd.levels.rooms.standard.entrance.PillarsEntranceRoom;
import com.qsr.customspd.test.HeadlessRun;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The wandmaker stands in the entrance room of the floor that holds his quest room, whichever
 * entrance room the prison rolled. Prison floors draw the plain entrance room (weight 2) or the
 * chasm bridge and pillars variants (weight 4 each), and the variants do not extend EntranceRoom.
 * Placing him from PrisonPainter, only next to an EntranceRoom, left him out on most of those
 * floors; v2.5.4's PrisonLevel.createMobs places him beside roomEntrance whatever its class.
 */
class PrisonWandmakerPlacementTest {

	private static final Set<Class<?>> PRISON_ENTRANCES = new LinkedHashSet<Class<?>>(Arrays.<Class<?>>asList(
			EntranceRoom.class, ChasmBridgeEntranceRoom.class, PillarsEntranceRoom.class));

	@Test
	void theWandmakerStandsInEachPrisonEntranceRoom() {
		Set<Class<?>> seen = new LinkedHashSet<>();
		for (long seed = 1; seed <= 60 && seen.size() < PRISON_ENTRANCES.size(); seed++) {
			try (HeadlessRun run = HeadlessRun.start(seed * 104729L)) {
				String floor = Dungeon.wandmakerLevel;
				Level generated = run.generate(floor);
				assertTrue(generated instanceof PrisonLevel, "precondition: wandmaker floor " + floor + " is a prison floor");
				PrisonLevel level = (PrisonLevel) generated;

				Class<?> entrance = level.roomEntrance.getClass();
				String where = "seed " + run.seed + " floor " + floor + " (" + entrance.getSimpleName() + ")";
				assertTrue(PRISON_ENTRANCES.contains(entrance), where + ": a prison entrance room");
				assertTrue(hasQuestRoom(level), where + ": precondition, the quest room spawned");

				Wandmaker npc = null;
				for (Mob m : level.mobs) {
					if (m instanceof Wandmaker) npc = (Wandmaker) m;
				}
				assertNotNull(npc, where + ": the wandmaker must be placed");
				assertTrue(level.roomEntrance.inside(level.cellToPoint(npc.pos)),
						where + ": the wandmaker stands inside the entrance room");
				seen.add(entrance);
			}
		}
		assertEquals(PRISON_ENTRANCES, seen, "every prison entrance room came up on a wandmaker floor");
	}

	private static boolean hasQuestRoom(RegularLevel level) {
		for (Room r : level.rooms()) {
			if (r instanceof MassGraveRoom || r instanceof RitualSiteRoom || r instanceof RotGardenRoom) {
				return true;
			}
		}
		return false;
	}
}
