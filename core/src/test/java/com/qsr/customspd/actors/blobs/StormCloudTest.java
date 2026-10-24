package com.qsr.customspd.actors.blobs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.mobs.Elemental;
import com.qsr.customspd.levels.Terrain;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * StormCloud.evolve indexes the fire blob and Actor.findChar by the CELL, not by the column
 * counter of its x loop (upstream 1c01cf76e). The fixture cell is deliberately off row 0 so the
 * column index (5) and the cell index (29) differ: the old code cleared/looked at cell 5.
 */
class StormCloudTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	@Test
	void stormCloudPutsOutFireAndSoaksTheCellItCoversNotTheColumnIndex() {
		HeadlessLevel level = HeadlessLevel.install(8, 8);
		int cell = 3 * level.width() + 5;

		Fire fire = Blob.seed(cell, 4, Fire.class, level);
		assertEquals(4, fire.cur[cell], "fixture sanity: the fire starts on the cloud's cell");

		StormCloud cloud = Blob.seed(cell, 3, StormCloud.class, level);
		cloud.act();

		assertEquals(0, fire.cur[cell], "rain on a burning cell must clear its fire");
		assertEquals(Terrain.WATER, level.map[cell], "the cell under the cloud becomes water");
	}

	@Test
	void stormCloudDamagesAFieryCharStandingInIt() {
		HeadlessLevel level = HeadlessLevel.install(8, 8);
		int cell = 3 * level.width() + 5;

		Elemental.FireElemental inCloud = HeadlessLevel.at(new Elemental.FireElemental(), cell);
		StubSprite.attach(inCloud);
		int before = inCloud.HP;

		StormCloud cloud = Blob.seed(cell, 3, StormCloud.class, level);
		cloud.act();

		assertTrue(inCloud.HP < before, "a fiery enemy standing in the storm cloud must be damaged, HP "
				+ before + " -> " + inCloud.HP);
	}
}
