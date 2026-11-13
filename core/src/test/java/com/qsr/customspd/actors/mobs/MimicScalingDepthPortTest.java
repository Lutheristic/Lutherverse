package com.qsr.customspd.actors.mobs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.buffs.AscensionChallenge;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.levels.Level;
import com.qsr.customspd.test.HeadlessRun;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Port regression for upstream 79c2350cb: a mimic spawned during play (a cursed wand, a
 * distortion trap) takes its level from the scaling depth, so during the ascent it is as strong
 * as everything else (depth 26) instead of matching the floor it appears on.
 */
class MimicScalingDepthPortTest {

	private static final long SEED = 20261112L;

	/** A mimic's stats come from its level: (1 + level) * 6 HP. */
	private static int hpForLevel(int level) {
		return (1 + level) * 6;
	}

	@Test
	void aMimicSpawnedDuringTheAscentUsesTheScalingDepth() {
		try (HeadlessRun run = HeadlessRun.start(SEED)) {
			Level floor = run.enterFirstLevel();
			Buff.affect(Dungeon.hero, AscensionChallenge.class);

			Mimic mimic = Mimic.spawnAt(floor.entrance());

			assertEquals(26, Dungeon.scalingDepth(), "precondition: the ascent scales to depth 26");
			assertEquals(hpForLevel(26), mimic.HT);
		}
	}

	@Test
	void aMimicSpawnedOnTheWayDownUsesTheFloorDepth() {
		// unchanged by the port: without the ascent the scaling depth is the floor's
		try (HeadlessRun run = HeadlessRun.start(SEED)) {
			Level floor = run.enterFirstLevel();

			Mimic mimic = Mimic.spawnAt(floor.entrance());

			assertEquals(hpForLevel(Dungeon.depth), mimic.HT);
		}
	}
}
