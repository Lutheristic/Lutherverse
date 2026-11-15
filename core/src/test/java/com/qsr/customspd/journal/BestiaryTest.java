package com.qsr.customspd.journal;

import com.qsr.customspd.Badges;
import com.qsr.customspd.actors.mobs.CustomMob;
import com.qsr.customspd.actors.mobs.DwarfKing;
import com.qsr.customspd.actors.mobs.GaolShade;
import com.qsr.customspd.actors.mobs.Ghoul;
import com.qsr.customspd.actors.mobs.Gnoll;
import com.qsr.customspd.actors.mobs.HexwardMoth;
import com.qsr.customspd.actors.mobs.Keywraith;
import com.qsr.customspd.actors.mobs.Necromancer;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.actors.mobs.RipperDemon;
import com.qsr.customspd.actors.mobs.Skeleton;
import com.qsr.customspd.actors.mobs.Snake;
import com.qsr.customspd.actors.mobs.TumblerWisp;
import com.qsr.customspd.actors.mobs.WardstoneSentinel;
import com.qsr.customspd.actors.mobs.Wraith;
import com.qsr.customspd.actors.mobs.YogDzewa;
import com.qsr.customspd.items.quest.CorpseDust;
import com.qsr.customspd.items.wands.WandOfWarding;
import com.qsr.customspd.levels.traps.PoisonDartTrap;
import com.qsr.customspd.levels.traps.TenguDartTrap;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.SaveRoundtrip;
import com.qsr.customspd.test.StaticSnapshot;
import com.watabou.utils.Bundle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The journal bestiary as ported from v2.5.4, minus the Slice 2 entities: its sections, the
 * class conversions that credit a base class, the encounter clamp and skip flag, and its save.
 */
class BestiaryTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	private StaticSnapshot statics;

	@BeforeEach
	void snapshot() throws ReflectiveOperationException {
		statics = StaticSnapshot.capture(Arrays.<Class<?>>asList(
				Bestiary.class, Catalog.class, Badges.class, Journal.class, Document.class));
		clearBestiary();
		Bestiary.skipCountingEncounters = false;
	}

	@AfterEach
	void restore() {
		statics.restore();
	}

	@SuppressWarnings("unchecked")
	private static void clearBestiary() throws ReflectiveOperationException {
		Field seen = Bestiary.class.getDeclaredField("seen");
		Field counts = Bestiary.class.getDeclaredField("encounterCount");
		seen.setAccessible(true);
		counts.setAccessible(true);
		for (Bestiary cat : Bestiary.values()) {
			for (Map.Entry<Class<?>, Boolean> e : ((Map<Class<?>, Boolean>) seen.get(cat)).entrySet()) {
				e.setValue(false);
			}
			for (Map.Entry<Class<?>, Integer> e : ((Map<Class<?>, Integer>) counts.get(cat)).entrySet()) {
				e.setValue(0);
			}
		}
	}

	@Test
	void sectionsHoldTheSliceOneEntities() {
		int[] expected = {30, 13, 7, 13, 6, 8, 9, 31, 16};
		Bestiary[] cats = Bestiary.values();
		assertEquals(expected.length, cats.length);
		int total = 0;
		Set<Class<?>> all = new HashSet<>();
		for (int i = 0; i < cats.length; i++) {
			assertEquals(expected[i], cats[i].totalEntities(), cats[i].name());
			total += cats[i].totalEntities();
			for (Class<?> cls : cats[i].entities()) {
				assertTrue(all.add(cls), cls.getName() + " is listed in two sections");
			}
		}
		assertEquals(133, total);
		assertTrue(Bestiary.ALLY.entities().contains(WandOfWarding.Ward.WardSentry.class));
		for (Class<?> cls : Arrays.<Class<?>>asList(Keywraith.class, GaolShade.class, HexwardMoth.class,
				TumblerWisp.class, WardstoneSentinel.class, CustomMob.class)) {
			assertFalse(all.contains(cls), cls.getName() + " should stay out of the bestiary");
		}
	}

	@Test
	void conversionsCreditTheBaseClass() {
		Class<?>[][] pairs = {
				{CorpseDust.DustWraith.class, Wraith.class},
				{Necromancer.NecroSkeleton.class, Skeleton.class},
				{TenguDartTrap.class, PoisonDartTrap.class},
				{DwarfKing.DKGhoul.class, Ghoul.class},
				{YogDzewa.YogRipper.class, RipperDemon.class},
		};
		for (Class<?>[] p : pairs) {
			assertFalse(Bestiary.isSeen(p[1]), p[1].getName());
			Bestiary.setSeen(p[0]);
			Bestiary.countEncounter(p[0]);
			assertTrue(Bestiary.isSeen(p[1]), p[1].getName());
			assertEquals(1, Bestiary.encounterCount(p[1]), p[1].getName());
			assertFalse(Bestiary.isSeen(p[0]), p[0].getName());
		}
	}

	@Test
	void countingClampsAndCanBeSuppressed() {
		Bestiary.countEncounters(Rat.class, Integer.MAX_VALUE - 1);
		Bestiary.countEncounter(Rat.class);
		assertEquals(Integer.MAX_VALUE, Bestiary.encounterCount(Rat.class));
		Bestiary.countEncounter(Rat.class);
		assertEquals(Integer.MAX_VALUE, Bestiary.encounterCount(Rat.class));

		Bestiary.skipCountingEncounters = true;
		Bestiary.countEncounter(Gnoll.class);
		Bestiary.setSeen(Gnoll.class);
		Bestiary.skipCountingEncounters = false;
		assertEquals(0, Bestiary.encounterCount(Gnoll.class));
		assertTrue(Bestiary.isSeen(Gnoll.class));
	}

	@Test
	void storeRestoreRoundTrip() throws Exception {
		Bestiary.setSeen(Rat.class);                 // seen, uncounted
		Bestiary.setSeen(Gnoll.class);
		Bestiary.countEncounters(Gnoll.class, 3);    // seen and counted

		Bundle stored = new Bundle();
		Bestiary.store(stored);
		Bundle read = SaveRoundtrip.writeRead(stored);
		assertEquals(new HashSet<>(Arrays.asList("bestiary_classes", "bestiary_seen", "bestiary_encounters")),
				new HashSet<>(read.getKeys()));

		clearBestiary();
		Bestiary.restore(read);
		assertTrue(Bestiary.isSeen(Rat.class));
		assertEquals(0, Bestiary.encounterCount(Rat.class));
		assertTrue(Bestiary.isSeen(Gnoll.class));
		assertEquals(3, Bestiary.encounterCount(Gnoll.class));
		assertFalse(Bestiary.isSeen(Snake.class));
		assertEquals(0, Bestiary.encounterCount(Snake.class));

		// all three keys are needed, or nothing restores
		Bundle partial = new Bundle();
		partial.put("bestiary_classes", read.getClassArray("bestiary_classes"));
		partial.put("bestiary_encounters", read.getIntArray("bestiary_encounters"));
		clearBestiary();
		Bestiary.restore(SaveRoundtrip.writeRead(partial));
		assertFalse(Bestiary.isSeen(Rat.class));
		assertEquals(0, Bestiary.encounterCount(Gnoll.class));

		// an unlisted class in the array is skipped
		Bundle unlisted = new Bundle();
		unlisted.put("bestiary_classes", new Class[]{Keywraith.class, Snake.class});
		unlisted.put("bestiary_seen", new boolean[]{true, true});
		unlisted.put("bestiary_encounters", new int[]{5, 2});
		clearBestiary();
		Bestiary.restore(SaveRoundtrip.writeRead(unlisted));
		assertFalse(Bestiary.isSeen(Keywraith.class));
		assertEquals(0, Bestiary.encounterCount(Keywraith.class));
		assertTrue(Bestiary.isSeen(Snake.class));
		assertEquals(2, Bestiary.encounterCount(Snake.class));
	}

	@Test
	void journalSavesTheBestiary() throws Exception {
		Bestiary.setSeen(Snake.class);
		Bestiary.countEncounters(Snake.class, 4);
		Journal.saveGlobal(true);

		clearBestiary();
		Field loaded = Journal.class.getDeclaredField("loaded");
		loaded.setAccessible(true);
		loaded.setBoolean(null, false);
		Journal.loadGlobal();

		assertTrue(Bestiary.isSeen(Snake.class));
		assertEquals(4, Bestiary.encounterCount(Snake.class));
	}
}
