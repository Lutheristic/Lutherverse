package com.qsr.customspd.levels.rooms.secret;

import com.qsr.customspd.test.StaticSnapshot;
import com.watabou.utils.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 82eacce54: secret rooms are picked from their run queue the way special rooms are, the
 * front of the queue 60% of the time, the next 30% and the one after 10%, and the room picked goes
 * to the back. The old pick took the smallest of 4 random indices, so it could reach anywhere in
 * the queue.
 */
class SecretRoomPortTest {

	private static final int DRAWS = 2000;

	private static final List<Class<? extends SecretRoom>> QUEUE = Arrays.<Class<? extends SecretRoom>>asList(
			SecretGardenRoom.class, SecretLaboratoryRoom.class, SecretLibraryRoom.class,
			SecretLarderRoom.class, SecretWellRoom.class, SecretRunestoneRoom.class,
			SecretArtilleryRoom.class, SecretChestChasmRoom.class, SecretHoneypotRoom.class,
			SecretHoardRoom.class, SecretMazeRoom.class, SecretSummoningRoom.class);

	private StaticSnapshot statics;

	@BeforeEach
	void capture() {
		statics = StaticSnapshot.capture(Arrays.<Class<?>>asList(SecretRoom.class));
	}

	@AfterEach
	void restore() {
		statics.restore();
	}

	/** How often each queue position is picked, starting from the same queue every time. */
	private static int[] picks(List<Class<? extends SecretRoom>> queue, long seed) {
		int[] counts = new int[queue.size()];
		Random.pushGenerator(seed);
		try {
			for (int i = 0; i < DRAWS; i++) {
				SecretRoom.runSecrets = new ArrayList<>(queue);
				SecretRoom room = SecretRoom.createRoom();
				counts[queue.indexOf(room.getClass())]++;
				assertEquals(room.getClass(), SecretRoom.runSecrets.get(queue.size() - 1),
						"the room picked goes to the back of the queue");
				assertEquals(queue.size(), SecretRoom.runSecrets.size());
			}
		} finally {
			Random.popGenerator();
		}
		return counts;
	}

	@Test
	void picksComeFromTheFrontThreeOfTheQueue() {
		int[] counts = picks(QUEUE, 82L);
		for (int i = 3; i < counts.length; i++) {
			assertEquals(0, counts[i], "queue position " + i + " was picked: " + Arrays.toString(counts));
		}
		assertNear(0.6f, counts[0], "front");
		assertNear(0.3f, counts[1], "second");
		assertNear(0.1f, counts[2], "third");
	}

	@Test
	void aShortQueueFoldsTheMissingChancesOntoItsLastRoom() {
		int[] two = picks(QUEUE.subList(0, 2), 83L);
		assertNear(0.6f, two[0], "front of two");
		assertNear(0.4f, two[1], "last of two");

		int[] one = picks(QUEUE.subList(0, 1), 84L);
		assertEquals(DRAWS, one[0]);
	}

	private static void assertNear(float share, int count, String what) {
		float actual = count / (float) DRAWS;
		assertTrue(Math.abs(actual - share) < 0.04f, what + ": picked " + actual + " of the time, expected about " + share);
	}
}
