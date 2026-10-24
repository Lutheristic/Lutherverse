package com.qsr.customspd.items.weapon.melee;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port regression for upstream a5f740ab7 (v2.1.2, "fixed rapier lunge updating visibility after
 * attack not before"). The lunge moved the hero to the landing cell and attacked from there
 * without recomputing the hero's field of view, so the attack (and anything it triggered)
 * resolved against the view from the cell the hero left. It now observes right after landing.
 */
class RapierLungeObservePortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	@Test
	void theHerosViewIsRecomputedAtTheLandingCellBeforeTheAttack() {
		HeadlessLevel level = HeadlessLevel.install(10, 10);
		Hero hero = new Hero();
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		int start = 4 * level.width() + 2;
		HeadlessLevel.at(hero, start);
		int enemyCell = 4 * level.width() + 4;
		Rat rat = HeadlessLevel.at(new Rat(), enemyCell);
		StubSprite.attach(rat);
		rat.HP = rat.HT = 1000; // survives, so the attack's own side effects stay out of the way

		// A stale view: nothing visible, as if it had been computed before the hero moved.
		Arrays.fill(level.heroFOV, false);
		// The lunge only skips the "no target" check for a target outside the hero's view.
		try {
			Rapier.lungeAbility(hero, enemyCell, 1f, 0, new Rapier());
		} catch (NullPointerException e) {
			// The attack callback targets the AttackIndicator, a HUD element the headless run does
			// not have. That happens after landing, which is all this test reads; only that NPE
			// is tolerated, one thrown anywhere else is a real failure.
			StackTraceElement top = e.getStackTrace()[0];
			if (!top.getClassName().endsWith(".AttackIndicator") || !"target".equals(top.getMethodName())) {
				throw e;
			}
		}

		int landing = 4 * level.width() + 3;
		assertEquals(landing, hero.pos, "fixture: the hero lunged one cell towards the rat");
		assertTrue(level.heroFOV[landing],
				"upstream a5f740ab7: the view is recomputed from the landing cell");
	}
}
