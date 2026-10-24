package com.qsr.customspd.items.weapon.melee;

import com.qsr.customspd.test.HeadlessGdx;
import com.qsr.customspd.test.SaveRoundtrip;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port regression for upstream 4f8b638a9 (v2.4.0, "fixed guard ability not persisting block
 * tracking"), in the corrected form of upstream 1b7e2ed9d (v3.0.0): as first shipped, the commit's
 * storeInBundle read the flag from the bundle and restoreFromBundle wrote it, so nothing was
 * saved and every save cleared a live flag.
 */
class GuardTrackerSavePortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@Test
	void aSpentGuardStaysSpentAcrossASaveAndLoad() throws IOException {
		RoundShield.GuardTracker tracker = new RoundShield.GuardTracker();
		tracker.hasBlocked = true;

		RoundShield.GuardTracker loaded = SaveRoundtrip.of(tracker);

		assertTrue(loaded.hasBlocked, "the block already used must survive the save");
		assertTrue(tracker.hasBlocked, "saving must not clear the live flag");
	}

	@Test
	void anUnspentGuardStaysUnspentAcrossASaveAndLoad() throws IOException {
		RoundShield.GuardTracker tracker = new RoundShield.GuardTracker();

		assertFalse(SaveRoundtrip.of(tracker).hasBlocked);
	}
}
