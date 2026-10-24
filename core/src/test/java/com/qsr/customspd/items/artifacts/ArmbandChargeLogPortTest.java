package com.qsr.customspd.items.artifacts;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.scenes.GameScene;
import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.HeadlessLevel;
import com.qsr.customspd.utils.GLog;
import com.watabou.utils.Signal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream d633a10b3 (v2.4.0): charging the armband announces "full" once, when it fills, and
 * stays silent (no log spam, no stray partial charge) while it is already full.
 *
 * CPDU's pre-image was an older shape of charge() that already behaved this way, so this commit
 * is a convergence on upstream's form; the test is a characterisation of the behaviour both forms
 * share and is not expected to fail on the old code.
 */
class ArmbandChargeLogPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		HeadlessLevel.uninstall();
		Dungeon.hero = null;
		GameScene.updateItemDisplays = false;
	}

	@Test
	void fullIsAnnouncedOnceAndNeverAgain() {
		HeadlessLevel.install(8, 8);
		Hero hero = new Hero();
		Dungeon.hero = hero;
		MasterThievesArmband armband = new MasterThievesArmband();
		armband.charge = armband.chargeCap - 1;
		armband.partialCharge = 0.5f;

		String full = GLog.POSITIVE + Messages.get(MasterThievesArmband.class, "full");
		List<String> log = new ArrayList<>();
		Signal.Listener<String> listener = log::add;
		GLog.update.add(listener);
		try {
			armband.charge(hero, 10f);
			assertEquals(armband.chargeCap, armband.charge);
			assertEquals(0f, armband.partialCharge, 0f, "no stray partial charge once full");
			armband.charge(hero, 10f);
			armband.charge(hero, 10f);
		} finally {
			GLog.update.remove(listener);
		}

		int announcements = 0;
		for (String s : log) if (s.equals(full)) announcements++;
		assertEquals(1, announcements, "log was " + log);
		assertTrue(armband.charge == armband.chargeCap);
	}
}
