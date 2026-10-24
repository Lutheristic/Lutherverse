package com.qsr.customspd.items.weapon.enchantments;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.items.weapon.melee.WornShortsword;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port regression for upstream 89fe77784 (v2.4.0, "fixed cases where lucky enchant wouldn't proc
 * on killing blows"). Lucky only marked the defender when the hit looked lethal at proc time
 * (HP <= damage), which misses blows that kill after the proc (several attacks in one action,
 * e.g. gladiator fury). It now marks on every proc and relies on LuckProc expiring next turn;
 * a failed proc clears a mark left by an earlier hit in the same action.
 */
class LuckyProcPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private Rat rat;
	private Hero hero;

	private void setup() {
		HeadlessLevel level = HeadlessLevel.install(10, 10);
		hero = new Hero();
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, 4 * level.width() + 4);
		rat = HeadlessLevel.at(new Rat(), 4 * level.width() + 5);
		StubSprite.attach(rat);
		rat.HP = rat.HT = 1000;
	}

	@Test
	void aProcOnANonLethalHitStillMarksTheDefender() {
		setup();
		Lucky lucky = new Lucky();
		// +0 Lucky procs about 1 hit in 10; a damage-1 hit on a 1000 HP rat is never lethal.
		WornShortsword sword = new WornShortsword();
		for (int i = 0; i < 300 && rat.buff(Lucky.LuckProc.class) == null; i++) {
			lucky.proc(sword, hero, rat, 1);
		}
		assertNotNull(rat.buff(Lucky.LuckProc.class),
				"upstream 89fe77784: the mark no longer depends on the hit looking lethal");
	}

	@Test
	void aSureProcOnANonLethalHitMarksTheDefender() {
		setup();
		new Lucky().proc(1f, 5, hero, rat, 1);
		assertNotNull(rat.buff(Lucky.LuckProc.class));
	}

	@Test
	void aFailedProcClearsAnEarlierMark() {
		setup();
		Lucky lucky = new Lucky();
		lucky.proc(1f, 5, hero, rat, 1);
		assertNotNull(rat.buff(Lucky.LuckProc.class), "fixture: marked");
		lucky.proc(0f, 5, hero, rat, 1);
		assertNull(rat.buff(Lucky.LuckProc.class),
				"upstream 89fe77784: a failed proc overrides a prior successful one");
	}

	@Test
	void theWeaponOverloadAlsoClearsAnEarlierMarkOnAFailedProc() {
		setup();
		Lucky lucky = new Lucky();
		lucky.proc(1f, 5, hero, rat, 1);
		WornShortsword sword = new WornShortsword();
		// A 10% proc chance: within 300 tries the mark must be cleared by a failed proc at least
		// once, and a later success re-marks it, so look for the first failure directly.
		boolean cleared = false;
		for (int i = 0; i < 300 && !cleared; i++) {
			lucky.proc(sword, hero, rat, 1);
			cleared = rat.buff(Lucky.LuckProc.class) == null;
		}
		assertTrue(cleared);
	}
}
