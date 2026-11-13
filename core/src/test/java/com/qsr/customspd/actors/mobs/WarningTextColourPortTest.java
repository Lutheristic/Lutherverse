package com.qsr.customspd.actors.mobs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.items.weapon.curses.Explosive;
import com.qsr.customspd.items.weapon.melee.WornShortsword;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.sprites.CharSprite;
import com.qsr.customspd.sprites.GooSprite;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.RenderThreadCalls;
import com.qsr.customspd.test.StubSprite;
import com.qsr.customspd.ui.BossHealthBar;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port regression for upstream 7cb955ff0 (v2.3.0): floating text uses orange (WARNING) for
 * warnings and red (NEGATIVE) for damage. Damage numbers used to be orange while a char was above
 * half health; Goo's and the armored brute's enrage, Goo's pump-up, Tengu's bomb countdown and the
 * explosive curse running hot were red or white.
 *
 * The cornered-mob rage line is checked in MobRetreatPortTest.
 */
class WarningTextColourPortTest {

	private Hero prevHero;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@BeforeEach
	void setUp() {
		prevHero = Dungeon.hero;
		HeadlessLevel.install(10, 10);
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeadlessLevel.at(hero, cell(2, 2));
		StubSprite.attach(hero);
	}

	@AfterEach
	void tearDown() {
		BossHealthBar.assignBoss(null);
		HeadlessLevel.uninstall();
		Dungeon.hero = prevHero;
	}

	@Test
	void damageNumbersAreRedAboveHalfHealth() {
		Rat rat = placeMob(new Rat(), 5, 5);
		rat.HP = rat.HT = 100;

		rat.damage(1, new Rat());

		assertEquals(1, StubSprite.of(rat).statuses.size(), "the damage number");
		assertEquals(CharSprite.NEGATIVE, StubSprite.of(rat).statuses.get(0).color, "a healthy char still takes red damage numbers");
	}

	@Test
	void anArmoredBruteEnragesInOrange() {
		ArmoredBrute brute = placeMob(new ArmoredBrute(), 5, 5);
		Dungeon.level.heroFOV[brute.pos] = true;

		brute.triggerEnrage();

		assertEquals(CharSprite.WARNING, colourOf(StubSprite.of(brute).statuses, Messages.get(brute, "enraged")));
	}

	@Test
	void gooEnragesInOrange() throws Exception {
		Goo goo = placeGoo();

		goo.damage(goo.HT / 2 + 1, new Rat());

		assertTrue(goo.HP * 2 <= goo.HT, "precondition: goo dropped to half health");
		assertEquals(CharSprite.WARNING, colourOf(gooSprite(goo).statuses(), Messages.get(goo, "enraged")));
	}

	@Test
	void gooPumpingUpWarnsInOrange() throws Exception {
		Goo goo = placeGoo();
		Dungeon.level.heroFOV[goo.pos] = true;

		// a healthy goo pumps up when Random.Int(5) rolls 0
		long seed = 0;
		do {
			seed++;
			Random.pushGenerator(seed);
			int roll = Random.Int(5);
			Random.popGenerator();
			if (roll == 0) break;
		} while (true);

		Random.pushGenerator(seed);
		try {
			goo.doAttack(Dungeon.hero);
		} finally {
			Random.popGenerator();
		}

		assertEquals(CharSprite.WARNING, colourOf(gooSprite(goo).statuses(), Messages.get(goo, "!!!")));
	}

	@Test
	void tenguBombCountsDownInOrange() {
		// bombPos stays unset so the smoke effect, which needs a scene, is skipped
		Tengu.BombAbility bomb = new Tengu.BombAbility();
		List<Integer> colours = new ArrayList<>();
		List<String> texts = new ArrayList<>();

		try (RenderThreadCalls calls = RenderThreadCalls.capture()) {
			bomb.act();
			bomb.act();
			bomb.act();
			for (Callback c : calls.callbacks()) {
				texts.add((String) RenderThreadCalls.captured(c, "text"));
				colours.add((Integer) RenderThreadCalls.captured(c, "color"));
			}
		}

		assertEquals("[3..., 2..., 1...]", texts.toString(), "precondition: the three countdown texts");
		for (int i = 0; i < 3; i++) {
			assertEquals(CharSprite.WARNING, (int) colours.get(i), texts.get(i));
		}
	}

	@Test
	void theExplosiveCurseRunningHotIsOrange() throws Exception {
		Rat attacker = placeMob(new Rat(), 5, 5);
		Rat defender = placeMob(new Rat(), 6, 5);
		Explosive curse = new Explosive();
		Field durability = Explosive.class.getDeclaredField("durability");
		durability.setAccessible(true);

		List<StubSprite.Status> statuses = StubSprite.of(attacker).statuses;
		Random.pushGenerator(20231215L);
		try {
			// one point above hot: any hit that wears it down at all, short of exploding, runs it hot
			for (int i = 0; i < 50 && statuses.isEmpty(); i++) {
				durability.setInt(curse, 11);
				curse.proc(new WornShortsword(), attacker, defender, 1);
			}
		} finally {
			Random.popGenerator();
		}

		assertEquals(CharSprite.WARNING, colourOf(statuses, Messages.get(curse, "hot")));
	}

	// ------------------------------------------------------------------------------ helpers

	private static <T extends Mob> T placeMob(T mob, int x, int y) {
		HeadlessLevel.at(mob, cell(x, y));
		Dungeon.level.mobs.add(mob);
		StubSprite.attach(mob);
		return mob;
	}

	private static Goo placeGoo() throws Exception {
		Goo goo = new Goo();
		HeadlessLevel.at(goo, cell(5, 5));
		Dungeon.level.mobs.add(goo);
		HeadlessGooSprite sprite = allocate(HeadlessGooSprite.class);
		sprite.ch = goo;
		goo.sprite = sprite;
		return goo;
	}

	private static HeadlessGooSprite gooSprite(Goo goo) {
		return (HeadlessGooSprite) goo.sprite;
	}

	private static int colourOf(List<StubSprite.Status> statuses, String text) {
		StubSprite.Status found = null;
		for (StubSprite.Status s : statuses) {
			if (s.text.equals(text)) found = s;
		}
		assertNotNull(found, "no '" + text + "' status in " + statuses);
		return found.color;
	}

	private static int cell(int x, int y) {
		return y * Dungeon.level.width() + x;
	}

	/** Skips the constructor, which loads the sprite's texture. */
	private static <T> T allocate(Class<T> type) throws Exception {
		Field f = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
		f.setAccessible(true);
		Object unsafe = f.get(null);
		return type.cast(unsafe.getClass().getMethod("allocateInstance", Class.class).invoke(unsafe, type));
	}

	/**
	 * Goo casts its sprite to GooSprite. This one is allocated without its constructor, records
	 * status texts like StubSprite, and draws nothing.
	 */
	static class HeadlessGooSprite extends GooSprite {
		// allocated without a constructor, so field initializers never ran
		private StubSprite recorder;

		List<StubSprite.Status> statuses() {
			if (recorder == null) recorder = new StubSprite();
			return recorder.statuses;
		}

		@Override
		public void showStatus(int color, String text, Object... args) {
			showStatusWithIcon(color, text, -1, args);
		}

		@Override
		public void showStatusWithIcon(int color, String text, int icon, Object... args) {
			statuses();
			recorder.showStatusWithIcon(color, text, icon, args);
		}

		@Override public void pumpUp(int warnDist) {}
		@Override public void pumpAttack() {}
		@Override public void triggerEmitters() {}
		@Override public void clearEmitters() {}
		@Override public void spray(boolean on) {}
		@Override public void play(Animation anim) {}
		@Override public void idle() {}
		@Override public void attack(int cell) {}
		@Override public void flash() {}
		@Override public void bloodBurstA(PointF from, int damage) {}
		@Override public void burst(int color, int n) {}
		@Override public void add(State state) {}
		@Override public void remove(State state) {}
		@Override public Emitter emitter() { return new Emitter(); }
		@Override public Emitter centerEmitter() { return new Emitter(); }
		@Override public Emitter bottomEmitter() { return new Emitter(); }
	}
}
