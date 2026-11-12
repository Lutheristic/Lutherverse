package com.qsr.customspd.levels.builders;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.levels.rooms.Room;
import com.qsr.customspd.levels.rooms.standard.EmptyRoom;
import com.qsr.customspd.levels.rooms.standard.entrance.EntranceRoom;
import com.qsr.customspd.levels.rooms.standard.exit.ExitRoom;
import com.qsr.customspd.test.HeadlessGdx;
import com.watabou.utils.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream b454a8eb9 (v2.2.0): LineBuilder and BranchesBuilder place the rooms that
 * RegularBuilder.setupRooms moves onto the main path, and LoopBuilder and FigureEightBuilder can
 * build a level with no exit room.
 *
 * setupRooms takes a quarter of the multi-connection rooms, plus three, off multiConnections and
 * into mainPathRooms. LineBuilder and BranchesBuilder used to read only multiConnections, so
 * those rooms came back from build() unplaced: an empty rect with no doors. Loop and FigureEight
 * used to insert the exit into their loop unconditionally, and a null exit then threw in placeRoom.
 */
class BuilderMainPathPortTest {

	private static final int SEEDS = 10;
	/** RegularLevel.build retries a null layout forever; a test gives up somewhere. */
	private static final int MAX_BUILDS = 50;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	private int priorDepth;

	/** ConnectionRoom.createRoom reads its chances by Dungeon.depth, which is 0 outside a run. */
	@BeforeEach
	void sewerDepth() {
		priorDepth = Dungeon.depth;
		Dungeon.depth = 1;
	}

	@AfterEach
	void restoreDepth() {
		Dungeon.depth = priorDepth;
	}

	interface BuilderFactory {
		RegularBuilder create();
	}

	@Test
	void lineBuilderPlacesTheMainPathRooms() {
		assertEveryRoomPlaced(LineBuilder::new, 8, true);
	}

	/**
	 * BranchesBuilder hangs each main path room off the entrance and retries a placement until it
	 * fits, so enough of them box the entrance in for good. Three standard rooms put two on the
	 * entrance and leave one for the branches.
	 */
	@Test
	void branchesBuilderPlacesTheMainPathRooms() {
		assertEveryRoomPlaced(BranchesBuilder::new, 3, true);
	}

	@Test
	void loopBuilderBuildsWithoutAnExit() {
		assertEveryRoomPlaced(LoopBuilder::new, 8, false);
	}

	@Test
	void figureEightBuilderBuildsWithoutAnExit() {
		assertEveryRoomPlaced(FigureEightBuilder::new, 8, false);
	}

	private static void assertEveryRoomPlaced(BuilderFactory factory, int standardRooms, boolean withExit) {
		for (long seed = 1; seed <= SEEDS; seed++) {
			final long s = seed;
			assertTimeoutPreemptively(Duration.ofSeconds(30), () -> buildAndCheck(factory, standardRooms, withExit, s));
		}
	}

	private static void buildAndCheck(BuilderFactory factory, int standardRooms, boolean withExit, long seed) {
		Random.pushGenerator(seed);
		try {
			ArrayList<Room> rooms = new ArrayList<>();
			rooms.add(new EntranceRoom());
			if (withExit) rooms.add(new ExitRoom());
			for (int i = 0; i < standardRooms; i++) rooms.add(new EmptyRoom());

			RegularBuilder probe = factory.create();
			probe.setupRooms(new ArrayList<>(rooms));
			assertFalse(probe.mainPathRooms.isEmpty(), "precondition: setupRooms puts rooms on the main path");

			RegularBuilder builder = factory.create();
			ArrayList<Room> built = null;
			for (int attempt = 0; attempt < MAX_BUILDS && built == null; attempt++) {
				for (Room r : rooms) {
					r.neigbours.clear();
					r.connected.clear();
				}
				built = builder.build(new ArrayList<>(rooms));
			}
			String where = builder.getClass().getSimpleName() + ", seed " + seed;
			assertNotNull(built, where + ": no layout in " + MAX_BUILDS + " builds");
			for (Room r : rooms) {
				assertTrue(built.contains(r), where + ": " + r.getClass().getSimpleName() + " is missing from the layout");
				assertFalse(r.connected.isEmpty(), where + ": " + r.getClass().getSimpleName()
						+ " was left unplaced (" + r.left + "," + r.top + " to " + r.right + "," + r.bottom + ", no doors)");
			}
		} finally {
			Random.popGenerator();
		}
	}
}
