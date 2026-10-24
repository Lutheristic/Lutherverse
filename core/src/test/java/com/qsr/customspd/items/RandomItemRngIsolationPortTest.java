package com.qsr.customspd.items;

import com.qsr.customspd.items.armor.ClothArmor;
import com.qsr.customspd.items.weapon.melee.WornShortsword;
import com.qsr.customspd.test.HeadlessGdx;
import com.watabou.utils.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Port regression for upstream 1ac5ac4fa (v2.5.0, "fixed parchment scrap still affecting RNG in
 * some cases"). Weapon.random() and Armor.random() roll their curse/enchant/glyph on a private
 * generator seeded by one draw of the caller's, so how many numbers that roll consumes (none
 * extra for a plain item, several for a curse or a random glyph, and anything a modifier such as
 * parchment scrap changes) cannot shift what the level generator draws next.
 *
 * Checked against a hand replay of the level roll: the caller's stream after random() must be
 * exactly [level rolls, one Long, then the rest], whatever random() decided about enchantments.
 */
class RandomItemRngIsolationPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	private static void assertCallersStreamIsUnaffected(String what, Supplier<Item> make) {
		for (long seed = 1; seed <= 300; seed++) {
			long actual;
			Random.pushGenerator(seed);
			try {
				make.get();
				actual = Random.Long();
			} finally {
				Random.popGenerator();
			}

			long expected;
			Random.pushGenerator(seed);
			try {
				// the level roll: +1 on a 1/4 chance, +2 on a further 1/5
				if (Random.Int(4) == 0) {
					Random.Int(5);
				}
				Random.Long(); // the seed handed to the private generator
				expected = Random.Long();
			} finally {
				Random.popGenerator();
			}

			assertEquals(expected, actual, what + ", seed " + seed
					+ ": the caller's next draw must not depend on the enchantment roll");
		}
	}

	@Test
	void weaponRandomLeavesTheCallersStreamAlone() {
		assertCallersStreamIsUnaffected("Weapon.random", () -> new WornShortsword().random());
	}

	@Test
	void armorRandomLeavesTheCallersStreamAlone() {
		assertCallersStreamIsUnaffected("Armor.random", () -> new ClothArmor().random());
	}
}
