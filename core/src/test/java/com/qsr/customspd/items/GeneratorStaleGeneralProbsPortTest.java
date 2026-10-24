package com.qsr.customspd.items;

import com.qsr.customspd.test.HeadlessGdx;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Port regression test for upstream SPD 862b152ca (v2.4.1): "fixed saves from prior to v2.4 not
 * resetting generators".
 *
 * Generator saves the category-level deck ("general_probs") as a bare float array indexed by
 * Category ordinal. A save written when the Category enum had a different size (v2.4 added
 * categories) carries an array of the wrong length; restoring it either threw
 * ArrayIndexOutOfBounds (longer array) or smeared old weights over the wrong categories (shorter
 * array). The array is now applied only when its length matches the current enum, otherwise the
 * freshly reset deck stands.
 */
class GeneratorStaleGeneralProbsPortTest {

	private static final String GENERAL_PROBS = "general_probs";
	private static final float MARKER = 99f;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	private Bundle saved;

	@BeforeEach
	void snapshot() {
		saved = new Bundle();
		Generator.storeInBundle(saved);
	}

	@AfterEach
	void restore() {
		restoreWith(saved);
	}

	private static void restoreWith(Bundle b) {
		Random.pushGenerator(0xC0FFEEL);
		try {
			Generator.restoreFromBundle(b);
		} finally {
			Random.popGenerator();
		}
	}

	private static float[] generalProbsAfterRestoring(int length) {
		Bundle b = new Bundle();
		Generator.storeInBundle(b);
		float[] stale = new float[length];
		Arrays.fill(stale, MARKER);
		b.put(GENERAL_PROBS, stale);

		restoreWith(b);

		Bundle after = new Bundle();
		Generator.storeInBundle(after);
		return after.getFloatArray(GENERAL_PROBS);
	}

	@Test
	void aLongerStaleArrayIsIgnoredNotACrash() {
		int n = Generator.Category.values().length;
		float[] probs = generalProbsAfterRestoring(n + 3);
		assertEquals(n, probs.length);
		for (float p : probs) assertFalse(p == MARKER, "stale weights must not be applied");
	}

	@Test
	void aShorterStaleArrayIsIgnoredNotSmearedOverTheFirstCategories() {
		int n = Generator.Category.values().length;
		float[] probs = generalProbsAfterRestoring(n - 4);
		assertEquals(n, probs.length);
		for (float p : probs) assertFalse(p == MARKER, "stale weights must not be applied");
	}

	@Test
	void anArrayOfTheCurrentLengthStillRestores() {
		int n = Generator.Category.values().length;
		float[] probs = generalProbsAfterRestoring(n);
		assertEquals(n, probs.length);
		for (float p : probs) assertEquals(MARKER, p, "a matching save restores as before");
	}
}
