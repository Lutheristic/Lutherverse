package com.qsr.customspd.items.artifacts;

import com.qsr.customspd.items.scrolls.ScrollOfMirrorImage;
import com.qsr.customspd.items.scrolls.ScrollOfRecharging;
import com.qsr.customspd.test.HeadlessGdx;
import com.watabou.utils.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream c1e446580 (v2.5.0): the spellbook's upgrade order is weighted by the full
 * scroll table (defaultProbsTotal), not only the first of the two scroll decks. Mirror image and
 * recharging are 1+2 and 2+1 cards: equally likely overall, 1:2 in the first deck alone.
 */
class UnstableSpellbookPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@Test
	void upgradeOrderIsWeightedByBothScrollDecks() throws Exception {
		Field scrollsField = UnstableSpellbook.class.getDeclaredField("scrolls");
		scrollsField.setAccessible(true);
		int mirror = 0, recharging = 0;
		Random.pushGenerator(0x5BE11L);
		try {
			for (int n = 0; n < 3000; n++) {
				Object first = ((List<?>) scrollsField.get(new UnstableSpellbook())).get(0);
				if (first == ScrollOfMirrorImage.class) mirror++;
				if (first == ScrollOfRecharging.class) recharging++;
			}
		} finally {
			Random.popGenerator();
		}
		assertTrue(Math.abs(mirror - recharging) < 60,
				"mirror image and recharging must lead the list equally often: " + mirror + " vs " + recharging);
	}
}
