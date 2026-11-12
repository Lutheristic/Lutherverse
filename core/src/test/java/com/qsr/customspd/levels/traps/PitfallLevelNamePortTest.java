package com.qsr.customspd.levels.traps;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.SaveRoundtrip;
import com.qsr.customspd.test.StubSprite;
import com.watabou.utils.Bundle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 3d01bc8c2 (v2.1.4): a pitfall trap's delayed pit remembers which level it was set
 * on, so it cannot collapse the floor of a different level at the same depth. Upstream keys it
 * on depth and branch; CPDU has no branch, and two pack levels can share a depth, so the pit
 * keys on levelName. A pit from a save written before that has only its depth, and keeps
 * going by depth alone.
 */
class PitfallLevelNamePortTest {

	private static final int W = 8;
	private static final int PIT = 3 * W + 3;

	private int prevDepth;
	private String prevLevelName;
	private Hero prevHero;
	private Hero hero;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@BeforeEach
	void setUp() {
		prevDepth = Dungeon.depth;
		prevLevelName = Dungeon.levelName;
		prevHero = Dungeon.hero;
		HeadlessLevel.install(W, 8);
		hero = new Hero();
		StubSprite.attach(hero);
		Dungeon.hero = HeadlessLevel.at(hero, W + 1);
		Dungeon.depth = 3;
		Dungeon.levelName = "lower_halls";
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.depth = prevDepth;
		Dungeon.levelName = prevLevelName;
		Dungeon.hero = prevHero;
	}

	private PitfallTrap.DelayedPit pit(int depth, String levelName) {
		PitfallTrap.DelayedPit pit = Buff.append(hero, PitfallTrap.DelayedPit.class, 1);
		pit.pos = PIT;
		pit.depth = depth;
		pit.levelName = levelName;
		return pit;
	}

	/**
	 * Acts the pit. A pit that collapses starts by bursting particles over its cells through
	 * CellEmitter, which needs a live GameScene, so headless a collapse shows as that NPE.
	 */
	private static boolean collapses(PitfallTrap.DelayedPit pit) {
		try {
			assertTrue(pit.act(), "a pit that leaves the floor alone lets the turn go on");
			return false;
		} catch (NullPointerException e) {
			if (!e.getStackTrace()[0].getClassName().endsWith(".CellEmitter")) {
				throw e;
			}
			return true;
		}
	}

	@Test
	void aPitDoesNotCollapseAnotherLevelAtTheSameDepth() {
		PitfallTrap.DelayedPit pit = pit(3, "upper_halls");

		assertFalse(collapses(pit), "the pit was set on another level that shares this depth");
		assertNull(hero.buff(PitfallTrap.DelayedPit.class), "the pit is used up either way");
	}

	@Test
	void aPitCollapsesTheLevelItWasSetOn() {
		assertTrue(collapses(pit(3, "lower_halls")));
	}

	@Test
	void aPitStillIgnoresOtherDepths() {
		assertFalse(collapses(pit(4, "lower_halls")));
	}

	@Test
	void aSavedPitKeepsItsLevel() throws Exception {
		PitfallTrap.DelayedPit restored = SaveRoundtrip.of(pit(3, "upper_halls"));

		assertEquals(PIT, restored.pos);
		assertEquals(3, restored.depth);
		assertEquals("upper_halls", restored.levelName);
	}

	@Test
	void aPitFromAnOlderSaveGoesByDepthAlone() throws Exception {
		PitfallTrap.DelayedPit original = pit(3, "upper_halls");
		Bundle saved = new Bundle();
		saved.put("obj", original);
		// a save from before pits kept their level holds only pos and depth
		saved.getBundle("obj").remove("level");
		assertFalse(saved.getBundle("obj").contains("level"), "the fixture must really look like an old save");
		original.detach();

		PitfallTrap.DelayedPit restored = (PitfallTrap.DelayedPit) SaveRoundtrip.writeRead(saved).get("obj");
		assertEquals(PIT, restored.pos);
		assertEquals(3, restored.depth);
		restored.attachTo(hero);

		assertTrue(collapses(restored), "an old pit at the hero's depth collapses as it did before");

		PitfallTrap.DelayedPit deeper = (PitfallTrap.DelayedPit) SaveRoundtrip.writeRead(saved).get("obj");
		Dungeon.depth = 4;
		deeper.attachTo(hero);
		assertFalse(collapses(deeper), "and still leaves other depths alone");
	}
}
