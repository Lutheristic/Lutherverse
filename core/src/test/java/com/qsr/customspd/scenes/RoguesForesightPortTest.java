package com.qsr.customspd.scenes;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Talent;
import com.qsr.customspd.messages.Languages;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.test.HeadlessGdx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Rogue's foresight notices a level's secret 75% of the time at +1 and always at +2 (upstream
 * b7051a219), up from 60%/90%. The roll is seeded by the level, so each level is checked once.
 */
class RoguesForesightPortTest {

	private static final int LEVELS = 400;

	private long seed;
	private int depth;
	private String levelName;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
		Messages.setup(Languages.ENGLISH);
	}

	@BeforeEach
	void save() {
		seed = Dungeon.seed;
		depth = Dungeon.depth;
		levelName = Dungeon.levelName;
	}

	@AfterEach
	void restore() {
		Dungeon.seed = seed;
		Dungeon.depth = depth;
		Dungeon.levelName = levelName;
	}

	/** How many of LEVELS different levels the rogue notices a secret on. */
	private static int noticed(int points) {
		int count = 0;
		for (int i = 0; i < LEVELS; i++) {
			Dungeon.seed = 7919L * (i + 1);
			Dungeon.depth = 1 + i % 25;
			Dungeon.levelName = String.valueOf(Dungeon.depth);
			if (GameScene.rogueNoticesSecret(points)) count++;
		}
		return count;
	}

	@Test
	void twoPointsAlwaysNotice() {
		assertEquals(LEVELS, noticed(2));
	}

	@Test
	void onePointNoticesThreeLevelsInFour() {
		int n = noticed(1);
		assertTrue(n >= LEVELS * 0.70 && n <= LEVELS * 0.80, n + " of " + LEVELS);
	}

	@Test
	void theSameLevelGivesTheSameAnswer() {
		Dungeon.seed = 12345L;
		Dungeon.depth = 4;
		Dungeon.levelName = "4";
		boolean first = GameScene.rogueNoticesSecret(1);
		for (int i = 0; i < 20; i++) {
			assertEquals(first, GameScene.rogueNoticesSecret(1));
		}
	}

	@Test
	void theTalentTextMatches() {
		String desc = Talent.ROGUES_FORESIGHT.desc();
		assertTrue(desc.contains("_75% chance to notice_"), desc);
		assertTrue(desc.contains("_100% chance to notice_"), desc);
	}
}
