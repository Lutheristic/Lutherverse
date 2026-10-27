package com.qsr.customspd.items.weapon.melee;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.Char;
import com.qsr.customspd.actors.buffs.Buff;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.actors.mobs.Rat;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.test.SaveRoundtrip;
import com.qsr.customspd.test.StubSprite;
import com.qsr.customspd.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Signal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 11a410f07 (v2.4.0): the combo strike tracker no longer forgets each hit 5 turns after
 * it landed. Every hit counts until 5 turns pass without one, the second and later hits announce
 * the combo, and the buff shows the turns left. Saves before it stored an int[] of hits per turn
 * under recent_hits; those load as their sum with a full 5 turns.
 */
class SaiComboTrackerPortTest {

	private static final int W = 10;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
	}

	public static class FixedRollSai extends Sai {
		@Override
		public int damageRoll(Char owner) {
			return 5;
		}
	}

	public static class SturdyRat extends Rat {
		{
			HP = HT = 1000;
		}

		@Override
		public int drRoll() {
			return 0;
		}
	}

	private static Hero hero() {
		HeadlessLevel.install(W, 10);
		Hero hero = new Hero();
		hero.STR = 20;
		Dungeon.hero = hero;
		StubSprite.attach(hero);
		HeadlessLevel.at(hero, 4 * W + 4);
		return hero;
	}

	private static Sai.ComboStrikeTracker tracker(Hero hero) {
		return hero.buff(Sai.ComboStrikeTracker.class);
	}

	private static void turns(Sai.ComboStrikeTracker tracker, int n) {
		for (int i = 0; i < n; i++) tracker.act();
	}

	/** Damage of a +0 combo strike on a sturdy rat: the roll of 5 plus 3 per counted hit. */
	private static int comboStrike(Hero hero) {
		FixedRollSai sai = new FixedRollSai();
		hero.belongings.weapon = sai;
		int cell = 4 * W + 5;
		SturdyRat rat = HeadlessLevel.at(new SturdyRat(), cell);
		StubSprite.attach(rat);
		Dungeon.level.heroFOV[cell] = true;
		sai.duelistAbility(hero, cell);
		return rat.HT - rat.HP;
	}

	@Test
	void hitsCountUntilFiveTurnsPassWithoutOne() {
		Hero hero = hero();
		Sai.ComboStrikeTracker combo = Buff.affect(hero, Sai.ComboStrikeTracker.class);
		combo.addHit();
		turns(combo, 4);
		combo.addHit();
		turns(combo, 4);

		assertNotNull(tracker(hero), "four turns since the last hit");
		assertEquals(5 + 3 * 2, comboStrike(hero), "the first hit, eight turns old, still counts");
		assertNull(tracker(hero), "the strike spends the combo");
	}

	@Test
	void fiveTurnsWithoutAHitEndTheCombo() {
		Hero hero = hero();
		Sai.ComboStrikeTracker combo = Buff.affect(hero, Sai.ComboStrikeTracker.class);
		combo.addHit();
		combo.addHit();
		turns(combo, 4);
		assertNotNull(tracker(hero));
		turns(combo, 1);
		assertNull(tracker(hero), "gone on the fifth turn without a hit");
	}

	@Test
	void theComboIsAnnouncedFromTheSecondHit() {
		Hero hero = hero();
		hero.belongings.weapon = new Sai();
		List<String> log = new ArrayList<>();
		Signal.Listener<String> listener = text -> {
			log.add(text);
			return false;
		};
		GLog.update.add(listener);
		try {
			Sai.ComboStrikeTracker combo = Buff.affect(hero, Sai.ComboStrikeTracker.class);
			combo.addHit();
			assertTrue(log.isEmpty(), "one hit is no combo: " + log);
			combo.addHit();
			assertEquals(1, log.size());
			assertTrue(log.get(0).endsWith("2 hit combo!"), log.get(0));
		} finally {
			GLog.update.remove(listener);
		}
	}

	/** Upstream 9154f32c6: with no sai, gloves or gauntlet held the combo is not announced. */
	@Test
	void withoutAComboWeaponHeldTheComboIsNotAnnounced() {
		Hero hero = hero();
		hero.belongings.weapon = new Sword();
		List<String> log = new ArrayList<>();
		Signal.Listener<String> listener = text -> {
			log.add(text);
			return false;
		};
		GLog.update.add(listener);
		try {
			Sai.ComboStrikeTracker combo = Buff.affect(hero, Sai.ComboStrikeTracker.class);
			combo.addHit();
			combo.addHit();
			combo.addHit();
			assertTrue(log.isEmpty(), "nothing to announce it for: " + log);
			assertEquals(3, combo.hits, "the hits still count");
		} finally {
			GLog.update.remove(listener);
		}
	}

	@Test
	void theBuffShowsTheTurnsLeft() {
		Hero hero = hero();
		Sai.ComboStrikeTracker combo = Buff.affect(hero, Sai.ComboStrikeTracker.class);
		combo.addHit();
		combo.addHit();
		combo.addHit();
		assertEquals("5", combo.iconTextDisplay());
		turns(combo, 2);
		assertEquals("3", combo.iconTextDisplay(), "the icon counts down the turns, not the hits");
		String desc = combo.desc();
		assertTrue(desc.contains("Recent hits: 3."), desc);
		assertTrue(desc.contains("Turns until hits are lost: 3."), desc);
	}

	@Test
	void aSaveKeepsTheHitsAndTheTurnsLeft() throws Exception {
		Hero hero = hero();
		Sai.ComboStrikeTracker combo = Buff.affect(hero, Sai.ComboStrikeTracker.class);
		combo.addHit();
		combo.addHit();
		turns(combo, 2);

		Sai.ComboStrikeTracker restored = SaveRoundtrip.of(combo);
		combo.detach();
		restored.attachTo(hero);
		turns(restored, 2);
		assertNotNull(tracker(hero), "three turns were left at the save");
		turns(restored, 1);
		assertNull(tracker(hero));
	}

	private static Sai.ComboStrikeTracker fromOldSave(Hero hero, int[] hitsPerTurn) throws Exception {
		Bundle old = new Bundle();
		old.put("recent_hits", hitsPerTurn);
		Sai.ComboStrikeTracker restored = new Sai.ComboStrikeTracker();
		restored.restoreFromBundle(SaveRoundtrip.writeRead(old));
		restored.attachTo(hero);
		return restored;
	}

	@Test
	void anOldSavesHitsAreSummedAndKeptForFiveTurns() throws Exception {
		Hero hero = hero();
		// all three hits were about to expire when hits still expired turn by turn
		Sai.ComboStrikeTracker restored = fromOldSave(hero, new int[]{3, 0, 0, 0, 0, 0});
		turns(restored, 1);
		assertNotNull(tracker(hero), "the old hits get a fresh 5 turns");
		assertEquals(5 + 3 * 3, comboStrike(hero));
	}

	@Test
	void anOldSavesComboEndsFiveTurnsAfterLoading() throws Exception {
		Hero hero = hero();
		Sai.ComboStrikeTracker restored = fromOldSave(hero, new int[]{0, 1, 0, 0, 0, 2});
		turns(restored, 4);
		assertNotNull(tracker(hero));
		turns(restored, 1);
		assertNull(tracker(hero), "the newest hits would have lasted when hits expired turn by turn");
	}
}
