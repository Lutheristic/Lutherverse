package com.qsr.customspd.items.artifacts;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.buffs.Paralysis;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.sprites.CharSprite;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** How the hero's sprite looks while the timekeeper's hourglass holds them in stasis. */
class HourglassStasisFxPortTest {

	/** A stub that keeps the sprite states it is given. */
	private static final class StateSprite extends StubSprite {
		final Set<CharSprite.State> states = EnumSet.noneOf(CharSprite.State.class);

		@Override
		public void add(State state) {
			states.add(state);
		}

		@Override
		public void remove(State state) {
			states.remove(state);
		}
	}

	private Hero hero;
	private StateSprite sprite;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private TimekeepersHourglass.timeStasis stasis() throws Exception {
		HeadlessLevel level = HeadlessLevel.install(8, 8);
		hero = new Hero();
		Dungeon.hero = hero;
		sprite = new StateSprite();
		sprite.ch = hero;
		hero.sprite = sprite;
		HeadlessLevel.at(hero, 3 * level.width() + 3);

		TimekeepersHourglass hourglass = new TimekeepersHourglass();
		Field charge = Artifact.class.getDeclaredField("charge");
		charge.setAccessible(true);
		charge.setInt(hourglass, 2);
		return hourglass.new timeStasis();
	}

	/** upstream 70053d933 (v2.5.0): the hero is frozen in time, so they look paralysed, not invisible. */
	@Test
	void theHeroLooksFrozenWhileInStasis() throws Exception {
		TimekeepersHourglass.timeStasis stasis = stasis();

		stasis.attachTo(hero);
		assertTrue(sprite.states.contains(CharSprite.State.PARALYSED), "frozen in place during stasis");
		assertFalse(sprite.states.contains(CharSprite.State.INVISIBLE), "not drawn as invisible");

		stasis.detach();
		assertFalse(sprite.states.contains(CharSprite.State.PARALYSED), "free again once stasis ends");
	}

	/**
	 * upstream 7c66d1058 (v2.5.3): ending stasis clears each look on its own, so a hero still
	 * paralysed by something else keeps that look, and a leftover invisible look goes.
	 */
	@Test
	void endingStasisClearsEachLookOnItsOwn() throws Exception {
		TimekeepersHourglass.timeStasis stasis = stasis();
		Buff.affect(hero, Paralysis.class, 10f);
		sprite.states.add(CharSprite.State.INVISIBLE);

		stasis.attachTo(hero);
		stasis.detach();

		assertTrue(hero.paralysed > 0, "fixture: the paralysis outlasts the stasis");
		assertTrue(sprite.states.contains(CharSprite.State.PARALYSED), "still drawn paralysed");
		assertFalse(sprite.states.contains(CharSprite.State.INVISIBLE), "nothing keeps the hero invisible");
	}
}
