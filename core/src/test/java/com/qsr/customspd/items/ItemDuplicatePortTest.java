package com.qsr.customspd.items;

import com.qsr.customspd.assets.GeneralAsset;
import com.qsr.customspd.items.potions.PotionOfHealing;
import com.qsr.customspd.items.weapon.melee.Sword;
import com.qsr.customspd.test.HeadlessGdx;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port test for upstream SPD 8efb48d83 (v2.4.0): "added new buttons and bindings in the alchemy
 * scene".
 *
 * The alchemy scene's "repeat ingredients" button remembers the last craft by duplicating each
 * ingredient (Item.duplicate, new in that commit) BEFORE the recipe consumes the originals, then
 * later looks the copies up in the pack. The copy must therefore be an independent instance that
 * carries the original's class, quantity and level, so the original can be used up without
 * changing it. The scene itself is UI and has no headless driver; this pins the data primitive
 * and that the new repeat icon asset the scene loads is shipped.
 */
class ItemDuplicatePortTest {

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@Test
	void duplicateIsAnIndependentCopyWithTheSameClassQuantityAndLevel() {
		PotionOfHealing potions = new PotionOfHealing();
		potions.quantity(3);
		PotionOfHealing copy = (PotionOfHealing) potions.duplicate();

		assertNotNull(copy);
		assertNotSame(potions, copy);
		assertEquals(3, copy.quantity());

		// using up the original (as brewing does) must not change the remembered copy
		potions.quantity(0);
		assertEquals(3, copy.quantity());
	}

	@Test
	void duplicateKeepsUpgradeLevel() {
		Sword sword = new Sword();
		sword.level(2);
		Item copy = sword.duplicate();

		assertNotNull(copy);
		assertNotSame(sword, copy);
		assertEquals(Sword.class, copy.getClass());
		assertEquals(2, copy.level());
	}

	@Test
	void theRepeatIconTheAlchemySceneLoadsIsShipped() {
		assertEquals("interfaces/icons/repeat.png", GeneralAsset.ICON_REPEAT.getPath());
		File f = new File(GeneralAsset.ICON_REPEAT.getPath());
		// core:test runs with src/main/assets as the working directory (see HeadlessGdx)
		assertTrue(f.isFile(), "missing asset file " + f.getAbsolutePath());
	}
}
