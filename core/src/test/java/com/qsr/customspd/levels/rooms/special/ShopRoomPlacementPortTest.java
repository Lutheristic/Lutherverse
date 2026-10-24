package com.qsr.customspd.levels.rooms.special;

import com.qsr.customspd.items.Ankh;
import com.qsr.customspd.items.Heap;
import com.qsr.customspd.items.Item;
import com.qsr.customspd.levels.Terrain;
import com.qsr.customspd.levels.painters.Painter;
import com.qsr.customspd.levels.rooms.standard.ImpShopRoom;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.watabou.noosa.Game;
import com.watabou.utils.PlatformSupport;
import com.watabou.utils.Point;
import com.watabou.utils.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Shop item placement (upstream 6a05169b5, v2.3.2: "improved item placement logic in shops").
 *
 * Items go clockwise round the room starting at the cell inside the entrance, and once that ring
 * is full the walk steps one cell inward and starts the next ring, instead of scattering the
 * overflow over random free cells. The old scatter consumed a variable amount of RNG (one draw
 * per collision), which is the "seed problems with varying item count" the commit names.
 *
 * Driven on a 9x9 {@link ImpShopRoom}, the fixed-size shop CityBossLevel builds. Its interior is
 * 7x7: ring 1 has 24 cells, ring 2 has 16, ring 3 has 8, and the centre is the shopkeeper's. Each
 * ring leaves the cell in front of the entrance clear, so the walk places 23, 15 and 7 items; the
 * three entrance-column cells and the centre are only reached by the fill-in pass.
 * The room has no connections, which is exactly the state CityBossLevel's shop is in (connections
 * are not bundled), so entrance() is the fallback door too.
 */
class ShopRoomPlacementPortTest {

	private static final int RING1 = 23;
	private static final int RING2 = 15;
	private static final int RING3 = 7;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	private Game priorInstance;
	private PlatformSupport priorPlatform;
	private Object priorSceneClass;

	/** Level.drop asks Game.scene(); a bare Game answers null. Its constructor writes three statics, put back after. */
	@BeforeEach
	void bareGame() throws Exception {
		Field sceneClass = Game.class.getDeclaredField("sceneClass");
		sceneClass.setAccessible(true);
		priorInstance = Game.instance;
		priorPlatform = Game.platform;
		priorSceneClass = sceneClass.get(null);
		new Game(null, null);
	}

	@AfterEach
	void tearDown() throws Exception {
		Game.instance = priorInstance;
		Game.platform = priorPlatform;
		Field sceneClass = Game.class.getDeclaredField("sceneClass");
		sceneClass.setAccessible(true);
		sceneClass.set(null, priorSceneClass);
		HeadlessLevel.uninstall();
	}

	private static ImpShopRoom shop() {
		ImpShopRoom room = new ImpShopRoom();
		room.set(2, 2, 10, 10); // inclusive: 9x9
		assertEquals(9, room.width(), "test setup");
		assertEquals(9, room.height(), "test setup");
		return room;
	}

	/** Paints the room as ShopRoom.paint does: wall ring, special floor inside. */
	private static void walls(HeadlessLevel level, ImpShopRoom room) {
		Painter.fill(level, room, Terrain.WALL);
		Painter.fill(level, room, 1, Terrain.EMPTY_SP);
		level.buildFlagMaps();
	}

	/** Places {@code count} one-per-heap items and returns the room's for-sale heaps. */
	private static ArrayList<Heap> place(HeadlessLevel level, ImpShopRoom room, int count) {
		room.itemsToSpawn = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			room.itemsToSpawn.add(new Ankh());
		}
		// seeded, per the repo rule: anything that consumes RNG runs inside a pushed generator
		Random.pushGenerator(20261024L);
		try {
			room.placeItems(level);
		} finally {
			Random.popGenerator();
		}
		ArrayList<Heap> heaps = new ArrayList<>();
		for (Heap h : level.heaps.valueList()) {
			assertEquals(Heap.Type.FOR_SALE, h.type);
			assertEquals(1, h.items.size(), "one item per cell, nothing stacked on top of another");
			heaps.add(h);
		}
		return heaps;
	}

	/** 1 for the outermost ring of the room's interior, 2 for the next, and so on. */
	private static int ringOf(ImpShopRoom room, HeadlessLevel level, Heap h) {
		int x = h.pos % level.width();
		int y = h.pos / level.width();
		return Math.min(Math.min(x - room.left, room.right - x), Math.min(y - room.top, room.bottom - y));
	}

	private static int[] perRing(ImpShopRoom room, HeadlessLevel level, ArrayList<Heap> heaps) {
		int[] counts = new int[6];
		for (Heap h : heaps) {
			counts[ringOf(room, level, h)]++;
		}
		return counts;
	}

	@Test
	void anUnconnectedImpShopEntersAtTheMiddleOfItsBottomInset() {
		ImpShopRoom room = shop();
		Point door = room.entrance();
		// upstream 6a05169b5: new Door((left+right)/2 + 1, bottom-1), not the old left-wall (left, top+2)
		assertEquals((room.left + room.right) / 2 + 1, door.x);
		assertEquals(room.bottom - 1, door.y);
	}

	@Test
	void overflowFromTheOuterRingStepsInwardInsteadOfScatteringAtRandom() {
		HeadlessLevel level = HeadlessLevel.install(14, 14);
		ImpShopRoom room = shop();
		walls(level, room);

		ArrayList<Heap> heaps = place(level, room, 30);

		assertEquals(30, heaps.size());
		int[] rings = perRing(room, level, heaps);
		assertEquals(RING1, rings[1], "the outer ring fills completely first");
		assertEquals(30 - RING1, rings[2], "the overflow continues on the next ring in");
		assertEquals(0, rings[3] + rings[4], "and goes no deeper than it needs to");
	}

	@Test
	void aFullShopFillsAllThreeRingsAndLeavesTheCentreForTheShopkeeper() {
		HeadlessLevel level = HeadlessLevel.install(14, 14);
		ImpShopRoom room = shop();
		walls(level, room);

		ArrayList<Heap> heaps = place(level, room, RING1 + RING2 + RING3);

		int[] rings = perRing(room, level, heaps);
		assertEquals(RING1, rings[1]);
		assertEquals(RING2, rings[2]);
		assertEquals(RING3, rings[3]);
		assertEquals(0, rings[4], "the centre cell stays free");
	}

	@Test
	void whenTheRingsRunOutTheLeftoverItemsStillGetAFreeCell() {
		HeadlessLevel level = HeadlessLevel.install(14, 14);
		ImpShopRoom room = shop();
		walls(level, room);

		// one more than the rings hold: the fill-in pass takes the first free interior cell
		ArrayList<Heap> heaps = place(level, room, RING1 + RING2 + RING3 + 1);
		assertEquals(RING1 + RING2 + RING3 + 1, heaps.size(), "every item is placed");
		int centreColumn = (room.left + room.right) / 2;
		int leftover = 0;
		for (Heap h : heaps) {
			if (h.pos % level.width() == centreColumn && ringOf(room, level, h) >= 1) leftover++;
		}
		assertTrue(leftover >= 1, "the extra item lands on a cell the ring walk skipped (the entrance column or the centre)");

		// and the room can be filled to the last interior cell, none on the wall ring
		HeadlessLevel full = HeadlessLevel.install(14, 14);
		ImpShopRoom fullRoom = shop();
		walls(full, fullRoom);
		ArrayList<Heap> all = place(full, fullRoom, 49);
		assertEquals(49, all.size());
		for (Heap h : all) {
			assertTrue(ringOf(fullRoom, full, h) >= 1, "nothing is placed in a wall");
		}
	}
	@Test
	void theFirstItemSitsOnTheInsetBesideTheEntrance() {
		HeadlessLevel level = HeadlessLevel.install(14, 14);
		ImpShopRoom room = shop();
		walls(level, room);

		ArrayList<Heap> heaps = place(level, room, 1);

		// entrance (cx+1, bottom-1) insets to (cx, bottom-1); the walk then steps one cell left
		Point door = room.entrance();
		Heap first = heaps.get(0);
		assertEquals(door.x - 2, first.pos % level.width());
		assertEquals(door.y, first.pos / level.width());
		assertTrue(first.items.get(0) instanceof Item);
	}
}
