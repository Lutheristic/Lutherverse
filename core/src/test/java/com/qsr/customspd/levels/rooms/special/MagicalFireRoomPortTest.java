package com.qsr.customspd.levels.rooms.special;

import com.qsr.customspd.actors.blobs.Blob;
import com.qsr.customspd.items.Heap;
import com.qsr.customspd.items.scrolls.ScrollOfIdentify;
import com.qsr.customspd.levels.Terrain;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The eternal fire of a magical fire room, run one turn on a headless 9x9 level with the fire
 * on the centre cell.
 *
 * Upstream d0e62d920: the fire burns item heaps next to it, but not on the room's EMPTY_SP floor
 * and not on water either, so water spread into the room no longer lets it burn the prizes.
 */
class MagicalFireRoomPortTest {

	private static final int SIZE = 9;
	private static final int FIRE = 4 + 4 * SIZE;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
	}

	private static MagicalFireRoom.EternalFire fireAt(HeadlessLevel level, int cell) {
		return Blob.seed(cell, 1, MagicalFireRoom.EternalFire.class, level);
	}

	private static Heap scrollHeapAt(HeadlessLevel level, int cell) {
		Heap heap = new Heap();
		heap.pos = cell;
		heap.drop(new ScrollOfIdentify());
		level.heaps.put(cell, heap);
		return heap;
	}

	@Test
	void heapsOnWaterNextToTheFireDoNotBurn() {
		HeadlessLevel level = HeadlessLevel.install(SIZE, SIZE);
		int heapCell = FIRE + 1;
		level.map[heapCell] = Terrain.WATER;
		level.buildFlagMaps();
		MagicalFireRoom.EternalFire fire = fireAt(level, FIRE);
		Heap heap = scrollHeapAt(level, heapCell);

		fire.act();

		assertTrue(fire.volume > 0, "the fire is not on water itself, so it keeps burning");
		assertSame(heap, level.heaps.get(heapCell), "the heap on water is still there");
		assertEquals(1, heap.items.size(), "and still holds its scroll");
	}

	@Test
	void heapsOnPlainFloorNextToTheFireStillBurn() {
		HeadlessLevel level = HeadlessLevel.install(SIZE, SIZE);
		int heapCell = FIRE + 1;
		MagicalFireRoom.EternalFire fire = fireAt(level, FIRE);
		scrollHeapAt(level, heapCell);

		fire.act();

		assertTrue(fire.volume > 0);
		assertNull(level.heaps.get(heapCell), "the scroll next to the fire burns away with its heap");
	}

	@Test
	void heapsOnTheRoomsSpecialFloorDoNotBurn() {
		HeadlessLevel level = HeadlessLevel.install(SIZE, SIZE);
		int heapCell = FIRE + 1;
		level.map[heapCell] = Terrain.EMPTY_SP;
		level.buildFlagMaps();
		MagicalFireRoom.EternalFire fire = fireAt(level, FIRE);
		Heap heap = scrollHeapAt(level, heapCell);

		fire.act();

		assertSame(heap, level.heaps.get(heapCell));
		assertEquals(1, heap.items.size());
	}
}
