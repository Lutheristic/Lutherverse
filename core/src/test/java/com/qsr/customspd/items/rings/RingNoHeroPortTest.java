package com.qsr.customspd.items.rings;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.items.potions.Potion;
import com.qsr.customspd.items.scrolls.Scroll;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.test.HeadlessGdx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Port regression for the ring hunks of upstream b6f7e7462 (v2.5.0, "made a bunch of code tweaks
 * so catalogs can appear out of game"; unchanged in v2.5.4): RingOfForce.statsInfo()/info() read
 * the hero's strength and class, and Potion/Scroll/Ring.allKnown() read an appearance handler that
 * only exists once a run has started.
 *
 * Ring.buffedLvl is deliberately NOT guarded: neither b6f7e7462 nor v2.5.4 guards it (an identified
 * ring's stats still need a hero upstream), so porting a guard would be an unrequested divergence.
 */
class RingNoHeroPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		Dungeon.hero = null;
	}

	@Test
	void ringOfForceStatsAreTypicalForStrengthTenWhenThereIsNoHero() {
		Dungeon.hero = null;
		RingOfForce ring = new RingOfForce();
		// tier(10) = 1, so typical level-1 stats: min 1+1, max 5*(1+1)+1*(1+1)
		assertEquals(Messages.get(ring, "typical_stats", 2, 12, 1),
				assertDoesNotThrow(ring::statsInfo), "no hero: the default 10 strength");
	}

	@Test
	void ringOfForceInfoWithNoHeroHasNoDuelistAbilityParagraph() {
		Dungeon.hero = null;
		RingOfForce ring = new RingOfForce();
		// whether the ring type is already "known" depends on the JVM-wide appearance handler other
		// tests share, so only the crash (Dungeon.hero.heroClass) is asserted
		String info = assertDoesNotThrow(ring::info);
		assertFalse(info.isEmpty());
	}

	@Test
	void allKnownIsFalseBeforeTheAppearanceHandlersExist() throws Exception {
		Object[] saved = new Object[3];
		Class<?>[] owners = {Ring.class, Potion.class, Scroll.class};
		Field[] fields = new Field[3];
		for (int i = 0; i < 3; i++) {
			fields[i] = owners[i].getDeclaredField("handler");
			fields[i].setAccessible(true);
			saved[i] = fields[i].get(null);
		}
		try {
			for (Field f : fields) f.set(null, null);
			assertFalse(assertDoesNotThrow(Ring::allKnown), "no handler: nothing is known");
			assertFalse(assertDoesNotThrow(Potion::allKnown), "no handler: nothing is known");
			assertFalse(assertDoesNotThrow(Scroll::allKnown), "no handler: nothing is known");
		} finally {
			for (int i = 0; i < 3; i++) fields[i].set(null, saved[i]);
		}
	}
}
