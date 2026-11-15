package com.qsr.customspd.items;

import com.qsr.customspd.assets.GeneralAsset;
import com.qsr.customspd.items.potions.Potion;
import com.qsr.customspd.items.potions.PotionOfHealing;
import com.qsr.customspd.items.potions.exotic.PotionOfCleansing;
import com.qsr.customspd.items.rings.Ring;
import com.qsr.customspd.items.rings.RingOfHaste;
import com.qsr.customspd.items.scrolls.Scroll;
import com.qsr.customspd.items.scrolls.ScrollOfIdentify;
import com.qsr.customspd.test.HeadlessRun;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Port regression for upstream ebaace5f9 and d1c2d87d5 (v2.5.0, unchanged in v2.5.4). The journal
 * scene shows the catalog with no run, after clearing the potion, scroll and ring handlers. With
 * no handler, reset() must still give each item a real look: the first entry of its color, rune
 * or gem table, rather than whatever the bare item default is.
 */
class OutOfRunItemDefaultsPortTest {

	private HeadlessRun run;

	@BeforeEach
	void startRun() {
		// a real run creates the handlers; close() restores every static, handlers included
		run = HeadlessRun.start(20231208L);
	}

	@AfterEach
	void closeRun() {
		run.close();
	}

	private static Object field(Object item, Class<?> owner, String name) throws Exception {
		Field f = owner.getDeclaredField(name);
		f.setAccessible(true);
		return f.get(item);
	}

	@Test
	void potionsScrollsAndRingsFallBackToTheirFirstLook() throws Exception {
		Potion.clearColors();
		Scroll.clearLabels();
		Ring.clearGems();

		PotionOfHealing potion = assertDoesNotThrow(() -> new PotionOfHealing());
		assertEquals(GeneralAsset.POTION_CRIMSON, potion.image);
		assertEquals("crimson", field(potion, Potion.class, "color"));
		assertFalse(assertDoesNotThrow(potion::isKnown));

		ScrollOfIdentify scroll = assertDoesNotThrow(() -> new ScrollOfIdentify());
		assertEquals(GeneralAsset.SCROLL_KAUNAN, scroll.image);
		assertEquals("KAUNAN", field(scroll, Scroll.class, "rune"));
		assertFalse(assertDoesNotThrow(scroll::isKnown));

		RingOfHaste ring = assertDoesNotThrow(() -> new RingOfHaste());
		assertEquals(GeneralAsset.RING_GARNET, ring.image);
		assertEquals("garnet", field(ring, Ring.class, "gem"));
		assertFalse(assertDoesNotThrow(ring::isKnown));
	}

	@Test
	void anExoticFallsThroughToTheBaseDefault() {
		Potion.clearColors();

		PotionOfCleansing exotic = assertDoesNotThrow(() -> new PotionOfCleansing());
		assertEquals(GeneralAsset.POTION_CRIMSON, exotic.image,
				"ExoticPotion.reset skips its own mapping with no handler and keeps Potion's default");
	}
}
