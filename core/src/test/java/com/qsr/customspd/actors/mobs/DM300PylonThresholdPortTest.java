package com.qsr.customspd.actors.mobs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.StubSprite;
import com.qsr.customspd.ui.BossHealthBar;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port regression for upstream 4b836591b: DM-300's pylon threshold check read
 * {@code HP > 0}, so a hit that took it to 0 HP or below while pylons remained left it
 * alive (isAlive counts unfired pylons) at non-positive HP and no pylon fired. The check
 * now reads {@code threshold > 0}: HP is clamped up to the threshold and the next pylon fires.
 *
 * supercharge() casts the level to CavesBossLevel and the sprite to DM300Sprite, neither of
 * which exists headless, so the subject records the call instead of running it.
 */
class DM300PylonThresholdPortTest {

	private static final int SIZE = 12;

	static class RecordingDM300 extends DM300 {
		int supercharges = 0;

		@Override
		public void supercharge() {
			supercharges++;
		}
	}

	private RecordingDM300 dm300;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@BeforeEach
	void setUp() {
		Dungeon.challenges = 0;
		HeadlessLevel.install(SIZE, SIZE);
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeadlessLevel.at(hero, SIZE + 1);
		StubSprite.attach(hero);

		dm300 = HeadlessLevel.at(new RecordingDM300(), 6 * SIZE + 6);
		StubSprite.attach(dm300);
		Dungeon.level.mobs.add(dm300);
	}

	@AfterEach
	void tearDown() {
		BossHealthBar.assignBoss(null);
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	private int firstThreshold() {
		return dm300.HT / 3 * 2;
	}

	@Test
	void aHitPastZeroHpClampsToTheThresholdAndFiresAPylon() {
		dm300.damage(dm300.HT * 2, new Object());

		assertEquals(firstThreshold(), dm300.HP,
				"a huge hit while pylons remain clamps HP to the threshold");
		assertEquals(1, dm300.supercharges, "and the next pylon fires");
		assertTrue(dm300.isAlive());
	}

	@Test
	void aHitToExactlyZeroHpAlsoFiresAPylon() {
		dm300.damage(dm300.HT, new Object());

		assertEquals(firstThreshold(), dm300.HP);
		assertEquals(1, dm300.supercharges);
	}

	@Test
	void aHitBelowTheThresholdButAboveZeroStillFiresAPylon() {
		// worked before the fix too: shows the subject reaches the threshold check at all
		dm300.damage(dm300.HP - firstThreshold() + 10, new Object());

		assertEquals(firstThreshold(), dm300.HP);
		assertEquals(1, dm300.supercharges);
	}

	@Test
	void aHitAboveTheThresholdFiresNothing() {
		dm300.damage(10, new Object());

		assertEquals(dm300.HT - 10, dm300.HP);
		assertEquals(0, dm300.supercharges);
	}
}
