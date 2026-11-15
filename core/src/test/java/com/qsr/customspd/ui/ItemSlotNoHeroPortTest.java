package com.qsr.customspd.ui;

import com.qsr.customspd.Dungeon;
import com.qsr.customspd.actors.hero.Hero;
import com.qsr.customspd.items.armor.ClothArmor;
import com.qsr.customspd.items.weapon.missiles.ThrowingStone;
import com.qsr.customspd.messages.Messages;
import com.qsr.customspd.scenes.PixelScene;
import com.qsr.customspd.test.HeadlessRun;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.BitmapText;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Upstream ebaace5f9 shows item slots in the journal scene, where there is no hero, so the
 * strength check in an item slot must not need one.
 */
class ItemSlotNoHeroPortTest {

	private HeadlessRun run;
	private Hero priorHero;
	private BitmapText.Font priorFont;

	@BeforeEach
	void setUp() {
		run = HeadlessRun.start(20261115L);
		priorHero = Dungeon.hero;
		priorFont = PixelScene.pixelFont;
		PixelScene.pixelFont = new BitmapText.Font(
				TextureCache.create("test-item-slot-font", 4 * BitmapText.Font.LATIN_FULL.length(), 6),
				4, BitmapText.Font.LATIN_FULL);
	}

	@AfterEach
	void tearDown() {
		try {
			Dungeon.hero = priorHero;
			PixelScene.pixelFont = priorFont;
		} finally {
			run.close();
		}
	}

	@Test
	void anItemSlotNeedsNoHeroForAKnownWeapon() throws Exception {
		// a melee weapon's status() reads the hero's belongings, here and upstream alike, so the
		// weapon case uses a thrown weapon, as the journal does for its missile placeholder
		ThrowingStone stone = new ThrowingStone();
		stone.levelKnown = true;
		ClothArmor armor = new ClothArmor();
		armor.levelKnown = true;
		int stoneStr = stone.STRReq();
		int armorStr = armor.STRReq();

		Dungeon.hero = null;

		// ItemSlot.TXT_STRENGTH
		ItemSlot stoneSlot = assertDoesNotThrow(() -> new ItemSlot(stone));
		ItemSlot armorSlot = assertDoesNotThrow(() -> new ItemSlot(armor));
		assertEquals(Messages.format(":%d", stoneStr), extraText(stoneSlot));
		assertEquals(Messages.format(":%d", armorStr), extraText(armorSlot));
	}

	private static String extraText(ItemSlot slot) throws ReflectiveOperationException {
		Field f = ItemSlot.class.getDeclaredField("extra");
		f.setAccessible(true);
		return ((BitmapText) f.get(slot)).text();
	}
}
