package com.qsr.customspd.items;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.buffs.LockedFloor;
import com.qsr.customspd.actors.buffs.Regeneration;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.items.armor.Armor;
import com.qsr.customspd.test.HeadlessRun;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream b0c29ec80 (v2.1.4): the broken seal's shield regrows only while passive regeneration
 * is on, so a boss floor whose lock has run out stops it just as it stops hit point regen.
 */
class WarriorShieldRegenPortTest {

	private HeadlessRun run;
	private Hero hero;
	private BrokenSeal.WarriorShield shield;

	@BeforeEach
	void startRun() {
		run = HeadlessRun.start(20230630L);
		run.enterFirstLevel();
		hero = Dungeon.hero;

		Armor armor = hero.belongings.armor;
		assertNotNull(armor, "test setup: the warrior starts in armor");
		if (armor.checkSeal() == null) armor.affixSeal(new BrokenSeal());
		BrokenSeal.WarriorShield old = hero.buff(BrokenSeal.WarriorShield.class);
		if (old != null) old.detach();
		shield = Buff.affect(hero, BrokenSeal.WarriorShield.class);
		shield.setArmor(armor);
		assertTrue(shield.maxShield() > 0, "test setup: sealed armor gives the shield a maximum");
		assertEquals(0, shield.shielding(), "test setup: the shield starts empty");
	}

	@AfterEach
	void closeRun() {
		run.close();
	}

	@Test
	void theShieldRegrowsWhileRegenIsOn() {
		assertTrue(Regeneration.regenOn(), "test setup: nothing pauses regen");
		for (int i = 0; i < 31; i++) shield.act();
		assertEquals(1, shield.shielding(), "a point of shield every 30 turns");
	}

	@Test
	void theShieldStaysDownWhileRegenIsOff() {
		Buff.affect(hero, LockedFloor.class).removeTime(100);
		assertFalse(Regeneration.regenOn(), "test setup: the floor lock has run out");
		for (int i = 0; i < 91; i++) shield.act();
		assertEquals(0, shield.shielding(), "no shield regrows while regen is off");
	}
}
