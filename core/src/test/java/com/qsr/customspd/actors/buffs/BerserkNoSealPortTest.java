package com.qsr.customspd.actors.buffs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.items.BrokenSeal.WarriorShield;
import com.qsr.customspd.test.HeadlessRun;
import com.qsr.customspd.test.StubSprite;
import com.watabou.utils.Bundle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A warrior who unequips the broken seal keeps the WarriorShield buff (with a max shield of 0), so
 * "has the buff" let the berserker rage with nothing to rage with. The rage action now also
 * needs the shield to have a maximum above zero (upstream e58bd78e0).
 */
class BerserkNoSealPortTest {

	private HeadlessRun run;
	private Hero hero;

	@BeforeEach
	void startRun() {
		run = HeadlessRun.start(20231121L);
		run.enterFirstLevel();
		hero = Dungeon.hero;
		StubSprite.attach(hero);
	}

	@AfterEach
	void closeRun() {
		run.close();
	}

	private static String stateOf(Berserk berserk) {
		Bundle b = new Bundle();
		berserk.storeInBundle(b);
		return b.getString("state");
	}

	@Test
	void ragingWithoutASealEquippedDoesNotStartTheRage() {
		Buff.affect(hero, WarriorShield.class); // armor never set: max shield 0, as with the seal unequipped
		Berserk berserk = Buff.affect(hero, Berserk.class);
		assertEquals(0, hero.buff(WarriorShield.class).maxShield(), "test setup: no seal means no max shield");
		assertEquals("NORMAL", stateOf(berserk), "test setup");

		berserk.doAction();

		assertEquals("NORMAL", stateOf(berserk), "no seal, no rage");
	}
}
