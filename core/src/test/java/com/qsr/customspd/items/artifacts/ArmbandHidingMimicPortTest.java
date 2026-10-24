package com.qsr.customspd.items.artifacts;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Char;
import com.qsr.customspd.actors.buffs.Blindness;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Mimic;
import com.qsr.customspd.items.Item;
import com.qsr.customspd.scenes.CellSelector;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 9f230b495 (v2.4.0): a mimic lying in wait is NEUTRAL, which the armband's "only enemies
 * can be robbed" check refused with "no target" even though the mimic is a Mob it can steal from.
 */
class ArmbandHidingMimicPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() throws Exception {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
		setCurUser(null);
	}

	private static void setCurUser(Hero hero) throws Exception {
		Field f = Item.class.getDeclaredField("curUser");
		f.setAccessible(true);
		f.set(null, hero);
	}

	@Test
	void theArmbandCanBeAimedAtAHidingMimic() throws Exception {
		HeadlessLevel level = HeadlessLevel.install(8, 8);
		int center = 3 * level.width() + 3;

		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeadlessLevel.at(hero, center);
		Mimic mimic = HeadlessLevel.at(new Mimic(), center + 1);
		mimic.setLevel(0);
		assertSame(Char.Alignment.NEUTRAL, mimic.alignment, "fixture sanity: mimics hide as NEUTRAL");
		StubSprite.attachAll();

		MasterThievesArmband armband = new MasterThievesArmband();
		setCurUser(hero);
		Field f = MasterThievesArmband.class.getDeclaredField("targeter");
		f.setAccessible(true);
		CellSelector.Listener targeter = (CellSelector.Listener) f.get(armband);

		targeter.onSelect(center + 1);

		assertEquals(1, StubSprite.of(hero).attacks.size(), "the hero should swing at the hiding mimic");
		assertTrue(mimic.buff(Blindness.class) != null,
				"the steal attempt should have resolved and blinded the mimic");
	}
}
