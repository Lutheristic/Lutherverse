package com.qsr.customspd.items.wands;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.items.Item;
import com.qsr.customspd.mechanics.Ballistica;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import com.watabou.noosa.particles.Emitter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 613ee447d (v2.2.0): "fixed living earth not working when aimed at guardian". The
 * guardian is an ALLY, which the ally-targeting check (270340495) treated as "grant nothing", so
 * zapping the wand at your own guardian did not feed it any health.
 */
class WandOfLivingEarthGuardianPortTest {

	private static final class InertSprite extends StubSprite {
		private static Emitter inert() {
			return new Emitter() {
				@Override
				public void burst(Factory factory, int quantity) {
				}

				@Override
				public void pour(Factory factory, float interval) {
				}

				@Override
				public void start(Factory factory, float interval, int quantity) {
				}
			};
		}

		@Override
		public Emitter emitter() {
			return inert();
		}

		@Override
		public Emitter centerEmitter() {
			return inert();
		}
	}

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() throws Exception {
		setCurUser(null);
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private static void setCurUser(Hero hero) throws Exception {
		Field f = Item.class.getDeclaredField("curUser");
		f.setAccessible(true);
		f.set(null, hero);
	}

	@Test
	void zappingYourOwnGuardianFeedsItHealth() throws Exception {
		HeadlessLevel.install(8, 8);
		Hero hero = new Hero();
		Dungeon.hero = hero;
		hero.pos = 3 * 8 + 2;
		InertSprite heroSprite = new InertSprite();
		heroSprite.ch = hero;
		hero.sprite = heroSprite;
		setCurUser(hero);

		WandOfLivingEarth.EarthGuardian guardian = new WandOfLivingEarth.EarthGuardian();
		guardian.setInfo(hero, 0, 1); // wand level 0 -> HT 16, HP 1
		InertSprite guardianSprite = new InertSprite();
		guardianSprite.ch = guardian;
		guardian.sprite = guardianSprite;
		HeadlessLevel.at(guardian, hero.pos + 2);
		Dungeon.level.mobs.add(guardian);
		int before = guardian.HP;

		new WandOfLivingEarth().onZap(new Ballistica(hero.pos, guardian.pos, Ballistica.MAGIC_BOLT));

		assertTrue(guardian.HP > before,
				"a zap at the guardian must restore its health (was " + before + ", now " + guardian.HP + ")");
	}
}
