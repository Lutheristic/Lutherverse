package com.qsr.customspd.levels.rooms.secret;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.items.Heap;
import com.qsr.customspd.items.Item;
import com.qsr.customspd.items.scrolls.Scroll;
import com.qsr.customspd.items.scrolls.ScrollOfIdentify;
import com.qsr.customspd.items.scrolls.ScrollOfLullaby;
import com.qsr.customspd.items.scrolls.ScrollOfMagicMapping;
import com.qsr.customspd.items.scrolls.ScrollOfMirrorImage;
import com.qsr.customspd.items.scrolls.ScrollOfRage;
import com.qsr.customspd.items.scrolls.ScrollOfRecharging;
import com.qsr.customspd.items.scrolls.ScrollOfRemoveCurse;
import com.qsr.customspd.items.scrolls.ScrollOfRetribution;
import com.qsr.customspd.items.scrolls.ScrollOfTeleportation;
import com.qsr.customspd.items.scrolls.ScrollOfTerror;
import com.qsr.customspd.items.scrolls.ScrollOfTransmutation;
import com.qsr.customspd.levels.rooms.Room;
import com.qsr.customspd.levels.rooms.standard.EmptyRoom;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.HeadlessRun;
import com.watabou.utils.Random;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The secret library's scroll picks walk an insertion-ordered map. Random.chances walks the
 * map's key order, and a Class-keyed HashMap orders by identity hash, which differs between JVM
 * runs, so the same seed could stock the library differently from one run to the next. Upstream
 * v2.5.4 keeps a HashMap here; this is a deliberate divergence for seeded runs, the same as the
 * secret laboratory's.
 *
 * Identity-hash order is fixed within one JVM, so rerunning a seed proves nothing; these tests
 * check the order itself.
 */
class SecretLibraryScrollOrderTest {

	/** The static block's order, with its weights. */
	private static LinkedHashMap<Class<? extends Scroll>, Float> handBuilt() {
		LinkedHashMap<Class<? extends Scroll>, Float> chances = new LinkedHashMap<>();
		chances.put(ScrollOfIdentify.class,      1f);
		chances.put(ScrollOfRemoveCurse.class,   2f);
		chances.put(ScrollOfMirrorImage.class,   3f);
		chances.put(ScrollOfRecharging.class,    3f);
		chances.put(ScrollOfTeleportation.class, 3f);
		chances.put(ScrollOfLullaby.class,       4f);
		chances.put(ScrollOfMagicMapping.class,  4f);
		chances.put(ScrollOfRage.class,          4f);
		chances.put(ScrollOfRetribution.class,   4f);
		chances.put(ScrollOfTerror.class,        4f);
		chances.put(ScrollOfTransmutation.class, 6f);
		return chances;
	}

	@Test
	void theScrollChancesIterateInTheStaticBlocksOrder() throws Exception {
		Field f = SecretLibraryRoom.class.getDeclaredField("scrollChances");
		f.setAccessible(true);
		@SuppressWarnings("unchecked")
		Map<Class<? extends Scroll>, Float> scrollChances = (Map<Class<? extends Scroll>, Float>) f.get(null);
		assertEquals(new ArrayList<>(handBuilt().entrySet()), new ArrayList<>(scrollChances.entrySet()));
	}

	/** A generator that records every float it hands out. */
	private static final class RecordingRandom extends java.util.Random {
		final List<Float> floats = new ArrayList<>();

		RecordingRandom(long seed) {
			super(seed);
		}

		@Override
		public float nextFloat() {
			float f = super.nextFloat();
			floats.add(f);
			return f;
		}
	}

	/** A generator whose next float is the given one. */
	private static final class ReplayRandom extends java.util.Random {
		private final float value;

		ReplayRandom(float value) {
			this.value = value;
		}

		@Override
		public float nextFloat() {
			return value;
		}
	}

	@SuppressWarnings("unchecked")
	private static ArrayDeque<java.util.Random> generators() throws Exception {
		Field f = Random.class.getDeclaredField("generators");
		f.setAccessible(true);
		return (ArrayDeque<java.util.Random>) f.get(null);
	}

	@Test
	void aSeededLibraryPicksWhatAnOrderedMapPicks() throws Exception {
		int compared = 0;
		for (long seed = 1; seed <= 20; seed++) {
			try (HeadlessRun run = HeadlessRun.start(seed)) {
				HeadlessLevel level = HeadlessLevel.install(11, 11);
				SecretLibraryRoom library = new SecretLibraryRoom();
				library.set(1, 1, 9, 9);
				library.connected.put(new EmptyRoom(), new Room.Door(library.left, library.top + 4));
				Dungeon.level = null; // as during levelgen (Dungeon.newLevel)

				RecordingRandom recorder = new RecordingRandom(seed);
				generators().push(recorder);
				try {
					library.paint(level);
				} finally {
					Random.popGenerator();
				}

				Set<Class<?>> stocked = new HashSet<>();
				for (Heap h : level.heaps.valueList()) {
					for (Item item : h.items) {
						if (item instanceof Scroll) stocked.add(item.getClass());
					}
				}
				assertEquals(stocked.size(), recorder.floats.size(),
						"seed " + seed + ": one float per scroll pick and none for anything else");

				// replay each pick's float through Random.chances over the ordered map
				HashMap<Class<? extends Scroll>, Float> chances = handBuilt();
				Set<Class<?>> expected = new HashSet<>();
				for (float f : recorder.floats) {
					generators().push(new ReplayRandom(f));
					Class<? extends Scroll> pick;
					try {
						pick = Random.chances(chances);
					} finally {
						Random.popGenerator();
					}
					chances.put(pick, 0f);
					expected.add(pick);
				}
				assertEquals(expected, stocked, "seed " + seed + ": floats " + Arrays.toString(recorder.floats.toArray()));
				compared++;
				HeadlessLevel.uninstall();
			}
		}
		assertTrue(compared == 20);
	}
}
