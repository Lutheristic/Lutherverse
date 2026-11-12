package com.qsr.customspd.actors.mobs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Actor;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Port regression for the ripper demon hunk of upstream 9aea6b3e0: the leap wind-up waits
 * for the hero's next turn rounded up, gated between the ripper's own attack delay (half a
 * turn) and three times it. It used to gate the raw cooldown between 1 and 3 turns.
 */
class RipperLeapDelayPortTest {

	private static final int SIZE = 12;

	private Hero hero;
	private RipperDemon ripper;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@BeforeEach
	void setUp() throws Exception {
		HeadlessLevel.install(SIZE, SIZE);
		hero = new Hero();
		Dungeon.hero = hero;
		HeadlessLevel.at(hero, 2 * SIZE + 2);
		StubSprite.attach(hero);

		ripper = HeadlessLevel.at(new RipperDemon(), 2 * SIZE + 8);
		StubSprite.attach(ripper);
		ripper.enemy = hero;
		// the hero has not moved since the ripper last saw them, so it aims straight at them
		field(RipperDemon.class, "lastEnemyPos").setInt(ripper, hero.pos);
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private static Field field(Class<?> owner, String name) throws Exception {
		Field f = owner.getDeclaredField(name);
		f.setAccessible(true);
		return f;
	}

	/** How long the ripper spends getting ready to leap when the hero's next turn is heroCooldown away. */
	private float windUp(float heroCooldown) throws Exception {
		field(Actor.class, "time").setFloat(hero, heroCooldown);
		float before = ripper.cooldown();

		ripper.new Hunting().act(true, false);

		assertEquals(hero.pos, field(RipperDemon.class, "leapPos").getInt(ripper), "the ripper readies a leap at the hero");
		return ripper.cooldown() - before;
	}

	@Test
	void aSlowHeroFacesAtMostOneAndAHalfTurnsOfWindUp() throws Exception {
		assertEquals(1.5f, windUp(3f), 0.0001f);
	}

	@Test
	void aPartTurnIsRoundedUpToTheNextWholeTurn() throws Exception {
		assertEquals(1.5f, windUp(1.25f), 0.0001f);
	}

	@Test
	void aHeroAboutToActFacesHalfATurn() throws Exception {
		assertEquals(0.5f, windUp(0f), 0.0001f);
	}

	@Test
	void aHeroWithinATurnStillFacesOneTurn() throws Exception {
		// unchanged by the port: within one turn the wind-up was already one turn
		assertEquals(1f, windUp(0.5f), 0.0001f);
	}
}
