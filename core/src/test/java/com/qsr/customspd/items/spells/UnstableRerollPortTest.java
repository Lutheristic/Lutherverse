package com.qsr.customspd.items.spells;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.buffs.Haste;
import com.qsr.customspd.actors.buffs.Invisibility;
import com.qsr.customspd.actors.buffs.Levitation;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Mob;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.items.Item;
import com.qsr.customspd.items.potions.PotionOfFrost;
import com.qsr.customspd.items.potions.PotionOfHaste;
import com.qsr.customspd.items.potions.PotionOfInvisibility;
import com.qsr.customspd.items.potions.PotionOfLevitation;
import com.qsr.customspd.items.potions.PotionOfLiquidFlame;
import com.qsr.customspd.items.potions.PotionOfParalyticGas;
import com.qsr.customspd.items.potions.PotionOfPurity;
import com.qsr.customspd.items.potions.PotionOfToxicGas;
import com.qsr.customspd.items.potions.brews.UnstableBrew;
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
import com.qsr.customspd.sprites.HeroSprite;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.IdleCellSelector;
import com.qsr.customspd.test.StubSprite;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Random;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream bf0b95aa9 (v2.4.0): the unstable brew and spell reroll their random effect once when the
 * first roll does not suit how they are used. A drunk brew rerolls a potion meant for throwing, a
 * thrown brew rerolls a potion that does nothing useful when shattered, and the spell rerolls a
 * non-combat scroll while the hero sees an enemy, or a combat scroll while it sees none.
 *
 * Each case runs seeded inside Random.pushGenerator. The first two draws a seed gives are read
 * from the item's own effect table with Random.chances, the same call the item makes, so the test
 * knows which effect a single roll would have picked and which one a reroll picks, and checks the
 * item used the right one. Only seeds whose expected effect can run without a game scene are used,
 * and for the spell only seeds whose two rolls both can.
 */
class UnstableRerollPortTest {

	private static final Set<Class<?>> MUST_THROW = new HashSet<Class<?>>(Arrays.asList(
			PotionOfToxicGas.class, PotionOfLiquidFlame.class, PotionOfParalyticGas.class, PotionOfFrost.class));
	private static final Set<Class<?>> CAN_THROW = new HashSet<Class<?>>(Arrays.asList(
			PotionOfPurity.class, PotionOfLevitation.class));

	private static final Set<Class<?>> NON_COMBAT = new HashSet<Class<?>>(Arrays.asList(
			ScrollOfIdentify.class, ScrollOfRemoveCurse.class, ScrollOfMagicMapping.class, ScrollOfRecharging.class,
			ScrollOfLullaby.class, ScrollOfTeleportation.class, ScrollOfTransmutation.class));
	private static final Set<Class<?>> COMBAT = new HashSet<Class<?>>(Arrays.asList(
			ScrollOfMirrorImage.class, ScrollOfRecharging.class, ScrollOfLullaby.class, ScrollOfRetribution.class,
			ScrollOfRage.class, ScrollOfTeleportation.class, ScrollOfTerror.class));

	/**
	 * Scrolls whose reading this fixture can run. Identify is the one that is only non-combat: with
	 * no scene its item selector opens nothing, and the idle cell selector lets it cancel the
	 * hero's action headless.
	 */
	private static final Set<Class<?>> READABLE = new HashSet<Class<?>>(Arrays.asList(
			ScrollOfIdentify.class, ScrollOfRecharging.class, ScrollOfLullaby.class, ScrollOfRage.class));

	/** Drinkable potions that leave a buff to look for. */
	private static final Map<Class<?>, Class<? extends Buff>> BUFF_OF = new LinkedHashMap<>();
	static {
		BUFF_OF.put(PotionOfHaste.class, Haste.class);
		BUFF_OF.put(PotionOfInvisibility.class, Invisibility.class);
		BUFF_OF.put(PotionOfLevitation.class, Levitation.class);
	}

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
		staticField("curUser").set(null, null);
		staticField("curItem").set(null, null);
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private static Field staticField(String name) throws Exception {
		Field f = Item.class.getDeclaredField(name);
		f.setAccessible(true);
		return f;
	}

	@SuppressWarnings("unchecked")
	private static HashMap<Class<?>, Float> table(Class<?> owner, String field) throws Exception {
		Field f = owner.getDeclaredField(field);
		f.setAccessible(true);
		return (HashMap<Class<?>, Float>) f.get(null);
	}

	/** The first two effects Random.chances picks from the table under this seed. */
	private static Class<?>[] firstTwoRolls(long seed, HashMap<Class<?>, Float> table) {
		Random.pushGenerator(seed);
		try {
			return new Class<?>[]{Random.chances(table), Random.chances(table)};
		} finally {
			Random.popGenerator();
		}
	}

	private static Hero heroInFreshLevel() throws Exception {
		HeadlessLevel level = HeadlessLevel.install(9, 9);
		Hero hero = new Hero();
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, 1 * level.width() + 1);
		staticField("curUser").set(null, hero);
		return hero;
	}

	/**
	 * Reading a scroll casts the reader's sprite to HeroSprite, whose constructor loads textures.
	 * An instance allocated without its constructor runs the read animation, and the emitters
	 * the read scrolls burst into are detached ones that render nothing.
	 */
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

	private static String rolls(long seed, Class<?>[] two) {
		return "seed " + seed + " rolled " + two[0].getSimpleName() + " then " + two[1].getSimpleName();
	}

	@Test
	void aThrownBrewRerollsOnceUnlessTheFirstPotionIsMeantForThrowing() throws Exception {
		HashMap<Class<?>, Float> table = table(UnstableBrew.class, "potionChances");
		int rerolled = 0, kept = 0;

		for (long seed = 1; seed <= 120; seed++) {
			Class<?>[] two = firstTwoRolls(seed, table);
			boolean throwable = MUST_THROW.contains(two[0]) || CAN_THROW.contains(two[0]);

			heroInFreshLevel();
			Random.pushGenerator(seed);
			try {
				new UnstableBrew().shatter(5 * 9 + 5);
			} finally {
				Random.popGenerator();
			}

			assertEquals(throwable ? two[0] : two[1], staticField("curItem").get(null).getClass(), rolls(seed, two));
			if (throwable) kept++; else rerolled++;
		}

		assertTrue(rerolled > 0 && kept > 0, "the seeds must cover both cases: rerolled " + rerolled + ", kept " + kept);
	}

	@Test
	void aDrunkBrewRerollsOnceOutOfAPotionMeantForThrowing() throws Exception {
		HashMap<Class<?>, Float> table = table(UnstableBrew.class, "potionChances");
		int rerolled = 0, kept = 0;

		for (long seed = 1; seed <= 400; seed++) {
			Class<?>[] two = firstTwoRolls(seed, table);
			boolean reroll = MUST_THROW.contains(two[0]);
			Class<?> expected = reroll ? two[1] : two[0];
			if (!BUFF_OF.containsKey(expected)) continue;

			Hero hero = heroInFreshLevel();
			Random.pushGenerator(seed);
			try {
				new UnstableBrew().apply(hero);
			} finally {
				Random.popGenerator();
			}

			assertNotNull(hero.buff(BUFF_OF.get(expected)), rolls(seed, two));
			if (reroll) rerolled++; else kept++;
		}

		assertTrue(rerolled > 0 && kept > 0, "the seeds must cover both cases: rerolled " + rerolled + ", kept " + kept);
	}

	private int[] castSpells(boolean enemyInView) throws Exception {
		HashMap<Class<?>, Float> table = table(UnstableSpell.class, "scrollChances");
		int rerolled = 0, kept = 0;

		for (long seed = 1; seed <= 400; seed++) {
			Class<?>[] two = firstTwoRolls(seed, table);
			boolean reroll = enemyInView ? NON_COMBAT.contains(two[0]) : COMBAT.contains(two[0]);
			Class<?> expected = reroll ? two[1] : two[0];
			//both rolls readable here, so reading the wrong one fails an assertion rather than the fixture
			if (!READABLE.contains(two[0]) || !READABLE.contains(two[1])) continue;

			Hero hero = heroInFreshLevel();
			giveHeadlessHeroSprite(hero);
			if (enemyInView) {
				Field visible = Hero.class.getDeclaredField("visibleEnemies");
				visible.setAccessible(true);
				List<Mob> enemies = new ArrayList<>();
				enemies.add(new Rat());
				visible.set(hero, enemies);
			}
			assertEquals(enemyInView ? 1 : 0, hero.visibleEnemies());

			Random.pushGenerator(seed);
			try {
				new UnstableSpell().onCast(hero);
			} finally {
				Random.popGenerator();
			}

			assertEquals(expected, staticField("curItem").get(null).getClass(), rolls(seed, two));
			if (reroll) rerolled++; else kept++;
		}
		return new int[]{rerolled, kept};
	}

	@Test
	void withAnEnemyInViewTheSpellRerollsANonCombatScrollOnce() throws Exception {
		int[] counts = castSpells(true);

		assertTrue(counts[0] > 0 && counts[1] > 0,
				"the seeds must cover both cases: rerolled " + counts[0] + ", kept " + counts[1]);
	}

	@Test
	void withNoEnemyInViewTheSpellRerollsACombatScrollOnce() throws Exception {
		int[] counts = castSpells(false);

		assertTrue(counts[0] > 0 && counts[1] > 0,
				"the seeds must cover both cases: rerolled " + counts[0] + ", kept " + counts[1]);
	}
}
