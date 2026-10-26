package com.qsr.customspd.items;

import com.qsr.customspd.items.potions.Potion;
import com.qsr.customspd.items.potions.PotionOfFrost;
import com.qsr.customspd.items.potions.PotionOfHealing;
import com.qsr.customspd.items.potions.PotionOfLevitation;
import com.qsr.customspd.items.potions.PotionOfLiquidFlame;
import com.qsr.customspd.items.potions.PotionOfParalyticGas;
import com.qsr.customspd.items.potions.PotionOfStrength;
import com.qsr.customspd.items.potions.PotionOfToxicGas;
import com.qsr.customspd.items.potions.brews.AquaBrew;
import com.qsr.customspd.items.potions.brews.BlizzardBrew;
import com.qsr.customspd.items.potions.brews.Brew;
import com.qsr.customspd.items.potions.brews.CausticBrew;
import com.qsr.customspd.items.potions.brews.InfernalBrew;
import com.qsr.customspd.items.potions.brews.ShockingBrew;
import com.qsr.customspd.items.potions.brews.UnstableBrew;
import com.qsr.customspd.items.potions.elixirs.Elixir;
import com.qsr.customspd.items.potions.elixirs.ElixirOfAquaticRejuvenation;
import com.qsr.customspd.items.potions.elixirs.ElixirOfArcaneArmor;
import com.qsr.customspd.items.potions.elixirs.ElixirOfDragonsBlood;
import com.qsr.customspd.items.potions.elixirs.ElixirOfFeatherFall;
import com.qsr.customspd.items.potions.elixirs.ElixirOfHoneyedHealing;
import com.qsr.customspd.items.potions.elixirs.ElixirOfIcyTouch;
import com.qsr.customspd.items.potions.elixirs.ElixirOfMight;
import com.qsr.customspd.items.potions.elixirs.ElixirOfToxicEssence;
import com.qsr.customspd.items.potions.exotic.PotionOfCorrosiveGas;
import com.qsr.customspd.items.potions.exotic.PotionOfDragonsBreath;
import com.qsr.customspd.items.potions.exotic.PotionOfEarthenArmor;
import com.qsr.customspd.items.potions.exotic.PotionOfSnapFreeze;
import com.qsr.customspd.items.potions.exotic.PotionOfStormClouds;
import com.qsr.customspd.items.quest.Embers;
import com.qsr.customspd.items.quest.GooBlob;
import com.qsr.customspd.items.quest.MetalShard;
import com.qsr.customspd.items.scrolls.Scroll;
import com.qsr.customspd.items.scrolls.ScrollOfIdentify;
import com.qsr.customspd.items.scrolls.ScrollOfMagicMapping;
import com.qsr.customspd.items.scrolls.ScrollOfRecharging;
import com.qsr.customspd.items.scrolls.ScrollOfRemoveCurse;
import com.qsr.customspd.items.scrolls.ScrollOfTeleportation;
import com.qsr.customspd.items.scrolls.ScrollOfTransmutation;
import com.qsr.customspd.items.scrolls.ScrollOfUpgrade;
import com.qsr.customspd.items.scrolls.exotic.ScrollOfPassage;
import com.qsr.customspd.items.spells.Alchemize;
import com.qsr.customspd.items.spells.BeaconOfReturning;
import com.qsr.customspd.items.spells.CurseInfusion;
import com.qsr.customspd.items.spells.MagicalInfusion;
import com.qsr.customspd.items.spells.PhaseShift;
import com.qsr.customspd.items.spells.ReclaimTrap;
import com.qsr.customspd.items.spells.Recycle;
import com.qsr.customspd.items.spells.Spell;
import com.qsr.customspd.items.spells.SummonElemental;
import com.qsr.customspd.items.spells.TelekineticGrab;
import com.qsr.customspd.items.spells.UnstableSpell;
import com.qsr.customspd.items.spells.WildEnergy;
import com.qsr.customspd.items.stones.StoneOfAugmentation;
import com.qsr.customspd.plants.Sungrass;
import com.qsr.customspd.test.HeadlessGdx;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream a83be1d28 and 938a78883 (v2.4.0) rebalanced what every brew, elixir and spell costs and
 * how many it makes; elixir of toxic essence now takes a potion of corrosive gas and wild energy a
 * scroll of recharging. This pins the whole table as v2.5.4 has it (feather fall per c63539188),
 * ingredients included, so any later drift in a single number shows up here. Every registered
 * recipe that makes a brew, elixir or spell must have a row, so a new one cannot slip past.
 */
class AlchemyCostTablePortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	/** One v2.5.4 recipe: its inputs in declaration order, energy cost, output and output quantity. */
	private static final class Row {
		final List<Object> inputs;
		final int cost;
		final Class<? extends Item> output;
		final int outQuantity;

		Row(int cost, Class<? extends Item> output, int outQuantity, Object... inputs) {
			this.inputs = Arrays.asList(inputs);
			this.cost = cost;
			this.output = output;
			this.outQuantity = outQuantity;
		}
	}

	private static final Map<Class<? extends Recipe.SimpleRecipe>, Row> TABLE = new LinkedHashMap<>();
	static {
		TABLE.put(AquaBrew.Recipe.class, new Row(8, AquaBrew.class, 8, PotionOfStormClouds.class, 1));
		TABLE.put(BlizzardBrew.Recipe.class, new Row(8, BlizzardBrew.class, 1, PotionOfFrost.class, 1));
		TABLE.put(CausticBrew.Recipe.class, new Row(1, CausticBrew.class, 1, PotionOfToxicGas.class, 1, GooBlob.class, 1));
		TABLE.put(InfernalBrew.Recipe.class, new Row(12, InfernalBrew.class, 1, PotionOfLiquidFlame.class, 1));
		TABLE.put(ShockingBrew.Recipe.class, new Row(10, ShockingBrew.class, 1, PotionOfParalyticGas.class, 1));

		TABLE.put(ElixirOfAquaticRejuvenation.Recipe.class, new Row(6, ElixirOfAquaticRejuvenation.class, 1, PotionOfHealing.class, 1, GooBlob.class, 1));
		TABLE.put(ElixirOfArcaneArmor.Recipe.class, new Row(8, ElixirOfArcaneArmor.class, 1, PotionOfEarthenArmor.class, 1, GooBlob.class, 1));
		TABLE.put(ElixirOfDragonsBlood.Recipe.class, new Row(10, ElixirOfDragonsBlood.class, 1, PotionOfDragonsBreath.class, 1));
		TABLE.put(ElixirOfFeatherFall.Recipe.class, new Row(10, ElixirOfFeatherFall.class, 1, PotionOfLevitation.class, 1));
		TABLE.put(ElixirOfHoneyedHealing.Recipe.class, new Row(4, ElixirOfHoneyedHealing.class, 1, PotionOfHealing.class, 1, Honeypot.ShatteredPot.class, 1));
		TABLE.put(ElixirOfIcyTouch.Recipe.class, new Row(6, ElixirOfIcyTouch.class, 1, PotionOfSnapFreeze.class, 1));
		TABLE.put(ElixirOfMight.Recipe.class, new Row(16, ElixirOfMight.class, 1, PotionOfStrength.class, 1));
		TABLE.put(ElixirOfToxicEssence.Recipe.class, new Row(8, ElixirOfToxicEssence.class, 1, PotionOfCorrosiveGas.class, 1));

		TABLE.put(BeaconOfReturning.Recipe.class, new Row(12, BeaconOfReturning.class, 5, ScrollOfPassage.class, 1));
		TABLE.put(CurseInfusion.Recipe.class, new Row(6, CurseInfusion.class, 4, ScrollOfRemoveCurse.class, 1, MetalShard.class, 1));
		TABLE.put(MagicalInfusion.Recipe.class, new Row(12, MagicalInfusion.class, 1, ScrollOfUpgrade.class, 1));
		TABLE.put(PhaseShift.Recipe.class, new Row(10, PhaseShift.class, 6, ScrollOfTeleportation.class, 1));
		TABLE.put(ReclaimTrap.Recipe.class, new Row(8, ReclaimTrap.class, 5, ScrollOfMagicMapping.class, 1, MetalShard.class, 1));
		TABLE.put(Recycle.Recipe.class, new Row(12, Recycle.class, 12, ScrollOfTransmutation.class, 1));
		TABLE.put(SummonElemental.Recipe.class, new Row(10, SummonElemental.class, 6, Embers.class, 1));
		TABLE.put(TelekineticGrab.Recipe.class, new Row(10, TelekineticGrab.class, 8, LiquidMetal.class, 10));
		TABLE.put(WildEnergy.Recipe.class, new Row(4, WildEnergy.class, 5, ScrollOfRecharging.class, 1, MetalShard.class, 1));
	}

	private static List<Recipe> registeredRecipes() throws Exception {
		List<Recipe> all = new ArrayList<>();
		for (String fieldName : new String[]{
				"variableRecipes", "oneIngredientRecipes", "twoIngredientRecipes", "threeIngredientRecipes"}) {
			Field f = Recipe.class.getDeclaredField(fieldName);
			f.setAccessible(true);
			all.addAll(Arrays.asList((Recipe[]) f.get(null)));
		}
		return all;
	}

	@Test
	void everyBrewElixirAndSpellRecipeMatchesV254() throws Exception {
		List<String> wrong = new ArrayList<>();
		int checked = 0;

		for (Recipe recipe : registeredRecipes()) {
			if (!(recipe instanceof Recipe.SimpleRecipe)) continue;
			Recipe.SimpleRecipe simple = (Recipe.SimpleRecipe) recipe;
			ArrayList<Item> ingredients = simple.getIngredients();
			Item out = simple.sampleOutput(ingredients);
			if (!(out instanceof Brew || out instanceof Elixir || out instanceof Spell)) continue;

			Row row = TABLE.get(simple.getClass());
			if (row == null) {
				wrong.add(simple.getClass().getName() + ": no v2.5.4 row");
				continue;
			}
			checked++;

			List<Object> actualInputs = new ArrayList<>();
			for (Item i : ingredients) {
				actualInputs.add(i.getClass());
				actualInputs.add(i.quantity());
			}
			String name = simple.getClass().getName();
			if (!row.inputs.equals(actualInputs)) wrong.add(name + ": inputs " + actualInputs + ", expected " + row.inputs);
			if (simple.cost(ingredients) != row.cost) wrong.add(name + ": cost " + simple.cost(ingredients) + ", expected " + row.cost);
			if (out.getClass() != row.output) wrong.add(name + ": makes " + out.getClass().getSimpleName());
			if (out.quantity() != row.outQuantity) wrong.add(name + ": makes " + out.quantity() + ", expected " + row.outQuantity);
		}

		assertTrue(wrong.isEmpty(), "recipes differ from v2.5.4:\n" + String.join("\n", wrong));
		assertEquals(TABLE.size(), checked, "every row must belong to a registered recipe");
	}

	private static ArrayList<Item> items(Item... items) {
		return new ArrayList<>(Arrays.asList(items));
	}

	@Test
	void theHandWrittenRecipesCostWhatV254Charges() {
		Potion potion = new PotionOfHealing();
		potion.anonymize();
		Scroll scroll = new ScrollOfIdentify();
		scroll.anonymize();

		ArrayList<Item> brew = items(potion, new Sungrass.Seed());
		ArrayList<Item> spell = items(scroll, new StoneOfAugmentation());
		ArrayList<Item> alchemize = items(new Sungrass.Seed(), new StoneOfAugmentation());

		assertEquals(1, new UnstableBrew.Recipe().cost(brew));
		assertEquals(1, new UnstableBrew.Recipe().sampleOutput(brew).quantity());
		assertEquals(1, new UnstableSpell.Recipe().cost(spell));
		assertEquals(1, new UnstableSpell.Recipe().sampleOutput(spell).quantity());
		assertEquals(2, new Alchemize.Recipe().cost(alchemize));
		assertEquals(8, new Alchemize.Recipe().sampleOutput(alchemize).quantity());
	}

	@Test
	void aWholeBrewOfTheResizedSpellsIsWorthOneBrew() {
		for (Item stack : new Item[]{new PhaseShift().quantity(6), new ReclaimTrap().quantity(5),
				new TelekineticGrab().quantity(8)}) {
			assertEquals(60, stack.value(), stack.getClass().getSimpleName() + " gold");
			assertEquals(12, stack.energyVal(), stack.getClass().getSimpleName() + " energy");
		}
	}

	/**
	 * Upstream 5cc36a8f0 moved each recipe's output count into a private OUT_QUANTITY constant that
	 * value() and energyVal() divide by. One row per item that commit touched: the gold and energy a
	 * whole brew is worth and how many one brew makes, so the prices cannot move with the refactor.
	 */
	private static final class Pricing {
		final Class<?> recipe;
		final Item item;
		final int gold;
		final int energy;
		final int perBrew;

		Pricing(Class<?> recipe, Item item, int gold, int energy, int perBrew) {
			this.recipe = recipe;
			this.item = item;
			this.gold = gold;
			this.energy = energy;
			this.perBrew = perBrew;
		}
	}

	private static List<Pricing> pricing() {
		return Arrays.asList(
				new Pricing(AquaBrew.Recipe.class, new AquaBrew(), 60, 12, 8),
				new Pricing(ElixirOfFeatherFall.Recipe.class, new ElixirOfFeatherFall(), 60, 12, 1),
				new Pricing(Alchemize.Recipe.class, new Alchemize(), 20, 4, 8),
				new Pricing(BeaconOfReturning.Recipe.class, new BeaconOfReturning(), 60, 12, 5),
				new Pricing(CurseInfusion.Recipe.class, new CurseInfusion(), 60, 12, 4),
				new Pricing(PhaseShift.Recipe.class, new PhaseShift(), 60, 12, 6),
				new Pricing(ReclaimTrap.Recipe.class, new ReclaimTrap(), 60, 12, 5),
				new Pricing(Recycle.Recipe.class, new Recycle(), 60, 12, 12),
				// summon elemental has no price of its own
				new Pricing(SummonElemental.Recipe.class, new SummonElemental(), 0, 0, 6),
				new Pricing(TelekineticGrab.Recipe.class, new TelekineticGrab(), 60, 12, 8),
				new Pricing(WildEnergy.Recipe.class, new WildEnergy(), 60, 12, 5));
	}

	@Test
	void eachRecipeKeepsItsOutputCountInOneConstant() throws Exception {
		for (Pricing p : pricing()) {
			String name = p.recipe.getName();
			Field f = p.recipe.getDeclaredField("OUT_QUANTITY");
			int mods = f.getModifiers();
			assertTrue(Modifier.isPrivate(mods) && Modifier.isStatic(mods) && Modifier.isFinal(mods),
					name + ".OUT_QUANTITY must be private static final");
			assertEquals(int.class, f.getType(), name + ".OUT_QUANTITY type");
			f.setAccessible(true);
			assertEquals(p.perBrew, f.getInt(null), name + ".OUT_QUANTITY");

			Recipe recipe = (Recipe) p.recipe.getDeclaredConstructor().newInstance();
			ArrayList<Item> ingredients = recipe instanceof Recipe.SimpleRecipe
					? ((Recipe.SimpleRecipe) recipe).getIngredients()
					: items(new Sungrass.Seed(), new StoneOfAugmentation());
			assertEquals(p.perBrew, recipe.sampleOutput(ingredients).quantity(), name + " makes");
		}
	}

	@Test
	void goldAndEnergyValuesAreUnchangedAtEveryStackSize() {
		for (Pricing p : pricing()) {
			String name = p.item.getClass().getSimpleName();
			for (int q = 1; q <= 2 * p.perBrew + 1; q++) {
				p.item.quantity(q);
				assertEquals((int) (p.gold * (q / (float) p.perBrew)), p.item.value(), name + " x" + q + " gold");
				assertEquals((int) (p.energy * (q / (float) p.perBrew)), p.item.energyVal(), name + " x" + q + " energy");
			}
		}
	}
}
