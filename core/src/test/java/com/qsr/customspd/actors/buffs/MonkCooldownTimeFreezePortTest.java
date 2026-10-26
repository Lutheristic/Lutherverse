package com.qsr.customspd.actors.buffs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.test.HeadlessRun;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Monk ability cooldowns were a flat 5 turns counted from when the ability's own action ended
 * (upstream a768e9d39). v2.5.0 (bf56446d0) removed them but kept the cooldown field and its
 * icon, so a cooldown loaded from an older save still counts down on the buff icon.
 */
class MonkCooldownTimeFreezePortTest {

	private HeadlessRun run;
	private Hero hero;

	@BeforeEach
	void startRun() {
		run = HeadlessRun.start(20231011L);
		run.enterFirstLevel();
		hero = Dungeon.hero;
		hero.timeToNow();
	}

	@AfterEach
	void closeRun() {
		run.close();
	}

	@Test
	void iconFadePercentIsClampedToOne() {
		MonkEnergy energy = Buff.affect(hero, MonkEnergy.class);
		energy.cooldown = 8;
		assertEquals(1f, energy.iconFadePercent(), 0.001f);
	}
}
