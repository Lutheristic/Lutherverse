package com.qsr.customspd.actors.buffs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.test.HeadlessRun;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The Amulet-ascension speed debuff: "hero speed is halved and capped at 1x at 6+ stacks". The
 * comparison used {@code > 6}, so exactly 6 stacks (the documented threshold) did nothing
 * (upstream 38d432207).
 */
class AscensionHeroSpeedPortTest {

	private HeadlessRun run;
	private AscensionChallenge challenge;

	@BeforeEach
	void startRun() throws Exception {
		run = HeadlessRun.start(20240519L);
		run.enterFirstLevel();
		Hero hero = Dungeon.hero;
		challenge = Buff.affect(hero, AscensionChallenge.class);
		assertEquals(challenge, hero.buff(AscensionChallenge.class), "test setup: the challenge must be attached");
	}

	@AfterEach
	void closeRun() {
		run.close();
	}

	private void setStacks(float stacks) throws Exception {
		Field f = AscensionChallenge.class.getDeclaredField("stacks");
		f.setAccessible(true);
		f.setFloat(challenge, stacks);
	}

	@Test
	void exactlySixStacksHalvesAndCapsHeroSpeed() throws Exception {
		setStacks(6f);
		assertEquals(1f, AscensionChallenge.modifyHeroSpeed(3f), "6 stacks caps a 3x speed at 1x");
		assertEquals(0.5f, AscensionChallenge.modifyHeroSpeed(1f), "6 stacks halves a 1x speed");
	}

	@Test
	void belowSixStacksLeavesHeroSpeedAlone() throws Exception {
		setStacks(5.5f);
		assertEquals(3f, AscensionChallenge.modifyHeroSpeed(3f));
	}

	@Test
	void aboveSixStacksStillHalvesAndCapsHeroSpeed() throws Exception {
		setStacks(7f);
		assertEquals(1f, AscensionChallenge.modifyHeroSpeed(3f));
	}
}
