package com.qsr.customspd.actors.mobs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Actor;
import com.qsr.customspd.levels.Terrain;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.watabou.utils.PathFinder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Same guard as NecromancerSummonPortTest, for the spectral necromancer's own override: a summon
 * onto a tile that became impassable moves to a free neighbour, or waits a turn if there is none.
 *
 * The target is a chasm: impassable but not solid, so Wraith.spawnAt would accept it on its own.
 */
class SpectralNecromancerSummonTest {

	private static final int W = 8;
	private static final int TARGET = 4 * W + 4;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@BeforeEach
	void setUp() {
		Dungeon.hero = null;
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	/** A chasm target walled in on every side except the given neighbours. */
	private static HeadlessLevel chasmTarget(int... openNeighbours) {
		HeadlessLevel level = HeadlessLevel.install(W, W);
		level.map[TARGET] = Terrain.CHASM;
		for (int offset : PathFinder.NEIGHBOURS8) {
			level.map[TARGET + offset] = Terrain.WALL;
		}
		for (int cell : openNeighbours) {
			level.map[cell] = Terrain.EMPTY;
		}
		level.buildFlagMaps();
		return level;
	}

	private static SpectralNecromancer summoningOnto(int cell) {
		SpectralNecromancer necro = HeadlessLevel.at(new SpectralNecromancer(), W + 1);
		necro.summoning = true;
		necro.summoningPos = cell;
		return necro;
	}

	private static void summon(SpectralNecromancer necro) {
		try {
			necro.summonMinion();
		} catch (NullPointerException e) {
			// GameScene.add registers a wraith with the level, then needs a scene for its
			// sprite. Only that NPE is tolerated.
			StackTraceElement[] st = e.getStackTrace();
			if (!(st[0].getClassName().endsWith(".GameScene") && "add".equals(st[0].getMethodName()))) {
				throw e;
			}
		}
	}

	@Test
	void summonOntoNewlyImpassableTileWaitsInsteadOfFinishing() {
		HeadlessLevel level = chasmTarget();
		SpectralNecromancer necro = summoningOnto(TARGET);

		summon(necro);

		assertTrue(level.mobs.isEmpty(), "no wraith may be summoned onto an impassable tile");
		assertNull(Actor.findChar(TARGET), "the impassable tile must stay empty");
		assertTrue(necro.summoning, "the summon must stay pending so it can retry next turn");
		assertEquals(1f, necro.cooldown(), 0.001f, "the necromancer must wait a turn instead");
	}

	@Test
	void summonOntoNewlyImpassableTileMovesToAFreeNeighbour() {
		int free = TARGET + 1;
		HeadlessLevel level = chasmTarget(free);
		SpectralNecromancer necro = summoningOnto(TARGET);

		summon(necro);

		assertEquals(free, necro.summoningPos, "the summon moves to the free neighbour");
		assertFalse(necro.summoning, "the summon goes ahead");
		List<Mob> mobs = new ArrayList<>(level.mobs);
		assertEquals(1, mobs.size(), "one wraith is summoned");
		assertTrue(mobs.get(0) instanceof Wraith, "the summon is a wraith");
		assertEquals(free, mobs.get(0).pos, "the wraith lands on the free neighbour, not the chasm");
	}
}
