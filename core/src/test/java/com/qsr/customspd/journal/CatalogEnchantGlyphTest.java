package com.qsr.customspd.journal;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.items.BrokenSeal;
import com.qsr.customspd.items.Dewdrop;
import com.qsr.customspd.items.EnergyCrystal;
import com.qsr.customspd.items.Gold;
import com.qsr.customspd.items.armor.Armor;
import com.qsr.customspd.items.armor.LeatherArmor;
import com.qsr.customspd.items.armor.glyphs.Repulsion;
import com.qsr.customspd.items.armor.glyphs.Stone;
import com.qsr.customspd.items.armor.glyphs.Thorns;
import com.qsr.customspd.items.armor.glyphs.Viscosity;
import com.qsr.customspd.items.keys.IronKey;
import com.qsr.customspd.items.quest.CeremonialCandle;
import com.qsr.customspd.items.quest.CorpseDust;
import com.qsr.customspd.items.quest.DarkGold;
import com.qsr.customspd.items.quest.DwarfToken;
import com.qsr.customspd.items.quest.Embers;
import com.qsr.customspd.items.weapon.Weapon;
import com.qsr.customspd.items.weapon.enchantments.Blazing;
import com.qsr.customspd.items.weapon.enchantments.Chilling;
import com.qsr.customspd.items.weapon.enchantments.Grim;
import com.qsr.customspd.items.weapon.enchantments.Vampiric;
import com.qsr.customspd.items.weapon.melee.Dagger;
import com.qsr.customspd.items.weapon.melee.Longsword;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.test.SaveRoundtrip;
import com.qsr.customspd.test.StaticSnapshot;
import com.qsr.customspd.test.StubSprite;
import com.watabou.utils.Bundle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Enchantments and glyphs are cataloged, along with the pickups that never enter the backpack
 * (upstream a3f2ab7a8). Each test runs on a real headless run with an empty catalog.
 */
class CatalogEnchantGlyphTest {

	private HeadlessRun run;
	private Hero hero;

	@BeforeEach
	void startRun() throws ReflectiveOperationException {
		run = HeadlessRun.start(20240611L);
		run.enterFirstLevel();
		hero = Dungeon.hero;
		hero.sprite = new StubSprite();
		clearSeen();
	}

	@AfterEach
	void closeRun() {
		if (run != null) run.close();
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

	private static List<Class<?>> concat(Class<?>[]... groups) {
		List<Class<?>> out = new ArrayList<>();
		for (Class<?>[] group : groups) out.addAll(Arrays.asList(group));
		return out;
	}

	@Test
	void enchantmentsAndGlyphsHaveTheirOwnEquipmentSections() {
		assertEquals(concat(Weapon.Enchantment.common, Weapon.Enchantment.uncommon,
						Weapon.Enchantment.rare, Weapon.Enchantment.curses),
				new ArrayList<>(Catalog.ENCHANTMENTS.items()));
		assertEquals(concat(Armor.Glyph.common, Armor.Glyph.uncommon, Armor.Glyph.rare, Armor.Glyph.curses),
				new ArrayList<>(Catalog.GLYPHS.items()));
		assertEquals(Arrays.asList(Catalog.MELEE_WEAPONS, Catalog.ARMOR, Catalog.ENCHANTMENTS, Catalog.GLYPHS),
				Catalog.equipmentCatalogs.subList(0, 4));

		assertTrue(Catalog.MISC_EQUIPMENT.items().contains(BrokenSeal.class));
		for (Class<?> quest : new Class<?>[]{ CorpseDust.class, Embers.class, CeremonialCandle.class,
				DarkGold.class, DwarfToken.class }) {
			assertTrue(Catalog.MISC_CONSUMABLES.items().contains(quest), quest.getSimpleName() + " is not listed");
		}
	}

	@Test
	void collectingAnIdentifiedWeaponRecordsItsEnchantment() {
		Weapon unknown = new Longsword();
		unknown.enchant(new Chilling());
		assertTrue(unknown.collect());
		assertFalse(Catalog.isSeen(Chilling.class), "an unidentified weapon keeps its enchantment hidden");

		Weapon known = new Longsword();
		known.enchant(new Blazing());
		known.identify(false);
		assertFalse(Catalog.isSeen(Blazing.class), "the weapon has not been picked up yet");
		assertTrue(known.collect());
		assertTrue(Catalog.isSeen(Blazing.class));
	}

	@Test
	void identifyingACarriedWeaponRecordsItsEnchantment() {
		Weapon weapon = new Longsword();
		weapon.enchant(new Grim());
		assertTrue(weapon.collect());
		assertFalse(Catalog.isSeen(Grim.class));

		weapon.identify();
		assertTrue(Catalog.isSeen(Grim.class));
	}

	@Test
	void enchantingAnIdentifiedCarriedWeaponRecordsTheEnchantment() {
		Weapon loose = new Longsword();
		loose.identify(false);
		loose.enchant(new Chilling());
		assertFalse(Catalog.isSeen(Chilling.class), "a weapon on the floor is not the hero's");

		Weapon carried = new Longsword();
		carried.identify(false);
		assertTrue(carried.collect());
		carried.enchant(new Vampiric());
		assertTrue(Catalog.isSeen(Vampiric.class));
	}

	@Test
	void glyphsAreRecordedLikeEnchantments() {
		Armor unknown = new LeatherArmor();
		unknown.inscribe(new Repulsion());
		assertTrue(unknown.collect());
		assertFalse(Catalog.isSeen(Repulsion.class), "an unidentified armor keeps its glyph hidden");
		unknown.identify();
		assertTrue(Catalog.isSeen(Repulsion.class), "identifying a carried armor");

		Armor known = new LeatherArmor();
		known.inscribe(new Viscosity());
		known.identify(false);
		assertTrue(known.collect());
		assertTrue(Catalog.isSeen(Viscosity.class), "collecting an identified armor");

		Armor loose = new LeatherArmor();
		loose.identify(false);
		loose.inscribe(new Thorns());
		assertFalse(Catalog.isSeen(Thorns.class), "an armor on the floor is not the hero's");

		known.inscribe(new Stone());
		assertTrue(Catalog.isSeen(Stone.class), "inscribing an identified carried armor");
	}

	@Test
	void cleansingTheEnchantmentOrGlyphOfCarriedGearIsSafe() {
		Weapon weapon = new Longsword();
		weapon.enchant(new Blazing());
		weapon.identify(false);
		assertTrue(weapon.collect());
		weapon.enchant(null);
		assertEquals(null, weapon.enchantment);

		Armor armor = new LeatherArmor();
		armor.inscribe(new Viscosity());
		armor.identify(false);
		assertTrue(armor.collect());
		armor.inscribe(null);
		assertEquals(null, armor.glyph);
	}

	@Test
	void pickupsThatNeverEnterTheBackpackAreRecorded() {
		new Gold(10).doPickUp(hero, hero.pos);
		new EnergyCrystal(2).doPickUp(hero, hero.pos);
		new Dewdrop().doPickUp(hero, hero.pos);
		new IronKey(Dungeon.levelName).doPickUp(hero, hero.pos);

		assertTrue(Catalog.isSeen(Gold.class), "gold");
		assertTrue(Catalog.isSeen(EnergyCrystal.class), "energy crystal");
		assertTrue(Catalog.isSeen(Dewdrop.class), "dewdrop");
		assertTrue(Catalog.isSeen(IronKey.class), "iron key");
	}

	@Test
	void theStartingSealIsRecordedThoughItIsNeverCollected() throws Exception {
		run.close();
		run = null;
		StaticSnapshot catalog = StaticSnapshot.capture(Collections.<Class<?>>singletonList(Catalog.class));
		try {
			clearSeen();
			try (HeadlessRun fresh = HeadlessRun.start(20240611L)) {
				assertTrue(Catalog.isSeen(BrokenSeal.class), "the warrior starts with the seal on his armor");
			}
		} finally {
			catalog.restore();
		}
	}

	@Test
	void enchantmentAndGlyphEntriesSurviveASave() throws Exception {
		Catalog.setSeen(Blazing.class);
		Catalog.setSeen(Viscosity.class);

		Bundle stored = new Bundle();
		Catalog.store(stored);
		Bundle loaded = SaveRoundtrip.writeRead(stored);
		clearSeen();
		Catalog.restore(loaded);

		assertTrue(Catalog.isSeen(Blazing.class));
		assertTrue(Catalog.isSeen(Viscosity.class));
		assertEquals(1, Catalog.ENCHANTMENTS.totalSeen());
		assertEquals(1, Catalog.GLYPHS.totalSeen());
	}

	@Test
	void aJournalFromBeforeTheseSectionsStillLoads() throws Exception {
		Bundle old = new Bundle();
		old.put("catalog_items", new Class[]{ Dagger.class });

		Catalog.restore(SaveRoundtrip.writeRead(old));

		assertTrue(Catalog.isSeen(Dagger.class));
		assertEquals(0, Catalog.ENCHANTMENTS.totalSeen());
		assertEquals(0, Catalog.GLYPHS.totalSeen());
	}
}
