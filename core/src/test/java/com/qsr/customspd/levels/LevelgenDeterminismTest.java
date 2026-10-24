package com.qsr.customspd.levels;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.mobs.Mob;
import com.qsr.customspd.actors.mobs.npcs.ImpShopkeeper;
import com.qsr.customspd.items.Heap;
import com.qsr.customspd.items.Item;
import com.qsr.customspd.items.journal.Guidebook;
import com.qsr.customspd.levels.features.LevelTransition;
import com.qsr.customspd.levels.rooms.special.ShopRoom;
import com.qsr.customspd.levels.rooms.standard.ImpShopRoom;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.test.SaveRoundtrip;
import com.watabou.noosa.Game;
import com.watabou.utils.PathFinder;
import com.watabou.utils.PlatformSupport;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
	private static final String[] GOLDEN_31337 = {"423edf1bc1c76a19", "4984884c338cae4f", "51b28538a0cae7f0", "0df0bdd2379cd532", "b75b194f306e1b09", "5d2953b5e4e25a0e"};
	private static final String[] GOLDEN_8675309 = {"b6ba4fe634b21710", "d516983c6c9c5aa5", "ed88aa02970ea031", "00bbcff5314b0ba4", "9d7b34ac85ad378d", "d8498c92b72749a9"};

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
	 * Parked, not passing: boss floors whose exit stays cut off even with the gate open. Empty now.
	 * Floor 20 used to be here: CPDU's ImpShopRoom.paint still walled its whole rect (the
	 * pre-baea3b7e8 standalone-room paint) across CityBossLevel's exit hallway, boxing in the stairs
	 * after the Dwarf King (cpdu-jw1e). A parked floor must be named here; this ratchet then fails
	 * once the floor is fixed, so the entry gets removed.
	 */
	private static final Set<String> KNOWN_DISCONNECTED_BOSS_FLOORS = Collections.emptySet();

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

	/**
	 * baea3b7e8: the imp shop decides its stock during levelgen (so a seed's shop is fixed before
	 * the city boss falls), keeps it through a save, and once the shop spawns its items sit on
	 * for-sale heaps the hero can walk to with the boss gate open.
	 */
	@Test
	void theImpShopDecidesItsItemsAtLevelgenAndIsReachableOnceSpawned() throws Exception {
		try (HeadlessRun run = HeadlessRun.start(7919L)) {
			CityBossLevel level = (CityBossLevel) run.generate("20");
			Field shopField = CityBossLevel.class.getDeclaredField("impShop");
			shopField.setAccessible(true);
			ImpShopRoom shop = (ImpShopRoom) shopField.get(level);

			// Stock is decided at levelgen, not lazily. Read the field itself: itemCount() would
			// generate the stock on first call and hide a paint() that decides nothing.
			List<String> decided = stockOf(shop);
			assertNotNull(decided, "ImpShopRoom.paint must decide itemsToSpawn during levelgen");
			assertFalse(decided.isEmpty(), "the imp shop has items from levelgen");
			assertFalse(shop.shopSpawned());
			for (Heap h : level.heaps.valueList()) {
				assertTrue(h.type != Heap.Type.FOR_SALE, "nothing is for sale before the Imp quest completes");
			}

			// The stock survives a save/load of the room (the 'items' bundle key): the field is read
			// before any itemCount() call, so a missing key shows up as null, not as regenerated stock.
			ImpShopRoom reloaded = SaveRoundtrip.of(shop);
			assertEquals(decided, stockOf(reloaded), "stock after a save roundtrip, item by item and in order");

			Method spawn = CityBossLevel.class.getDeclaredMethod("spawnShop");
			spawn.setAccessible(true);
			// ImpShopRoom.placeShopkeeper asks Game.scene() (null here, so the shopkeeper goes to
			// level.mobs); give it a bare Game and put the statics back afterwards (the constructor
			// writes instance, platform and sceneClass).
			Field sceneClassField = Game.class.getDeclaredField("sceneClass");
			sceneClassField.setAccessible(true);
			Game priorInstance = Game.instance;
			PlatformSupport priorPlatform = Game.platform;
			Object priorSceneClass = sceneClassField.get(null);
			try {
				new Game(null, null);
				spawn.invoke(level);
			} finally {
				Game.instance = priorInstance;
				Game.platform = priorPlatform;
				sceneClassField.set(null, priorSceneClass);
			}
			assertTrue(shop.shopSpawned());
			boolean shopkeeper = false;
			for (Mob m : level.mobs) shopkeeper |= m instanceof ImpShopkeeper;
			assertTrue(shopkeeper, "an ImpShopkeeper stands in the shop");

			boolean[] gate = bossGate(level);
			boolean[] walkable = new boolean[level.length()];
			for (int i = 0; i < walkable.length; i++) {
				walkable[i] = level.passable[i] || level.avoid[i] || gate[i];
			}
			PathFinder.setMapSize(level.width(), level.height());
			PathFinder.buildDistanceMap(level.entrance(), walkable);
			List<String> placed = new ArrayList<>();
			for (Heap h : level.heaps.valueList()) {
				if (h.type != Heap.Type.FOR_SALE) continue;
				for (Item item : h.items) placed.add(describe(item));
				assertTrue(PathFinder.distance[h.pos] != Integer.MAX_VALUE,
						"for-sale heap at " + h.pos + " is unreachable with the gate open");
			}
			// spawnShop places exactly the stock levelgen decided (it must not regenerate it).
			List<String> expectedSorted = new ArrayList<>(decided);
			List<String> placedSorted = new ArrayList<>(placed);
			Collections.sort(expectedSorted);
			Collections.sort(placedSorted);
			assertEquals(expectedSorted, placedSorted, "for-sale heaps hold exactly the levelgen stock");
		}
	}

	// ------------------------------------------------------------------------------ helpers

	/** The room's itemsToSpawn field as class+quantity strings, read WITHOUT triggering lazy generation; null if unset. */
	private static List<String> stockOf(ImpShopRoom room) throws Exception {
		Field f = ShopRoom.class.getDeclaredField("itemsToSpawn");
		f.setAccessible(true);
		@SuppressWarnings("unchecked")
		List<Item> items = (List<Item>) f.get(room);
		if (items == null) return null;
		List<String> out = new ArrayList<>();
		for (Item item : items) out.add(describe(item));
		return out;
	}

	private static String describe(Item item) {
		return item.getClass().getName() + " x" + item.quantity();
	}

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
