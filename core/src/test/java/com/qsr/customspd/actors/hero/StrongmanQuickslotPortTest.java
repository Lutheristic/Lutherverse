package com.qsr.customspd.actors.hero;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.scenes.GameScene;
import com.qsr.customspd.test.HeadlessRun;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Strongman raises effective strength, which changes item displays (strength-penalty colouring,
 * quickslot text), so upgrading it must refresh them (upstream 811a3b505).
 */
class StrongmanQuickslotPortTest {

	private HeadlessRun run;
	private Hero hero;

	@BeforeEach
	void startRun() {
		run = HeadlessRun.start(20240428L);
		run.enterFirstLevel();
		hero = Dungeon.hero;
	}

	@AfterEach
	void closeRun() {
		run.close();
	}

	@Test
	void upgradingStrongmanRefreshesItemDisplays() {
		GameScene.updateItemDisplays = false;
		hero.upgradeTalent(Talent.STRONGMAN);
		assertTrue(GameScene.updateItemDisplays, "strongman changes strength, so item displays must refresh");
	}

	@Test
	void upgradingAnUnrelatedTalentDoesNotRefreshItemDisplays() {
		GameScene.updateItemDisplays = false;
		hero.upgradeTalent(Talent.HEARTY_MEAL);
		assertFalse(GameScene.updateItemDisplays, "control: only the listed talents refresh item displays");
	}
}
