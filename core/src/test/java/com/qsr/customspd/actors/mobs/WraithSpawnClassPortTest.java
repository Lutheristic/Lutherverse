package com.qsr.customspd.actors.mobs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.items.quest.CorpseDust;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.HeadlessRun;
import com.watabou.utils.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream cdec9a68b: Wraith.spawnAt takes the wraith class to spawn. A named class is spawned
 * as given and never rolls the exotic tormented spirit; no class keeps the 1 in 100 exotic roll
 * the tomb, haunted heap and distortion trap spawns always had.
 */
class WraithSpawnClassPortTest {

	private static final int SIZE = 9;
	private static final int CENTER = SIZE / 2 * SIZE + SIZE / 2;

	private HeadlessRun run;
	private HeadlessLevel level;

	@BeforeEach
	void setUp() {
		run = HeadlessRun.start(20231101L);
		level = HeadlessLevel.install(SIZE, SIZE);
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		run.close();
	}

	/** Spawns at the centre with the seed and returns the wraith that landed on the level. */
	private Wraith spawn(long seed, Class<? extends Wraith> wraithClass, boolean anyClass) {
		List<Mob> before = new ArrayList<>(level.mobs);
		Random.pushGenerator(seed);
		try {
			if (anyClass) Wraith.spawnAt(CENTER);
			else Wraith.spawnAt(CENTER, wraithClass);
		} catch (NullPointerException e) {
			// GameScene.add puts the wraith on the level, then needs a scene for its sprite.
			StackTraceElement[] st = e.getStackTrace();
			if (!(st[0].getClassName().endsWith(".GameScene") && "add".equals(st[0].getMethodName()))) {
				throw e;
			}
		} finally {
			Random.popGenerator();
		}
		Wraith spawned = null;
		for (Mob m : level.mobs) {
			if (!before.contains(m)) spawned = (Wraith) m;
		}
		assertNotNull(spawned, "seed " + seed + ": nothing spawned on an open cell");
		level.mobs.remove(spawned);
		return spawned;
	}

	@Test
	void aNamedWraithClassIsSpawnedAsGiven() {
		Wraith w = spawn(1, CorpseDust.DustWraith.class, false);
		assertEquals(CorpseDust.DustWraith.class, w.getClass());
		assertEquals(Dungeon.scalingDepth(), w.attackSkill(null) - 10, "it is levelled like any spawned wraith");
	}

	@Test
	void onlyAnUnnamedSpawnRollsTheExoticSpirit() {
		int exotic = 0;
		for (long seed = 1; seed <= 1000; seed++) {
			assertEquals(Wraith.class, spawn(seed, Wraith.class, false).getClass(), "seed " + seed);
			if (spawn(seed, null, true) instanceof TormentedSpirit) exotic++;
		}
		assertTrue(exotic > 0 && exotic < 50, exotic + " tormented spirits in 1000 unnamed spawns");
	}
}
