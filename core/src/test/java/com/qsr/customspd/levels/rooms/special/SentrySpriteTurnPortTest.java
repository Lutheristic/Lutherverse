package com.qsr.customspd.levels.rooms.special;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream c5fac25ed: a sentry is a fixed statue, so its sprite does not flip to face whatever it
 * looks at or shoots.
 */
class SentrySpriteTurnPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
	}

	@Test
	void aSentryDoesNotTurnToFaceItsTarget() {
		HeadlessLevel.install(8, 8);
		int w = Dungeon.level.width();
		int from = 3 * w + 4;

		SentryRoom.SentrySprite sprite = new SentryRoom.SentrySprite();
		sprite.flipHorizontal = false;
		sprite.turnTo(from, from - 2);
		assertFalse(sprite.flipHorizontal, "a target to the left leaves the sentry facing right");

		sprite.flipHorizontal = true;
		sprite.turnTo(from, from + 2);
		assertTrue(sprite.flipHorizontal, "a target to the right leaves the sentry facing left");
	}
}
