package com.qsr.customspd.actors.hero;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.buffs.LostInventory;
import com.qsr.customspd.items.KindOfWeapon;
import com.qsr.customspd.test.HeadlessRun;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 2466974a7: Belongings caches whether the hero's inventory is lost instead of looking
 * up the LostInventory buff on every call. The cache has to follow the buff: set when it
 * attaches, cleared when it detaches, and set again when a saved hero's buffs are restored.
 */
class LostInventoryFlagPortTest {

	private HeadlessRun run;

	@BeforeEach
	void startRun() {
		run = HeadlessRun.start(20240205L);
		run.enterFirstLevel();
	}

	@AfterEach
	void closeRun() {
		run.close();
	}

	@Test
	void theFlagFollowsTheBuffThroughAttachAndDetach() {
		Hero hero = Dungeon.hero;
		KindOfWeapon weapon = hero.belongings.weapon;
		assertNotNull(weapon, "test setup: the warrior starts with a weapon");
		assertFalse(weapon.keptThoughLostInvent, "test setup: the starting weapon is not kept");
		assertMatchesBuff(hero);
		assertFalse(hero.belongings.lostInventory());
		assertSame(weapon, hero.belongings.weapon());

		Buff.affect(hero, LostInventory.class);
		assertMatchesBuff(hero);
		assertTrue(hero.belongings.lostInventory());
		assertNull(hero.belongings.weapon(), "a lost inventory hides the weapon");

		hero.buff(LostInventory.class).detach();
		assertMatchesBuff(hero);
		assertFalse(hero.belongings.lostInventory());
		assertSame(weapon, hero.belongings.weapon(), "the weapon is back once the buff is gone");
	}

	@Test
	void theFlagIsRestoredWithTheSavedBuff() {
		Hero before = Dungeon.hero;
		Buff.affect(before, LostInventory.class);
		run.saveGame();

		run.loadGame();
		Hero after = Dungeon.hero;
		assertNotSame(before, after, "test setup: the load built a new hero");
		assertNotNull(after.buff(LostInventory.class), "test setup: the buff was saved");
		assertMatchesBuff(after);
		assertTrue(after.belongings.lostInventory());
		assertNull(after.belongings.weapon(), "the restored hero's weapon stays lost");

		after.buff(LostInventory.class).detach();
		assertMatchesBuff(after);
		run.saveGame();
		run.loadGame();
		assertNull(Dungeon.hero.buff(LostInventory.class));
		assertMatchesBuff(Dungeon.hero);
		assertNotNull(Dungeon.hero.belongings.weapon(), "a hero saved without the buff has its weapon");
	}

	private static void assertMatchesBuff(Hero hero) {
		assertEquals(hero.buff(LostInventory.class) != null, hero.belongings.lostInventory(),
				"the cached flag matches the buff");
	}
}
