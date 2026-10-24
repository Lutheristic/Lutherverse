package com.qsr.customspd.actors.buffs;

import com.qsr.customspd.assets.Asset;
import com.qsr.customspd.assets.GeneralAsset;
import com.qsr.customspd.ui.BuffIndicator;
import kotlin.Pair;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Wand and scroll empowerment get their own buff icon instead of sharing the upgrade arrow
 * (upstream df0d1ddeb). Upstream appends it to a sprite sheet; CPDU ships one PNG per icon, so the
 * new pair has to resolve to real 7px and 16px files or the first buff-bar refresh would crash.
 */
class WandBuffIconPortTest {

	/** core:test runs with src/main/assets as its working directory (see HeadlessGdx). */
	private static final File ASSETS = new File(".");

	@Test
	void wandAndScrollEmpowerUseTheWandIcon() {
		Pair<Asset, Asset> wand = BuffIndicator.WAND;
		assertEquals(wand, new WandEmpower().icon());
		assertEquals(wand, new ScrollEmpower().icon());
		assertNotEquals(BuffIndicator.UPGRADE, wand);
	}

	@Test
	void theWandIconFilesExistAtTheirSmallAndLargeSizes() throws Exception {
		assertTrue(new File(ASSETS, GeneralAsset.STAR.getPath()).isFile(),
				"fixture sanity: working directory must be core/src/main/assets, was " + ASSETS.getAbsolutePath());

		BufferedImage small = ImageIO.read(new File(ASSETS, BuffIndicator.WAND.getFirst().getPath()));
		BufferedImage large = ImageIO.read(new File(ASSETS, BuffIndicator.WAND.getSecond().getPath()));
		assertEquals(BuffIndicator.SIZE_SMALL, small.getWidth());
		assertEquals(BuffIndicator.SIZE_SMALL, small.getHeight());
		assertEquals(BuffIndicator.SIZE_LARGE, large.getWidth());
		assertEquals(BuffIndicator.SIZE_LARGE, large.getHeight());
	}
}
