package com.qsr.customspd.effects;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Actor;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.buffs.Healing;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Brute;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.items.Dewdrop;
import com.qsr.customspd.items.EnergyCrystal;
import com.qsr.customspd.items.Gold;
import com.qsr.customspd.items.food.SupplyRation;
import com.qsr.customspd.items.potions.PotionOfStrength;
import com.qsr.customspd.items.potions.elixirs.ElixirOfMight;
import com.qsr.customspd.items.potions.exotic.PotionOfShielding;
import com.qsr.customspd.items.wands.WandOfWarding;
import com.qsr.customspd.sprites.CharSprite;
import com.qsr.customspd.test.HeadlessRun;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.PathFinder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Heal / shield / experience / currency call sites show their floating text with the matching
 * icon (upstream 1c8b13fa6's call-site hunks). Runs on a real headless run (F4) so the hero,
 * level and statics the call sites touch are genuine; the sprites are recording stubs, since
 * the icon id handed to showStatusWithIcon is the whole contract under test (plain showStatus
 * funnels through it with NO_ICON, which is what the unported call sites record).
 */
class FloatingTextCallSiteIconTest {

	/** Records every status shown instead of drawing it. */
	static final class RecordingSprite extends CharSprite {
		final List<Integer> icons = new ArrayList<>();
		final List<String> texts = new ArrayList<>();

		@Override
		public void showStatusWithIcon(int color, String text, int icon, Object... args) {
			icons.add(icon);
			texts.add(text);
		}

		// buff visual states build particle emitters, which need a scene; nothing to draw here
		@Override
		public void add(State state) {}

		@Override
		public void remove(State state) {}

		// a detached emitter: heal call sites burst particles, which go nowhere headless
		@Override
		public Emitter emitter() {
			return new Emitter();
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

	@Test
	void currencyPickupsShowGoldAndEnergyIcons() {
		new Gold(25).doPickUp(hero, hero.pos);
		assertEquals(FloatingText.GOLD, firstIcon(heroSprite), "gold pickup");
		assertEquals("25", heroSprite.texts.get(0), "the gold amount, no longer formatted as %+d");

		heroSprite.icons.clear();
		heroSprite.texts.clear();
		new EnergyCrystal(3).doPickUp(hero, hero.pos);
		assertEquals(FloatingText.ENERGY, firstIcon(heroSprite), "energy crystal pickup");
		assertEquals("3", heroSprite.texts.get(0));
	}

	@Test
	void healingSourcesShowTheHealingIcon() {
		hero.HP = 1;
		Dewdrop.consumeDew(1, hero, true);
		assertEquals(FloatingText.HEALING, firstIcon(heroSprite), "a dewdrop heal");

		heroSprite.icons.clear();
		hero.HP = 1;
		Buff.affect(hero, Healing.class).setHeal(10, 0, 2);
		hero.buff(Healing.class).act();
		assertEquals(FloatingText.HEALING, firstIcon(heroSprite), "a Healing buff tick");

		heroSprite.icons.clear();
		heroSprite.texts.clear();
		invoke(new SupplyRation(), SupplyRation.class, "satisfy", hero);
		assertEquals(FloatingText.HEALING, firstIcon(heroSprite), "a supply ration's +5 HP");
		assertEquals("5", heroSprite.texts.get(0));

		WandOfWarding.Ward ward = new WandOfWarding.Ward();
		RecordingSprite wardSprite = new RecordingSprite();
		ward.sprite = wardSprite;
		ward.tier = 4;
		ward.wandHeal(1);
		assertEquals(FloatingText.HEALING, firstIcon(wardSprite), "a ward healed by its wand");
	}

	@Test
	void shieldSourcesShowTheShieldingIcon() {
		new PotionOfShielding().apply(hero);
		assertEquals(FloatingText.SHIELDING, firstIcon(heroSprite), "a potion of shielding");

		Brute brute = new Brute();
		RecordingSprite bruteSprite = new RecordingSprite();
		brute.sprite = bruteSprite;
		brute.pos = freeCellNextToHero();
		Dungeon.level.heroFOV[brute.pos] = false; // the in-view SpellSprite needs a scene
		invoke(brute, Brute.class, "triggerEnrage");
		assertEquals(FloatingText.SHIELDING, firstIcon(bruteSprite), "an enraged brute's shield");
		assertEquals(Integer.toString(brute.HT / 2 + 4), bruteSprite.texts.get(0));
	}

	@Test
	void strengthGainsShowTheStrengthIcon() {
		int str = hero.STR;
		new PotionOfStrength().apply(hero);
		assertEquals(FloatingText.STRENGTH, firstIcon(heroSprite), "a potion of strength");
		assertEquals("1", heroSprite.texts.get(0));
		assertEquals(str + 1, hero.STR, "and the potion still grants the point (d7bd161f2)");

		heroSprite.icons.clear();
		new ElixirOfMight().apply(hero);
		assertEquals(FloatingText.STRENGTH, firstIcon(heroSprite), "an elixir of might");
		assertEquals(str + 2, hero.STR);
	}

	@Test
	void killingAMobShowsTheExperienceIcon() {
		Rat rat = new Rat();
		rat.pos = freeCellNextToHero();
		Dungeon.level.mobs.add(rat);
		Actor.add(rat);
		rat.destroy();
		assertEquals(FloatingText.EXPERIENCE, firstIcon(heroSprite), "exp from a kill");
		assertEquals(Integer.toString(rat.EXP), heroSprite.texts.get(0), "the exp amount, no longer \"+%dEXP\"");
	}

	/** The icon of the first status shown, or Integer.MIN_VALUE when nothing was shown at all. */
	private static int firstIcon(RecordingSprite sprite) {
		return sprite.icons.isEmpty() ? Integer.MIN_VALUE : sprite.icons.get(0);
	}

	private static void invoke(Object target, Class<?> owner, String method) {
		try {
			Method m = owner.getDeclaredMethod(method);
			m.setAccessible(true);
			m.invoke(target);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static void invoke(Object target, Class<?> owner, String method, Hero arg) {
		try {
			Method m = owner.getDeclaredMethod(method, Hero.class);
			m.setAccessible(true);
			m.invoke(target, arg);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}
}
