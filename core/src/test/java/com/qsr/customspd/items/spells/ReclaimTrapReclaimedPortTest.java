package com.qsr.customspd.items.spells;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.levels.Terrain;
import com.qsr.customspd.levels.traps.Trap;
import com.qsr.customspd.mechanics.Ballistica;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reclaim Trap flags the trap it fires as {@code reclaimed} before activating it (upstream
 * 7d7743153, v2.5.0). The flag is what lets a trap tell "part of the level" from "spawned by the
 * spell", so it scales by the ascension depth and, on killing the hero, awards the
 * death-from-friendly-magic badge.
 */
class ReclaimTrapReclaimedPortTest {

	/** Records what the spell hands it. Public with a public no-arg constructor: Reflection.newInstance. */
	public static class SpyTrap extends Trap {
		static boolean reclaimedAtActivation = false;
		static int activations = 0;

		@Override
		public void activate() {
			activations++;
			reclaimedAtActivation = reclaimed;
		}
	}

	private Hero prevHero;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = prevHero;
		SpyTrap.reclaimedAtActivation = false;
		SpyTrap.activations = 0;
	}

	@Test
	void theSpellFlagsTheTrapItFiresAsReclaimed() throws Exception {
		HeadlessLevel level = HeadlessLevel.install(8, 8);
		prevHero = Dungeon.hero;
		Dungeon.hero = new Hero();
		HeadlessLevel.at(Dungeon.hero, 1 * level.width() + 1);
		level.map[3 * level.width() + 3] = Terrain.EMPTY;

		ReclaimTrap spell = new ReclaimTrap();
		Field stored = ReclaimTrap.class.getDeclaredField("storedTrap");
		stored.setAccessible(true);
		stored.set(spell, SpyTrap.class);

		Ballistica bolt = new Ballistica(Dungeon.hero.pos, 5 * level.width() + 5, Ballistica.STOP_TARGET);
		spell.affectTarget(bolt, Dungeon.hero);

		assertEquals(1, SpyTrap.activations, "test setup: the stored trap must fire");
		assertTrue(SpyTrap.reclaimedAtActivation, "the trap must already be flagged reclaimed when it activates");
	}
}
