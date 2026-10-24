package com.qsr.customspd.items;

import com.qsr.customspd.items.bombs.Bomb;
import com.qsr.customspd.test.HeadlessGdx;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Port regression test for upstream SPD e9e432b2c (v2.2.0): "fixed new bomb logic causing crashes
 * with frozen heaps".
 *
 * Heap.freeze() defuses bombs by calling {@code bomb.fuse.freeze()}. Only a LIT bomb has a fuse; an
 * ordinary bomb lying in a heap has {@code fuse == null}, so freezing any heap holding a bomb
 * threw a NullPointerException. A null fuse now means nothing to freeze.
 */
class HeapFreezeUnlitBombPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@Test
	void freezingAHeapWithAnUnlitBombDoesNotCrash() {
		Heap heap = new Heap();
		heap.pos = 0;
		Bomb bomb = new Bomb();
		assertNull(bomb.fuse, "test premise: a bomb in a heap is unlit");
		heap.items.add(bomb);

		assertDoesNotThrow(heap::freeze);
		assertEquals(1, heap.items.size(), "the unlit bomb is untouched");
	}

	@Test
	void freezingAHeapWithALitBombStillDefusesIt() {
		Heap heap = new Heap();
		heap.pos = 0;
		Bomb bomb = new Bomb();
		bomb.fuse = new Bomb.Fuse().ignite(bomb);
		assertNotNull(bomb.fuse);
		heap.items.add(bomb);

		heap.freeze();

		assertNull(bomb.fuse, "freezing defuses a lit bomb, as before");
	}
}
