package com.qsr.customspd.items.spells;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.buffs.Barrier;
import com.qsr.customspd.actors.buffs.Invisibility;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.hero.HeroClass;
import com.qsr.customspd.actors.hero.Talent;
import com.qsr.customspd.actors.mobs.Elemental;
import com.qsr.customspd.items.Gold;
import com.qsr.customspd.items.Item;
import com.qsr.customspd.items.potions.Potion;
import com.qsr.customspd.items.potions.PotionOfHealing;
import com.qsr.customspd.items.potions.PotionOfLevitation;
import com.qsr.customspd.items.potions.brews.AquaBrew;
import com.qsr.customspd.items.potions.elixirs.ElixirOfFeatherFall;
import com.qsr.customspd.items.potions.elixirs.ElixirOfMight;
import com.qsr.customspd.items.potions.exotic.PotionOfMastery;
import com.qsr.customspd.items.scrolls.Scroll;
import com.qsr.customspd.items.scrolls.ScrollOfIdentify;
import com.qsr.customspd.items.weapon.melee.Sword;
import com.qsr.customspd.mechanics.Ballistica;
import com.qsr.customspd.scenes.CellSelector;
import com.qsr.customspd.sprites.HeroSprite;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.IdleCellSelector;
import com.qsr.customspd.test.StubSprite;
import com.qsr.customspd.windows.WndBag;
import com.watabou.noosa.Group;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 2b8d344cd (v2.4.0): brews, elixirs and spells trigger the on-potion and on-scroll
 * talents. Potions, scrolls and spells carry a talentChance next to their talentFactor, and the
 * talents only fire when Random.Float() falls under it. Alchemy items made several at a time
 * trigger at 1 / their recipe's output quantity; elixir of might, magical infusion and recycle
 * are made from strength, upgrade and transmutation and count double. Spells call
 * Talent.onScrollUsed themselves: inventory and targeted spells once the spell is used, and
 * alchemize, beacon of returning, summon elemental and unstable spell from their own cast code.
 *
 * The fields are read and written by reflection so the file compiles against code without them.
 */
class TalentChancePortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
		IdleCellSelector.install();
	}

	@AfterAll
	static void restoreCellSelector() {
		IdleCellSelector.uninstall();
	}

	@AfterEach
	void tearDown() throws Exception {
		itemStatic("curUser").set(null, null);
		itemStatic("curItem").set(null, null);
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private static Field itemStatic(String name) throws Exception {
		Field f = Item.class.getDeclaredField(name);
		f.setAccessible(true);
		return f;
	}

	private static Field field(Class<?> owner, String name) throws Exception {
		for (Class<?> c = owner; c != null; c = c.getSuperclass()) {
			try {
				Field f = c.getDeclaredField(name);
				f.setAccessible(true);
				return f;
			} catch (NoSuchFieldException e) {
				// keep looking up the hierarchy
			}
		}
		throw new NoSuchFieldException(owner.getName() + "." + name);
	}

	private static float chanceOf(Item item) throws Exception {
		return field(item.getClass(), "talentChance").getFloat(item);
	}

	private static float factorOf(Item item) throws Exception {
		return field(item.getClass(), "talentFactor").getFloat(item);
	}

	private static <T extends Item> T withChance(T item, float chance) throws Exception {
		field(item.getClass(), "talentChance").setFloat(item, chance);
		return item;
	}

	private static int outQuantity(Class<?> item) throws Exception {
		Field f = Class.forName(item.getName() + "$Recipe").getDeclaredField("OUT_QUANTITY");
		f.setAccessible(true);
		return f.getInt(null);
	}

	private HeadlessLevel level;

	/** A mage with 100 HP at (3,3) whose sprite has a parent, so teleport tweens can attach. */
	private Hero mage() throws Exception {
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

	private static void giveHeadlessHeroSprite(Hero hero) throws Exception {
		Field f = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
		f.setAccessible(true);
		Object unsafe = f.get(null);
		HeroSprite sprite = (HeroSprite) unsafe.getClass()
				.getMethod("allocateInstance", Class.class).invoke(unsafe, HeadlessHeroSprite.class);
		sprite.ch = hero;
		hero.sprite = sprite;
	}

	private static boolean shieldedThenCleared(Hero hero) {
		Barrier barrier = hero.buff(Barrier.class);
		if (barrier == null) return false;
		barrier.detach();
		return true;
	}

	private static boolean invisibleThenCleared(Hero hero) {
		Invisibility invis = hero.buff(Invisibility.class);
		if (invis == null) return false;
		invis.detach();
		return true;
	}

	private static final int USES = 400;

	/** 400 uses at a 25% chance; the seeds are fixed, so the counts are too. */
	private static void assertQuarterOfTheTime(int triggered, String what) {
		assertTrue(triggered >= 70 && triggered <= 130,
				what + " triggered " + triggered + " times in " + USES + " uses at a 25% chance");
	}

	@Test
	void alchemyItemsMadeSeveralAtATimeTriggerAtOneOverTheirOutputQuantity() throws Exception {
		Item[] items = {
				new AquaBrew(), new ElixirOfFeatherFall(), new Alchemize(), new BeaconOfReturning(),
				new CurseInfusion(), new PhaseShift(), new ReclaimTrap(), new Recycle(),
				new SummonElemental(), new TelekineticGrab(), new WildEnergy()
		};
		boolean anyBelowOne = false;
		for (Item item : items) {
			int made = outQuantity(item.getClass());
			assertEquals(1f / made, chanceOf(item), 0.00001f, item.getClass().getSimpleName() + " makes " + made);
			if (made > 1) anyBelowOne = true;
		}
		assertTrue(anyBelowOne, "fixture: some of these are made several at a time");

		assertEquals(1f, chanceOf(new PotionOfHealing()), "control: a potion always triggers");
		assertEquals(1f, chanceOf(new ScrollOfIdentify()), "control: a scroll always triggers");
		assertEquals(1f, chanceOf(new MagicalInfusion()), "control: magical infusion is made one at a time");
		assertEquals(1f, chanceOf(new UnstableSpell()), "control: unstable spell is made one at a time");
	}

	@Test
	void alchemyItemsMadeFromStrengthUpgradeOrTransmutationCountDouble() throws Exception {
		assertEquals(2f, factorOf(new ElixirOfMight()));
		assertEquals(2f, factorOf(new MagicalInfusion()));
		assertEquals(2f, factorOf(new Recycle()));
		assertEquals(1f, factorOf(new AquaBrew()), "control");
		assertEquals(1f, factorOf(new PhaseShift()), "control");
		assertEquals(1f, factorOf(new Alchemize()), "control");
	}

	@Test
	void aDrunkPotionTriggersTheTalentsOnlyAtItsChance() throws Exception {
		Hero hero = mage();
		hero.talents.get(1).put(Talent.LIQUID_WILLPOWER, 2);
		Method drink = Potion.class.getDeclaredMethod("drink", Hero.class);
		drink.setAccessible(true);

		int triggered = 0;
		Random.pushGenerator(2401);
		try {
			for (int i = 0; i < USES; i++) {
				PotionOfLevitation potion = withChance(new PotionOfLevitation(), 0.25f);
				potion.collect(hero.belongings.backpack);
				drink.invoke(potion, hero);
				if (shieldedThenCleared(hero)) triggered++;
			}
		} finally {
			Random.popGenerator();
		}
		assertQuarterOfTheTime(triggered, "drinking");
	}

	@Test
	void aThrownPotionTriggersTheTalentsOnlyAtItsChance() throws Exception {
		Hero hero = mage();
		hero.talents.get(1).put(Talent.LIQUID_WILLPOWER, 2);
		Method onThrow = Potion.class.getDeclaredMethod("onThrow", int.class);
		onThrow.setAccessible(true);
		int target = 6 * level.width() + 6;

		int triggered = 0;
		Random.pushGenerator(2402);
		try {
			for (int i = 0; i < USES; i++) {
				onThrow.invoke(withChance(new PotionOfLevitation(), 0.25f), target);
				if (shieldedThenCleared(hero)) triggered++;
			}
		} finally {
			Random.popGenerator();
		}
		assertQuarterOfTheTime(triggered, "throwing");
	}

	@Test
	void masteryWhichSkipsDrinkAlsoChecksItsChance() throws Exception {
		Hero hero = mage();
		hero.talents.get(1).put(Talent.LIQUID_WILLPOWER, 2);

		PotionOfMastery potion = withChance(new PotionOfMastery(), 0f);
		potion.collect(hero.belongings.backpack);
		itemStatic("curItem").set(null, potion);
		Field selector = PotionOfMastery.class.getDeclaredField("itemSelector");
		selector.setAccessible(true);
		Sword sword = new Sword();
		((WndBag.ItemSelector) selector.get(potion)).onSelect(sword);

		assertTrue(sword.masteryPotionBonus, "fixture: the potion took effect");
		assertNull(hero.buff(Barrier.class), "a 0% chance never triggers");
	}

	@Test
	void aReadScrollTriggersTheTalentsOnlyAtItsChance() throws Exception {
		Hero hero = mage();
		hero.talents.get(1).put(Talent.INSCRIBED_STEALTH, 2);
		giveHeadlessHeroSprite(hero);
		Method readAnimation = Scroll.class.getDeclaredMethod("readAnimation");
		readAnimation.setAccessible(true);

		int triggered = 0;
		Random.pushGenerator(2403);
		try {
			for (int i = 0; i < USES; i++) {
				readAnimation.invoke(withChance(new ScrollOfIdentify(), 0.25f));
				if (invisibleThenCleared(hero)) triggered++;
			}
		} finally {
			Random.popGenerator();
		}
		assertQuarterOfTheTime(triggered, "reading");
	}

	/** An inventory spell with no effect of its own. */
	static class ProbeInventorySpell extends InventorySpell {
		@Override protected void onItemSelected(Item item) {}
	}

	private static void useOnAnItem(Hero hero, InventorySpell spell) throws Exception {
		spell.collect(hero.belongings.backpack);
		itemStatic("curItem").set(null, spell);
		spell.itemSelector.onSelect(new Gold(1));
	}

	@Test
	void usingAnInventorySpellTriggersTheOnScrollTalentsAtItsFactor() throws Exception {
		Hero hero = mage();
		hero.talents.get(1).put(Talent.INSCRIBED_STEALTH, 2);
		ProbeInventorySpell spell = new ProbeInventorySpell();
		field(InventorySpell.class, "talentFactor").setFloat(spell, 2f);

		useOnAnItem(hero, spell);

		Invisibility invis = hero.buff(Invisibility.class);
		assertNotNull(invis, "the spell triggers inscribed stealth");
		assertEquals(10f, invis.cooldown(), 0.0001f, "5 turns at +2, doubled by the factor");
	}

	@Test
	void anInventorySpellTriggersOnlyAtItsChance() throws Exception {
		Hero hero = mage();
		hero.talents.get(1).put(Talent.INSCRIBED_STEALTH, 2);

		int triggered = 0;
		Random.pushGenerator(2404);
		try {
			for (int i = 0; i < USES; i++) {
				useOnAnItem(hero, withChance(new ProbeInventorySpell(), 0.25f));
				if (invisibleThenCleared(hero)) triggered++;
			}
		} finally {
			Random.popGenerator();
		}
		assertQuarterOfTheTime(triggered, "an inventory spell");
	}

	/** A targeted spell whose bolt lands at once and does nothing. */
	static class ProbeTargetedSpell extends TargetedSpell {
		@Override protected void affectTarget(Ballistica bolt, Hero hero) {}
		@Override protected void fx(Ballistica bolt, Callback callback) { callback.call(); }
	}

	@Test
	void castingATargetedSpellTriggersTheOnScrollTalents() throws Exception {
		Hero hero = mage();
		hero.talents.get(1).put(Talent.INSCRIBED_STEALTH, 1);
		ProbeTargetedSpell spell = new ProbeTargetedSpell();
		spell.collect(hero.belongings.backpack);
		itemStatic("curItem").set(null, spell);

		Field targeter = TargetedSpell.class.getDeclaredField("targeter");
		targeter.setAccessible(true);
		((CellSelector.Listener) targeter.get(null)).onSelect(hero.pos + 2);

		Invisibility invis = hero.buff(Invisibility.class);
		assertNotNull(invis, "the spell triggers inscribed stealth");
		assertEquals(3f, invis.cooldown(), 0.0001f, "3 turns at +1");
	}

	@Test
	void castingAnUnstableSpellTriggersTheOnScrollTalentsButNotItsScroll() throws Exception {
		HashMap<?, ?> scrolls = (HashMap<?, ?>) field(UnstableSpell.class, "scrollChances").get(null);
		long seed = 0;
		for (long s = 1; s < 200 && seed == 0; s++) {
			Random.pushGenerator(s);
			try {
				@SuppressWarnings("unchecked")
				Object first = Random.chances((HashMap<Object, Float>) scrolls);
				if (first == ScrollOfIdentify.class) seed = s;
			} finally {
				Random.popGenerator();
			}
		}
		assertTrue(seed != 0, "fixture: a seed whose first roll is identify");

		Hero hero = mage();
		hero.talents.get(1).put(Talent.INSCRIBED_POWER, 1);
		giveHeadlessHeroSprite(hero);
		UnstableSpell spell = new UnstableSpell();
		spell.collect(hero.belongings.backpack);

		Random.pushGenerator(seed);
		try {
			spell.onCast(hero);
		} finally {
			Random.popGenerator();
		}

		assertEquals(ScrollOfIdentify.class, itemStatic("curItem").get(null).getClass(), "fixture: identify was read");
		assertNotNull(hero.buff(com.qsr.customspd.actors.buffs.ScrollEmpower.class),
				"the spell triggers inscribed power once its scroll is read");
	}

	/** An elemental with a sprite that has a parent, so the summon's appear tween can attach. */
	public static class ProbeElemental extends Elemental.AllyNewBornElemental {
		public ProbeElemental() {
			new Group().add(StubSprite.attach(this));
		}
	}

	@Test
	void summoningAnElementalTriggersTheOnScrollTalentsAtItsChance() throws Exception {
		Hero hero = mage();
		hero.talents.get(1).put(Talent.INSCRIBED_STEALTH, 1);
		Field summonClass = SummonElemental.class.getDeclaredField("summonClass");
		summonClass.setAccessible(true);

		SummonElemental always = withChance(new SummonElemental(), 1f);
		summonClass.set(always, ProbeElemental.class);
		always.collect(hero.belongings.backpack);
		always.onCast(hero);
		assertNotNull(hero.buff(Invisibility.class), "the summon triggers inscribed stealth");

		hero.buff(Invisibility.class).detach();
		for (com.qsr.customspd.actors.mobs.Mob m : level.mobs.toArray(new com.qsr.customspd.actors.mobs.Mob[0])) {
			level.mobs.remove(m);
			com.qsr.customspd.actors.Actor.remove(m);
		}
		SummonElemental never = withChance(new SummonElemental(), 0f);
		summonClass.set(never, ProbeElemental.class);
		never.collect(hero.belongings.backpack);
		never.onCast(hero);
		assertNull(hero.buff(Invisibility.class), "a 0% chance never triggers");
	}

	@Test
	void returningByBeaconTriggersTheOnScrollTalents() throws Exception {
		Hero hero = mage();
		hero.talents.get(1).put(Talent.INSCRIBED_STEALTH, 1);
		BeaconOfReturning beacon = withChance(new BeaconOfReturning(), 1f);
		beacon.returnPos = 5 * level.width() + 5;
		beacon.collect(hero.belongings.backpack);

		Method returnBeacon = BeaconOfReturning.class.getDeclaredMethod("returnBeacon", Hero.class);
		returnBeacon.setAccessible(true);
		String levelName = Dungeon.levelName;
		try {
			//a beacon set on the current level teleports within it instead of switching scenes
			Dungeon.levelName = "headless";
			beacon.returnLevel = Dungeon.levelName;
			returnBeacon.invoke(beacon, hero);
		} finally {
			Dungeon.levelName = levelName;
		}

		assertEquals(5 * level.width() + 5, hero.pos, "fixture: the hero returned");
		assertNotNull(hero.buff(Invisibility.class), "the beacon triggers inscribed stealth");
	}
}
