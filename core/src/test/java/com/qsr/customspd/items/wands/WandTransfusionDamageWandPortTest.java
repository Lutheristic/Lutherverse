package com.qsr.customspd.items.wands;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Char;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.buffs.WandEmpower;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Skeleton;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 544c1e926 (v2.4.0): "transfusion now uses damage wand logic". The wand is a
 * DamageWand, so its damage to the undead is rolled by damageRoll() - same 3+lvl to 6+2*lvl
 * range as before - and an empowered-wand charge (the mage's wand empower) adds to the hit and
 * is spent by it, like for any other damage wand.
 */
class WandTransfusionDamageWandPortTest {

	/** A stub sprite whose particle emitters swallow everything (there is no scene to host them). */
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
	void transfusionIsADamageWandAndItsHitOnTheUndeadUsesWandEmpower() throws Exception {
		assertTrue(DamageWand.class.isAssignableFrom(WandOfTransfusion.class));

		HeadlessLevel.install(8, 8);
		Hero hero = new Hero();
		Dungeon.hero = hero;
		hero.pos = 3 * 8 + 2;
		InertSprite heroSprite = new InertSprite();
		heroSprite.ch = hero;
		hero.sprite = heroSprite;
		setCurUser(hero);
		Buff.affect(hero, WandEmpower.class).set(5, 1);

		Skeleton skeleton = HeadlessLevel.at(new Skeleton(), hero.pos + 2);
		skeleton.alignment = Char.Alignment.ENEMY;
		skeleton.HP = skeleton.HT = 1000;
		InertSprite sprite = new InertSprite();
		sprite.ch = skeleton;
		skeleton.sprite = sprite;

		WandOfTransfusion wand = new WandOfTransfusion();
		Ballistica beam = new Ballistica(hero.pos, skeleton.pos, Ballistica.PROJECTILE);
		beam.path.clear(); // CellEmitter has no scene to draw into
		wand.onZap(beam);

		int dealt = 1000 - skeleton.HP;
		assertTrue(dealt >= 3 + 5 && dealt <= 6 + 5,
				"a +0 transfusion hit (3-6) plus the empower boost of 5, got " + dealt);
		assertNull(hero.buff(WandEmpower.class), "the single empowered shot is spent by the hit");
		assertEquals(3, wand.min(0));
		assertEquals(6, wand.max(0));
	}
}
