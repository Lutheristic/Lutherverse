package com.qsr.customspd.journal;

import com.qsr.customspd.Badges;
import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.hero.HeroClass;
import com.qsr.customspd.actors.hero.Talent;
import com.qsr.customspd.items.Dewdrop;
import com.qsr.customspd.items.Gold;
import com.qsr.customspd.items.Item;
import com.qsr.customspd.items.food.Food;
import com.qsr.customspd.items.keys.IronKey;
import com.qsr.customspd.items.potions.Potion;
import com.qsr.customspd.items.potions.PotionOfHealing;
import com.qsr.customspd.items.potions.PotionOfLevitation;
import com.qsr.customspd.items.potions.PotionOfToxicGas;
import com.qsr.customspd.items.potions.brews.CausticBrew;
import com.qsr.customspd.items.quest.GooBlob;
import com.qsr.customspd.items.scrolls.Scroll;
import com.qsr.customspd.items.scrolls.ScrollOfIdentify;
import com.qsr.customspd.items.stones.Runestone;
import com.qsr.customspd.items.stones.StoneOfDeepSleep;
import com.qsr.customspd.items.weapon.enchantments.Blazing;
import com.qsr.customspd.sprites.HeroSprite;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.IdleCellSelector;
import com.qsr.customspd.test.SaveRoundtrip;
import com.qsr.customspd.test.StaticSnapshot;
import com.qsr.customspd.test.StubSprite;
import com.watabou.noosa.Group;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Bundle;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The catalog counts how often each entry is used (upstream f6299e07c), keeps the counts in the
 * journal file next to the seen flags, and still reads journal files written before it did.
 */
class CatalogUseCountTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
		IdleCellSelector.install();
	}

	@AfterAll
	static void restoreCellSelector() {
		IdleCellSelector.uninstall();
	}

	private StaticSnapshot statics;

	@BeforeEach
	void snapshot() throws Exception {
		statics = StaticSnapshot.capture(Arrays.<Class<?>>asList(Catalog.class, Badges.class, Journal.class,
				Notes.class, Potion.class, Scroll.class));
		clearCatalog();
	}

	@AfterEach
	void restore() throws Exception {
		itemStatic("curUser").set(null, null);
		itemStatic("curItem").set(null, null);
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
		statics.restore();
	}

	@SuppressWarnings("unchecked")
	static void clearCatalog() throws ReflectiveOperationException {
		Field seen = Catalog.class.getDeclaredField("seen");
		Field uses = Catalog.class.getDeclaredField("useCount");
		seen.setAccessible(true);
		uses.setAccessible(true);
		for (Catalog cat : Catalog.values()) {
			for (Map.Entry<Class<?>, Boolean> e : ((Map<Class<?>, Boolean>) seen.get(cat)).entrySet()) {
				e.setValue(false);
			}
			for (Map.Entry<Class<?>, Integer> e : ((Map<Class<?>, Integer>) uses.get(cat)).entrySet()) {
				e.setValue(0);
			}
		}
	}

	private static Field itemStatic(String name) throws Exception {
		Field f = Item.class.getDeclaredField(name);
		f.setAccessible(true);
		return f;
	}

	private HeadlessLevel level;

	private Hero hero() throws Exception {
		level = HeadlessLevel.install(9, 9);
		Hero hero = new Hero();
		hero.heroClass = HeroClass.MAGE;
		Talent.initClassTalents(hero);
		hero.HP = hero.HT = 100;
		Dungeon.hero = hero;
		new Group().add(StubSprite.attach(hero));
		HeadlessLevel.at(hero, 3 * level.width() + 3);
		itemStatic("curUser").set(null, hero);
		Potion.initColors();
		Scroll.initLabels();
		return hero;
	}

	/** Scroll.readAnimation casts the reader's sprite to HeroSprite, whose constructor loads textures. */
	static class HeadlessHeroSprite extends HeroSprite {
		@Override public Emitter emitter() { return new Emitter(); }
		@Override public Emitter centerEmitter() { return new Emitter(); }
		@Override public Emitter bottomEmitter() { return new Emitter(); }
	}

	static void giveHeadlessHeroSprite(Hero hero) throws Exception {
		Field f = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
		f.setAccessible(true);
		Object unsafe = f.get(null);
		HeroSprite sprite = (HeroSprite) unsafe.getClass().getMethod("allocateInstance", Class.class)
				.invoke(unsafe, HeadlessHeroSprite.class);
		sprite.ch = hero;
		hero.sprite = sprite;
	}

	// ------------------------------------------------------------------------ counting

	@Test
	void usesAddUpPerEntry() {
		Catalog.countUse(Food.class);
		Catalog.countUse(Food.class);
		Catalog.countUses(Food.class, 3);
		assertEquals(5, Catalog.useCount(Food.class));
		assertEquals(0, Catalog.useCount(PotionOfHealing.class));

		Catalog.countUse(Hero.class);
		assertEquals(0, Catalog.useCount(Hero.class), "a class outside the catalog is not counted");
	}

	@Test
	void aCountThatWouldOverflowStopsAtTheMaximum() {
		Catalog.countUses(Gold.class, Integer.MAX_VALUE - 5);
		Catalog.countUses(Gold.class, 10);
		assertEquals(Integer.MAX_VALUE, Catalog.useCount(Gold.class));
		Catalog.countUse(Gold.class);
		assertEquals(Integer.MAX_VALUE, Catalog.useCount(Gold.class), "a maxed count stays maxed");
	}

	// ------------------------------------------------------------------------ the journal file

	@Test
	void seenFlagsAndCountsSurviveASave() throws Exception {
		Catalog.setSeen(Food.class);
		Catalog.countUses(Food.class, 4);
		Catalog.countUses(Dewdrop.class, 7);
		Catalog.setSeen(Blazing.class);

		Bundle stored = new Bundle();
		Catalog.store(stored);
		Bundle loaded = SaveRoundtrip.writeRead(stored);
		assertTrue(loaded.contains("catalog_classes"));
		assertTrue(loaded.contains("catalog_seen"));
		assertTrue(loaded.contains("catalog_uses"));
		assertFalse(loaded.contains("catalog_items"), "the old key is only read, no longer written");

		clearCatalog();
		Catalog.restore(loaded);

		assertTrue(Catalog.isSeen(Food.class));
		assertEquals(4, Catalog.useCount(Food.class));
		assertFalse(Catalog.isSeen(Dewdrop.class), "an entry kept only for its count stays unseen");
		assertEquals(7, Catalog.useCount(Dewdrop.class));
		assertTrue(Catalog.isSeen(Blazing.class));
		assertEquals(1, Catalog.FOOD.totalSeen());
		assertEquals(0, Catalog.MISC_CONSUMABLES.totalSeen());
	}

	@Test
	void aJournalFromBeforeCountsAndEnchantmentsStillLoads() throws Exception {
		// what CPDU wrote before this: seen items only, no enchantment or glyph entries, no counts
		Bundle old = new Bundle();
		old.put("catalog_items", new Class[]{ Food.class, PotionOfHealing.class });
		Catalog.restore(SaveRoundtrip.writeRead(old));

		assertTrue(Catalog.isSeen(Food.class));
		assertTrue(Catalog.isSeen(PotionOfHealing.class));
		assertEquals(0, Catalog.useCount(Food.class));
		assertEquals(0, Catalog.ENCHANTMENTS.totalSeen());
		assertEquals(0, Catalog.GLYPHS.totalSeen());

		// the next save moves it to the new keys
		Catalog.countUse(Food.class);
		Bundle stored = new Bundle();
		Catalog.store(stored);
		clearCatalog();
		Catalog.restore(SaveRoundtrip.writeRead(stored));
		assertTrue(Catalog.isSeen(Food.class));
		assertTrue(Catalog.isSeen(PotionOfHealing.class));
		assertEquals(1, Catalog.useCount(Food.class));
	}

	// ------------------------------------------------------------------------ what counts a use

	@Test
	void drinkingOrThrowingAPotionCountsAUseUnlessItIsAnonymous() throws Exception {
		Hero hero = hero();
		Method drink = Potion.class.getDeclaredMethod("drink", Hero.class);
		drink.setAccessible(true);
		Method onThrow = Potion.class.getDeclaredMethod("onThrow", int.class);
		onThrow.setAccessible(true);

		PotionOfLevitation drunk = new PotionOfLevitation();
		drunk.collect(hero.belongings.backpack);
		drink.invoke(drunk, hero);
		assertEquals(1, Catalog.useCount(PotionOfLevitation.class), "drinking");

		onThrow.invoke(new PotionOfLevitation(), 6 * level.width() + 6);
		assertEquals(2, Catalog.useCount(PotionOfLevitation.class), "throwing");

		PotionOfLevitation anonymous = new PotionOfLevitation();
		anonymous.anonymize();
		anonymous.collect(hero.belongings.backpack);
		drink.invoke(anonymous, hero);
		assertEquals(2, Catalog.useCount(PotionOfLevitation.class), "an anonymous potion is not counted");
	}

	@Test
	void readingAScrollCountsAUseUnlessItIsAnonymous() throws Exception {
		Hero hero = hero();
		giveHeadlessHeroSprite(hero);
		Method readAnimation = Scroll.class.getDeclaredMethod("readAnimation");
		readAnimation.setAccessible(true);

		readAnimation.invoke(new ScrollOfIdentify());
		assertEquals(1, Catalog.useCount(ScrollOfIdentify.class));

		ScrollOfIdentify anonymous = new ScrollOfIdentify();
		anonymous.anonymize();
		readAnimation.invoke(anonymous);
		assertEquals(1, Catalog.useCount(ScrollOfIdentify.class), "an anonymous scroll is not counted");
	}

	@Test
	void aThrownRunestoneCountsAUse() throws Exception {
		hero();
		Method onThrow = Runestone.class.getDeclaredMethod("onThrow", int.class);
		onThrow.setAccessible(true);
		onThrow.invoke(new StoneOfDeepSleep(), 6 * level.width() + 6);
		assertEquals(1, Catalog.useCount(StoneOfDeepSleep.class));
	}

	@Test
	void usingKeysCountsEachKey() {
		Notes.reset();
		IronKey key = new IronKey("floor");
		key.quantity(2);
		Notes.add(key);
		Notes.remove(new IronKey("floor"));
		assertEquals(1, Catalog.useCount(IronKey.class));
		Notes.remove(new IronKey("elsewhere"));
		assertEquals(1, Catalog.useCount(IronKey.class), "a key the journal does not hold is not used");
	}

	@Test
	void aDewdropThatHealsCountsAUse() throws Exception {
		Hero hero = hero();
		hero.HP = 50;
		assertTrue(new Dewdrop().doPickUp(hero, hero.pos));
		assertEquals(1, Catalog.useCount(Dewdrop.class));

		hero.HP = hero.HT;
		new Dewdrop().doPickUp(hero, hero.pos);
		assertEquals(1, Catalog.useCount(Dewdrop.class), "a dewdrop left on the floor is not used");
	}

	@Test
	void brewingWithAGooBlobCountsTheBlob() throws Exception {
		hero();
		PotionOfToxicGas gas = new PotionOfToxicGas();
		gas.identify();
		ArrayList<Item> ingredients = new ArrayList<>();
		ingredients.add(gas);
		ingredients.add(new GooBlob());
		Item made = new CausticBrew.Recipe().brew(ingredients);
		assertTrue(made instanceof CausticBrew, "fixture: the brew was made");
		assertEquals(1, Catalog.useCount(GooBlob.class));
	}
}
