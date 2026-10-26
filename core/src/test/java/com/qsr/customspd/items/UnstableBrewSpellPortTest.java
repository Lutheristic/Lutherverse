package com.qsr.customspd.items;

import com.qsr.customspd.ShatteredPixelDungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.assets.GeneralAsset;
import com.qsr.customspd.items.potions.Potion;
import com.qsr.customspd.items.potions.PotionOfExperience;
import com.qsr.customspd.items.potions.PotionOfFrost;
import com.qsr.customspd.items.potions.PotionOfHaste;
import com.qsr.customspd.items.potions.PotionOfHealing;
import com.qsr.customspd.items.potions.PotionOfInvisibility;
import com.qsr.customspd.items.potions.PotionOfLevitation;
import com.qsr.customspd.items.potions.PotionOfLiquidFlame;
import com.qsr.customspd.items.potions.PotionOfMindVision;
import com.qsr.customspd.items.potions.PotionOfParalyticGas;
import com.qsr.customspd.items.potions.PotionOfPurity;
import com.qsr.customspd.items.potions.PotionOfToxicGas;
import com.qsr.customspd.items.potions.brews.UnstableBrew;
import com.qsr.customspd.items.potions.exotic.ExoticPotion;
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
import com.qsr.customspd.items.scrolls.exotic.ExoticScroll;
import com.qsr.customspd.items.spells.UnstableSpell;
import com.qsr.customspd.items.stones.StoneOfAugmentation;
import com.qsr.customspd.journal.Document;
import com.qsr.customspd.plants.Sungrass;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.SaveRoundtrip;
import com.qsr.customspd.test.StaticSnapshot;
import com.qsr.customspd.windows.WndJournal;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream a00355b71 (v2.4.0): the catalysts are gone. The alchemical catalyst becomes the unstable
 * brew (potion + seed) and the arcane catalyst becomes the unstable spell (scroll + runestone),
 * each for 1 energy since 938a78883; the other pairing each catalyst accepted no longer brews
 * anything. Saves carry the old class names, so Bundle aliases map them. The alchemy guide loses
 * its catalysts page, leaving nine.
 *
 * The random-effect tables keep v2.5.4's entries and weights in a LinkedHashMap, not upstream's
 * HashMap: Random.chances walks the key set, and Class keys hash by identity, so a HashMap would
 * roll a different effect for the same seed from one JVM run to the next.
 *
 * Removing the two catalyst sprites shifts the GeneralAsset ordinals ExoticPotion and ExoticScroll
 * count across to find their art, so the exotic images are pinned here too.
 */
class UnstableBrewSpellPortTest {

	private static final String OLD_BREW = "com.qsr.customspd.items.potions.AlchemicalCatalyst";
	private static final String OLD_SPELL = "com.qsr.customspd.items.spells.ArcaneCatalyst";

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
		ShatteredPixelDungeon.registerBundleAliases();
	}

	private static Bundlable restoreUnderOldName(Bundlable obj, String oldClassName) throws Exception {
		Bundle saved = new Bundle();
		saved.put("obj", obj);
		String json = saved.toString().replace(obj.getClass().getName(), oldClassName);
		assertTrue(json.contains(oldClassName), "the fixture must really carry the old class name");
		Bundle read = Bundle.read(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
		return SaveRoundtrip.writeRead(read).get("obj");
	}

	private static ArrayList<Item> items(Item... items) {
		return new ArrayList<>(Arrays.asList(items));
	}

	private static Potion potion(Potion p) {
		p.anonymize();
		return p;
	}

	private static Scroll scroll(Scroll s) {
		s.anonymize();
		return s;
	}

	private static boolean offered(ArrayList<Item> ingredients, Class<?> recipeClass) {
		for (Recipe r : Recipe.findRecipes(ingredients)) {
			if (r.getClass() == recipeClass) return true;
		}
		return false;
	}

	@Test
	void theCatalystNamesNoLongerResolveByThemselves() {
		assertNull(Reflection.forName(OLD_BREW));
		assertNull(Reflection.forName(OLD_SPELL));
	}

	@Test
	void anOldAlchemicalCatalystStackLoadsAsUnstableBrew() throws Exception {
		Bundlable restored = restoreUnderOldName(new UnstableBrew().quantity(3), OLD_BREW);

		assertTrue(restored instanceof UnstableBrew, "restored as " + restored);
		assertEquals(3, ((UnstableBrew) restored).quantity());
	}

	@Test
	void anOldArcaneCatalystStackLoadsAsUnstableSpell() throws Exception {
		Bundlable restored = restoreUnderOldName(new UnstableSpell().quantity(4), OLD_SPELL);

		assertTrue(restored instanceof UnstableSpell, "restored as " + restored);
		assertEquals(4, ((UnstableSpell) restored).quantity());
	}

	@Test
	void aPotionAndASeedBrewAnUnstableBrewForOneEnergy() {
		ArrayList<Item> ingredients = items(potion(new PotionOfHealing()), new Sungrass.Seed());
		UnstableBrew.Recipe recipe = new UnstableBrew.Recipe();

		assertTrue(recipe.testIngredients(ingredients));
		assertEquals(1, recipe.cost(ingredients));
		assertTrue(recipe.sampleOutput(ingredients) instanceof UnstableBrew);
		assertTrue(offered(ingredients, UnstableBrew.Recipe.class), "the alchemy pot must offer it");
	}

	@Test
	void aPotionAndARunestoneNoLongerBrewAnything() {
		ArrayList<Item> ingredients = items(potion(new PotionOfHealing()), new StoneOfAugmentation());

		assertFalse(new UnstableBrew.Recipe().testIngredients(ingredients));
		assertTrue(Recipe.findRecipes(ingredients).isEmpty(), "offered " + Recipe.findRecipes(ingredients));
	}

	@Test
	void aScrollAndARunestoneMakeAnUnstableSpellForOneEnergy() {
		ArrayList<Item> ingredients = items(scroll(new ScrollOfIdentify()), new StoneOfAugmentation());
		UnstableSpell.Recipe recipe = new UnstableSpell.Recipe();

		assertTrue(recipe.testIngredients(ingredients));
		assertEquals(1, recipe.cost(ingredients));
		assertTrue(recipe.sampleOutput(ingredients) instanceof UnstableSpell);
		assertTrue(offered(ingredients, UnstableSpell.Recipe.class), "the alchemy pot must offer it");
	}

	@Test
	void aScrollAndASeedNoLongerMakeAnything() {
		ArrayList<Item> ingredients = items(scroll(new ScrollOfIdentify()), new Sungrass.Seed());

		assertFalse(new UnstableSpell.Recipe().testIngredients(ingredients));
		assertTrue(Recipe.findRecipes(ingredients).isEmpty(), "offered " + Recipe.findRecipes(ingredients));
	}

	@Test
	void theUnstableBrewCanBeDrunkOrThrown() {
		UnstableBrew brew = new UnstableBrew();
		ArrayList<String> actions = brew.actions(new Hero());

		assertEquals(Potion.AC_CHOOSE, brew.defaultAction());
		assertTrue(actions.contains(Potion.AC_DRINK));
		assertTrue(actions.contains(Item.AC_THROW));
	}

	private static Map<?, ?> chanceMap(Class<?> owner, String field) throws Exception {
		Field f = owner.getDeclaredField(field);
		f.setAccessible(true);
		return (Map<?, ?>) f.get(null);
	}

	@Test
	void theBrewsEffectTableIsOrderedWithUpstreamsWeights() throws Exception {
		Map<?, ?> chances = chanceMap(UnstableBrew.class, "potionChances");

		assertSame(LinkedHashMap.class, chances.getClass(), "a HashMap rolls differently per JVM run for one seed");
		Map<Class<?>, Float> expected = new LinkedHashMap<>();
		expected.put(PotionOfHealing.class, 3f);
		expected.put(PotionOfMindVision.class, 2f);
		expected.put(PotionOfFrost.class, 2f);
		expected.put(PotionOfLiquidFlame.class, 2f);
		expected.put(PotionOfToxicGas.class, 2f);
		expected.put(PotionOfHaste.class, 2f);
		expected.put(PotionOfInvisibility.class, 2f);
		expected.put(PotionOfLevitation.class, 2f);
		expected.put(PotionOfParalyticGas.class, 2f);
		expected.put(PotionOfPurity.class, 2f);
		expected.put(PotionOfExperience.class, 1f);
		assertEquals(new ArrayList<>(expected.entrySet()), new ArrayList<>(chances.entrySet()));
	}

	@Test
	void theSpellsEffectTableIsOrderedWithUpstreamsWeights() throws Exception {
		Map<?, ?> chances = chanceMap(UnstableSpell.class, "scrollChances");

		assertSame(LinkedHashMap.class, chances.getClass(), "a HashMap rolls differently per JVM run for one seed");
		Map<Class<?>, Float> expected = new LinkedHashMap<>();
		expected.put(ScrollOfIdentify.class, 3f);
		expected.put(ScrollOfRemoveCurse.class, 2f);
		expected.put(ScrollOfMagicMapping.class, 2f);
		expected.put(ScrollOfMirrorImage.class, 2f);
		expected.put(ScrollOfRecharging.class, 2f);
		expected.put(ScrollOfLullaby.class, 2f);
		expected.put(ScrollOfRetribution.class, 2f);
		expected.put(ScrollOfRage.class, 2f);
		expected.put(ScrollOfTeleportation.class, 2f);
		expected.put(ScrollOfTerror.class, 2f);
		expected.put(ScrollOfTransmutation.class, 1f);
		assertEquals(new ArrayList<>(expected.entrySet()), new ArrayList<>(chances.entrySet()));
	}

	@Test
	void theAlchemyGuideHasNinePagesAndAButtonForEach() throws Exception {
		assertEquals(Arrays.asList("Potions", "Stones", "Energy_Food", "Exotic_Potions", "Exotic_Scrolls",
						"Bombs", "Weapons", "Brews_Elixirs", "Spells"),
				new ArrayList<>(Document.ALCHEMY_GUIDE.pageNames()));

		Field buttons = WndJournal.AlchemyTab.class.getDeclaredField("NUM_BUTTONS");
		buttons.setAccessible(true);
		Field sprites = WndJournal.AlchemyTab.class.getDeclaredField("spriteIndexes");
		sprites.setAccessible(true);
		assertEquals(9, buttons.getInt(null));
		assertEquals(9, ((Object[]) sprites.get(null)).length);
	}

	@Test
	void exoticPotionsAndScrollsStillShowTheirExoticArt() {
		StaticSnapshot statics = StaticSnapshot.capture(Arrays.<Class<?>>asList(Potion.class, Scroll.class));
		Random.pushGenerator(2400L);
		try {
			Potion.initColors();
			Scroll.initLabels();
			for (Map.Entry<Class<? extends Potion>, Class<? extends ExoticPotion>> e : ExoticPotion.regToExo.entrySet()) {
				GeneralAsset regular = (GeneralAsset) Reflection.newInstance(e.getKey()).image();
				GeneralAsset exotic = (GeneralAsset) Reflection.newInstance(e.getValue()).image();
				assertEquals(regular.name().replace("POTION_", "EXOTIC_"), exotic.name(), e.getValue().getSimpleName());
			}
			for (Map.Entry<Class<? extends Scroll>, Class<? extends ExoticScroll>> e : ExoticScroll.regToExo.entrySet()) {
				GeneralAsset regular = (GeneralAsset) Reflection.newInstance(e.getKey()).image();
				GeneralAsset exotic = (GeneralAsset) Reflection.newInstance(e.getValue()).image();
				assertEquals(regular.name().replace("SCROLL_", "EXOTIC_"), exotic.name(), e.getValue().getSimpleName());
			}
		} finally {
			Random.popGenerator();
			statics.restore();
		}
	}
}
