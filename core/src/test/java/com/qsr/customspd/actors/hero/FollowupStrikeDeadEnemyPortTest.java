package com.qsr.customspd.actors.hero;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.items.weapon.missiles.ThrowingKnife;
import com.qsr.customspd.test.HeadlessRun;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Followup Strike armed itself on a missile hit even when that hit killed the target, leaving a
 * tracker that could never be consumed (upstream d912818f5).
 */
class FollowupStrikeDeadEnemyPortTest {

	private HeadlessRun run;
	private Hero hero;

	@BeforeEach
	void startRun() {
		run = HeadlessRun.start(20231008L);
		run.enterFirstLevel();
		hero = Dungeon.hero;
		hero.talents.get(0).put(Talent.FOLLOWUP_STRIKE, 2);
		hero.belongings.thrownWeapon = new ThrowingKnife();
	}

	@AfterEach
	void closeRun() {
		run.close();
	}

	@Test
	void aMissileHitThatKillsTheTargetLeavesNoTracker() {
		Rat rat = new Rat();
		rat.HP = 0;

		Talent.onAttackProc(hero, rat, 3);

		assertNull(hero.buff(Talent.FollowupStrikeTracker.class), "a dead target cannot be followed up on");
	}

	@Test
	void aMissileHitOnALivingTargetStillArmsTheTracker() {
		Rat rat = new Rat();

		Talent.onAttackProc(hero, rat, 3);

		Talent.FollowupStrikeTracker tracker = hero.buff(Talent.FollowupStrikeTracker.class);
		assertNotNull(tracker, "control: a living enemy is still marked");
		assertEquals(rat.id(), tracker.object);
	}
}
