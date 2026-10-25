package com.qsr.customspd.items;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Char;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Statue;
import com.qsr.customspd.items.scrolls.ScrollOfTransmutation;
import com.qsr.customspd.items.wands.CursedWand;
import com.qsr.customspd.items.wands.Wand;
import com.qsr.customspd.items.wands.WandOfMagicMissile;
import com.qsr.customspd.items.weapon.Weapon;
import com.qsr.customspd.levels.traps.GuardianTrap;
import com.qsr.customspd.test.HeadlessRun;
import com.watabou.utils.Random;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream cf7ddd4db (v2.2.0): drops made outside levelgen no longer disturb the item decks.
 * A statue takes one weapon from the deck and uncurses it instead of drawing again until it gets
 * an uncursed one; a guardian, a transmuted wand and a cursed wand's transmogrify roll from the
 * default probabilities and leave the decks alone.
 */
class NonLevelgenDeckDrawsPortTest {

	private static final long RUN_SEED = 4242L;

	@Test
	void aStatueTakesOneWeaponFromTheDeckAndUncursesIt() {
		try (HeadlessRun run = HeadlessRun.start(RUN_SEED)) {
			for (long seed = 1; seed <= 20; seed++) {
				float before;
				Statue statue;
				Random.pushGenerator(seed);
				try {
					Generator.fullReset();
					before = weaponCards();
					statue = new Statue();
				} finally {
					Random.popGenerator();
				}
				assertEquals(1f, before - weaponCards(), "seed " + seed + ": one card from the weapon decks, no re-roll");
				assertFalse(weaponOf(statue).cursed, "seed " + seed + ": a statue's weapon is never cursed");
			}
		}
	}

	@Test
	void aGuardiansWeaponComesFromTheDefaultsNotTheDecks() {
		try (HeadlessRun run = HeadlessRun.start(RUN_SEED)) {
			for (long seed = 1; seed <= 20; seed++) {
				String before = decks();
				GuardianTrap.Guardian guardian;
				Random.pushGenerator(seed);
				try {
					guardian = new GuardianTrap.Guardian();
				} finally {
					Random.popGenerator();
				}
				assertEquals(before, decks(), "seed " + seed + ": a guardian must not draw from the decks");
				Weapon w = weaponOf(guardian);
				assertFalse(w.cursed);
				assertNull(w.enchantment);
				assertEquals(0, w.level());
			}
		}
	}

	@Test
	void transmutingAWandLeavesTheDecksAlone() {
		try (HeadlessRun run = HeadlessRun.start(RUN_SEED)) {
			for (long seed = 1; seed <= 20; seed++) {
				String before = decks();
				Item result;
				Random.pushGenerator(seed);
				try {
					result = ScrollOfTransmutation.changeItem(new WandOfMagicMissile());
				} finally {
					Random.popGenerator();
				}
				assertTrue(result instanceof Wand, "a wand transmutes into a wand");
				assertNotSame(WandOfMagicMissile.class, result.getClass(), "into a different wand");
				assertEquals(before, decks(), "seed " + seed + ": transmuting must not draw from the decks");
			}
		}
	}

	@Test
	void aCursedWandsTransmogrifyLeavesTheDecksAlone() throws Exception {
		try (HeadlessRun run = HeadlessRun.start(RUN_SEED)) {
			run.enterFirstLevel();
			Hero hero = Dungeon.hero;
			Method veryRare = CursedWand.class.getDeclaredMethod("veryRareEffect", Item.class, Char.class, int.class);
			veryRare.setAccessible(true);
			int checked = 0;
			for (long seed = 1; checked < 5 && seed <= 200; seed++) {
				// Only seeds whose effect roll lands on transmogrify (case 3).
				Random.pushGenerator(seed);
				int effect = Random.Int(4);
				Random.popGenerator();
				if (effect != 3) continue;

				Wand wand = new WandOfMagicMissile();
				hero.belongings.backpack.items.add(wand);
				// Artifacts always come from their deck so each stays unique; leave them out.
				String before = decks(Generator.Category.ARTIFACT);
				Random.pushGenerator(seed);
				try {
					veryRare.invoke(null, wand, hero, hero.pos);
				} catch (InvocationTargetException e) {
					rethrowUnlessHeadlessDrop(e.getCause());
				} finally {
					Random.popGenerator();
				}
				assertFalse(hero.belongings.contains(wand), "precondition: seed " + seed + " transmogrified the wand");
				assertEquals(before, decks(Generator.Category.ARTIFACT), "seed " + seed + ": transmogrify must not draw from the decks");
				checked++;
			}
			assertEquals(5, checked, "seeds that reach transmogrify");
		}
	}

	// ------------------------------------------------------------------------------ helpers

	/** Every deck's cards, seed, draw count and active half. */
	private static String decks(Generator.Category... skip) {
		List<Generator.Category> skipped = Arrays.asList(skip);
		StringBuilder sb = new StringBuilder();
		for (Generator.Category cat : Generator.Category.values()) {
			if (skipped.contains(cat)) continue;
			sb.append(cat).append(Arrays.toString(cat.probs)).append(' ').append(cat.seed)
					.append('/').append(cat.dropped).append('/').append(cat.using2ndProbs).append('\n');
		}
		return sb.toString();
	}

	/** Cards left across the melee weapon tier decks. */
	private static float weaponCards() {
		float sum = 0;
		for (Generator.Category tier : Generator.wepTiers) {
			for (float p : tier.probs) sum += p;
		}
		return sum;
	}

	private static Weapon weaponOf(Statue statue) {
		try {
			Field f = Statue.class.getDeclaredField("weapon");
			f.setAccessible(true);
			return (Weapon) f.get(statue);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * The transmogrified item is dropped through Level.drop, which then asks Game.scene() for the
	 * live scene; a headless run has no Game. The item is already drawn by then, which is all this
	 * test reads, so only that NullPointerException is let through.
	 */
	private static void rethrowUnlessHeadlessDrop(Throwable t) throws Exception {
		StackTraceElement[] st = t.getStackTrace();
		boolean headlessDrop = t instanceof NullPointerException && st.length > 1
				&& st[0].getClassName().endsWith(".Game") && "scene".equals(st[0].getMethodName())
				&& st[1].getClassName().endsWith(".Level") && "drop".equals(st[1].getMethodName());
		if (!headlessDrop) {
			if (t instanceof Exception) throw (Exception) t;
			throw (Error) t;
		}
	}
}
