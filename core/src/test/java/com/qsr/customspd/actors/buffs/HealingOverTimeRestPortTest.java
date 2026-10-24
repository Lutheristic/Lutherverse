package com.qsr.customspd.actors.buffs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.items.artifacts.ChaliceOfBlood;
import com.qsr.customspd.items.potions.elixirs.ElixirOfAquaticRejuvenation;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import com.watabou.noosa.particles.Emitter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 3ae042f3a (v2.3.0): healing-over-time sources only interrupt a resting hero when the
 * hero reaches full HP (or the buff runs out) - not on every tick. Pins the interrupt for well
 * fed, the chalice of blood and the elixir of aquatic rejuvenation, and that a tick which is
 * still short of full health leaves the rest alone.
 */
class HealingOverTimeRestPortTest {

	/** Swallows the heal particles: there is no scene to host them. */
	private static final class InertSprite extends StubSprite {
		private static Emitter inert() {
			return new Emitter() {
				@Override
				public void burst(Factory factory, int quantity) {
				}
			};
		}

		@Override
		public Emitter emitter() {
			return inert();
		}
	}

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private static Hero restingHero(int hp) {
		HeadlessLevel.install(8, 8);
		Hero hero = new Hero();
		Dungeon.hero = hero;
		hero.pos = 3 * 8 + 2;
		InertSprite sprite = new InertSprite();
		sprite.ch = hero;
		hero.sprite = sprite;
		hero.HT = 20;
		hero.HP = hp;
		hero.resting = true;
		return hero;
	}

	@Test
	void wellFedStopsRestingOnlyWhenTheHeroReachesFullHealthOrTheBuffEnds() {
		Hero hero = restingHero(10);
		WellFed wellFed = Buff.affect(hero, WellFed.class);

		wellFed.left = 19; // the next act() lands on a heal tick (left % 18 == 0)
		wellFed.act();
		assertEquals(11, hero.HP);
		assertTrue(hero.resting, "a heal tick that is still short of full health leaves the rest alone");

		hero.HP = hero.HT - 1;
		wellFed.left = 19;
		wellFed.act();
		assertEquals(hero.HT, hero.HP);
		assertFalse(hero.resting, "the heal tick that fills the hero's health interrupts the rest");

		hero.resting = true;
		wellFed.left = 0; // the next act() runs the buff out
		wellFed.act();
		assertNull(hero.buff(WellFed.class));
		assertFalse(hero.resting, "the buff running out interrupts the rest");
	}

	@Test
	void chaliceOfBloodStopsRestingWhenItTopsTheHeroUp() {
		Hero hero = restingHero(19);
		ChaliceOfBlood chalice = new ChaliceOfBlood();
		chalice.level(10); // 2.5 HP per charge: always at least 2

		chalice.charge(hero, 1f);

		assertEquals(hero.HT, hero.HP);
		assertFalse(hero.resting, "reaching full health interrupts the rest");
	}

	@Test
	void aquaticRejuvenationStopsRestingWhenItTopsTheHeroUpOrRunsOut() {
		Hero hero = restingHero(19);
		Dungeon.level.water[hero.pos] = true;
		ElixirOfAquaticRejuvenation.AquaHealing healing =
				Buff.affect(hero, ElixirOfAquaticRejuvenation.AquaHealing.class);
		healing.set(10);

		healing.act();

		assertEquals(hero.HT, hero.HP);
		assertFalse(hero.resting, "reaching full health interrupts the rest");
	}
}
