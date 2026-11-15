package com.qsr.customspd.assets;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upstream draws the catalog tab icon from icons.png (Icons.CATALOG, 80,48,13,16 at v2.5.4).
 * CPDU ships one PNG per icon, so the cut lives at interfaces/icons/catalog.png. A missing or
 * empty file only shows up when the journal opens, so pin the path, size and pixel count.
 */
class CatalogIconAssetTest {

	/** core:test runs with src/main/assets as its working directory (see HeadlessGdx). */
	private static final File ASSETS = new File(".");

	@Test
	void catalogIconIsTheThirteenBySixteenUpstreamCut() throws Exception {
		assertTrue(new File(ASSETS, GeneralAsset.STAR.getPath()).isFile(),
				"fixture sanity: working directory must be core/src/main/assets, was " + ASSETS.getAbsolutePath());

		GeneralAsset icon = GeneralAsset.valueOf("ICON_CATALOG");
		assertEquals("interfaces/icons/catalog.png", icon.getPath());

		File png = new File(ASSETS, icon.getPath());
		assertTrue(png.isFile(), "missing file " + icon.getPath());

		BufferedImage img = ImageIO.read(png);
		assertNotNull(img, icon.getPath() + " is not a readable image");
		assertEquals(13, img.getWidth(), "width");
		assertEquals(16, img.getHeight(), "height");

		int opaque = 0;
		for (int y = 0; y < img.getHeight(); y++) {
			for (int x = 0; x < img.getWidth(); x++) {
				if ((img.getRGB(x, y) >>> 24) != 0) opaque++;
			}
		}
		assertEquals(200, opaque, "pixels with alpha > 0");
	}
}
