package com.qsr.customspd.journal;

import com.qsr.customspd.Badges;
import com.qsr.customspd.Dungeon;
import com.qsr.customspd.items.Generator;
import com.qsr.customspd.items.Item;
import com.qsr.customspd.items.armor.Armor;
import com.qsr.customspd.items.food.Food;
import com.qsr.customspd.items.potions.exotic.ExoticPotion;
import com.qsr.customspd.items.scrolls.exotic.ExoticScroll;
import com.qsr.customspd.items.weapon.Weapon;
import com.qsr.customspd.items.weapon.melee.Dagger;
import com.qsr.customspd.items.weapon.missiles.darts.TippedDart;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.SaveRoundtrip;
import com.qsr.customspd.test.StaticSnapshot;
import com.watabou.utils.Bundle;
import com.watabou.utils.Reflection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The journal catalog split into upstream's equipment and consumable categories (6a313b135).
 * Catalog, Badges and Journal statics are snapshotted around every test.
 */
class CatalogCategoriesTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	private StaticSnapshot statics;
	private String savedSeedText;

	@BeforeEach
	void snapshot() {
		statics = StaticSnapshot.capture(Arrays.<Class<?>>asList(Catalog.class, Badges.class, Journal.class));
		savedSeedText = Dungeon.customSeedText;
		Dungeon.customSeedText = "";
	}

	@AfterEach
	void restore() {
		Dungeon.customSeedText = savedSeedText;
		statics.restore();
	}

	@SuppressWarnings("unchecked")
	private static void clearSeen() throws ReflectiveOperationException {
		Field seen = Catalog.class.getDeclaredField("seen");
		seen.setAccessible(true);
		for (Catalog cat : Catalog.values()) {
			Map<Class<?>, Boolean> map = (Map<Class<?>, Boolean>) seen.get(cat);
			for (Map.Entry<Class<?>, Boolean> e : map.entrySet()) {
				e.setValue(false);
			}
		}
	}

	private static List<Class<?>> concat(Generator.Category... cats) {
		List<Class<?>> out = new ArrayList<>();
		for (Generator.Category cat : cats) out.addAll(Arrays.asList(cat.classes));
		return out;
	}

	@Test
	void everyCategoryIsListedOnceAsEquipmentOrConsumable() {
		List<Catalog> listed = new ArrayList<>(Catalog.equipmentCatalogs);
		listed.addAll(Catalog.consumableCatalogs);
		assertEquals(Arrays.asList(Catalog.values()).size(), listed.size());
		assertEquals(new LinkedHashSet<>(Arrays.asList(Catalog.values())), new LinkedHashSet<>(listed));
	}

	@Test
	void everyItemBelongsToOneCategoryAndCanBeShown() {
		Set<Class<?>> all = new LinkedHashSet<>();
		for (Catalog cat : Catalog.values()) {
			assertTrue(cat.totalItems() > 0, cat + " is empty");
			for (Class<?> cls : cat.items()) {
				assertTrue(all.add(cls), cls.getSimpleName() + " is listed in more than one category");
				if (cat == Catalog.ENCHANTMENTS) {
					assertTrue(Weapon.Enchantment.class.isAssignableFrom(cls), cls + " is not an enchantment");
				} else if (cat == Catalog.GLYPHS) {
					assertTrue(Armor.Glyph.class.isAssignableFrom(cls), cls + " is not a glyph");
				} else {
					assertTrue(Item.class.isAssignableFrom(cls), cls + " is not an item");
				}
				// the journal builds every entry with Reflection.newInstance
				assertNotNull(Reflection.newInstance(cls), cls + " cannot be instantiated for the journal");
			}
		}
	}

	@Test
	void everyCategoryHasATitle() {
		for (Catalog cat : Catalog.values()) {
			String title = cat.title();
			assertNotEquals(Messages.NO_TEXT_FOUND, title, cat + " has no title string");
			// a missing key comes back as a lookup path
			assertFalse(title.endsWith(".title"), cat + " has no title string: " + title);
			assertFalse(title.isEmpty(), cat + " has an empty title");
		}
	}

	@Test
	void weaponCategoriesFollowTheGeneratorTiers() {
		assertEquals(concat(Generator.Category.WEP_T1, Generator.Category.WEP_T2, Generator.Category.WEP_T3,
						Generator.Category.WEP_T4, Generator.Category.WEP_T5),
				new ArrayList<>(Catalog.MELEE_WEAPONS.items()));
		assertEquals(concat(Generator.Category.MIS_T1, Generator.Category.MIS_T2, Generator.Category.MIS_T3,
						Generator.Category.MIS_T4, Generator.Category.MIS_T5),
				new ArrayList<>(Catalog.THROWN_WEAPONS.items()));
	}

	@Test
	void exoticAndTippedDartCategoriesMirrorTheirBaseItemOrder() {
		// the maps behind these categories are ordered like the regular potions, scrolls and seeds
		List<Class<?>> exoticPotions = new ArrayList<>();
		for (Class<?> potion : Generator.Category.POTION.classes) exoticPotions.add(ExoticPotion.regToExo.get(potion));
		assertEquals(exoticPotions, new ArrayList<>(Catalog.EXOTIC_POTIONS.items()));

		List<Class<?>> exoticScrolls = new ArrayList<>();
		for (Class<?> scroll : Generator.Category.SCROLL.classes) exoticScrolls.add(ExoticScroll.regToExo.get(scroll));
		assertEquals(exoticScrolls, new ArrayList<>(Catalog.EXOTIC_SCROLLS.items()));

		List<Class<?>> darts = new ArrayList<>();
		for (Class<?> seed : Generator.Category.SEED.classes) darts.add(TippedDart.types.get(seed));
		assertEquals(darts, new ArrayList<>(Catalog.TIPPED_DARTS.items()));
	}

	@Test
	void newCategoriesPersistNextToTheIdentificationBadges() throws Exception {
		// a player who earned every identification badge: those categories come back from the
		// badges, but categories without a badge must still be written to the journal file
		for (Badges.Badge b : Catalog.catalogBadges.values()) Badges.unlock(b);
		Badges.unlock(Badges.Badge.ALL_ITEMS_IDENTIFIED);
		clearSeen();
		Catalog.setSeen(Food.class);

		Bundle stored = new Bundle();
		Catalog.store(stored);
		Bundle loaded = SaveRoundtrip.writeRead(stored);

		clearSeen();
		Catalog.restore(loaded);

		assertTrue(Catalog.isSeen(Food.class), "food has no badge, so it must survive a save");
		assertTrue(Catalog.isSeen(Dagger.class), "melee weapons come back from ALL_WEAPONS_IDENTIFIED");
		assertEquals(Catalog.MELEE_WEAPONS.totalItems(), Catalog.MELEE_WEAPONS.totalSeen());
		assertEquals(1, Catalog.FOOD.totalSeen());
		assertEquals(0, Catalog.BOMBS.totalSeen(), "nothing marks a category without a badge as seen");
	}

	@Test
	void seenItemsWithoutBadgesRoundTrip() throws Exception {
		clearSeen();
		Map<Catalog, Class<?>> picks = new LinkedHashMap<>();
		for (Catalog cat : Catalog.values()) {
			Class<?> first = cat.items().iterator().next();
			picks.put(cat, first);
			Catalog.setSeen(first);
		}

		Bundle stored = new Bundle();
		Catalog.store(stored);
		Bundle loaded = SaveRoundtrip.writeRead(stored);
		clearSeen();
		Catalog.restore(loaded);

		for (Map.Entry<Catalog, Class<?>> e : picks.entrySet()) {
			assertTrue(Catalog.isSeen(e.getValue()), e.getValue().getSimpleName() + " was lost");
			assertEquals(1, e.getKey().totalSeen(), e.getKey() + " gained entries on load");
		}
	}
}
