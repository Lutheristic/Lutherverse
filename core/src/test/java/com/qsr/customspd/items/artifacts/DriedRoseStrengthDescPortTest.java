package com.qsr.customspd.items.artifacts;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.npcs.Ghost;
import com.qsr.customspd.items.weapon.melee.Sword;
import com.qsr.customspd.test.HeadlessGdx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 6e54fd04b (v2.4.0) with the key name 4bddef477 settled on: when the ghost carries
 * gear, the dried rose's description also states the ghost's strength.
 */
class DriedRoseStrengthDescPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() throws Exception {
		Ghost.Quest.reset();
		setQuestFlag("processed", false);
		Dungeon.hero = null;
	}

	private static void setQuestFlag(String name, boolean value) throws Exception {
		Field f = Ghost.Quest.class.getDeclaredField(name);
		f.setAccessible(true);
		f.setBoolean(null, value);
	}

	@Test
	void theDescriptionStatesTheGhostsStrengthOnlyWhenItHasGear() throws Exception {
		// A finished ghost quest keeps desc() off the (scene-dependent) "no quest" branch.
		setQuestFlag("spawned", true);
		setQuestFlag("processed", true);
		Dungeon.hero = new Hero();
		DriedRose rose = new DriedRose();
		assertFalse(rose.desc().contains("Ghost's strength"), "no gear, no gear lines");

		Field weapon = DriedRose.class.getDeclaredField("weapon");
		weapon.setAccessible(true);
		weapon.set(rose, new Sword());

		String desc = rose.desc();
		assertTrue(desc.contains("Ghost's weapon: "), desc);
		assertTrue(desc.contains("Ghost's strength: " + rose.ghostStrength()), desc);
	}
}
