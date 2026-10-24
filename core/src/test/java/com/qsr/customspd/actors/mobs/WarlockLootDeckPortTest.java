package com.qsr.customspd.actors.mobs;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.items.Generator;
import com.qsr.customspd.items.Item;
import com.qsr.customspd.items.potions.PotionOfHealing;
import com.qsr.customspd.test.HeadlessRun;
import com.watabou.utils.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Warlock loot re-rolls a healing potion rather than handing the rejected rolls back to the deck
 * (upstream cf7ddd4db, v2.2.0: "fixed a few cases of non-levelgen drops interacting with decks").
 *
 * Warlock.createLoot draws with randomUsingDefaults, which reads the fixed default weights and never
 * touches the live deck, so there was nothing to give back. The old code added every rejected
 * healing roll to Category.POTION.probs anyway, inflating the live deck's healing weight
 * (3 in each half of the deck) and so changing what later deck-based potion draws produce.
 */
class WarlockLootDeckPortTest {

	/** A Warlock whose guaranteed-healing branch can no longer fire, so every drop goes through the re-roll. */
	private static Warlock exhaustedWarlock() {
		Dungeon.LimitedDrops.WARLOCK_HP.count = 99; // Random.Int(8) > 99 never holds
		return new Warlock();
	}

	@Test
	void rerollingHealingLeavesTheLivePotionDeckAlone() {
		try (HeadlessRun run = HeadlessRun.start(20261024L)) {
			Warlock warlock = exhaustedWarlock();

			// non-vacuity: the default weights do roll healing, so the re-roll branch really runs
			int healingRolls = 0;
			Random.pushGenerator(1L);
			try {
				for (int n = 0; n < 300; n++) {
					if (Generator.randomUsingDefaults(Generator.Category.POTION) instanceof PotionOfHealing) healingRolls++;
				}
			} finally {
				Random.popGenerator();
			}
			assertTrue(healingRolls > 0, "test setup: randomUsingDefaults must be able to roll healing");

			float[] deckBefore = Generator.Category.POTION.probs.clone();
			Random.pushGenerator(2L);
			try {
				for (int n = 0; n < 300; n++) {
					Item drop = warlock.createLoot();
					assertFalse(drop instanceof PotionOfHealing, "a Warlock out of guaranteed healing never drops one");
				}
			} finally {
				Random.popGenerator();
			}

			assertArrayEquals(deckBefore, Generator.Category.POTION.probs,
					"re-rolled healing potions must not be added back into the live potion deck");
		}
	}
}
