package com.qsr.customspd.journal;

import com.qsr.customspd.Badges;
import com.qsr.customspd.Dungeon;
import com.qsr.customspd.items.Generator;
import com.qsr.customspd.items.Item;
import com.qsr.customspd.items.potions.Potion;
import com.qsr.customspd.items.potions.PotionOfHealing;
import com.qsr.customspd.items.scrolls.Scroll;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessRun;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The badges redesigned for the expanded catalog (b003d1129): the old per-category identification
 * badges are internal now, kept only so saves from before the split still restore their categories,
 * and the researcher / well equipped / fully stocked / king's name badges replace them.
 */
class CatalogBadgesTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	private StaticSnapshot statics;
	private String savedSeedText;

	@BeforeEach
	void snapshot() throws ReflectiveOperationException {
		statics = StaticSnapshot.capture(Arrays.<Class<?>>asList(
				Catalog.class, Badges.class, Journal.class, Document.class));
		savedSeedText = Dungeon.customSeedText;
		Dungeon.customSeedText = "";
		clearSeen();
		for (Badges.Badge b : Badges.Badge.values()) Badges.disown(b);
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
			for (Map.Entry<Class<?>, Boolean> e : ((Map<Class<?>, Boolean>) seen.get(cat)).entrySet()) {
				e.setValue(false);
			}
		}
	}

	private static int foundGuidePages() {
		int found = 0;
		for (Document doc : Document.values()) {
			if (doc.isLoreDoc()) continue;
			for (String page : doc.pageNames()) {
				if (doc.isPageFound(page)) found++;
			}
		}
		return found;
	}

	private static List<Class<?>> everyCatalogItem() {
		List<Class<?>> all = new ArrayList<>();
		for (Catalog cat : Catalog.values()) all.addAll(cat.items());
		return all;
	}

	// ------------------------------------------------------------------ saves from before the split

	@Test
	void oldIdentificationBadgesStillRestoreTheirCategories() throws Exception {
		// a pre-split journal file: the weapons badge stood in for every weapon, so none was written
		Badges.unlock(Badges.Badge.ALL_WEAPONS_IDENTIFIED);
		Bundle old = new Bundle();
		old.put("catalog_items", new Class[]{ PotionOfHealing.class });

		Catalog.restore(SaveRoundtrip.writeRead(old));

		assertEquals(Catalog.MELEE_WEAPONS.totalItems(), Catalog.MELEE_WEAPONS.totalSeen(),
				"every melee weapon comes back from ALL_WEAPONS_IDENTIFIED");
		assertTrue(Catalog.isSeen(PotionOfHealing.class));
		assertEquals(1, Catalog.POTIONS.totalSeen());

		// from now on the file itself holds them, so the badge is no longer needed for them
		Bundle stored = new Bundle();
		Catalog.store(stored);
		List<Class> written = Arrays.asList(SaveRoundtrip.writeRead(stored).getClassArray("catalog_items"));
		for (Class<?> weapon : Catalog.MELEE_WEAPONS.items()) {
			assertTrue(written.contains(weapon), weapon.getSimpleName() + " was not written to the journal file");
		}
	}

	@Test
	void identifyingAWholeCategoryNoLongerUnlocksTheOldBadges() {
		for (Class<?> armor : Catalog.ARMOR.items()) Catalog.setSeen(armor);
		assertEquals(Catalog.ARMOR.totalItems(), Catalog.ARMOR.totalSeen());
		assertFalse(Badges.isUnlocked(Badges.Badge.ALL_ARMOR_IDENTIFIED));
		assertEquals(Badges.BadgeType.HIDDEN, Badges.Badge.ALL_ARMOR_IDENTIFIED.type);
	}

	// ------------------------------------------------------------------------------ researcher

	@Test
	void researcherBadgesFollowTheNumberOfEntries() {
		int entries = foundGuidePages();
		List<Class<?>> items = everyCatalogItem();
		int next = 0;
		int[] thresholds = { 40, 80, 160 };
		Badges.Badge[] badges = { Badges.Badge.RESEARCHER_1, Badges.Badge.RESEARCHER_2, Badges.Badge.RESEARCHER_3 };
		for (int t = 0; t < thresholds.length; t++) {
			while (entries < thresholds[t] - 1) {
				Catalog.setSeen(items.get(next++));
				entries++;
			}
			assertFalse(Badges.isUnlocked(badges[t]), badges[t] + " at " + entries + " entries");
			Catalog.setSeen(items.get(next++));
			entries++;
			assertTrue(Badges.isUnlocked(badges[t]), badges[t] + " at " + entries + " entries");
			for (int lower = 0; lower < t; lower++) {
				assertTrue(Badges.isUnlocked(badges[lower]), badges[lower] + " must stay unlocked");
			}
		}
	}

	@Test
	void grandmasterResearcherNeedsEveryEntry() {
		List<Class<?>> items = everyCatalogItem();
		for (Document doc : Document.values()) {
			if (doc.isLoreDoc()) continue;
			for (String page : doc.pageNames()) doc.findPage(page);
		}
		for (int i = 0; i < items.size() - 1; i++) Catalog.setSeen(items.get(i));
		assertFalse(Badges.isUnlocked(Badges.Badge.RESEARCHER_5), "one entry is still missing");

		Catalog.setSeen(items.get(items.size() - 1));
		assertTrue(Badges.isUnlocked(Badges.Badge.RESEARCHER_5));
		assertTrue(Badges.isUnlocked(Badges.Badge.RESEARCHER_3));
	}

	@Test
	void aFullJournalEarnsEveryResearcherBadge() {
		// the enchantment and glyph sections take the total past 320, so researcher 4 can be earned
		int total = 0;
		for (Catalog cat : Catalog.values()) total += cat.totalItems();
		for (Document doc : Document.values()) {
			if (!doc.isLoreDoc()) total += doc.pageNames().size();
		}
		assertTrue(total >= 320, total + " entries, so researcher 4 can never be earned");

		for (Document doc : Document.values()) {
			if (doc.isLoreDoc()) continue;
			for (String page : doc.pageNames()) doc.findPage(page);
		}
		for (Class<?> item : everyCatalogItem()) Catalog.setSeen(item);
		assertTrue(Badges.isUnlocked(Badges.Badge.RESEARCHER_5));
		assertTrue(Badges.isUnlocked(Badges.Badge.RESEARCHER_3));
		assertTrue(Badges.isUnlocked(Badges.Badge.RESEARCHER_4));
	}

	// ----------------------------------------------------------------------- task badges

	@Test
	void wellEquippedNeedsOneEntryInEveryEquipmentCategory() {
		List<Catalog> equipment = Catalog.equipmentCatalogs;
		for (int i = 0; i < equipment.size() - 1; i++) {
			Catalog.setSeen(equipment.get(i).items().iterator().next());
		}
		assertFalse(Badges.isUnlocked(Badges.Badge.CATALOG_ONE_EQUIPMENT));
		Catalog.setSeen(equipment.get(equipment.size() - 1).items().iterator().next());
		assertTrue(Badges.isUnlocked(Badges.Badge.CATALOG_ONE_EQUIPMENT));
	}

	@Test
	void readingTheKingsLastPageUnlocksRodney() {
		assertFalse(Document.HALLS_KING.isPageFound(Document.KING_ATTRITION));
		Document.HALLS_KING.findPage(Document.KING_ATTRITION);
		assertFalse(Badges.isUnlocked(Badges.Badge.RODNEY), "finding the page is not reading it");

		Document.HALLS_KING.readPage(Document.KING_ATTRITION);
		assertTrue(Badges.isUnlocked(Badges.Badge.RODNEY));

		assertTrue(Document.HALLS_KING.unreadPage(Document.KING_ATTRITION));
		assertFalse(Document.HALLS_KING.isPageRead(Document.KING_ATTRITION));
		assertTrue(Document.HALLS_KING.isPageFound(Document.KING_ATTRITION), "unreading keeps the page");
	}

	@Test
	void fullyStockedNeedsEveryPotionAndScrollKnownByALivingHero() {
		try (HeadlessRun run = HeadlessRun.start(20261025L)) {
			Dungeon.customSeedText = ""; // a regular run, so the per-run badge can unlock
			List<Class<?>> all = new ArrayList<>(Arrays.asList(Generator.Category.POTION.classes));
			all.addAll(Arrays.asList(Generator.Category.SCROLL.classes));
			for (int i = 0; i < all.size() - 1; i++) {
				((Item) Reflection.newInstance(all.get(i))).identify();
			}
			assertFalse(Badges.isUnlocked(Badges.Badge.CATALOG_POTIONS_SCROLLS));

			Dungeon.hero.HP = 0;
			((Item) Reflection.newInstance(all.get(all.size() - 1))).identify();
			assertTrue(Potion.allKnown() && Scroll.allKnown(), "test setup: everything is known");
			Badges.validateCatalogBadges();
			assertFalse(Badges.isUnlocked(Badges.Badge.CATALOG_POTIONS_SCROLLS), "not after the hero has died");

			Dungeon.hero.HP = Dungeon.hero.HT;
			Badges.validateCatalogBadges();
			assertTrue(Badges.isUnlocked(Badges.Badge.CATALOG_POTIONS_SCROLLS));
		}
	}
}
