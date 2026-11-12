package com.qsr.customspd.items.quest;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Mob;
import com.qsr.customspd.actors.mobs.Wraith;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.test.StubSprite;
import com.watabou.utils.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 123e20b66: the corpse dust spawner picks its wraith from every visible open cell more
 * than 3 tiles from the hero, never beside them; the summon cost caps at 49 turns instead of 25;
 * and power is only spent when a wraith actually spawns.
 */
class CorpseDustSpawnerPortTest {

	private static final int SIZE = 15;
	private static final int CENTER = SIZE / 2 * SIZE + SIZE / 2;

	private HeadlessRun run;
	private Hero hero;
	private HeadlessLevel level;
	private CorpseDust.DustGhostSpawner spawner;

	@BeforeEach
	void setUp() {
		run = HeadlessRun.start(20230724L);
		hero = Dungeon.hero;
		level = HeadlessLevel.install(SIZE, SIZE);
		HeadlessLevel.at(hero, CENTER);
		StubSprite.attach(hero);
		seeWithin(SIZE);
		hero.belongings.backpack.items.add(new CorpseDust());
		spawner = Buff.affect(hero, CorpseDust.DustGhostSpawner.class);
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		run.close();
	}

	/** The hero sees every cell within the given distance, and nothing further. */
	private void seeWithin(int distance) {
		for (int i = 0; i < level.length(); i++) {
			level.heroFOV[i] = level.distance(i, hero.pos) <= distance;
		}
	}

	/** A wraith the spawner already summoned, standing on the cell. */
	private void existingDustWraith(int cell) {
		Wraith w = HeadlessLevel.at(new Wraith(), cell);
		level.mobs.add(w);
	}

	/** One spawner turn; the wraith it summoned, or null. */
	private Mob tick() {
		List<Mob> before = new ArrayList<>(level.mobs);
		try {
			spawner.act();
		} catch (NullPointerException e) {
			// GameScene.add puts the wraith on the level, then needs a scene for its sprite.
			// Only that NPE is tolerated.
			StackTraceElement[] st = e.getStackTrace();
			if (!(st[0].getClassName().endsWith(".GameScene") && "add".equals(st[0].getMethodName()))) {
				throw e;
			}
		}
		for (Mob m : level.mobs) {
			if (!before.contains(m)) return m;
		}
		return null;
	}

	@Test
	void aDustWraithNeverSpawnsWithin3TilesOfTheHero() {
		int spawned = 0;
		for (long seed = 1; seed <= 40; seed++) {
			spawner.spawnPower = 0;
			Mob w;
			Random.pushGenerator(seed);
			try {
				w = tick();
			} finally {
				Random.popGenerator();
			}
			assertNotNull(w, "seed " + seed + ": the first wraith costs one turn of power");
			assertTrue(level.distance(w.pos, hero.pos) > 3,
					"seed " + seed + ": a wraith spawned " + level.distance(w.pos, hero.pos) + " tiles from the hero");
			level.mobs.remove(w);
			spawned++;
		}
		assertEquals(40, spawned);
	}

	@Test
	void theSummonCostCapsAt49Turns() {
		// six wraiths already out: the seventh would cost 7*7 = 49 turns
		int[] cells = {SIZE + 1, 2 * SIZE - 2, SIZE * (SIZE - 2) + 1, SIZE * (SIZE - 1) - 2, SIZE + 3, 2 * SIZE - 4};
		for (int cell : cells) existingDustWraith(cell);

		int turn = 0;
		Mob w = null;
		while (w == null && turn < 100) {
			turn++;
			w = tick();
		}
		assertNotNull(w, "no wraith in 100 turns with six already out");
		assertEquals(49, turn, "turns of power the seventh wraith took");
	}

	@Test
	void aSpawnWithNowhereToGoKeepsItsPower() {
		existingDustWraith(SIZE + 1); // a second wraith costs 2*2 = 4 turns
		seeWithin(2);                 // every visible cell is too close to the hero

		spawner.spawnPower = 3;
		assertNull(tick(), "nowhere far enough from the hero to spawn");
		assertEquals(4, spawner.spawnPower, "the failed summon keeps its power");

		seeWithin(SIZE);
		assertNotNull(tick(), "with room to spawn the kept power summons right away");
	}
}
