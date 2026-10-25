package com.qsr.customspd.items.scrolls.exotic;

import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.qsr.customspd.items.armor.ClothArmor;
import com.qsr.customspd.items.armor.glyphs.Obfuscation;
import com.qsr.customspd.items.armor.glyphs.Stone;
import com.qsr.customspd.items.armor.glyphs.Swiftness;
import com.qsr.customspd.items.weapon.enchantments.Blazing;
import com.qsr.customspd.items.weapon.enchantments.Chilling;
import com.qsr.customspd.items.weapon.enchantments.Lucky;
import com.qsr.customspd.items.weapon.melee.Shortsword;
import com.qsr.customspd.scenes.PixelScene;
import com.qsr.customspd.test.HeadlessGdx;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.utils.PlatformSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Port regression for upstream ed9230a95 (v3.0.0): the enchant and glyph windows keep their
 * item in a static so they can be restored. Opening one left the other's item in place, so
 * backing out of a later glyph window's cancel confirmation re-showed the old enchant window.
 */
class EnchantWindowStatePortTest {

	private PlatformSupport oldPlatform;
	private Camera oldUiCamera;

	@BeforeAll
	static void boot() {
		HeadlessGdx.boot();
	}

	@BeforeEach
	void setUp() {
		// Windows need a ui camera and a text splitter; with no font generator, text is
		// laid out without being measured.
		oldPlatform = Game.platform;
		oldUiCamera = PixelScene.uiCamera;
		PixelScene.uiCamera = new Camera(0, 0, 400, 400, 1);
		Game.platform = new PlatformSupport() {
			@Override public void updateDisplaySize() {}
			@Override public void updateSystemUI() {}
			@Override public boolean connectedToUnmeteredNetwork() { return false; }
			@Override public boolean supportsVibration() { return false; }
			@Override public void setupFontGenerators(int pageSize, boolean systemFont) {}
			@Override protected FreeTypeFontGenerator getGeneratorForString(String input) { return null; }
			@Override public String[] splitforTextBlock(String text, boolean multiline) { return text.split(" "); }
		};
	}

	@AfterEach
	void tearDown() throws Exception {
		// "Yes, cancel" clears every saved window field.
		new ScrollOfEnchantment.WndConfirmCancel().onSelect(0);
		Game.platform = oldPlatform;
		PixelScene.uiCamera = oldUiCamera;
	}

	@Test
	void backingOutOfAGlyphCancelDoesNotReopenAnOldEnchantWindow() throws Exception {
		new ScrollOfEnchantment.WndEnchantSelect(new Shortsword(), new Blazing(), new Chilling(), new Lucky());

		ClothArmor armor = new ClothArmor();
		new ScrollOfEnchantment.WndGlyphSelect(armor, new Stone(), new Swiftness(), new Obfuscation());

		// "No, go back" re-shows whichever window still has its item saved.
		new ScrollOfEnchantment.WndConfirmCancel().onSelect(1);

		assertNull(saved(ScrollOfEnchantment.WndEnchantSelect.class, "wep"),
				"the earlier weapon must not be kept once a glyph window opens");
		assertSame(armor, saved(ScrollOfEnchantment.WndGlyphSelect.class, "arm"),
				"going back returns to the glyph window for this armor");
	}

	@Test
	void openingAnEnchantWindowClearsAnOldGlyphArmor() throws Exception {
		new ScrollOfEnchantment.WndGlyphSelect(new ClothArmor(), new Stone(), new Swiftness(), new Obfuscation());
		new ScrollOfEnchantment.WndEnchantSelect(new Shortsword(), new Blazing(), new Chilling(), new Lucky());

		assertNull(saved(ScrollOfEnchantment.WndGlyphSelect.class, "arm"),
				"the earlier armor must not be kept once an enchant window opens");
	}

	private static Object saved(Class<?> wnd, String name) throws Exception {
		Field f = wnd.getDeclaredField(name);
		f.setAccessible(true);
		return f.get(null);
	}
}
