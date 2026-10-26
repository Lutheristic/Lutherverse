package com.qsr.customspd.actors.buffs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.blobs.Blob;
import com.qsr.customspd.actors.blobs.ToxicGas;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import com.qsr.customspd.ui.BuffIndicator;
import com.watabou.utils.PathFinder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream a83be1d28 (v2.4.0): frost imbue chills for 3 turns instead of 2, and toxic imbue spreads
 * 54 units of gas a turn across the 9 cells around its bearer instead of 50 on one cell, piling
 * the share of any wall cell onto the centre. Once its turns run out it lingers for 5 more with
 * no icon, still keeping the bearer immune to the gas, but spreading none.
 */
class ImbueRebalancePortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private static Hero heroAt(HeadlessLevel level, int x, int y) {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		return HeadlessLevel.at(hero, y * level.width() + x);
	}

	private static int gasAt(int cell) {
		Blob gas = Dungeon.level.blobs.get(ToxicGas.class);
		return gas == null ? 0 : gas.cur[cell];
	}

	private static int totalGas() {
		Blob gas = Dungeon.level.blobs.get(ToxicGas.class);
		return gas == null ? 0 : gas.volume;
	}

	@Test
	void frostImbueChillsForThreeTurns() {
		HeadlessLevel level = HeadlessLevel.install(8, 8);
		Rat rat = HeadlessLevel.at(new Rat(), 3 * level.width() + 3);
		StubSprite.attach(rat);

		new FrostImbue().proc(rat);

		Chill chill = rat.buff(Chill.class);
		assertNotNull(chill);
		assertEquals(3f, chill.cooldown(), 0.001f);
	}

	@Test
	void toxicImbueSpreadsSixUnitsOnEachOfTheNineCells() {
		HeadlessLevel level = HeadlessLevel.install(9, 9);
		Hero hero = heroAt(level, 4, 4);
		Buff.affect(hero, ToxicImbue.class).set(ToxicImbue.DURATION);

		hero.buff(ToxicImbue.class).act();

		assertEquals(6, gasAt(hero.pos));
		for (int i : PathFinder.NEIGHBOURS8) {
			assertEquals(6, gasAt(hero.pos + i), "neighbour " + (hero.pos + i));
		}
		assertEquals(54, totalGas());
	}

	@Test
	void inACornerTheWallsShareGoesToTheCentre() {
		HeadlessLevel level = HeadlessLevel.install(9, 9);
		Hero hero = heroAt(level, 1, 1);
		Buff.affect(hero, ToxicImbue.class).set(ToxicImbue.DURATION);

		hero.buff(ToxicImbue.class).act();

		//five of the eight neighbours are wall
		assertEquals(6 + 5 * 6, gasAt(hero.pos));
		assertEquals(54, totalGas());
	}

	@Test
	void spentToxicImbueLingersFiveTurnsWithoutGasOrIcon() {
		HeadlessLevel level = HeadlessLevel.install(9, 9);
		Hero hero = heroAt(level, 4, 4);
		ToxicImbue imbue = Buff.affect(hero, ToxicImbue.class);
		imbue.set(1);

		imbue.act();
		assertEquals(54, totalGas(), "the last turn of the imbue still spreads gas");
		assertSame(hero.buff(ToxicImbue.class), imbue, "a spent imbue is not removed at once");

		for (int turn = 1; turn <= 4; turn++) {
			imbue.act();
			assertSame(imbue, hero.buff(ToxicImbue.class), "still lingering after " + turn + " more turns");
			assertSame(BuffIndicator.NONE, imbue.icon(), "a lingering imbue shows no icon");
			assertTrue(hero.isImmune(ToxicGas.class), "a lingering imbue keeps the gas immunity");
		}
		assertEquals(54, totalGas(), "a lingering imbue spreads no gas");

		imbue.act();
		assertNull(hero.buff(ToxicImbue.class), "gone five turns after running out");
	}
}
