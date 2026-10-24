package com.qsr.customspd.actors.buffs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Mob;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port regression test for upstream SPD 5c3a143fc (v2.3.0): "fixed amok not properly resetting
 * aggro in some cases".
 *
 * An amok-ed enemy fights other enemies, and they fight back. Amok.detach used to clear only the
 * amok-ed mob's own enemy, so every OTHER enemy still targeting it kept chasing it after the
 * buff ended. It now also clears each enemy that targets the amok-ed mob (the same
 * enemy-to-enemy reset StoneOfAggression.Aggression already did).
 */
class AmokDetachPortTest {

	private Hero prevHero;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@BeforeEach
	void saveStatics() {
		prevHero = Dungeon.hero;
		Dungeon.hero = null;
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = prevHero;
	}

	private static Mob place(HeadlessLevel level, int x, int y) {
		Mob m = HeadlessLevel.at(new Rat(), y * level.width() + x);
		StubSprite.attach(m);
		level.mobs.add(m);
		return m;
	}

	@Test
	void enemiesTargetingTheAmokedMobAreResetWhenAmokEnds() {
		HeadlessLevel level = HeadlessLevel.install(8, 8);
		Mob amoked = place(level, 3, 3);
		Mob other = place(level, 4, 3);

		Buff.affect(amoked, Amok.class, 10f);
		assertNotNull(amoked.buff(Amok.class), "test setup: amok must be attached");

		amoked.aggro(other);
		other.aggro(amoked);
		assertTrue(other.isTargeting(amoked), "test setup: the bystander enemy is chasing the amok-ed mob");

		amoked.buff(Amok.class).detach();

		assertFalse(other.isTargeting(amoked),
				"an enemy that was chasing the amok-ed mob must lose it when amok ends");
		assertFalse(amoked.isTargeting(other),
				"the amok-ed mob must lose the enemy it was fighting");
	}
}
