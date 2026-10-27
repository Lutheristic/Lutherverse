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
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Upstream 7aa596a7c (v2.5.0): the unstable brew and spell reroll their random effect until it
 * suits how they are used, where bf0b95aa9 rerolled only once. A drunk brew never uses a potion
 * meant for throwing, a thrown brew only uses one that does something when shattered, and the
 * spell only reads combat scrolls while the hero sees an enemy and only non-combat scrolls while
 * it sees none (recharging, lullaby and teleportation count as both, so they are rerolled either
 * way).
 *
 * Each case runs 500 seeds inside Random.pushGenerator. The draws a seed gives are replayed from
 * the item's own effect table with Random.chances, the same call the item makes, until one suits,
 * so the test knows which effect the item must end on and checks it used that one. Only seeds
 * whose expected effect can run without a game scene are run. Each case also checks that some of
 * its seeds needed three or more rolls, which a single reroll gets wrong.
 */
class UnstableRerollPortTest {

	private static final int SEEDS = 500;

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
	 * Scrolls whose reading this fixture can run. With no scene identify's item selector opens
	 * nothing, and the idle cell selector lets it cancel the hero's action headless.
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

	private interface Suits {
		boolean test(Class<?> effect);
	}

	/** The effects Random.chances picks from the table under this seed, up to the first that suits. */
	private static List<Class<?>> rollsUntil(long seed, HashMap<Class<?>, Float> table, Suits suits) {
		List<Class<?>> rolls = new ArrayList<>();
		Random.pushGenerator(seed);
		try {
			do {
				rolls.add(Random.chances(table));
				assertTrue(rolls.size() < 100, "seed " + seed + " never rolled a suitable effect");
			} while (!suits.test(rolls.get(rolls.size() - 1)));
		} finally {
			Random.popGenerator();
		}
		return rolls;
	}

	private static Class<?> last(List<Class<?>> rolls) {
		return rolls.get(rolls.size() - 1);
	}

	private static String describe(long seed, List<Class<?>> rolls) {
		StringBuilder b = new StringBuilder("seed " + seed + " rolled");
		for (Class<?> c : rolls) b.append(' ').append(c.getSimpleName());
		return b.toString();
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

	/** Counts of seeds run that settled on the first, second, and third or later roll. */
	private static void assertCoverage(int[] byRolls) {
		assertTrue(byRolls[0] > 0 && byRolls[1] > 0 && byRolls[2] > 0,
				"the seeds must settle on the first, second and a later roll: "
						+ byRolls[0] + ", " + byRolls[1] + ", " + byRolls[2]);
	}

	private static void count(int[] byRolls, List<Class<?>> rolls) {
		byRolls[Math.min(rolls.size(), 3) - 1]++;
	}

	@Test
	void aThrownBrewRerollsUntilThePotionIsMeantForThrowing() throws Exception {
		HashMap<Class<?>, Float> table = table(UnstableBrew.class, "potionChances");
		int[] byRolls = new int[3];

		for (long seed = 1; seed <= SEEDS; seed++) {
			List<Class<?>> rolls = rollsUntil(seed, table, new Suits() {
				@Override public boolean test(Class<?> p) { return MUST_THROW.contains(p) || CAN_THROW.contains(p); }
			});

			heroInFreshLevel();
			Random.pushGenerator(seed);
			try {
				new UnstableBrew().shatter(5 * 9 + 5);
			} finally {
				Random.popGenerator();
			}

			assertEquals(last(rolls), staticField("curItem").get(null).getClass(), describe(seed, rolls));
			count(byRolls, rolls);
		}

		assertCoverage(byRolls);
	}

	@Test
	void aDrunkBrewRerollsUntilThePotionIsNotMeantForThrowing() throws Exception {
		HashMap<Class<?>, Float> table = table(UnstableBrew.class, "potionChances");
		int[] byRolls = new int[3];

		for (long seed = 1; seed <= SEEDS; seed++) {
			List<Class<?>> rolls = rollsUntil(seed, table, new Suits() {
				@Override public boolean test(Class<?> p) { return !MUST_THROW.contains(p); }
			});
			if (!BUFF_OF.containsKey(last(rolls))) continue;

			Hero hero = heroInFreshLevel();
			Random.pushGenerator(seed);
			try {
				new UnstableBrew().apply(hero);
			} finally {
				Random.popGenerator();
			}

			assertNotNull(hero.buff(BUFF_OF.get(last(rolls))), describe(seed, rolls));
			count(byRolls, rolls);
		}

		assertCoverage(byRolls);
	}

	private int[] castSpells(final boolean enemyInView) throws Exception {
		HashMap<Class<?>, Float> table = table(UnstableSpell.class, "scrollChances");
		int[] byRolls = new int[3];

		for (long seed = 1; seed <= SEEDS; seed++) {
			List<Class<?>> rolls = rollsUntil(seed, table, new Suits() {
				@Override public boolean test(Class<?> s) {
					return enemyInView ? !NON_COMBAT.contains(s) : !COMBAT.contains(s);
				}
			});
			if (!READABLE.contains(last(rolls))) continue;

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
			} catch (RuntimeException e) {
				//only the expected scroll is known to read headless, so this means another one was read
				fail(describe(seed, rolls) + " but the spell read a scroll other than "
						+ last(rolls).getSimpleName() + ": " + e);
			} finally {
				Random.popGenerator();
			}

			assertEquals(last(rolls), staticField("curItem").get(null).getClass(), describe(seed, rolls));
			count(byRolls, rolls);
		}
		return byRolls;
	}

	@Test
	void withAnEnemyInViewTheSpellRerollsUntilItHasACombatOnlyScroll() throws Exception {
		assertCoverage(castSpells(true));
	}

	@Test
	void withNoEnemyInViewTheSpellRerollsUntilItHasANonCombatOnlyScroll() throws Exception {
		assertCoverage(castSpells(false));
	}
}
