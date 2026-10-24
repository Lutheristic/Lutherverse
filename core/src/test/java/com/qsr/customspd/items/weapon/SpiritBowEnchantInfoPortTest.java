package com.qsr.customspd.items.weapon;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.items.weapon.enchantments.Blazing;
import com.qsr.customspd.items.weapon.melee.Sword;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.test.HeadlessGdx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port regression for upstream 396581f80 (v2.3.0, "fixed spirit bow using different enchant desc
 * logic than weapons"). SpiritBow.info() built its enchantment line by hand (lowercase "it has a
 * ..." and a raw {@code Messages.get(enchantment, "desc")}), unlike every melee weapon, which
 * capitalises the sentence and calls {@code enchantment.desc()} so subclasses can override it.
 *
 * Upstream's hardened-enchant branches of the same hunk are not covered here: CPDU has no
 * {@code enchantHardened} (blacksmith quest rewards, upstream de86487aa) yet.
 */
class SpiritBowEnchantInfoPortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@AfterEach
	void tearDown() {
		Dungeon.hero = null;
	}

	@Test
	void anEnchantedSpiritBowDescribesItsEnchantmentLikeAMeleeWeapon() {
		Dungeon.hero = new Hero();

		SpiritBow bow = new SpiritBow();
		bow.enchant(new Blazing());
		Sword sword = new Sword();
		sword.enchant(new Blazing());

		String line = Messages.capitalize(Messages.get(Weapon.class, "enchanted", bow.enchantment.name()));
		String bowInfo = bow.info();

		assertTrue(sword.info().contains(line + " " + sword.enchantment.desc()),
				"fixture: melee weapons use the capitalised sentence plus enchantment.desc()");
		assertTrue(bowInfo.contains(line + " " + bow.enchantment.desc()),
				"upstream 396581f80: the spirit bow must use the same logic as weapons");
	}
}
