package com.qsr.customspd.levels.builders;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.levels.Level;
import com.qsr.customspd.levels.rooms.Room;
import com.qsr.customspd.levels.rooms.standard.EmptyRoom;
import com.qsr.customspd.levels.rooms.standard.entrance.EntranceRoom;
import com.qsr.customspd.levels.rooms.standard.exit.ExitRoom;
import com.qsr.customspd.test.HeadlessGdx;
import com.watabou.utils.Point;
import com.watabou.utils.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 175bf532d (v2.2.0): RegularBuilder.createBranches gives up after 100 failed branch
 * attempts, and every builder that branches then returns null so RegularLevel.build lays the floor
 * out again. Before, a room that could not be placed was retried forever and level generation hung.
 *
 * The level here has one dead-end room that takes a door on no edge, so no branch can ever reach
 * it. Every build runs under a timeout, so the old endless retry fails the test instead of hanging
 * the suite.
 */
class BranchFailureLimitPortTest {

	private static final int STANDARD_ROOMS = 6;
	/**
	 * A failed branch tries its room 10 times (or never reaches it when a tunnel fails first), and
	 * the builder stops after the 101st failure.
	 */
	private static final int MAX_PLACEMENTS = 101 * 10;
	/** Loop and FigureEight can fail to close their loop and return null before branching; retry those. */
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
	void loopBuilderGivesUpOnAnUnplaceableBranch() {
		assertGivesUp(LoopBuilder::new);
	}

	@Test
	void figureEightBuilderGivesUpOnAnUnplaceableBranch() {
		assertGivesUp(FigureEightBuilder::new);
	}

	@Test
	void lineBuilderGivesUpOnAnUnplaceableBranch() {
		assertGivesUp(LineBuilder::new);
	}

	@Test
	void branchesBuilderGivesUpOnAnUnplaceableBranch() {
		assertGivesUp(BranchesBuilder::new);
	}

	private static void assertGivesUp(BuilderFactory factory) {
		assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
			Random.pushGenerator(175L);
			try {
				ArrayList<Room> rooms = new ArrayList<>();
				rooms.add(new EntranceRoom());
				rooms.add(new ExitRoom());
				for (int i = 0; i < STANDARD_ROOMS; i++) rooms.add(new EmptyRoom());
				SealedRoom sealed = new SealedRoom();
				rooms.add(sealed);

				RegularBuilder builder = factory.create();
				String where = builder.getClass().getSimpleName();
				for (int attempt = 0; attempt < MAX_BUILDS && sealed.placements == 0; attempt++) {
					for (Room r : rooms) {
						r.neigbours.clear();
						r.connected.clear();
					}
					assertNull(builder.build(new ArrayList<>(rooms)), where + " built a layout around a room that cannot be placed");
				}
				assertTrue(sealed.placements > 0, where + " never got as far as branching in " + MAX_BUILDS + " builds");
				assertEquals(0, sealed.placements % 10, where + ": each failed branch tries its room 10 times");
				assertTrue(sealed.placements >= 100, where + " gave up after trying the room only " + sealed.placements + " times");
				assertTrue(sealed.placements <= MAX_PLACEMENTS, where + " tried the room " + sealed.placements
						+ " times; it should stop after 101 failed branches");
			} finally {
				Random.popGenerator();
			}
		});
	}

	/** A one-door dead end that accepts a door on no edge, so placing it can never connect. */
	private static final class SealedRoom extends Room {

		int placements = 0;

		@Override public int minWidth() { return 4; }
		@Override public int maxWidth() { return 4; }
		@Override public int minHeight() { return 4; }
		@Override public int maxHeight() { return 4; }

		@Override
		public int maxConnections(int direction) {
			return 1;
		}

		@Override
		public boolean canConnect(Point p) {
			return false;
		}

		/** Builder.placeRoom sizes the room first, so this counts placement attempts. */
		@Override
		public boolean setSizeWithLimit(int w, int h) {
			placements++;
			return super.setSizeWithLimit(w, h);
		}

		@Override
		public void paint(Level level) {
		}
	}
}
