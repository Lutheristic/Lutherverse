package com.qsr.customspd.items.potions;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Char;
import com.qsr.customspd.actors.blobs.Blob;
import com.qsr.customspd.actors.blobs.Fire;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.buffs.Burning;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Port regression test for upstream SPD cf8e53f9a (v2.3.0): "fixed potion shatters not
 * cleansing fire/ooze outside of FOV".
 *
 * Potion.splash both cleans the landing cell (clears fire, strips Burning/Ooze from an ally
 * standing there) and draws the splash. Every shatter() only called it when the cell was in the
 * hero's field of view, so a potion shattering out of sight left fire and burning allies alone.
 * The clean-up now always runs; only the visual stays gated on FOV.
 *
 * HeadlessLevel leaves heroFOV all false, i.e. every cell is out of sight, which is the case
 * under test. The control checks that an enemy is not cleansed.
 */
class PotionShatterOutOfSightPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	private Hero prevHero;

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = prevHero;
	}

	private void setUpHero(HeadlessLevel level) {
		prevHero = Dungeon.hero;
		Dungeon.hero = new Hero();
		HeadlessLevel.at(Dungeon.hero, 1 * level.width() + 1);
	}

	private Rat ratAt(int cell, Char.Alignment alignment) {
		Rat rat = HeadlessLevel.at(new Rat(), cell);
		rat.alignment = alignment;
		StubSprite.attach(rat);
		return rat;
	}

	@Test
	void aPotionShatteringOutOfSightClearsFireOnItsCell() {
		HeadlessLevel level = HeadlessLevel.install(8, 8);
		setUpHero(level);
		int cell = 5 * level.width() + 5;
		Blob.seed(cell, 10, Fire.class);
		assertEquals(10, Blob.volumeAt(cell, Fire.class), "fixture: the fire is burning");

		new PotionOfToxicGas().shatter(cell);

		assertEquals(0, Blob.volumeAt(cell, Fire.class), "the shatter must put the fire out even out of sight");
	}

	@Test
	void aPotionShatteringOutOfSightExtinguishesAnAllyOnItsCell() {
		HeadlessLevel level = HeadlessLevel.install(8, 8);
		setUpHero(level);
		int cell = 5 * level.width() + 5;
		Rat ally = ratAt(cell, Char.Alignment.ALLY);
		Buff.affect(ally, Burning.class);
		assertNotNull(ally.buff(Burning.class), "fixture: the ally is burning");

		new PotionOfToxicGas().shatter(cell);

		assertNull(ally.buff(Burning.class), "the shatter must extinguish the ally even out of sight");
	}

	@Test
	void controlAnEnemyOnTheCellKeepsBurning() {
		HeadlessLevel level = HeadlessLevel.install(8, 8);
		setUpHero(level);
		int cell = 5 * level.width() + 5;
		Rat enemy = ratAt(cell, Char.Alignment.ENEMY);
		Buff.affect(enemy, Burning.class);

		new PotionOfToxicGas().shatter(cell);

		assertNotNull(enemy.buff(Burning.class), "only allies are cleansed");
	}
}
