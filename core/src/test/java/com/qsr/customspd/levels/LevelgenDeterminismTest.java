package com.qsr.customspd.levels;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.mobs.Mob;
import com.qsr.customspd.items.Heap;
import com.qsr.customspd.items.Item;
import com.qsr.customspd.items.journal.Guidebook;
import com.qsr.customspd.levels.features.LevelTransition;
import com.qsr.customspd.test.HeadlessRun;
import com.watabou.utils.PathFinder;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * F4 levelgen pins on the shipped dungeon layout, via {@link HeadlessRun}:
 *  - determinism: the same seed must rebuild the same floors — map cell for cell, and the same
 *    mobs, item heaps and transitions. Seeded runs are the contract behind seed sharing and
 *    daily runs, and Dungeon.init's RNG ordering has silently broken it before (see the comment
 *    block in Dungeon.init).
 *  - cross-JVM determinism: golden digests of those floors. The in-JVM comparison cannot see the
 *    repo's main hazard, identity-hashed keys (Class, enum, object) whose iteration order varies
 *    between JVM runs but not within one; every test JVM checks itself against the same goldens.
 *  - non-vacuity: a different seed must give a different map, or "equal" proves nothing.
 *  - connectivity: every generated floor's entrance must reach its regular exit, boss floors
 *    included.
 */
class LevelgenDeterminismTest {

	/** Floors 1-6 of the default layout: four sewer floors, the SewerBossLevel, the first prison floor (shop). */
	private static final int FLOORS = 6;

	@Test
	void theSameSeedRebuildsTheSameFloors() {
		List<Floor> first = descendThrough(31337L);
		List<Floor> second = descendThrough(31337L);
		assertEquals(FLOORS, first.size());
		for (int i = 0; i < FLOORS; i++) {
			String where = "floor " + (i + 1) + " from the same seed: ";
			Floor a = first.get(i), b = second.get(i);
			assertTrue(Arrays.equals(a.map, b.map), where + "the map must regenerate cell for cell");
			assertEquals(a.mobs, b.mobs, where + "mobs (type@pos)");
			assertEquals(a.heaps, b.heaps, where + "heaps (pos: class x quantity)");
			assertEquals(a.transitions, b.transitions, where + "transitions");
		}
		assertFalse(first.get(0).mobs.isEmpty() || first.get(0).heaps.isEmpty(),
				"precondition: floor 1 has mobs and heaps to compare");
	}

	@Test
	void differentSeedsBuildDifferentFloors() {
		List<Floor> a = descendThrough(31337L);
		List<Floor> b = descendThrough(8675309L);
		for (int i = 0; i < FLOORS; i++) {
			assertFalse(Arrays.equals(a.get(i).map, b.get(i).map),
					"floor " + (i + 1) + " came out identical for two different seeds");
		}
	}

	/*
	 * Golden digests of floors 1-6 (map, mobs, heaps, transitions; see Floor.digest) for two seeds.
	 * They change whenever levelgen output changes ON PURPOSE: a ported room, a new mob or item in
	 * the default layout, a reordered RNG call. Then: confirm the change is intended, run this test,
	 * and paste the "actual" array its failure message prints over the matching constant. A failure
	 * with NO intended levelgen change — especially one that comes and goes between runs while
	 * theSameSeedRebuildsTheSameFloors stays green — is cross-JVM nondeterminism: look for a
	 * HashMap/HashSet keyed by Class, enum or object feeding Random (CLAUDE.md, Determinism).
	 */
	private static final String[] GOLDEN_31337 = {"423edf1bc1c76a19", "8cf0575648892a63", "d211599934105b86", "90a273e29aa617f2", "b75b194f306e1b09", "eeff7a8cd71cf5d8"};
	private static final String[] GOLDEN_8675309 = {"a8ba55024f65bac0", "4ed8f86ac21cddb2", "9e9bf3b3835dbf04", "72e7881cf56cf1a4", "9d7b34ac85ad378d", "55c7c907b3e300bb"};

	@Test
	void floorDigestsMatchTheGoldenValues() {
		String drift = goldenDrift(31337L, "GOLDEN_31337", GOLDEN_31337)
				+ goldenDrift(8675309L, "GOLDEN_8675309", GOLDEN_8675309);
		if (!drift.isEmpty()) {
			throw new AssertionError("floors no longer match the golden digests (read the comment above GOLDEN_31337 before updating):" + drift);
		}
	}

	/** One sample per region of the default layout, all regular (non-boss) floors. */
	private static final String[] CONNECTIVITY_FLOORS = {"2", "4", "7", "12", "17", "22"};

	@Test
	void everyFloorsEntranceReachesItsExit() {
		int checked = 0;
		for (long seed = 1; seed <= 20; seed++) {
			try (HeadlessRun run = HeadlessRun.start(seed * 1000003L)) {
				for (String name : CONNECTIVITY_FLOORS) {
					Level level = run.generate(name);
					assertReachable(level, "seed " + run.seed + " floor " + name);
					checked++;
				}
			}
		}
		assertEquals(20 * CONNECTIVITY_FLOORS.length, checked);
	}

	/** The default layout's boss floors. */
	private static final String[] BOSS_FLOORS = {"5", "10", "15", "20", "25"};

	/**
	 * Parked, not passing: boss floors whose exit stays cut off even with the gate open. Floor 20:
	 * CPDU's ImpShopRoom.paint still walls its whole rect (the pre-v2 standalone-room paint;
	 * v2.5.4's paint only picks the stock), and CityBossLevel places that rect across the exit
	 * hallway, so after the Dwarf King the stairs and the imp shop are boxed in. Filed as a
	 * follow-up; this ratchet fails once it is fixed, so the entry gets removed then.
	 */
	private static final Set<String> KNOWN_DISCONNECTED_BOSS_FLOORS = Collections.singleton("20");

	/**
	 * Boss floors are sealed by design: the exit cannot be reached until the boss falls (a locked
	 * exit cell on 5 and 25, the pylon gate on 15, the locked arena door on 20, and on 10 no exit
	 * at all — PrisonBossLevel's start map walls it in and setMapEnd builds the real one after
	 * Tengu). So "the entrance reaches the exit" does not hold at generation for 10/15/20 even with
	 * the exit cell as the goal. What must hold is both halves: sealed now (no boss skip), and
	 * connected once the boss's gate opens (no soft-lock after the fight).
	 */
	@Test
	void bossFloorsAreSealedUntilTheBossFallsThenConnected() {
		int checked = 0;
		for (long seed = 1; seed <= 10; seed++) {
			try (HeadlessRun run = HeadlessRun.start(seed * 7919L)) {
				for (String name : BOSS_FLOORS) {
					assertTrue(Dungeon.layout.getDungeon().get(name).getBoss(), "floor " + name + " is a boss floor");
					Level level = run.generate(name);
					String where = "seed " + run.seed + " boss floor " + name + " (" + level.getClass().getSimpleName() + ")";
					assertFalse(exitReachable(level, false, new boolean[level.length()]),
							where + ": the exit is reachable before the boss is beaten");
					boolean[] gate = bossGate(level);
					if (gate != null && KNOWN_DISCONNECTED_BOSS_FLOORS.contains(name)) {
						assertFalse(exitReachable(level, true, gate), where + " now reaches its exit with the gate"
								+ " open: the parked defect is fixed, remove it from KNOWN_DISCONNECTED_BOSS_FLOORS");
					} else if (gate != null) {
						assertReachable(level, where + " with its gate open", gate);
					}
					checked++;
				}
			}
		}
		assertEquals(10 * BOSS_FLOORS.length, checked);
	}

	// ------------------------------------------------------------------------------ helpers

	/** What a same-seed rebuild must reproduce of one floor. */
	private static final class Floor {
		final int[] map;
		final List<String> mobs = new ArrayList<>();
		final List<String> heaps = new ArrayList<>();
		final List<String> transitions = new ArrayList<>();

		Floor(Level level) {
			map = level.map.clone();
			// Level.mobs / heaps iteration order is not the contract; compare sorted multisets.
			for (Mob m : level.mobs) {
				mobs.add(m.getClass().getName() + "@" + m.pos);
			}
			for (Heap h : level.heaps.valueList()) {
				for (Item item : h.items) {
					// EntranceRoom places floor 1's Guidebook from an UNSEEDED generator, and a
					// Guidebook on grass perturbs the rest of the seeded floor (paintGrass skips heap
					// cells), so no same-seed comparison holds once it drops. HeadlessRun reads the
					// intro page to prevent that; fail loudly, not intermittently, if it ever drops.
					if (item instanceof Guidebook) {
						throw new AssertionError("floor " + Dungeon.levelName + " dropped a Guidebook at " + h.pos
								+ ": HeadlessRun must mark GUIDE_INTRO read, or floor 1 is not seed-deterministic");
					}
					heaps.add(h.pos + ": " + item.getClass().getName() + " x" + item.quantity());
				}
			}
			for (LevelTransition t : level.transitions) {
				transitions.add(t.type + "@" + t.cell() + " -> " + t.destLevel + "/" + t.destType);
			}
			Collections.sort(mobs);
			Collections.sort(heaps);
			Collections.sort(transitions);
		}

		/** First 16 hex digits of a SHA-256 over the map, mobs, heaps and transitions. */
		String digest() {
			StringBuilder sb = new StringBuilder("map:");
			for (int t : map) sb.append(t).append(',');
			sb.append("|mobs:").append(mobs).append("|heaps:").append(heaps).append("|transitions:").append(transitions);
			try {
				byte[] hash = MessageDigest.getInstance("SHA-256").digest(sb.toString().getBytes(StandardCharsets.UTF_8));
				StringBuilder hex = new StringBuilder();
				for (int i = 0; i < 8; i++) hex.append(String.format("%02x", hash[i]));
				return hex.toString();
			} catch (NoSuchAlgorithmException e) {
				throw new IllegalStateException(e);
			}
		}
	}

	/** "" when the seed's floors match; otherwise the expected digests and a paste-ready actual. */
	private static String goldenDrift(long seed, String constant, String[] golden) {
		List<Floor> floors = descendThrough(seed);
		String[] actual = new String[floors.size()];
		for (int i = 0; i < actual.length; i++) actual[i] = floors.get(i).digest();
		if (Arrays.equals(golden, actual)) return "";
		StringBuilder literal = new StringBuilder("{");
		for (int i = 0; i < actual.length; i++) literal.append(i == 0 ? "" : ", ").append('"').append(actual[i]).append('"');
		return "\n seed " + seed + ", floors 1-" + FLOORS + ": expected " + Arrays.toString(golden)
				+ "\n   " + constant + " = " + literal.append("};");
	}

	/** A new run on the seed, descending floor 1 -> FLOORS the way the hero would; returns each floor. */
	private static List<Floor> descendThrough(long seed) {
		List<Floor> maps = new ArrayList<>();
		try (HeadlessRun run = HeadlessRun.start(seed)) {
			Level level = run.enterFirstLevel();
			while (true) {
				assertEquals(String.valueOf(maps.size() + 1), Dungeon.levelName, "the default layout's floor order");
				assertReachable(level, "seed " + seed + " floor " + Dungeon.levelName);
				maps.add(new Floor(level));
				if (maps.size() == FLOORS) break;
				level = run.descend();
			}
			assertEquals("SewerBossLevel", Dungeon.layout.getDungeon().get("5").getLayout(),
					"floor 5 is the boss layout this test means to cover");
		}
		return maps;
	}

	/**
	 * Flood fill from the entrance over passable-or-avoid cells (chasms, known traps); on a
	 * SECRETS-feeling floor hidden doors also count, since RegularPainter only guarantees that
	 * rooms there are not totally isolated.
	 */
	private static void assertReachable(Level level, String where) {
		assertReachable(level, where, new boolean[level.length()]);
	}

	/** As above, with {@code opened} cells also walkable (a boss gate). */
	private static void assertReachable(Level level, String where, boolean[] opened) {
		LevelTransition exit = level.getTransition(LevelTransition.Type.REGULAR_EXIT);
		assertNotNull(exit, where + ": no exit transition");
		// getTransition falls back to an entrance when no exit exists: rule that out explicitly.
		assertEquals(LevelTransition.Type.REGULAR_EXIT, exit.type, where + ": no regular exit");
		assertTrue(level.entrance() != exit.cell(), where + ": entrance and exit share a cell");

		// The exit cell is the goal, not a step: a boss floor's LOCKED_EXIT is solid until the
		// boss dies, which is a gate, not a connectivity failure.
		if (!exitReachable(level, true, opened)) {
			fail(where + ": exit cell " + exit.cell() + " is unreachable from entrance " + level.entrance()
					+ " (feeling " + level.feeling + ")");
		}
	}

	private static boolean exitReachable(Level level, boolean exitIsGoal, boolean[] opened) {
		int exit = level.getTransition(LevelTransition.Type.REGULAR_EXIT).cell();
		boolean[] walkable = new boolean[level.length()];
		for (int i = 0; i < walkable.length; i++) {
			walkable[i] = level.passable[i] || level.avoid[i] || opened[i]
					|| (level.feeling == Level.Feeling.SECRETS && level.secret[i]);
		}
		if (exitIsGoal) walkable[exit] = true;
		PathFinder.setMapSize(level.width(), level.height());
		PathFinder.buildDistanceMap(level.entrance(), walkable);
		return PathFinder.distance[exit] != Integer.MAX_VALUE;
	}

	/**
	 * The cells a boss floor opens when its boss falls, as its unseal() does; null for
	 * PrisonBossLevel, whose post-Tengu map does not exist yet. On Sewer/Halls boss floors the
	 * gate is the locked exit cell itself, which the reachability check already treats as the goal.
	 */
	private static boolean[] bossGate(Level level) {
		boolean[] gate = new boolean[level.length()];
		if (level instanceof PrisonBossLevel) {
			return null;
		} else if (level instanceof CavesBossLevel) {
			// CavesBossLevel.unseal: the gate's top row becomes EMPTY.
			for (int x = CavesBossLevel.gate.left; x < CavesBossLevel.gate.right; x++) {
				gate[x + CavesBossLevel.gate.top * level.width()] = true;
			}
		} else if (level instanceof CityBossLevel) {
			// CityBossLevel.unseal: the locked arena doors become DOOR.
			for (int i = 0; i < gate.length; i++) {
				gate[i] = level.map[i] == Terrain.LOCKED_DOOR;
			}
		}
		return gate;
	}
}
