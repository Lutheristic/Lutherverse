package com.qsr.customspd.actors.buffs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Actor;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.test.HeadlessRun;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Monk ability cooldowns are now a flat 5 turns counted from when the ability's own action ends
 * (the hero's remaining action time is added), instead of per-ability constants that baked in
 * how long the ability's action took. That keeps the cooldown honest when time freeze or the
 * like eats part of the action (upstream a768e9d39).
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
	void abilityCooldownIsFiveTurnsPlusTheHeroActionTimeStillPending() {
		MonkEnergy energy = Buff.affect(hero, MonkEnergy.class);
		energy.energy = 10;
		hero.spendConstant(Actor.TICK * 3);
		assertEquals(3f, hero.cooldown(), 0.001f, "test setup: three ticks of action left");

		energy.abilityUsed(new MonkEnergy.MonkAbility.Dash());

		assertEquals(8, energy.cooldown, "5 base + 3 pending ticks");
	}

	@Test
	void everyAbilityReportsABaseCooldownOfFive() {
		assertEquals(5, new MonkEnergy.MonkAbility.Focus().cooldown());
		assertEquals(5, new MonkEnergy.MonkAbility.Dash().cooldown());
		assertEquals(5, new MonkEnergy.MonkAbility.DragonKick().cooldown());
		assertEquals(5, new MonkEnergy.MonkAbility.Meditate().cooldown());
		assertEquals(5, new MonkEnergy.MonkAbility.Flurry().cooldown());
	}

	@Test
	void iconFadePercentIsClampedToOne() {
		MonkEnergy energy = Buff.affect(hero, MonkEnergy.class);
		energy.cooldown = 8;
		assertEquals(1f, energy.iconFadePercent(), 0.001f);
	}
}
