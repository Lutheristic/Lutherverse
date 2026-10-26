package com.qsr.customspd.actors.hero;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.scenes.GameScene;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.SaveRoundtrip;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 935b831af (v2.2.0) replaced the mage's T3 empowering scrolls, whose scroll empower
 * buff had moved to inscribed power, with desperate power, and listed EMPOWERING_SCROLLS in
 * removedTalents so old saves drop it quietly. 3c92d61b2 refreshes the quickslots when desperate
 * power is upgraded, since it changes the levels shown on wands. TEST_SUBJECT and
 * TESTED_HYPOTHESIS, which upstream bfee2fb98 replaced and v2.5.4 lists as removed, are dropped
 * the same way.
 *
 * A talent name the game no longer knows is skipped either way; without the removedTalents entry
 * the skip goes through Talent.valueOf's exception and Game.reportException, which is what these
 * tests watch for.
 */
class DesperatePowerTalentPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	private boolean priorUpdateItemDisplays;

	@BeforeEach
	void saveItemDisplayFlag() {
		priorUpdateItemDisplays = GameScene.updateItemDisplays;
	}

	@AfterEach
	void tearDown() {
		GameScene.updateItemDisplays = priorUpdateItemDisplays;
		Dungeon.hero = null;
	}

	private static Hero hero(HeroClass cls) {
		Hero hero = new Hero();
		hero.heroClass = cls;
		hero.subClass = HeroSubClass.NONE;
		Talent.initClassTalents(hero);
		return hero;
	}

	/** Restores with Game.reportException's output captured, and returns what it reported. */
	private static String restoreCapturingReports(Bundle loaded, Hero fresh) {
		Game priorGame = Game.instance;
		PrintStream priorErr = System.err;
		ByteArrayOutputStream err = new ByteArrayOutputStream();
		// with no Game instance, reportException prints the stack trace to System.err
		Game.instance = null;
		System.setErr(new PrintStream(err, true));
		try {
			Talent.restoreTalentsFromBundle(loaded, fresh);
		} finally {
			System.setErr(priorErr);
			Game.instance = priorGame;
		}
		return err.toString();
	}

	private static Bundle savedWith(HeroClass cls, String tierKey, String talent, int points) throws Exception {
		Bundle saved = new Bundle();
		Talent.storeTalentsInBundle(saved, hero(cls));
		Bundle tier = saved.getBundle(tierKey);
		tier.put(talent, points);
		saved.put(tierKey, tier);
		return SaveRoundtrip.writeRead(saved);
	}

	@Test
	void theMageHasDesperatePowerInPlaceOfEmpoweringScrolls() {
		Hero mage = hero(HeroClass.MAGE);
		assertTrue(mage.talents.get(2).containsKey(Talent.DESPERATE_POWER));
		assertEquals(3, Talent.DESPERATE_POWER.maxPoints());
	}

	@Test
	void aSaveWithEmpoweringScrollsDropsItQuietly() throws Exception {
		Bundle loaded = savedWith(HeroClass.MAGE, "talents_tier_3", "EMPOWERING_SCROLLS", 2);
		Hero fresh = new Hero();
		fresh.heroClass = HeroClass.MAGE;
		fresh.subClass = HeroSubClass.NONE;

		String reported = restoreCapturingReports(loaded, fresh);

		assertEquals("", reported);
		assertEquals(0, fresh.pointsInTalent(Talent.DESPERATE_POWER), "the points are not carried over");
	}

	@Test
	void aMetamorphosisInvolvingEmpoweringScrollsIsDroppedQuietly() throws Exception {
		Bundle saved = new Bundle();
		Talent.storeTalentsInBundle(saved, hero(HeroClass.WARRIOR));
		Bundle replacements = new Bundle();
		replacements.put("HOLD_FAST", "EMPOWERING_SCROLLS");
		saved.put("replacements", replacements);
		Bundle loaded = SaveRoundtrip.writeRead(saved);
		Hero fresh = new Hero();
		fresh.heroClass = HeroClass.WARRIOR;
		fresh.subClass = HeroSubClass.NONE;

		String reported = restoreCapturingReports(loaded, fresh);

		assertEquals("", reported);
		assertTrue(fresh.metamorphedTalents.isEmpty());
	}

	@Test
	void aSaveWithTestSubjectDropsItQuietly() throws Exception {
		Bundle loaded = savedWith(HeroClass.WARRIOR, "talents_tier_1", "TEST_SUBJECT", 2);
		Hero fresh = new Hero();
		fresh.heroClass = HeroClass.WARRIOR;
		fresh.subClass = HeroSubClass.NONE;

		assertEquals("", restoreCapturingReports(loaded, fresh));
	}

	@Test
	void aSaveWithTestedHypothesisDropsItQuietly() throws Exception {
		Bundle loaded = savedWith(HeroClass.MAGE, "talents_tier_1", "TESTED_HYPOTHESIS", 1);
		Hero fresh = new Hero();
		fresh.heroClass = HeroClass.MAGE;
		fresh.subClass = HeroSubClass.NONE;

		assertEquals("", restoreCapturingReports(loaded, fresh));
	}

	@Test
	void controlAnUnknownTalentIsStillReported() throws Exception {
		Bundle loaded = savedWith(HeroClass.MAGE, "talents_tier_3", "NOT_A_TALENT", 1);
		Hero fresh = new Hero();
		fresh.heroClass = HeroClass.MAGE;
		fresh.subClass = HeroSubClass.NONE;

		String reported = restoreCapturingReports(loaded, fresh);

		assertTrue(reported.contains("NOT_A_TALENT"), "the capture sees what reportException prints: " + reported);
	}

	@Test
	void upgradingDesperatePowerRefreshesTheQuickslots() {
		Hero mage = hero(HeroClass.MAGE);
		Dungeon.hero = mage;

		GameScene.updateItemDisplays = false;
		Talent.onTalentUpgraded(mage, Talent.ALLY_WARP);
		assertFalse(GameScene.updateItemDisplays, "control: ally warp shows nothing on items");

		Talent.onTalentUpgraded(mage, Talent.DESPERATE_POWER);
		assertTrue(GameScene.updateItemDisplays);
	}
}
