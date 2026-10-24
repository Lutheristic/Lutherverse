package com.qsr.customspd.items.artifacts;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.items.Item;
import com.qsr.customspd.plants.Rotberry;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Upstream 5a70a482a (v2.1.2): the sandals of nature plant a seed only on a cell the hero can
 * see, at most 3 cells away - not on any cell within 3 steps of walking distance, which let
 * the ability reach around corners and into the dark. Out-of-range picks cost nothing.
 */
class SandalsOfNatureRangePortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() throws Exception {
		setCurUser(null);
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private static void setCurUser(Hero hero) throws Exception {
		Field f = Item.class.getDeclaredField("curUser");
		f.setAccessible(true);
		f.set(null, hero);
	}

	private SandalsOfNature ready(Hero hero) throws Exception {
		HeadlessLevel.install(8, 8);
		Dungeon.hero = hero;
		hero.pos = 3 * 8 + 2;
		setCurUser(hero);
		SandalsOfNature sandals = new SandalsOfNature();
		sandals.curSeedEffect = Rotberry.Seed.class;
		sandals.charge = 100;
		return sandals;
	}

	@Test
	void anUnseenCellIsOutOfRangeEvenWhenItIsCloseToWalkTo() throws Exception {
		Hero hero = new Hero();
		SandalsOfNature sandals = ready(hero);
		int cell = hero.pos + 2;
		Dungeon.level.heroFOV[cell] = false;

		assertDoesNotThrow(() -> sandals.cellSelector.onSelect(cell),
				"an unseen cell is refused with the out-of-range message, not planted into");
		assertEquals(100, sandals.charge, "a refused pick costs no charge");
	}

	@Test
	void aSeenCellMoreThanThreeAwayIsOutOfRange() throws Exception {
		Hero hero = new Hero();
		SandalsOfNature sandals = ready(hero);
		int cell = hero.pos + 4;
		Dungeon.level.heroFOV[cell] = true;

		assertDoesNotThrow(() -> sandals.cellSelector.onSelect(cell));
		assertEquals(100, sandals.charge, "a refused pick costs no charge");
	}
}
