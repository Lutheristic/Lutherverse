package com.qsr.customspd.items.scrolls.exotic;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.items.Generator;
import com.qsr.customspd.items.Item;
import com.qsr.customspd.items.ItemStatusHandler;
import com.qsr.customspd.items.potions.Potion;
import com.qsr.customspd.items.rings.Ring;
import com.qsr.customspd.items.scrolls.Scroll;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import com.watabou.noosa.Group;
import com.watabou.utils.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The scroll of divination picks the item types it identifies with Random.element over the
 * unknown potion, scroll and ring types, so a seeded run only repeats if those sets iterate in a
 * fixed order. They come back as LinkedHashSets in the order of the generator's class tables;
 * a HashSet of classes would iterate by identity hash, which can change from one JVM run to the
 * next. That is a deliberate difference from upstream, which declares a plain HashSet.
 *
 * Identity hashes do not change inside one JVM, so no test here can show a HashSet giving two
 * orders. Instead this pins the declared ordered type, the table order, and that one seed gives
 * the scroll the same picks twice.
 */
class DivinationSeededPicksTest {

	private static final Class<?>[] OWNERS = {Potion.class, Scroll.class, Ring.class};

	private final Object[] savedHandlers = new Object[OWNERS.length];
	private Hero prevHero;
	private Hero hero;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@BeforeEach
	void setUp() throws Exception {
		for (int i = 0; i < OWNERS.length; i++) {
			savedHandlers[i] = handler(OWNERS[i]).get(null);
		}
		prevHero = Dungeon.hero;
		HeadlessLevel level = HeadlessLevel.install(8, 8);
		hero = new Hero();
		StubSprite.attach(hero);
		// the identify sparkle is added to the reader's sprite parent
		hero.sprite.parent = new Group();
		Dungeon.hero = HeadlessLevel.at(hero, 3 * level.width() + 3);
		curUser().set(null, hero);
	}

	@AfterEach
	void tearDown() throws Exception {
		curUser().set(null, null);
		HeadlessLevel.uninstall();
		Dungeon.hero = prevHero;
		for (int i = 0; i < OWNERS.length; i++) {
			handler(OWNERS[i]).set(null, savedHandlers[i]);
		}
	}

	private static Field handler(Class<?> owner) throws Exception {
		Field f = owner.getDeclaredField("handler");
		f.setAccessible(true);
		return f;
	}

	private static Field curUser() throws Exception {
		Field f = Item.class.getDeclaredField("curUser");
		f.setAccessible(true);
		return f;
	}

	@Test
	void unknownTypesComeBackInAnOrderedSet() throws Exception {
		assertEquals(LinkedHashSet.class, ItemStatusHandler.class.getMethod("unknown").getReturnType(),
				"ItemStatusHandler.unknown() must promise an iteration order");
		for (Class<?> owner : OWNERS) {
			assertEquals(LinkedHashSet.class, owner.getMethod("getUnknown").getReturnType(),
					owner.getSimpleName() + ".getUnknown() must promise an iteration order");
		}
	}

	@Test
	void unknownTypesFollowTheGeneratorTables() {
		freshHandlers();
		assertEquals(Arrays.asList(Generator.Category.POTION.classes), new ArrayList<>(Potion.getUnknown()));
		assertEquals(Arrays.asList(Generator.Category.SCROLL.classes), new ArrayList<>(Scroll.getUnknown()));
		assertEquals(Arrays.asList(Generator.Category.RING.classes), new ArrayList<>(Ring.getUnknown()));
	}

	@Test
	void theSameSeedIdentifiesTheSameTypes() throws Exception {
		Set<List<Class<?>>> outcomes = new LinkedHashSet<>();
		for (long seed = 1; seed <= 4; seed++) {
			List<Class<?>> first = readWithSeed(seed);
			assertEquals(4, first.size(), "a scroll of divination identifies four types");
			assertEquals(first, readWithSeed(seed), "seed " + seed + " identified different types on a rerun");
			outcomes.add(first);
		}
		assertTrue(outcomes.size() > 1, "every seed identified the same types, so the picks may not follow the seed");
	}

	/** Fresh appearance handlers with nothing known, as at the start of a run. */
	private static void freshHandlers() {
		Random.pushGenerator(99L);
		try {
			Scroll.initLabels();
			Potion.initColors();
			Ring.initGems();
		} finally {
			Random.popGenerator();
		}
	}

	/** Reads a scroll of divination from a fresh start; returns the types it identified, in table order. */
	private List<Class<?>> readWithSeed(long seed) {
		freshHandlers();
		Random.pushGenerator(seed);
		try {
			new ScrollOfDivination().doRead();
		} catch (RuntimeException e) {
			// the scroll opens a window listing what it identified; windows need the UI camera,
			// so headless that is where it stops, after the types are identified
			if (!openingTheResultWindow(e)) throw e;
		} finally {
			Random.popGenerator();
		}
		List<Class<?>> known = new ArrayList<>();
		addKnown(known, Generator.Category.POTION.classes, Potion.getUnknown());
		addKnown(known, Generator.Category.SCROLL.classes, Scroll.getUnknown());
		addKnown(known, Generator.Category.RING.classes, Ring.getUnknown());
		return known;
	}

	private static void addKnown(List<Class<?>> known, Class<?>[] table, Set<?> unknown) {
		for (Class<?> c : table) {
			if (!unknown.contains(c)) known.add(c);
		}
	}

	private static boolean openingTheResultWindow(Throwable e) {
		for (StackTraceElement frame : e.getStackTrace()) {
			if (frame.getClassName().equals(ScrollOfDivination.class.getName() + "$WndDivination")) {
				return true;
			}
		}
		return false;
	}
}
