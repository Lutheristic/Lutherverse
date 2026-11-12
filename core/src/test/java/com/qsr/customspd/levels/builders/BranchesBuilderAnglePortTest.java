package com.qsr.customspd.levels.builders;

import com.qsr.customspd.levels.rooms.Room;
import com.qsr.customspd.levels.rooms.standard.EmptyRoom;
import com.qsr.customspd.levels.rooms.standard.entrance.EntranceRoom;
import com.qsr.customspd.test.HeadlessGdx;
import com.watabou.utils.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream b3450cc73 (v2.2.0): BranchesBuilder rolls five branch angles and keeps the one nearest
 * the direction back to the entrance, so branches curl toward it and the level stays compact.
 * RegularBuilder's default is a single uniform roll, which lands 90 degrees off that direction on
 * average.
 */
class BranchesBuilderAnglePortTest {

	private static final int SAMPLES = 400;
	/** Mean distance in degrees from the entrance direction; a uniform roll averages 90. */
	private static final double MAX_MEAN_OFFSET = 50;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@Test
	void branchAnglesLeanTowardTheEntrance() {
		Random.pushGenerator(2023L);
		try {
			BranchesBuilder builder = new BranchesBuilder();
			Room entrance = new EntranceRoom();
			entrance.set(0, 0, 6, 6);
			builder.entrance = entrance;

			// Right of, left of and above the entrance: the entrance lies at 270, 90 and 180 degrees.
			// Below it would point at 0, where the builder's plain difference of angles wraps.
			int[][] offsets = {{30, 0}, {-30, 0}, {0, -30}};
			for (int[] offset : offsets) {
				Room from = new EmptyRoom();
				from.set(offset[0], offset[1], offset[0] + 6, offset[1] + 6);
				float toEntrance = Builder.angleBetweenRooms(from, entrance);
				if (toEntrance < 0) toEntrance += 360f;

				double total = 0;
				for (int i = 0; i < SAMPLES; i++) {
					float angle = builder.randomBranchAngle(from);
					double diff = Math.abs(angle - toEntrance) % 360;
					total += Math.min(diff, 360 - diff);
				}
				double mean = total / SAMPLES;
				assertTrue(mean < MAX_MEAN_OFFSET, "branches from a room at " + offset[0] + "," + offset[1]
						+ " point " + Math.round(mean) + " degrees away from the entrance on average");
			}
		} finally {
			Random.popGenerator();
		}
	}
}
