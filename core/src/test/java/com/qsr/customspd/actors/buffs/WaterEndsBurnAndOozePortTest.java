package com.qsr.customspd.actors.buffs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.levels.Terrain;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.SaveRoundtrip;
import com.watabou.utils.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port regression for upstream 5ed4162d5 (v2.5.0): burning and ooze end as soon as their
 * target steps into water, but only once they have dealt at least one turn of damage. A debuff
 * that already hurt its target ends on entering water with no further tick.
 */
class WaterEndsBurnAndOozePortTest {

	private static final int W = 8;
	private static final int DRY = 3 * W + 3;
	private static final int WET = 3 * W + 4;

	private int prevDepth;
	private Hero prevHero;
	private HeadlessLevel level;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@BeforeEach
	void setUp() {
		prevDepth = Dungeon.depth;
		prevHero = Dungeon.hero;
		level = HeadlessLevel.install(W, 8);
		level.map[WET] = Terrain.WATER;
		level.water[WET] = true;
		// pressing a cell reads the hero's time buffs, so one stands out of the way
		Dungeon.hero = HeadlessLevel.at(new Hero(), W + 1);
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.depth = prevDepth;
		Dungeon.hero = prevHero;
	}

	private static Rat rat(int cell) {
		Rat rat = new Rat();
		rat.HP = rat.HT = 100;
		return HeadlessLevel.at(rat, cell);
	}

	private void stepInto(Rat rat, int cell) {
		rat.pos = cell;
		level.occupyCell(rat);
	}

	@Test
	void burnThatHasAlreadyHurtEndsInWaterWithNoFurtherDamage() {
		Dungeon.depth = 0;
		Rat rat = rat(DRY);
		Burning burning = Buff.affect(rat, Burning.class);
		burning.reignite(rat);

		Random.pushGenerator(1234L);
		try {
			burning.act();
			int hpBeforeWater = rat.HP;
			assertTrue(hpBeforeWater < 100, "fixture: the burn hurt the rat on dry land");

			stepInto(rat, WET);

			assertNull(rat.buff(Burning.class), "water puts the burn out");
			assertEquals(hpBeforeWater, rat.HP, "with no extra tick of damage");
		} finally {
			Random.popGenerator();
		}
	}

	@Test
	void burnCaughtStraightIntoWaterStillDealsOneTick() {
		Dungeon.depth = 0;
		Rat rat = rat(DRY);
		Buff.affect(rat, Burning.class).reignite(rat);

		Random.pushGenerator(1234L);
		try {
			stepInto(rat, WET);
		} finally {
			Random.popGenerator();
		}

		assertNull(rat.buff(Burning.class), "water puts the burn out");
		assertTrue(rat.HP < 100, "but a burn that never acted still deals its one turn of damage");
	}

	@Test
	void oozeThatHasAlreadyHurtEndsInWaterWithNoFurtherDamage() {
		Dungeon.depth = 10; // past the sewers ooze always hurts: 1 + 10/5 = 3 a turn
		Rat rat = rat(DRY);
		Ooze ooze = Buff.affect(rat, Ooze.class);
		ooze.set(Ooze.DURATION);

		ooze.act();
		assertEquals(97, rat.HP, "fixture: the ooze hurt the rat on dry land");

		stepInto(rat, WET);

		assertNull(rat.buff(Ooze.class), "water washes the ooze away");
		assertEquals(97, rat.HP, "with no extra tick of damage");
	}

	@Test
	void oozeKeepsWorkingOnAFlyingTargetOverWater() {
		Dungeon.depth = 10;
		Rat rat = rat(WET);
		rat.flying = true;
		Ooze ooze = Buff.affect(rat, Ooze.class);
		ooze.set(Ooze.DURATION);

		ooze.act();

		assertEquals(97, rat.HP, "the ooze hurts a flier");
		assertNotNull(rat.buff(Ooze.class), "and water below a flier does not wash it away");
	}

	/**
	 * Upstream facd27b18 (v2.5.3): in the sewers ooze only hurts on half its turns, and a turn
	 * that rolled no damage did not count as acting, so water could keep giving it a first turn.
	 */
	@Test
	void sewerOozeTurnWithNoDamageStillCountsAsActing() {
		Dungeon.depth = 1;
		Rat rat = rat(DRY);
		Ooze ooze = Buff.affect(rat, Ooze.class);
		ooze.set(Ooze.DURATION);

		Random.pushGenerator(seedRollingNoDamageThenDamage());
		try {
			ooze.act();
			assertEquals(100, rat.HP, "fixture: the first sewer turn rolled no damage");

			stepInto(rat, WET);
		} finally {
			Random.popGenerator();
		}

		assertNull(rat.buff(Ooze.class), "water washes the ooze away");
		assertEquals(100, rat.HP, "without giving it another first turn");
	}

	/** A seed whose first two Random.Int(2) rolls are 1 (no ooze damage) then 0 (damage). */
	private static long seedRollingNoDamageThenDamage() {
		for (long seed = 1; ; seed++) {
			Random.pushGenerator(seed);
			try {
				if (Random.Int(2) == 1 && Random.Int(2) == 0) return seed;
			} finally {
				Random.popGenerator();
			}
		}
	}

	@Test
	void whetherTheBurnHasActedSurvivesASave() throws Exception {
		Dungeon.depth = 0;
		Rat rat = rat(DRY);
		Burning burning = Buff.affect(rat, Burning.class);
		burning.reignite(rat);

		Random.pushGenerator(1234L);
		try {
			burning.act();
			Burning restored = SaveRoundtrip.of(burning);
			burning.detach();
			restored.attachTo(rat);
			int hpBeforeWater = rat.HP;

			stepInto(rat, WET);

			assertNull(rat.buff(Burning.class), "the restored burn goes out in water");
			assertEquals(hpBeforeWater, rat.HP, "without a fresh first tick");
		} finally {
			Random.popGenerator();
		}
	}
}
