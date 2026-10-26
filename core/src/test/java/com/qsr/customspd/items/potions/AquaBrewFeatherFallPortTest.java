package com.qsr.customspd.items.potions;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.ShatteredPixelDungeon;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.items.Item;
import com.qsr.customspd.items.Recipe;
import com.qsr.customspd.items.bags.ScrollHolder;
import com.qsr.customspd.items.potions.brews.AquaBrew;
import com.qsr.customspd.items.potions.elixirs.ElixirOfFeatherFall;
import com.qsr.customspd.items.potions.exotic.PotionOfStormClouds;
import com.qsr.customspd.levels.Terrain;
import com.qsr.customspd.levels.features.Chasm;
import com.qsr.customspd.messages.Languages;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.SaveRoundtrip;
import com.qsr.customspd.test.StubSprite;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream e67d25967 (v2.4.0): aqua blast and feather fall stop being spells. Aqua blast becomes
 * aqua brew, thrown like the other brews, and feather fall becomes an elixir the hero drinks,
 * whose protection lasts 50 turns instead of 30. Saves carry the old class names, so Bundle
 * aliases map them onto the new classes, and a bag that held one under its old category keeps it
 * on load even though it could no longer be put there.
 *
 * Upstream c63539188 (v2.5.0): one elixir per brew, for 10 energy, and landing in a chasm costs
 * the protection 10 turns instead of ending it.
 */
class AquaBrewFeatherFallPortTest {

	private static final String OLD_AQUA = "com.qsr.customspd.items.spells.AquaBlast";
	private static final String OLD_FEATHER = "com.qsr.customspd.items.spells.FeatherFall";
	private static final String OLD_FEATHER_BUFF = "com.qsr.customspd.items.spells.FeatherFall$FeatherBuff";

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
		ShatteredPixelDungeon.registerBundleAliases();
		Messages.setup(Languages.ENGLISH);
	}

	@AfterEach
	void tearDown() throws Exception {
		curUser().set(null, null);
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private static Field curUser() throws Exception {
		Field f = Item.class.getDeclaredField("curUser");
		f.setAccessible(true);
		return f;
	}

	private static Hero heroOn(HeadlessLevel level, int cell) throws Exception {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, cell);
		curUser().set(null, hero);
		return hero;
	}

	/** Bundles the object the way a save does, then reads it back as a save written before the rename. */
	private static Bundlable restoreUnderOldName(Bundlable obj, String newClassName, String oldClassName) throws Exception {
		Bundle saved = new Bundle();
		saved.put("obj", obj);
		String json = saved.toString().replace(newClassName, oldClassName);
		assertTrue(json.contains(oldClassName), "the fixture must really carry the old class name");
		Bundle read = Bundle.read(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
		return SaveRoundtrip.writeRead(read).get("obj");
	}

	private static ArrayList<Item> identified(ArrayList<Item> ingredients) {
		for (Item i : ingredients) {
			if (i instanceof Potion) ((Potion) i).anonymize();
		}
		return ingredients;
	}

	@Test
	void theOldClassNamesNoLongerResolveByThemselves() {
		assertNull(Reflection.forName(OLD_AQUA));
		assertNull(Reflection.forName(OLD_FEATHER));
		assertNull(Reflection.forName(OLD_FEATHER_BUFF));
	}

	@Test
	void anOldAquaBlastStackLoadsAsAquaBrew() throws Exception {
		Bundlable restored = restoreUnderOldName(new AquaBrew().quantity(5), AquaBrew.class.getName(), OLD_AQUA);

		assertTrue(restored instanceof AquaBrew, "restored as " + restored);
		assertEquals(5, ((AquaBrew) restored).quantity());
	}

	@Test
	void anOldFeatherFallStackLoadsAsTheElixir() throws Exception {
		Bundlable restored = restoreUnderOldName(new ElixirOfFeatherFall().quantity(2),
				ElixirOfFeatherFall.class.getName(), OLD_FEATHER);

		assertTrue(restored instanceof ElixirOfFeatherFall, "restored as " + restored);
		assertEquals(2, ((ElixirOfFeatherFall) restored).quantity());
	}

	@Test
	void anOldFeatherFallBuffLoadsAsTheElixirsBuff() throws Exception {
		Bundlable restored = restoreUnderOldName(new ElixirOfFeatherFall.FeatherBuff(),
				ElixirOfFeatherFall.FeatherBuff.class.getName(), OLD_FEATHER_BUFF);

		assertTrue(restored instanceof ElixirOfFeatherFall.FeatherBuff, "restored as " + restored);
	}

	@Test
	void aScrollHolderFromAnOldSaveKeepsTheAquaBlastItHeld() throws Exception {
		assertFalse(new ScrollHolder().canHold(new AquaBrew()),
				"a brew no longer fits in the scroll holder, so only the load path can keep it there");

		ScrollHolder holder = new ScrollHolder();
		holder.items.add(new AquaBrew().quantity(3));

		Bundlable restored = restoreUnderOldName(holder, AquaBrew.class.getName(), OLD_AQUA);

		ScrollHolder loaded = (ScrollHolder) restored;
		assertEquals(1, loaded.items.size(), "the aqua blast the holder held must survive the load");
		assertTrue(loaded.items.get(0) instanceof AquaBrew, "kept as " + loaded.items.get(0));
		assertEquals(3, loaded.items.get(0).quantity());
	}

	@Test
	void stormCloudsBrewsEightAquaBrewsForEightEnergy() {
		AquaBrew.Recipe recipe = new AquaBrew.Recipe();
		ArrayList<Item> ingredients = identified(recipe.getIngredients());

		assertTrue(ingredients.get(0) instanceof PotionOfStormClouds);
		assertTrue(recipe.testIngredients(ingredients));
		assertEquals(8, recipe.cost(ingredients), "a83be1d28 cut the cost from 11");
		Item out = recipe.sampleOutput(ingredients);
		assertTrue(out instanceof AquaBrew, "brews " + out);
		assertEquals(8, out.quantity());
		assertTrue(registered(ingredients, AquaBrew.Recipe.class), "the alchemy pot must offer the recipe");
	}

	@Test
	void levitationBrewsOneFeatherFallElixirForTenEnergy() {
		ElixirOfFeatherFall.Recipe recipe = new ElixirOfFeatherFall.Recipe();
		ArrayList<Item> ingredients = identified(recipe.getIngredients());

		assertTrue(ingredients.get(0) instanceof PotionOfLevitation);
		assertTrue(recipe.testIngredients(ingredients));
		assertEquals(10, recipe.cost(ingredients));
		Item out = recipe.sampleOutput(ingredients);
		assertTrue(out instanceof ElixirOfFeatherFall, "brews " + out);
		assertEquals(1, out.quantity());
		assertEquals(60, out.value(), "one elixir is worth the whole brew");
		assertTrue(registered(ingredients, ElixirOfFeatherFall.Recipe.class), "the alchemy pot must offer the recipe");
	}

	private static boolean registered(ArrayList<Item> ingredients, Class<?> recipeClass) {
		for (Recipe r : Recipe.findRecipes(ingredients)) {
			if (r.getClass() == recipeClass) return true;
		}
		return false;
	}

	@Test
	void aquaBrewIsThrownNotDrunk() throws Exception {
		HeadlessLevel level = HeadlessLevel.install(8, 8);
		Hero hero = heroOn(level, 3 * level.width() + 3);
		AquaBrew brew = new AquaBrew();

		assertEquals(Item.AC_THROW, brew.defaultAction());
		assertFalse(brew.actions(hero).contains(Potion.AC_DRINK));
	}

	@Test
	void aquaBrewFloodsTheCellsAroundWhereItShatters() throws Exception {
		HeadlessLevel level = HeadlessLevel.install(9, 9);
		heroOn(level, 1 * level.width() + 1);
		int cell = 5 * level.width() + 5;

		Random.pushGenerator(4024L);
		try {
			new AquaBrew().shatter(cell);
		} finally {
			Random.popGenerator();
		}

		assertEquals(Terrain.WATER, level.map[cell]);
		for (int i : PathFinder.NEIGHBOURS8) {
			assertEquals(Terrain.WATER, level.map[cell + i], "neighbour " + (cell + i) + " of the burst");
		}
	}

	@Test
	void drinkingFeatherFallProtectsForFiftyTurns() throws Exception {
		HeadlessLevel level = HeadlessLevel.install(8, 8);
		Hero hero = heroOn(level, 3 * level.width() + 3);

		new ElixirOfFeatherFall().apply(hero);

		ElixirOfFeatherFall.FeatherBuff buff = hero.buff(ElixirOfFeatherFall.FeatherBuff.class);
		assertNotNull(buff, "drinking the elixir grants feather fall");
		assertEquals(50f, buff.cooldown(), 0.001f);
	}

	@Test
	void landingInAChasmCostsTenTurnsOfFeatherFall() throws Exception {
		HeadlessLevel level = HeadlessLevel.install(8, 8);
		Hero hero = heroOn(level, 3 * level.width() + 3);
		new ElixirOfFeatherFall().apply(hero);

		Chasm.heroLand();

		ElixirOfFeatherFall.FeatherBuff buff = hero.buff(ElixirOfFeatherFall.FeatherBuff.class);
		assertNotNull(buff, "one fall no longer uses up the elixir");
		assertEquals(40f, buff.cooldown(), 0.001f);
	}

	@Test
	void landingWithLessThanTenTurnsLeftEndsFeatherFall() throws Exception {
		HeadlessLevel level = HeadlessLevel.install(8, 8);
		Hero hero = heroOn(level, 3 * level.width() + 3);
		Buff.append(hero, ElixirOfFeatherFall.FeatherBuff.class, 8f);

		Chasm.heroLand();

		assertNull(hero.buff(ElixirOfFeatherFall.FeatherBuff.class));
	}
}
