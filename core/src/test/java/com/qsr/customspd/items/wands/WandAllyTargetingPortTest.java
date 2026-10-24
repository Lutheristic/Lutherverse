package com.qsr.customspd.items.wands;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Char;
import com.qsr.customspd.actors.buffs.Barrier;
import com.qsr.customspd.actors.buffs.Charm;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Mimic;
import com.qsr.customspd.actors.mobs.Rat;
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Upstream 270340495 (v2.2.0): "fixed some wands giving buffs when shooting NPCs/allies". Wand of
 * living earth only builds rock armor, and wand of transfusion only shields the user and charms
 * the target, when the target is an enemy (or a hidden, neutral mimic) - not an ally or a
 * neutral bystander.
 */
class WandAllyTargetingPortTest {

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

	private static Hero hero() throws Exception {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		hero.pos = 3 * 8 + 2;
		InertSprite sprite = new InertSprite();
		sprite.ch = hero;
		hero.sprite = sprite;
		setCurUser(hero);
		return hero;
	}

	private static <T extends Char> T target(T ch, Char.Alignment alignment, int cell) {
		HeadlessLevel.at(ch, cell);
		ch.alignment = alignment;
		ch.HP = ch.HT = 1000;
		InertSprite sprite = new InertSprite();
		sprite.ch = ch;
		ch.sprite = sprite;
		return ch;
	}

	private static void livingEarthAt(Hero hero, Char ch) {
		new WandOfLivingEarth().onZap(new Ballistica(hero.pos, ch.pos, Ballistica.MAGIC_BOLT));
	}

	/** Transfusion's beam is cut to an empty path: CellEmitter has no scene to draw into. */
	private static void transfusionAt(Hero hero, Char ch) {
		Ballistica beam = new Ballistica(hero.pos, ch.pos, Ballistica.PROJECTILE);
		beam.path.clear();
		new WandOfTransfusion().onZap(beam);
	}

	@Test
	void livingEarthBuildsArmorOnlyFromEnemiesAndHiddenMimics() throws Exception {
		HeadlessLevel.install(8, 8);
		Hero hero = hero();
		int cell = hero.pos + 2;

		livingEarthAt(hero, target(new Rat(), Char.Alignment.ALLY, cell));
		assertNull(hero.buff(WandOfLivingEarth.RockArmor.class), "shooting an ally grants no rock armor");
		HeadlessLevel.uninstall();

		HeadlessLevel.install(8, 8);
		hero = hero();
		livingEarthAt(hero, target(new Rat(), Char.Alignment.NEUTRAL, cell));
		assertNull(hero.buff(WandOfLivingEarth.RockArmor.class), "nor does shooting a neutral bystander");
		HeadlessLevel.uninstall();

		HeadlessLevel.install(8, 8);
		hero = hero();
		livingEarthAt(hero, target(new Rat(), Char.Alignment.ENEMY, cell));
		assertNotNull(hero.buff(WandOfLivingEarth.RockArmor.class), "an enemy still does");
		HeadlessLevel.uninstall();

		HeadlessLevel.install(8, 8);
		hero = hero();
		livingEarthAt(hero, target(new Mimic(), Char.Alignment.NEUTRAL, cell));
		assertNotNull(hero.buff(WandOfLivingEarth.RockArmor.class), "and so does a hiding (neutral) mimic");
	}

	@Test
	void transfusionShieldsAndCharmsOnlyEnemiesAndHiddenMimics() throws Exception {
		HeadlessLevel.install(8, 8);
		Hero hero = hero();
		int cell = hero.pos + 2;
		Rat neutral = target(new Rat(), Char.Alignment.NEUTRAL, cell);

		transfusionAt(hero, neutral);
		assertNull(hero.buff(Barrier.class), "a neutral bystander gives the user no shield");
		assertNull(neutral.buff(Charm.class), "and is not charmed");
		HeadlessLevel.uninstall();

		HeadlessLevel.install(8, 8);
		hero = hero();
		Rat enemy = target(new Rat(), Char.Alignment.ENEMY, cell);
		transfusionAt(hero, enemy);
		assertNotNull(hero.buff(Barrier.class), "an enemy still shields the user");
		assertNotNull(enemy.buff(Charm.class), "and is charmed");
		HeadlessLevel.uninstall();

		HeadlessLevel.install(8, 8);
		hero = hero();
		Mimic mimic = target(new Mimic(), Char.Alignment.NEUTRAL, cell);
		transfusionAt(hero, mimic);
		assertNotNull(hero.buff(Barrier.class), "a hiding (neutral) mimic is treated as an enemy");
	}
}
