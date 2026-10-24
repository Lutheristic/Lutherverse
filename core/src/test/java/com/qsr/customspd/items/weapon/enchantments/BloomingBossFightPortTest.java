package com.qsr.customspd.items.weapon.enchantments;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.buffs.LockedFloor;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.levels.Terrain;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Port regression for upstream d5ef9fef1 (v2.5.0, "blooming now makes furrowed grass when
 * turtling in boss fights"). Blooming always planted high grass. Once a boss arena's LockedFloor
 * has switched passive regeneration off (Regeneration.regenOn() false), it plants furrowed grass
 * instead, so a hero standing still cannot keep growing high grass around herself for the fight.
 */
class BloomingBossFightPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private Hero hero;
	private Rat rat;
	private HeadlessLevel level;

	private void setup() {
		level = HeadlessLevel.install(10, 10);
		hero = new Hero();
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, 4 * level.width() + 4);
		rat = HeadlessLevel.at(new Rat(), 4 * level.width() + 5);
		StubSprite.attach(rat);
		rat.HP = rat.HT = 1000;
	}

	/**
	 * A sure proc of strength 10, which plants exactly one patch, on the defender's own cell.
	 * The patch's leaf burst asks CellEmitter.get for a scene emitter the headless run does not
	 * have; that happens after the terrain is set, which is all these tests read. Only that NPE
	 * is tolerated; one thrown anywhere else is a real failure.
	 */
	private void bloom() {
		try {
			new Blooming().proc(1f, 10, hero, rat, 1);
		} catch (NullPointerException e) {
			StackTraceElement top = e.getStackTrace()[0];
			if (!top.getClassName().endsWith(".CellEmitter") || !"get".equals(top.getMethodName())) {
				throw e;
			}
		}
	}

	@Test
	void outsideABossLockdownBloomingPlantsHighGrass() {
		setup();
		assertEquals(Terrain.EMPTY, level.map[rat.pos], "fixture: bare floor under the rat");

		bloom();

		assertEquals(Terrain.HIGH_GRASS, level.map[rat.pos]);
	}

	@Test
	void duringABossLockdownWithRegenerationOffBloomingPlantsFurrowedGrass() {
		setup();
		Buff.affect(hero, LockedFloor.class).removeTime(100); // the 50-turn grace is long gone

		bloom();

		assertEquals(Terrain.FURROWED_GRASS, level.map[rat.pos],
				"upstream d5ef9fef1: no high grass while LockedFloor has switched regeneration off");
	}

	@Test
	void duringTheGraceTimeOfABossLockdownBloomingStillPlantsHighGrass() {
		setup();
		Buff.affect(hero, LockedFloor.class); // fresh: regeneration is still on

		bloom();

		assertEquals(Terrain.HIGH_GRASS, level.map[rat.pos]);
	}
}
