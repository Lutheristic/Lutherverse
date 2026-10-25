package com.qsr.customspd.levels;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.mobs.Mob;
import com.qsr.customspd.items.Heap;
import com.qsr.customspd.items.Item;
import com.qsr.customspd.levels.features.LevelTransition;
import com.qsr.customspd.levels.traps.Trap;
import com.qsr.customspd.levels.traps.WornDartTrap;
import com.qsr.customspd.modding.DungeonLayout;
import com.qsr.customspd.modding.LevelType;
import com.qsr.customspd.plants.Sungrass;
import com.qsr.customspd.test.HeadlessRun;
import com.watabou.utils.PathFinder;
import kotlinx.serialization.json.Json;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Roadmap #10c: a CUSTOM (mod-authored, map-from-JSON) level end to end: layout JSON through
 * the real kotlinx decoding into DungeonLayout, Dungeon.newLevel's CustomLevel branch, entering
 * it, descending custom -> custom, and the level save/load round trip. The shipped dungeon.json
 * has no non-REGULAR level, so the layout is a test resource: two small custom floors, the first
 * carrying one of each spawn kind (a mob with HP and AI state, an item, a hidden trap, a plant).
 *
 * Seam: Dungeon.init loads the layout from the asset retriever; the test layout is swapped in
 * right after init, before the first level is generated (the first point InterlevelScene
 * consults it). Dungeon.loadGame re-reads the layout from the retriever (it would drop the test
 * layout), so the round trip here is the per-level one: saveAll/saveLevel -> loadLevel.
 */
class CustomLevelEndToEndTest {

	private static final String LAYOUT = "custom-level-e2e-layout.json";
	private static final String SHUFFLE_LAYOUT = "custom-level-shuffle-layout.json";
	private static final long SEED = 424242L;

	@Test
	void aCustomLayoutPlaysThroughEnterDescendAndReload() {
		DungeonLayout layout = Json.Default.decodeFromString(DungeonLayout.Companion.serializer(), resource(LAYOUT));
		assertEquals(LevelType.CUSTOM, layout.getDungeon().get("c1").getType(), "precondition: c1 is a custom level");

		try (HeadlessRun run = HeadlessRun.start(SEED)) {
			Dungeon.layout = layout;
			Dungeon.levelName = layout.getStart();
			Dungeon.depth = layout.getDungeon().get(layout.getStart()).getDepth();

			// ---- c1: generated from the layout and entered
			Level c1 = run.enterFirstLevel();
			assertSame(CustomLevel.class, c1.getClass(), "Dungeon.newLevel builds a CustomLevel for type custom");
			assertEquals(9, c1.width());
			assertEquals(7, c1.height());
			int[] expected = toArray(layout.getDungeon().get("c1").getCustomLayout().getMap());
			expected[cell(c1, 6, 4)] = Terrain.GRASS; // Level.plant grows grass under the seed
			assertArrayEquals(expected, c1.map, "c1's map is the layout's, cell for cell");

			LevelTransition entrance = c1.getTransition(null);
			LevelTransition exit = c1.getTransition(LevelTransition.Type.REGULAR_EXIT);
			assertEquals(LevelTransition.Type.SURFACE, entrance.type, "an entrance listed as surface");
			assertEquals(cell(c1, 1, 1), entrance.cell());
			assertEquals(LevelTransition.Type.REGULAR_EXIT, exit.type);
			assertEquals(cell(c1, 7, 5), exit.cell());
			assertEquals("c2", exit.destLevel);
			assertEquals(cell(c1, 1, 1), Dungeon.hero.pos, "the hero arrives on the entrance");
			assertReachable(c1);

			assertEquals(Collections.singletonList("Rat@" + cell(c1, 6, 2) + " hp3 WANDERING"), mobs(c1), "the layout's mob");
			assertEquals(Collections.singletonList(cell(c1, 2, 4) + ": PotionOfHealing x2"), heaps(c1), "the layout's item");
			Trap trap = c1.traps.get(cell(c1, 3, 3));
			assertTrue(trap instanceof WornDartTrap, "the layout's hidden trap: " + trap);
			assertFalse(trap.visible, "a hidden trap starts hidden");
			assertTrue(c1.plants.get(cell(c1, 6, 4)) instanceof Sungrass, "the layout's plant");
			List<String> c1Mobs = mobs(c1);
			List<String> c1Heaps = heaps(c1);
			int[] c1Map = c1.map.clone();

			// ---- descend custom -> custom
			Level c2 = run.descend();
			assertSame(CustomLevel.class, c2.getClass());
			assertEquals("c2", Dungeon.levelName);
			assertEquals(2, Dungeon.depth, "depth comes from the layout");
			assertEquals(5, c2.viewDistance, "view_distance from the layout");
			assertArrayEquals(toArray(layout.getDungeon().get("c2").getCustomLayout().getMap()), c2.map);
			LevelTransition c2Entrance = c2.getTransition(LevelTransition.Type.REGULAR_ENTRANCE, "c1");
			assertNotNull(c2Entrance, "c2's entrance leads back to c1");
			assertEquals(cell(c2, 1, 1), c2Entrance.cell());
			assertEquals(c2Entrance.cell(), Dungeon.hero.pos, "the hero arrives on c2's entrance from c1");
			assertReachable(c2);

			// ---- reload: c2 through saveLevel/loadLevel, c1 from the save the descent wrote
			run.saveLevel();
			Level c2Loaded = run.loadLevel();
			assertSame(CustomLevel.class, c2Loaded.getClass());
			assertArrayEquals(c2.map, c2Loaded.map);
			assertEquals(5, c2Loaded.viewDistance, "restoreFromBundle re-reads the layout");
			assertSame(layout.getDungeon().get("c2").getCustomLayout(), ((CustomLevel) c2Loaded).layout);

			Dungeon.levelName = "c1";
			Dungeon.depth = 1;
			Level c1Loaded = run.loadLevel();
			assertSame(CustomLevel.class, c1Loaded.getClass());
			assertArrayEquals(c1Map, c1Loaded.map);
			assertEquals(c1Mobs, mobs(c1Loaded), "c1's mob (type, pos, HP, state) survives the save");
			assertEquals(c1Heaps, heaps(c1Loaded));
			assertTrue(c1Loaded.traps.get(cell(c1Loaded, 3, 3)) instanceof WornDartTrap);
			assertTrue(c1Loaded.plants.get(cell(c1Loaded, 6, 4)) instanceof Sungrass);
			assertEquals("c2", c1Loaded.getTransition(LevelTransition.Type.REGULAR_EXIT).destLevel);
		}
	}

	/**
	 * cpdu-s26z: a layout with shuffle_items permutes which item spec lands on which cell. That
	 * shuffle must draw from the level's seeded generator, not Collections.shuffle's own unseeded
	 * one, or the same seed gives a different dungeon every run. Six distinct fixed items give 720
	 * arrangements, so an unseeded shuffle matches itself on all of three rebuilds with
	 * probability ~1/500000. Canary (run once, reverted): CustomLevel back on Collections.shuffle
	 * fails this test.
	 */
	@Test
	void shuffleItemsPlacesTheSameItemsOnTheSameCellsForTheSameSeed() {
		List<String> first = shuffledPlacement(SEED);
		for (int rebuild = 0; rebuild < 3; rebuild++) {
			assertEquals(first, shuffledPlacement(SEED), "same seed, same shuffled item placement (rebuild " + rebuild + ")");
		}

		// ...and the shuffle is real: across a few seeds the placement is not always the layout's own order.
		List<String> layoutOrder = Arrays.asList("PotionOfHealing", "PotionOfHaste", "PotionOfFrost",
				"PotionOfLevitation", "PotionOfInvisibility", "PotionOfMindVision");
		boolean sawShuffle = !first.equals(layoutOrder);
		for (long seed = 1; seed <= 4 && !sawShuffle; seed++) {
			sawShuffle = !shuffledPlacement(seed).equals(layoutOrder);
		}
		assertTrue(sawShuffle, "shuffle_items actually reorders the items");
	}

	/** Item class names by ascending cell, for the shuffle layout built fresh under {@code seed}. */
	private static List<String> shuffledPlacement(long seed) {
		DungeonLayout layout = Json.Default.decodeFromString(DungeonLayout.Companion.serializer(), resource(SHUFFLE_LAYOUT));
		try (HeadlessRun run = HeadlessRun.start(seed)) {
			Dungeon.layout = layout;
			Dungeon.levelName = layout.getStart();
			Dungeon.depth = layout.getDungeon().get(layout.getStart()).getDepth();
			Level s1 = run.enterFirstLevel();
			List<String> out = new ArrayList<>();
			for (int x = 2; x <= 7; x++) {
				Heap heap = s1.heaps.get(cell(s1, x, 2));
				assertNotNull(heap, "an item sits at (" + x + ",2)");
				assertEquals(1, heap.items.size());
				out.add(heap.items.get(0).getClass().getSimpleName());
			}
			return out;
		}
	}

	// ------------------------------------------------------------------------------ helpers

	private static int cell(Level level, int x, int y) {
		return x + y * level.width();
	}

	private static List<String> mobs(Level level) {
		List<String> out = new ArrayList<>();
		for (Mob m : level.mobs) {
			String state = m.state == m.WANDERING ? "WANDERING" : m.state == m.SLEEPING ? "SLEEPING"
					: m.state == m.HUNTING ? "HUNTING" : m.state.getClass().getSimpleName();
			out.add(m.getClass().getSimpleName() + "@" + m.pos + " hp" + m.HP + " " + state);
		}
		Collections.sort(out);
		return out;
	}

	private static List<String> heaps(Level level) {
		List<String> out = new ArrayList<>();
		for (Heap h : level.heaps.valueList()) {
			for (Item item : h.items) {
				out.add(h.pos + ": " + item.getClass().getSimpleName() + " x" + item.quantity());
			}
		}
		Collections.sort(out);
		return out;
	}

	private static void assertReachable(Level level) {
		boolean[] walkable = new boolean[level.length()];
		for (int i = 0; i < walkable.length; i++) walkable[i] = level.passable[i] || level.avoid[i];
		PathFinder.setMapSize(level.width(), level.height());
		PathFinder.buildDistanceMap(level.entrance(), walkable);
		assertTrue(PathFinder.distance[level.exit()] != Integer.MAX_VALUE, Dungeon.levelName + ": entrance reaches exit");
	}

	private static int[] toArray(List<Integer> map) {
		int[] out = new int[map.size()];
		for (int i = 0; i < out.length; i++) out[i] = map.get(i);
		return out;
	}

	private static String resource(String name) {
		try (InputStream in = CustomLevelEndToEndTest.class.getResourceAsStream(name)) {
			assertNotNull(in, "test resource " + name);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buf = new byte[4096];
			for (int n; (n = in.read(buf)) > 0; ) out.write(buf, 0, n);
			return new String(out.toByteArray(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
