package com.qsr.customspd.items.bags;

import com.qsr.customspd.items.ArcaneResin;
import com.qsr.customspd.items.LiquidMetal;
import com.qsr.customspd.items.Stylus;
import com.qsr.customspd.items.Waterskin;
import com.qsr.customspd.items.potions.PotionOfHealing;
import com.qsr.customspd.items.scrolls.ScrollOfIdentify;
import com.qsr.customspd.test.HeadlessGdx;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream 213f7a2f6 (v2.5.0, cpdu-1be): the waterskin fits in the potion bandolier and arcane styli
 * fit in the scroll holder. Everything each bag already held must still fit, and the two bags must
 * not start taking each other's items.
 */
class BagHoldPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@Test
	void potionBandolierHoldsTheWaterskin() {
		PotionBandolier bandolier = new PotionBandolier();
		assertTrue(bandolier.canHold(new Waterskin()), "the waterskin fits in the potion bandolier");
		assertTrue(bandolier.canHold(new PotionOfHealing()));
		assertTrue(bandolier.canHold(new LiquidMetal()));
		assertFalse(bandolier.canHold(new Stylus()), "styli belong in the scroll holder");
	}

	@Test
	void scrollHolderHoldsArcaneStyli() {
		ScrollHolder holder = new ScrollHolder();
		assertTrue(holder.canHold(new Stylus()), "arcane styli fit in the scroll holder");
		assertTrue(holder.canHold(new ScrollOfIdentify()));
		assertTrue(holder.canHold(new ArcaneResin()));
		assertFalse(holder.canHold(new Waterskin()), "the waterskin belongs in the potion bandolier");
	}
}
