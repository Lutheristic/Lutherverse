package com.qsr.customspd.actors.hero;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.test.StubSprite;
import com.watabou.noosa.particles.Emitter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Hearty meal is one tier: 1+2*points HP when the Warrior eats at or below 30% health, nothing
 * above it (upstream 375368403 replaced the 50%/25% two-tier heal).
 */
class HeartyMealThresholdPortTest {

	/** A stub whose emitter exists, since hearty meal bursts healing particles. */
	private static final class BurstSprite extends StubSprite {
		@Override
		public Emitter emitter() {
			return new Emitter();
		}
	}

	private HeadlessRun run;
	private Hero hero;

	@BeforeEach
	void startRun() {
		run = HeadlessRun.start(20240327L);
		run.enterFirstLevel();
		hero = Dungeon.hero;
		BurstSprite sprite = new BurstSprite();
		sprite.ch = hero;
		hero.sprite = sprite;
		hero.HP = hero.HT = 20;
	}

	@AfterEach
	void closeRun() {
		run.close();
	}

	@Test
	void heartyMealHealsThreeAtOrBelowThirtyPercent() {
		hero.upgradeTalent(Talent.HEARTY_MEAL);
		hero.HP = 6; // exactly 30% of 20
		Talent.onFoodEaten(hero, 0f, null);
		assertEquals(9, hero.HP, "+1 hearty meal heals 3 HP at 30% health");
	}

	@Test
	void heartyMealHealsFiveAtTwoPoints() {
		hero.upgradeTalent(Talent.HEARTY_MEAL);
		hero.upgradeTalent(Talent.HEARTY_MEAL);
		hero.HP = 2;
		Talent.onFoodEaten(hero, 0f, null);
		assertEquals(7, hero.HP, "+2 hearty meal heals 5 HP");
	}

	@Test
	void heartyMealDoesNothingAboveThirtyPercent() {
		hero.upgradeTalent(Talent.HEARTY_MEAL);
		hero.HP = 7; // 35%: the old 50% tier healed 2 here
		Talent.onFoodEaten(hero, 0f, null);
		assertEquals(7, hero.HP, "no healing above 30% health");
	}
}
