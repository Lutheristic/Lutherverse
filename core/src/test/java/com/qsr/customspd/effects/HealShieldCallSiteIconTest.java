package com.qsr.customspd.effects;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Actor;
import com.qsr.customspd.actors.blobs.WaterOfHealth;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.buffs.Charm;
import com.qsr.customspd.actors.buffs.Corruption;
import com.qsr.customspd.actors.buffs.WellFed;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.hero.HeroClass;
import com.qsr.customspd.actors.hero.Talent;
import com.qsr.customspd.actors.mobs.Bat;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.actors.mobs.Succubus;
import com.qsr.customspd.items.artifacts.ChaliceOfBlood;
import com.qsr.customspd.items.food.PhantomMeat;
import com.qsr.customspd.items.weapon.enchantments.Blocking;
import com.qsr.customspd.items.weapon.enchantments.Vampiric;
import com.qsr.customspd.sprites.CharSprite;
import com.qsr.customspd.test.HeadlessRun;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.PathFinder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The heal / shield sites of upstream 34b5035e7 ("floating text + icons for loads more
 * heal/shield cases") and b859b7a9e (Integer.toString amounts): a representative call site per
 * category - heal over time (WellFed), instant heal (Water of Health, Corruption, Phantom Meat,
 * Chalice), shield (Restored Willpower, Blocking), mob heal (Bat) and mob heal + shield
 * (Succubus) - each must hand showStatusWithIcon the right icon and amount. The sprites are
 * recording stubs; the icon id and text are the whole contract. Complements
 * FloatingTextCallSiteIconTest, which covers the 1c8b13fa6 sites.
 */
class HealShieldCallSiteIconTest {

	/** Records every status shown instead of drawing it. */
	static final class RecordingSprite extends CharSprite {
		final List<Integer> icons = new ArrayList<>();
		final List<String> texts = new ArrayList<>();

		@Override
		public void showStatusWithIcon(int color, String text, int icon, Object... args) {
			icons.add(icon);
			texts.add(text);
		}

		@Override
		public void add(State state) {}

		@Override
		public void remove(State state) {}

		@Override
		public Emitter emitter() {
			return new Emitter();
		}

		void clear() {
			icons.clear();
			texts.clear();
		}
	}

	private HeadlessRun run;
	private Hero hero;
	private RecordingSprite heroSprite;

	@BeforeEach
	void startRun() {
		run = HeadlessRun.start(20231204L);
		run.enterFirstLevel();
		hero = Dungeon.hero;
		heroSprite = new RecordingSprite();
		hero.sprite = heroSprite;
	}

	@AfterEach
	void closeRun() {
		run.close();
	}

	private int freeCellNextToHero() {
		for (int offset : PathFinder.NEIGHBOURS8) {
			int cell = hero.pos + offset;
			if (Dungeon.level.passable[cell] && Actor.findChar(cell) == null) return cell;
		}
		throw new IllegalStateException("no free cell next to the hero");
	}

	private RecordingSprite spriteOn(com.qsr.customspd.actors.Char ch) {
		RecordingSprite sprite = new RecordingSprite();
		ch.sprite = sprite;
		return sprite;
	}

	// ---------------------------------------------------------------- heal over time

	@Test
	void wellFedTickShowsOneHealingOnlyWhenItHeals() throws Exception {
		WellFed buff = Buff.affect(hero, WellFed.class);
		Field left = WellFed.class.getDeclaredField("left");
		left.setAccessible(true);

		heroSprite.clear();
		hero.HP = hero.HT; // a tick at full health heals nothing, so shows nothing (3ae042f3a guard)
		left.setInt(buff, 37); // act() decrements to 36, a multiple of 18
		buff.act();
		assertTrue(heroSprite.icons.isEmpty(), "no heal text at full HP");

		hero.HP = 1;
		left.setInt(buff, 37);
		buff.act();
		assertEquals(1, heroSprite.icons.size(), "one status for the tick");
		assertEquals(FloatingText.HEALING, (int) heroSprite.icons.get(0));
		assertEquals("1", heroSprite.texts.get(0));
		assertEquals(2, hero.HP);
	}

	// ---------------------------------------------------------------- instant heals

	@Test
	void waterOfHealthShowsTheFullHeal() throws Exception {
		hero.HP = 1;
		Method m = WaterOfHealth.class.getDeclaredMethod("affectHero", Hero.class);
		m.setAccessible(true);
		try {
			m.invoke(new WaterOfHealth(), hero);
		} catch (InvocationTargetException e) {
			// the shaft-of-light CellEmitter after the heal needs a GameScene; the heal and its
			// floating text come before it, which is all this test is about
			if (!(e.getCause() instanceof NullPointerException)) throw e;
		}
		assertEquals(hero.HT, hero.HP);
		assertEquals(FloatingText.HEALING, firstIcon(heroSprite));
		assertEquals(Integer.toString(hero.HT), heroSprite.texts.get(0));
	}

	@Test
	void corruptionHealShowsTheFullHeal() {
		Rat rat = new Rat();
		RecordingSprite sprite = spriteOn(rat);
		rat.HP = 1;
		Corruption.corruptionHeal(rat);
		assertEquals(rat.HT, rat.HP);
		assertEquals(FloatingText.HEALING, firstIcon(sprite));
		assertEquals(Integer.toString(rat.HT), sprite.texts.get(0));
	}

	@Test
	void phantomMeatShowsAQuarterOfMaxHp() {
		hero.HP = 1;
		PhantomMeat.effect(hero);
		// the meal's other effects (barkskin, invisibility) may float text of their own
		int heal = heroSprite.icons.indexOf(FloatingText.HEALING);
		assertTrue(heal >= 0, "a healing status: " + heroSprite.icons);
		assertEquals(Integer.toString(hero.HT / 4), heroSprite.texts.get(heal));
	}

	@Test
	void chaliceShowsAnIntegerHealInsideItsGuard() {
		ChaliceOfBlood chalice = new ChaliceOfBlood();

		hero.HP = hero.HT;
		chalice.charge(hero, 10f);
		assertTrue(heroSprite.icons.isEmpty(), "no heal text at full HP");

		hero.HP = 1;
		chalice.charge(hero, 10f);
		assertEquals(FloatingText.HEALING, firstIcon(heroSprite));
		String text = heroSprite.texts.get(0);
		assertTrue(text.matches("\\d+"), "an int amount, not a float like \"5.0\": " + text);
		assertEquals(1 + Integer.parseInt(text), hero.HP);
	}

	// ---------------------------------------------------------------- shields

	@Test
	void restoredWillpowerShowsTheShieldItGrants() {
		// the non-warrior branch: a barrier of 5% max HP (the warrior's supercharge needs a seal)
		hero.heroClass = HeroClass.MAGE;
		hero.talents.get(0).put(Talent.RESTORED_WILLPOWER, 2);
		Talent.onHealingPotionUsed(hero);
		assertEquals(FloatingText.SHIELDING, firstIcon(heroSprite));
		assertTrue(heroSprite.texts.get(0).matches("\\d+"), heroSprite.texts.get(0));
	}

	@Test
	void blockingEnchantShowsItsShield() {
		Rat rat = new Rat();
		rat.pos = freeCellNextToHero();
		new Blocking().proc(1f, 5, hero, rat, 3);
		assertEquals(FloatingText.SHIELDING, firstIcon(heroSprite));
		assertEquals("5", heroSprite.texts.get(0));
	}

	@Test
	void vampiricStrengthOverloadShowsItsHeal() {
		// CPDU's proc(probability, strength, ...) overload, which pack custom mobs reach through
		// CustomMob's enchantments: the same heal as the weapon path (upstream 1c8b13fa6's icon)
		Rat rat = new Rat();
		rat.pos = freeCellNextToHero();
		hero.HP = 1;
		new Vampiric().proc(1f, 50, hero, rat, 10); // heals strength% of damage = 5
		assertEquals(FloatingText.HEALING, firstIcon(heroSprite));
		assertEquals("5", heroSprite.texts.get(0));
		assertEquals(6, hero.HP);
	}

	// ---------------------------------------------------------------- mob heals

	@Test
	void batLifestealShowsItsHeal() {
		Bat bat = new Bat();
		RecordingSprite sprite = spriteOn(bat);
		bat.pos = freeCellNextToHero();
		bat.HP = 1;
		bat.attackProc(hero, 10); // heals min(damage - 4, missing HP) = 6
		assertEquals(FloatingText.HEALING, firstIcon(sprite));
		assertEquals("6", sprite.texts.get(0));
	}

	@Test
	void succubusShowsAHealWhenHurtAndAShieldWhenFull() {
		Succubus succubus = new Succubus();
		RecordingSprite sprite = spriteOn(succubus);
		succubus.pos = freeCellNextToHero();
		Buff.affect(hero, Charm.class, 10f).object = succubus.id();
		Dungeon.level.heroFOV[succubus.pos] = false; // the in-view charm sound/particles need a scene

		succubus.HP = 1;
		succubus.attackProc(hero, 10);
		assertEquals(FloatingText.HEALING, firstIcon(sprite), "hurt: plain heal");
		assertEquals("5", sprite.texts.get(0));

		sprite.clear();
		succubus.HP = succubus.HT;
		succubus.attackProc(hero, 10);
		assertEquals(FloatingText.SHIELDING, firstIcon(sprite), "full HP: the 5 + damage becomes shield");
		assertEquals("15", sprite.texts.get(0));
	}

	/** The icon of the first status shown, or Integer.MIN_VALUE when nothing was shown at all. */
	private static int firstIcon(RecordingSprite sprite) {
		return sprite.icons.isEmpty() ? Integer.MIN_VALUE : sprite.icons.get(0);
	}
}
