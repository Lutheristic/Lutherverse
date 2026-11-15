package com.qsr.customspd.sprites;

import com.qsr.customspd.assets.GeneralAsset;
import com.qsr.customspd.test.HeadlessRun;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Copying an ItemSprite into another keeps its glow (upstream a3f2ab7a8), so the journal's grid
 * cells and popups show enchantment and glyph glows.
 */
class ItemSpriteCopyGlowTest {

	private static Object glowing(ItemSprite sprite) throws ReflectiveOperationException {
		Field f = ItemSprite.class.getDeclaredField("glowing");
		f.setAccessible(true);
		return f.get(sprite);
	}

	@Test
	void copyKeepsTheGlow() throws Exception {
		try (HeadlessRun run = HeadlessRun.start(20240611L)) {
			ItemSprite.Glowing glow = new ItemSprite.Glowing(0xFF0000);
			ItemSprite src = new ItemSprite(GeneralAsset.WORN_SHORTSWORD, glow);
			ItemSprite dst = new ItemSprite();

			dst.copy(src);

			assertSame(glow, glowing(dst), "copied sprite keeps the source glow");
			assertSame(src.texture, dst.texture, "texture");
			assertEquals(src.frame().left, dst.frame().left, "frame left");
			assertEquals(src.frame().top, dst.frame().top, "frame top");
			assertEquals(src.frame().right, dst.frame().right, "frame right");
			assertEquals(src.frame().bottom, dst.frame().bottom, "frame bottom");
		}
	}

	@Test
	void copyOfAnUnglowingSpriteLeavesNoGlow() throws Exception {
		try (HeadlessRun run = HeadlessRun.start(20240611L)) {
			ItemSprite src = new ItemSprite(GeneralAsset.WORN_SHORTSWORD, null);
			ItemSprite dst = new ItemSprite();

			dst.copy(src);

			assertNull(glowing(dst));
		}
	}
}
