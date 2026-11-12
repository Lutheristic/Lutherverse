package com.qsr.customspd.items;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.levels.Terrain;
import com.qsr.customspd.test.HeadlessRun;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 345b6fe79 (Dewdrop hunk): a hero at full health still picks up dew lying on stairs,
 * and the special-floor entrance tile counts as stairs, so dew on it does not get stuck there.
 */
class DewdropSpecialStairsPortTest {

	private HeadlessRun run;
	private Hero hero;

	@BeforeEach
	void startRun() {
		run = HeadlessRun.start(20240419L);
		run.enterFirstLevel();
		hero = Dungeon.hero;
		Waterskin flask = hero.belongings.getItem(Waterskin.class);
		if (flask != null) flask.detachAll(hero.belongings.backpack);
		assertNull(hero.belongings.getItem(Waterskin.class), "test setup: no waterskin to take the dew");
		hero.HP = hero.HT;
	}

	@AfterEach
	void closeRun() {
		run.close();
	}

	private boolean picksUpDewOn(int terrain) {
		int cell = hero.pos;
		Dungeon.level.map[cell] = terrain;
		return new Dewdrop().doPickUp(hero, cell);
	}

	@Test
	void aFullHealthHeroLeavesDewOnPlainFloor() {
		assertFalse(picksUpDewOn(Terrain.EMPTY));
	}

	@Test
	void aFullHealthHeroPicksUpDewOnTheSpecialEntrance() {
		assertTrue(picksUpDewOn(Terrain.ENTRANCE));
		assertTrue(picksUpDewOn(Terrain.ENTRANCE_SP));
	}
}
