package com.qsr.customspd.journal;

import com.qsr.customspd.Badges;
import com.qsr.customspd.Dungeon;
import com.qsr.customspd.Statistics;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.hero.HeroClass;
import com.qsr.customspd.actors.hero.Talent;
import com.qsr.customspd.items.Item;
import com.qsr.customspd.items.artifacts.SandalsOfNature;
import com.qsr.customspd.items.potions.Potion;
import com.qsr.customspd.items.scrolls.InventoryScroll;
import com.qsr.customspd.items.scrolls.Scroll;
import com.qsr.customspd.items.scrolls.ScrollOfUpgrade;
import com.qsr.customspd.items.spells.InventorySpell;
import com.qsr.customspd.items.spells.MagicalInfusion;
import com.qsr.customspd.items.weapon.melee.Longsword;
import com.qsr.customspd.items.weapon.melee.Mace;
import com.qsr.customspd.plants.Sungrass;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.IdleCellSelector;
import com.qsr.customspd.test.StaticSnapshot;
import com.qsr.customspd.test.StubSprite;
import com.qsr.customspd.windows.WndBag;
import com.watabou.noosa.Group;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Upgrades count once per upgrader and once for the upgraded item, and the sandals count the
 * upgrades they gain from seeds (upstream 1bfe1f606).
 */
class UpgradeUseCountTest {

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
				Statistics.class, Potion.class, Scroll.class));
		CatalogUseCountTest.clearCatalog();
	}

	@AfterEach
	void restore() throws Exception {
		itemStatic("curUser").set(null, null);
		itemStatic("curItem").set(null, null);
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
		statics.restore();
	}

	private static Field itemStatic(String name) throws Exception {
		Field f = Item.class.getDeclaredField(name);
		f.setAccessible(true);
		return f;
	}

	private static WndBag.ItemSelector selector(Class<?> owner, String name, Object item) throws Exception {
		Field f = owner.getDeclaredField(name);
		f.setAccessible(true);
		return (WndBag.ItemSelector) f.get(item);
	}

	private Hero hero() throws Exception {
		HeadlessLevel level = HeadlessLevel.install(9, 9);
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

	@Test
	void aScrollOfUpgradeCountsOnceAndCountsTheItem() throws Exception {
		Hero hero = hero();
		CatalogUseCountTest.giveHeadlessHeroSprite(hero);
		ScrollOfUpgrade scroll = new ScrollOfUpgrade();
		scroll.collect(hero.belongings.backpack);
		itemStatic("curItem").set(null, scroll);
		Longsword sword = new Longsword();

		selector(InventoryScroll.class, "itemSelector", scroll).onSelect(sword);

		assertEquals(1, sword.level(), "fixture: the sword was upgraded");
		assertEquals(1, Catalog.useCount(ScrollOfUpgrade.class), "one scroll was read");
		assertEquals(1, Catalog.useCount(Longsword.class), "one upgrade went into the sword");
	}

	@Test
	void magicalInfusionCountsOnceAndCountsTheItem() throws Exception {
		Hero hero = hero();
		MagicalInfusion infusion = new MagicalInfusion();
		infusion.collect(hero.belongings.backpack);
		itemStatic("curItem").set(null, infusion);
		Mace mace = new Mace();

		selector(InventorySpell.class, "itemSelector", infusion).onSelect(mace);

		assertEquals(1, mace.level(), "fixture: the mace was upgraded");
		assertEquals(1, Catalog.useCount(MagicalInfusion.class), "one infusion was used");
		assertEquals(1, Catalog.useCount(Mace.class), "one upgrade went into the mace");
	}

	private static void feedSeeds(Hero hero, SandalsOfNature sandals, int seeds) throws Exception {
		WndBag.ItemSelector feed = selector(SandalsOfNature.class, "itemSelector", sandals);
		for (int i = 0; i < seeds; i++) {
			Sungrass.Seed seed = new Sungrass.Seed();
			seed.collect(hero.belongings.backpack);
			feed.onSelect(seed);
		}
	}

	@Test
	void sandalsCountTheUpgradesTheyGrowFromSeeds() throws Exception {
		Hero hero = hero();
		SandalsOfNature sandals = new SandalsOfNature();
		sandals.collect(hero.belongings.backpack);

		feedSeeds(hero, sandals, 3);
		assertEquals(1, sandals.level(), "fixture: three seeds grow the sandals");
		assertEquals(3, Catalog.useCount(SandalsOfNature.class), "the first level is three upgrades");

		sandals.level(2);
		feedSeeds(hero, sandals, 9);
		assertEquals(3, sandals.level(), "fixture: nine more seeds grow them to the last level");
		assertEquals(7, Catalog.useCount(SandalsOfNature.class), "the last level is four upgrades");
	}
}
