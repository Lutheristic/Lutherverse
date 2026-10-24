package com.qsr.customspd.items.artifacts;

import com.qsr.customspd.plants.Rotberry;
import com.qsr.customspd.test.HeadlessGdx;
import com.watabou.utils.Bundle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 63c36166e (v2.5.1): "fixed bundling error with invalid seeds in sandals of nature".
 * A save whose seed list is not a readable class array used to crash the restore with a
 * NullPointerException; it now restores with no stored seeds.
 */
class SandalsOfNatureBundlePortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@Test
	void anUnreadableSeedListRestoresAsNoSeeds() {
		Bundle bundle = new Bundle();
		bundle.put("seeds", "not-a-class-array");

		SandalsOfNature sandals = new SandalsOfNature();
		assertDoesNotThrow(() -> sandals.restoreFromBundle(bundle));
		assertTrue(sandals.seeds.isEmpty(), "no seeds survive an unreadable list");
	}

	@Test
	void storedSeedsStillRoundTrip() {
		SandalsOfNature sandals = new SandalsOfNature();
		sandals.seeds.add(Rotberry.Seed.class);
		Bundle bundle = new Bundle();
		sandals.storeInBundle(bundle);

		SandalsOfNature restored = new SandalsOfNature();
		restored.restoreFromBundle(bundle);
		assertEquals(1, restored.seeds.size());
		assertEquals(Rotberry.Seed.class, restored.seeds.get(0));
	}
}
